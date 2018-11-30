#!/bin/bash
set -euo pipefail

function configureTravis {
  mkdir -p ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v52 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}

configureTravis

export DEPLOY_PULL_REQUEST=true

export PARAMS=""
if [ "${TRAVIS_REPO_SLUG}" == "SonarSource/slang" ];then
  echo "Building slang"
  PARAMS=" -Dsonar.organization=sonarsource "
else
  echo "Building slang-enterprise"
fi

regular_gradle_build_deploy_analyze $PARAMS
