#!/bin/bash

# Define paths
APP_PROPS_FILE="$HOME/Projects/fremontmi/src/main/resources/application.properties"
DEV_PROPS_FILE="$HOME/Projects/westmichigansoftware/config/dev/application.properties"
PROD_PROPS_FILE="$HOME/Projects/westmichigansoftware/config/prod/application.properties"
POM_FILE="pom.xml"
USER_DATA_SCRIPT="ec2-user-data.sh"
S3_BUCKET="s3://fremontmi/deployment"
LATEST_JAR_NAME="fremontmi-latest.jar"
LAUNCH_TEMPLATE_NAME="fremontmi"
TARGET_GROUP_NAME="uat-fremontmi"
ASG_NAME="FremontMI"
SUBNETS=("subnet-7dca8431" "subnet-2fc76f44" "subnet-f5c22488")  # Subnets for UAT
SELECTED_SUBNET=${SUBNETS[$((RANDOM % ${#SUBNETS[@]}))]}  # Randomly select a subnet

# Backup dev properties and swap to prod
cp "$APP_PROPS_FILE" "$DEV_PROPS_FILE"
cp "$PROD_PROPS_FILE" "$APP_PROPS_FILE"

# Remove snapshot portion of the version in POM
sed -i.bak 's/<version>\(.*\)-SNAPSHOT<\/version>/<version>\1<\/version>/' "$POM_FILE"
rm "$POM_FILE.bak"

# Capture pre-incremented version
CURRENT_VERSION=$(awk '/<artifactId>fremontmi<\/artifactId>/ {getline; print}' "$POM_FILE" | sed -E 's/.*<version>([^<]+)<\/version>.*/\1/')

# Build the package
JAVA_HOME="/Users/paulpladziewicz/Library/Java/JavaVirtualMachines/corretto-21.0.3/Contents/Home"
MAVEN_HOME="/Users/paulpladziewicz/.m2/wrapper/dists/apache-maven-3.9.5-bin/2adeog8mj13csp1uusqnc1f2mo/apache-maven-3.9.5"
export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"
mvn clean package

# Upload the latest JAR to S3
LATEST_JAR="target/fremontmi-${CURRENT_VERSION}.jar"
aws s3 cp "$LATEST_JAR" "$S3_BUCKET/fremontmi-${CURRENT_VERSION}.jar"

echo "Uploaded ${LATEST_JAR} to S3 as ${LATEST_JAR_NAME} and fremontmi-${CURRENT_VERSION}.jar"

# Update EC2 user data with the latest version
sed -i.bak "s/JAR_VERSION=\".*\"/JAR_VERSION=\"${CURRENT_VERSION}\"/" "$USER_DATA_SCRIPT"
rm "$USER_DATA_SCRIPT.bak"

echo "Updated EC2 user data script with version ${CURRENT_VERSION}"

# Restore dev properties
cp "$DEV_PROPS_FILE" "$APP_PROPS_FILE"

# Increment version and re-add SNAPSHOT
IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_VERSION"
PATCH=$((PATCH + 1))
NEW_VERSION="${MAJOR}.${MINOR}.${PATCH}-SNAPSHOT"
sed -i.bak "/<artifactId>fremontmi<\/artifactId>/,/<\/version>/ s/<version>.*<\/version>/<version>${NEW_VERSION}<\/version>/" "$POM_FILE"
rm "$POM_FILE.bak"

# Git commit changes
git add "$POM_FILE" "$USER_DATA_SCRIPT"
git commit -m "Incrementing to $NEW_VERSION, updated EC2 user data, uploaded JAR to S3"

# Encode user data script as Base64
BASE64_USER_DATA=$(base64 -i "$USER_DATA_SCRIPT")

# Get the latest launch template version
LATEST_VERSION=$(aws ec2 describe-launch-templates \
    --launch-template-names "$LAUNCH_TEMPLATE_NAME" \
    --query "LaunchTemplates[0].LatestVersionNumber" \
    --output text)

# Create a new launch template version
NEW_LAUNCH_TEMPLATE_VERSION=$(aws ec2 create-launch-template-version \
    --launch-template-name "$LAUNCH_TEMPLATE_NAME" \
    --source-version "$LATEST_VERSION" \
    --version-description "Updated for version ${CURRENT_VERSION}" \
    --launch-template-data "{
        \"UserData\": \"$BASE64_USER_DATA\"
    }" \
    --query 'LaunchTemplateVersion.VersionNumber' \
    --output text)

# Set the new version as default
if [ -n "$NEW_LAUNCH_TEMPLATE_VERSION" ]; then
    aws ec2 modify-launch-template \
        --launch-template-name "$LAUNCH_TEMPLATE_NAME" \
        --default-version "$NEW_LAUNCH_TEMPLATE_VERSION"
    echo "Set default version to $NEW_LAUNCH_TEMPLATE_VERSION"
else
    echo "Failed to set new version"
fi

echo "New launch template version $NEW_LAUNCH_TEMPLATE_VERSION set as default for $LAUNCH_TEMPLATE_NAME."

echo "Launching instance in subnet: $SELECTED_SUBNET"

# Step 1: Launch EC2 instance from the latest launch template with subnet override
INSTANCE_ID=$(aws ec2 run-instances \
    --launch-template "LaunchTemplateName=$LAUNCH_TEMPLATE_NAME" \
    --network-interfaces "DeviceIndex=0,SubnetId=$SELECTED_SUBNET" \
    --query 'Instances[0].InstanceId' \
    --output text)

if [ -z "$INSTANCE_ID" ]; then
    echo "Failed to launch instance."
    exit 1
fi

echo "Launched EC2 instance: $INSTANCE_ID"

# Step 2: Wait for the instance to reach the 'running' state
echo "Waiting for instance $INSTANCE_ID to reach 'running' state..."
aws ec2 wait instance-running --instance-ids "$INSTANCE_ID"
echo "Instance $INSTANCE_ID is now in 'running' state."

# Step 3: Register the instance with the UAT target group
TARGET_GROUP_ARN=$(aws elbv2 describe-target-groups --names "$TARGET_GROUP_NAME" --query 'TargetGroups[0].TargetGroupArn' --output text)

aws elbv2 register-targets \
    --target-group-arn "$TARGET_GROUP_ARN" \
    --targets Id="$INSTANCE_ID"

echo "Registered instance $INSTANCE_ID with target group $TARGET_GROUP_NAME"

# Step 4: Confirm instance is ready for prod
read -p "Is the instance ready for production? (y/n): " CONFIRM

# Step 5: Deregister the instance and terminate based on confirmation
if [[ "$CONFIRM" == "y" ]]; then
    echo "Preparing to refresh ASG $ASG_NAME..."
    aws ec2 terminate-instances --instance-ids "$INSTANCE_ID"

    aws elbv2 deregister-targets \
        --target-group-arn "$TARGET_GROUP_ARN" \
        --targets Id="$INSTANCE_ID"

    echo "Instance terminated and deregistered from UAT. Initiating ASG refresh..."
    aws autoscaling start-instance-refresh --auto-scaling-group-name "$ASG_NAME"

else
    echo "Terminating UAT instance without refreshing ASG..."
    aws ec2 terminate-instances --instance-ids "$INSTANCE_ID"

    aws elbv2 deregister-targets \
        --target-group-arn "$TARGET_GROUP_ARN" \
        --targets Id="$INSTANCE_ID"

    echo "UAT instance terminated and deregistered."
fi