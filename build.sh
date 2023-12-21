#!/bin/bash
set -euo pipefail

echo "Building slang"
: "${CIRRUS_BUILD_ID?}" "${CIRRUS_REPO_FULL_NAME?}" "${SONAR_HOST_URL?}" "${SONAR_TOKEN?}"

INITIAL_VERSION=$(grep version gradle.properties | awk -F= '{print $2}')

./gradlew --no-daemon --console plain \
  -DbuildNumber="$CIRRUS_BUILD_ID" \
  build sonarqube \
  -Dsonar.host.url="$SONAR_HOST_URL" \
  -Dsonar.login="$SONAR_TOKEN" \
  -Dsonar.projectVersion="$INITIAL_VERSION" \
  -Dsonar.analysis.buildNumber="$CIRRUS_BUILD_ID" \
  -Dsonar.analysis.pipeline="$CIRRUS_BUILD_ID" \
  -Dsonar.analysis.sha1="${CIRRUS_BASE_SHA:-}" \
  -Dsonar.analysis.repository="$CIRRUS_REPO_FULL_NAME" \
  -Dsonar.organization=sonarsource
