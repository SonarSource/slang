@Library('SonarSource@2.0') _

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
    MAVEN_TOOL = 'Maven 3.3.x'
  }
  stages {
    stage('Notify') {
      steps {
        sendAllNotificationQaStarted()
      }
    }
    stage('QA') {
      parallel {
        stage('ruling-latest') {
          agent {
            label 'linux'
          }
          steps {
            runRuling "LATEST_RELEASE"
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

        stage('plugin-dev') {
          agent {
            label 'linux'
          }
          steps {
            runPlugin "DEV"
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

        stage('ci-windows') {
          agent {
            label 'windows'
          }
          steps {
             withQAEnv {
                withMaven(maven: MAVEN_TOOL) {
                  mavenSetBuildVersion()
                    sh "${mvnCommand()} clean verify"
                }
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
        repoxPromoteBuild()
      }
      post {
        always {
          sendAllNotificationPromote()
        }
      }
    }
  }
}

def mvnCommand() {
  return isUnix() ? 'mvn' : 'mvn.cmd'
}

def runRuling(String sqRuntimeVersion) {
  withQAEnv {
    withMaven(maven: MAVEN_TOOL) {
      mavenSetBuildVersion()
      dir('its') {
        sh 'git submodule update --init --recursive'
        sh "${mvnCommand()} -pl ruling ${itBuildArguments sqRuntimeVersion}"
      }
    }
  }
}

def runPlugin(String sqRuntimeVersion) {
  withQAEnv {
    withMaven(maven: MAVEN_TOOL) {
      mavenSetBuildVersion()
      dir('its') {
        sh "${mvnCommand()} -pl plugin ${itBuildArguments sqRuntimeVersion}"
      }
    }
  }
}

def withQAEnv(def body) {
  withCredentials([string(credentialsId: 'ARTIFACTORY_PRIVATE_API_KEY', variable: 'ARTIFACTORY_PRIVATE_API_KEY')]) {
    body.call()
  }
}

String itBuildArguments(String sqRuntimeVersion) {
  "-Dsonar.runtimeVersion=${sqRuntimeVersion} -Dorchestrator.artifactory.apiKey=${env.ARTIFACTORY_PRIVATE_API_KEY} " +
     "-Dorchestrator.configUrl=https://repox.sonarsource.com/orchestrator.properties/orch-h2.properties -Dmaven.test.redirectTestOutputToFile=false clean verify -e -V"
}

