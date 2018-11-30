#!/bin/bash
set -euo pipefail

if [ "${TRAVIS_REPO_SLUG}" == "SonarSource/slang" ];then
  echo "Building slang"
  
  export INITIAL_VERSION=$(cat gradle.properties | grep version | awk -F= '{print $2}')
  
  ./gradlew --no-daemon --console plain \
    -DbuildNumber=$TRAVIS_BUILD_NUMBER \
    build sonarqube \
    -Dsonar.host.url=$SONAR_HOST_URL \
    -Dsonar.login=$SONAR_TOKEN \
    -Dsonar.projectVersion=$INITIAL_VERSION \
    -Dsonar.analysis.buildNumber=$TRAVIS_BUILD_NUMBER \
    -Dsonar.analysis.pipeline=$TRAVIS_BUILD_NUMBER \
    -Dsonar.analysis.sha1=$TRAVIS_COMMIT \
    -Dsonar.analysis.repository=$TRAVIS_REPO_SLUG \
    -Dsonar.organization=sonarsource

else
  echo "Building slang-enterprise"
  ./private/private-travis.sh
fi

