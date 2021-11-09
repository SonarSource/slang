#! /usr/bin/env bash

readonly SCRIPT_DIRECTORY="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

readonly UNIFIED_AGENT_JAR="wss-unified-agent.jar"
readonly UNIFIED_AGENT_JAR_MD5_CHECKSUM="706694E349EA14CB04C4621B70D99A93" # MD5 hash for version 29.9.1.1
readonly UNIFIED_AGENT_JAR_URL="https://github.com/whitesource/unified-agent-distribution/releases/download/v21.9.1.1/wss-unified-agent.jar"

readonly MODULES=(
  "sonar-go-plugin"
)

readonly WHITESOURCE_SIGNATURE='Signed by "CN=whitesource software inc, O=whitesource software inc, STREET=79 Madison Ave, L=New York, ST=New York, OID.2.5.4.17=10016, C=US"'


check_unified_agent() {
  local path_to_jar="${1}"
  # Verify JAR checksum
  local checksum
  checksum="$(md5sum "${path_to_jar}" | cut --delimiter=" " --fields=1 | awk ' { print toupper($0) }')"
  if [[ "${checksum}" != "${UNIFIED_AGENT_JAR_MD5_CHECKSUM}" ]]; then
    echo "MD5 checksum mismatch." >&2
    echo "expected: ${UNIFIED_AGENT_JAR_MD5_CHECKSUM}" >&2
    echo "computed: ${checksum}" >&2
    exit 2
  fi

  # Verify JAR signature
  local path_to_verification_output="./jarsigner-output.txt"
  if ! jarsigner -verify -verbose "${path_to_jar}" > "${path_to_verification_output}" ; then
    echo "Could not verify jar signature" >&2
    exit 3
  fi
  if [[ $(grep --count "${WHITESOURCE_SIGNATURE}" "${path_to_verification_output}") -ne 1 ]]; then
    echo "Could not find signature line in verification output" >&2
    exit 4
  fi
}

get_unified_agent() {
  if [[ ! -f "${UNIFIED_AGENT_JAR}" ]]; then
    curl \
      --location \
      --remote-name \
      --remote-header-name \
      "${UNIFIED_AGENT_JAR_URL}"
  fi
  if [[ ! -f "${UNIFIED_AGENT_JAR}" ]]; then
    echo "Could not find downloaded Unified Agent" >&2
    exit 1
  fi
  check_unified_agent "${UNIFIED_AGENT_JAR}"
}

local_maven_expression() {
  mvn -q -Dexec.executable="echo" -Dexec.args="\${${1}}" --non-recursive org.codehaus.mojo:exec-maven-plugin:1.3.1:exec
}

get_gradle_property() {
  local module="${1}"
  local property="${2}"
  ./gradlew :"${module}":properties --quiet | grep --extended-regexp "^${property}:" | cut --delimiter=":" --fields=2 | tr --delete "[:space:]"
}

get_product_name() {
  local property="project.name"
  if [[ -f "pom.xml" ]]; then
    if command -v maven_expression >&2 2>/dev/null; then
      maven_expression "${property}"
    else
      local_maven_expression "${property}"
    fi
  fi
}

get_project_version() {
  local property="project.version"
  if command -v maven_expression >&2 2>/dev/null; then
    maven_expression "${property}"
  else
    local_maven_expression "${property}"
  fi
}

scan_gradle_module() {
  local module="${1}"
  WS_PRODUCTNAME=$(get_gradle_property "${module}" "name")
  export WS_PRODUCTNAME
  if [[ -z "${PROJECT_VERSION}" ]]; then
    PROJECT_VERSION=$(get_gradle_property "${module}" "version")
  fi
  export WS_PROJECTNAME="${WS_PRODUCTNAME} ${PROJECT_VERSION%-*}"
  pushd "${module}" || exit 1
  java -jar ../wss-unified-agent.jar -c whitesource.properties
  popd || exit 1
}

scan() {
  for module in "${MODULES[@]}"; do
    scan_gradle_module "${module}"
  done
}

get_unified_agent
scan
