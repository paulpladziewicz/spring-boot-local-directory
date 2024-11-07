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

# Capture the pre-incremented version without -SNAPSHOT
CURRENT_VERSION=$(awk '/<artifactId>fremontmi<\/artifactId>/ {getline; print}' "$POM_FILE" | sed -E 's/.*<version>([^<]+)<\/version>.*/\1/')

# Update the EC2 user data script with the pre-incremented version
USER_DATA_SCRIPT="ec2-user-data.sh"
sed -i.bak "s/fremontmi-[0-9]*\.[0-9]*\.[0-9]*\.jar/fremontmi-${CURRENT_VERSION}.jar/" "$USER_DATA_SCRIPT"
rm "$USER_DATA_SCRIPT.bak"

# Build the package
mvn clean package -Pprod

# Put application dev application properties back
cp "$DEV_PROPS_FILE" "$APP_PROPS_FILE"

# Update the package version and add snapshot back
POM_FILE="pom.xml"
IFS='.' read -r MAJOR MINOR PATCH <<< "${CURRENT_VERSION%%-SNAPSHOT}"
MAJOR_INCREMENT=0
MINOR_INCREMENT=0
PATCH_INCREMENT=1
MAJOR=$((MAJOR + MAJOR_INCREMENT))
MINOR=$((MINOR + MINOR_INCREMENT))
PATCH=$((PATCH + PATCH_INCREMENT))
if (( MAJOR_INCREMENT > 0 )); then
  MINOR=0
  PATCH=0
elif (( MINOR_INCREMENT > 0 )); then
  PATCH=0
fi
NEW_VERSION="${MAJOR}.${MINOR}.${PATCH}-SNAPSHOT"
sed -i.bak "/<artifactId>fremontmi<\/artifactId>/,/<\/version>/ s/<version>.*<\/version>/<version>${NEW_VERSION}<\/version>/" "$POM_FILE"
rm "$POM_FILE.bak"

# Update ec2-user-data
USER_DATA_SCRIPT="ec2-user-data.sh"
CURRENT_JAR_VERSION=$(awk '/<artifactId>fremontmi<\/artifactId>/ {getline; print}' "$POM_FILE" | sed -E 's/.*<version>([^<]+)<\/version>.*/\1/')
sed -i.bak "s/fremontmi-[0-9]*\.[0-9]*\.[0-9]*\.jar/fremontmi-${CURRENT_JAR_VERSION}.jar/" "$USER_DATA_SCRIPT"
rm "$USER_DATA_SCRIPT.bak"

# Git commit
#git add "$POM_FILE"
#git commit -m "Incrementing to $NEW_VERSION"


