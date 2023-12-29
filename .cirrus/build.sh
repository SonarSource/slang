#!/bin/bash
set -euo pipefail

echo "Building slang"
: "${SONAR_HOST_URL?}" "${SONAR_TOKEN?}"

export GIT_SHA1=${CIRRUS_CHANGE_IN_REPO?}
export GITHUB_BASE_BRANCH=${CIRRUS_BASE_BRANCH:-}
export GITHUB_REPO=${CIRRUS_REPO_FULL_NAME?}
export BUILD_NUMBER=${CI_BUILD_NUMBER?}
export PIPELINE_ID=${CIRRUS_BUILD_ID?}

INITIAL_VERSION=$(grep version gradle.properties | awk -F= '{print $2}')

git fetch --unshallow || true
if [ -n "${GITHUB_BASE_BRANCH}" ]; then
  git fetch origin "${GITHUB_BASE_BRANCH}"
fi

./gradlew build sonar \
  -DbuildNumber="$BUILD_NUMBER" \
  -Dsonar.host.url="$SONAR_HOST_URL" \
  -Dsonar.token="$SONAR_TOKEN" \
  -Dsonar.projectVersion="$INITIAL_VERSION" \
  -Dsonar.analysis.buildNumber="$BUILD_NUMBER" \
  -Dsonar.analysis.pipeline="$PIPELINE_ID" \
  -Dsonar.analysis.sha1="$GIT_SHA1" \
  -Dsonar.analysis.repository="$GITHUB_REPO" \
  -Dsonar.organization=sonarsource \
  --no-daemon --console plain
