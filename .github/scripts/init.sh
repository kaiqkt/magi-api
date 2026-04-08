#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

cd "$PROJECT_ROOT"

if [ -z "$1" ]; then
  exit 1
fi

RAW_NAME="$1"
ARTIFACT_ID=$(echo "$RAW_NAME" | sed 's/[-_]//g')
BASE_PACKAGE="com.kaiqkt.$ARTIFACT_ID"

if [[ "$OSTYPE" == "darwin"* ]]; then
  SED_INPLACE=("sed" "-i" "")
else
  SED_INPLACE=("sed" "-i")
fi

cat <<EOF > settings.gradle.kts
rootProject.name = "$RAW_NAME"
EOF

mkdir -p src/main/resources

cat <<EOF > src/main/resources/application.yml
---
server:
  port: 8080

spring:
  application:
    name: $RAW_NAME

management:
  endpoints:
    web:
      exposure:
        include: health, prometheus
      base-path: /
  metrics:
    tags:
      application: $RAW_NAME
EOF

OLD_PACKAGE="com.kaiqkt.template"
NEW_PACKAGE="$BASE_PACKAGE"

OLD_PACKAGE_PATH=$(echo "$OLD_PACKAGE" | tr '.' '/')
NEW_PACKAGE_PATH=$(echo "$NEW_PACKAGE" | tr '.' '/')

mkdir -p "src/main/kotlin/$NEW_PACKAGE_PATH"
mkdir -p "src/test/kotlin/$NEW_PACKAGE_PATH"

if [ -d "src/main/kotlin/$OLD_PACKAGE_PATH" ]; then
  mv src/main/kotlin/$OLD_PACKAGE_PATH/* src/main/kotlin/$NEW_PACKAGE_PATH/
  rm -rf src/main/kotlin/com/kaiqkt/template
fi

if [ -d "src/test/kotlin/$OLD_PACKAGE_PATH" ]; then
  mv src/test/kotlin/$OLD_PACKAGE_PATH/* src/test/kotlin/$NEW_PACKAGE_PATH/
  rm -rf src/test/kotlin/com/kaiqkt/template
fi

find . -type f \
  ! -path "./.git/*" \
  ! -path "./build/*" \
  ! -path "./.gradle/*" \
  -exec "${SED_INPLACE[@]}" "s|\${package}|$BASE_PACKAGE|g" {} +

find . -type f \
  ! -path "./.git/*" \
  ! -path "./build/*" \
  ! -path "./.gradle/*" \
  -exec "${SED_INPLACE[@]}" "s|\${artifactId}|$ARTIFACT_ID|g" {} +

chmod +x ./gradlew
./gradlew ktlintFormat
