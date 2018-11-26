@Library('SonarSource@2.1.1') _

pipeline {
  agent none
  parameters {
    string(name: 'GIT_SHA1', description: 'Git SHA1 (provided by travisci hook job)')
    string(name: 'CI_BUILD_NAME', defaultValue: 'sonar-css', description: 'Build Name (provided by travisci hook job)')
    string(name: 'CI_BUILD_NUMBER', description: 'Build Number (provided by travisci hook job)')
    string(name: 'GITHUB_BRANCH', defaultValue: 'master', description: 'Git branch (provided by travisci hook job)')
    string(name: 'GITHUB_REPOSITORY_OWNER', defaultValue: 'SonarSource', description: 'Github repository owner(provided by travisci hook job)')
  }
  environment {
    SONARSOURCE_QA = 'true'
  }
  stages {
    stage('Notify') {
      steps {
        sendAllNotificationQaStarted()
      }
    }
    stage('QA') {
      parallel {
        stage('ruling-latest-kotlin') {
          agent {
            label 'linux'
          }
          steps {
            runRuling "LATEST_RELEASE", 'ruling-kotlin'
          }
        }

        stage('ruling-latest-ruby') {
          agent {
            label 'linux'
          }
          steps {
            runRuling "LATEST_RELEASE", 'ruling-ruby'
          }
        }

        stage('ruling-latest-scala') {
          agent {
            label 'linux'
          }
          steps {
            runRuling "LATEST_RELEASE", 'ruling-scala'
          }
        }

        stage('private-ruling-latest-apex') {
          agent {
            label 'linux'
          }
          steps {
            runPrivateRuling "LATEST_RELEASE", 'ruling-apex'
          }
        }

        stage('plugin-lts') {
          agent {
            label 'linux'
          }
          steps {
            runPlugin "LATEST_RELEASE[6.7]"
          }
        }

        stage('private-plugin-lts') {
          agent {
            label 'linux'
          }
          steps {
            runPrivatePlugin "LATEST_RELEASE[6.7]"
          }
        }

        stage('plugin-dev') {
          agent {
            label 'linux'
          }
          steps {
            runPlugin "DEV"
          }
        }

        stage('private-plugin-dev') {
          agent {
            label 'linux'
          }
          steps {
            runPrivatePlugin "DEV"
          }
        }

        stage('plugin-latest-windows') {
          agent {
            label 'windows'
          }
          steps {
            runPlugin "LATEST_RELEASE"
          }
        }

        stage('private-plugin-latest-windows') {
          agent {
            label 'windows'
          }
          steps {
            runPrivatePlugin "LATEST_RELEASE"
          }
        }

        stage('ci-windows') {
          agent {
            label 'windows'
          }
          steps {
             withQAEnv {
               gradle "build -Pqa --info --console plain --no-daemon --build-cache"
             }
          }
        }
      }
      post {
        always {
          sendAllNotificationQaResult()
        }
      }
    }
    stage('Promote') {
      steps {
        repoxPromoteSonarEnterprise()
      }
      post {
        always {
          sendAllNotificationPromote()
        }
      }
    }
  }
}

def gradle(String args) {
  if (isUnix()) {
    sh "./gradlew ${args}"
  } else {
    bat "./gradlew.bat ${args}"
  }
}

def runRuling(String sqRuntimeVersion, String rulingName) {
  withQAEnv {
    sh 'git submodule update --init its/sources'
    gradle ":its:ruling:test -P${rulingName} ${itBuildArguments sqRuntimeVersion}"
  }
}

def runPrivateRuling(String sqRuntimeVersion, String rulingName) {
  withQAEnv {
    sh 'git submodule update --init private/its/sources'
    gradle ":private:its:ruling:test -P${rulingName} ${itBuildArguments sqRuntimeVersion}"
  }
}

def runPlugin(String sqRuntimeVersion) {
  withQAEnv {
    gradle ":its:plugin:test -Pplugin ${itBuildArguments sqRuntimeVersion}"
  }
}

def runPrivatePlugin(String sqRuntimeVersion) {
  withQAEnv {
    sh 'git submodule update --init private/its/sources'
    gradle ":private:its:plugin:test -Pplugin ${itBuildArguments sqRuntimeVersion}"
  }
}

def withQAEnv(def body) {
  withCredentials([string(credentialsId: 'ARTIFACTORY_PRIVATE_API_KEY', variable: 'ARTIFACTORY_PRIVATE_API_KEY'),usernamePassword(credentialsId: 'ARTIFACTORY_PRIVATE_USER', passwordVariable: 'ARTIFACTORY_PRIVATE_PASSWORD', usernameVariable: 'ARTIFACTORY_PRIVATE_USERNAME')]) {
    try {
      body.call()
    } catch (e) {
      uploadToCixLogs("slang")
      throw e
    }
  }
}

String itBuildArguments(String sqRuntimeVersion) {
  "-Dsonar.runtimeVersion=${sqRuntimeVersion} -Dorchestrator.artifactory.apiKey=${env.ARTIFACTORY_PRIVATE_API_KEY} " +
     "-Dorchestrator.configUrl=https://repox.sonarsource.com/orchestrator.properties/orch-h2.properties " +
     "-DbuildNumber=$CI_BUILD_NUMBER -Pqa --info --console plain --no-daemon --build-cache"
}
