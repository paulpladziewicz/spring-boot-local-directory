#!/bin/bash

# Define paths
APP_PROPS_FILE="$HOME/Projects/fremontmi/src/main/resources/application.properties"
DEV_PROPS_FILE="$HOME/Projects/config/dev/application.properties"
PROD_PROPS_FILE="$HOME/Projects/config/prod/application.properties"
POM_FILE="pom.xml"
USER_DATA_SCRIPT="ec2-user-data.sh"
S3_BUCKET="s3://fremontmi/deployment"
LATEST_JAR_NAME="fremontmi-latest.jar"

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

# Update EC2 user data with the latest version
sed -i.bak "s/JAR_VERSION=\".*\"/JAR_VERSION=\"${CURRENT_VERSION}\"/" "$USER_DATA_SCRIPT"
rm "$USER_DATA_SCRIPT.bak"

# Restore dev properties
cp "$DEV_PROPS_FILE" "$APP_PROPS_FILE"

# Increment version and re-add SNAPSHOT
IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_VERSION"
PATCH=$((PATCH + 1))
NEW_VERSION="${MAJOR}.${MINOR}.${PATCH}-SNAPSHOT"
sed -i.bak "/<artifactId>fremontmi<\/artifactId>/,/<\/version>/ s/<version>.*<\/version>/<version>${NEW_VERSION}<\/version>/" "$POM_FILE"
rm "$POM_FILE.bak"

# Git commit changes
#git add "$POM_FILE" "$USER_DATA_SCRIPT"
#git commit -m "Incrementing to $NEW_VERSION, updated EC2 user data, uploaded JAR to S3"

# Confirm upload and EC2 update
echo "Uploaded ${LATEST_JAR} to S3 as ${LATEST_JAR_NAME} and fremontmi-${CURRENT_VERSION}.jar"
echo "Updated EC2 user data script with version ${CURRENT_VERSION}"
