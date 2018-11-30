#!/bin/bash
set -euo pipefail

if [ "${TRAVIS_REPO_SLUG}" == "SonarSource/slang" ];then
  echo "Building slang"
  
  ./gradlew --no-daemon --console plain \
    -DbuildNumber=$BUILD_NUMBER \
    build sonarqube \
    -Dsonar.host.url=$SONAR_HOST_URL \
    -Dsonar.login=$SONAR_TOKEN \
    -Dsonar.projectVersion=$INITIAL_VERSION \
    -Dsonar.analysis.buildNumber=$BUILD_NUMBER \
    -Dsonar.analysis.pipeline=$BUILD_NUMBER \
    -Dsonar.analysis.sha1=$GIT_COMMIT \
    -Dsonar.analysis.repository=$TRAVIS_REPO_SLUG \
    -Dsonar.organization=sonarsource

else
  echo "Building slang-enterprise"
  ./private/private-travis.sh
fi

