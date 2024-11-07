#!/bin/bash

# Update dev application properties
APP_PROPS_FILE="$HOME/Projects/fremontmi/src/main/resources/application.properties"
DEV_PROPS_FILE="$HOME/Projects/config/dev/application.properties"
cp "$APP_PROPS_FILE" "$DEV_PROPS_FILE"

# Swap application properties to prod
PROD_PROPS_FILE="$HOME/Projects/config/prod/application.properties"
cp "$PROD_PROPS_FILE" "$APP_PROPS_FILE"

# Remove snapshot portion of the version
POM_FILE="pom.xml"
sed -i.bak 's/<version>\(.*\)-SNAPSHOT<\/version>/<version>\1<\/version>/' "$POM_FILE"
rm "$POM_FILE.bak"

# Capture the pre-incremented version
CURRENT_VERSION=$(awk '/<artifactId>fremontmi<\/artifactId>/ {getline; print}' "$POM_FILE" | sed -E 's/.*<version>([^<]+)<\/version>.*/\1/')

# Explicitly check and update EC2 user data with CURRENT_VERSION
USER_DATA_SCRIPT="ec2-user-data.sh"
sed -i.bak "s/^JAR_VERSION=\".*\"/JAR_VERSION=\"${CURRENT_VERSION}\"/" "$USER_DATA_SCRIPT"
rm "$USER_DATA_SCRIPT.bak"

# Build the package
# Set up environment variables
JAVA_HOME="/Users/paulpladziewicz/Library/Java/JavaVirtualMachines/corretto-21.0.3/Contents/Home"
MAVEN_HOME="/Users/paulpladziewicz/.m2/wrapper/dists/apache-maven-3.9.5-bin/2adeog8mj13csp1uusqnc1f2mo/apache-maven-3.9.5"
export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"
mvn clean package

# Put application dev application properties back
cp "$DEV_PROPS_FILE" "$APP_PROPS_FILE"

# Increment version and re-add SNAPSHOT
IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_VERSION"
MAJOR_INCREMENT=0
MINOR_INCREMENT=0
PATCH_INCREMENT=1
MAJOR=$((MAJOR + MAJOR_INCREMENT))
MINOR=$((MINOR + MINOR_INCREMENT))
PATCH=$((PATCH + PATCH_INCREMENT))
NEW_VERSION="${MAJOR}.${MINOR}.${PATCH}-SNAPSHOT"
sed -i.bak "/<artifactId>fremontmi<\/artifactId>/,/<\/version>/ s/<version>.*<\/version>/<version>${NEW_VERSION}<\/version>/" "$POM_FILE"
rm "$POM_FILE.bak"

# Git commit
#git add "$POM_FILE" "$USER_DATA_SCRIPT"
#git commit -m "Incrementing to $NEW_VERSION and updating EC2 user data script"
