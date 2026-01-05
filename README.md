# SLang

[![Build](https://github.com/SonarSource/slang-enterprise/actions/workflows/build.yml/badge.svg?branch=master)](https://github.com/SonarSource/slang-enterprise/actions/workflows/build.yml)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=org.sonarsource.slang%3Aslang&metric=alert_status)](https://sonarcloud.io/dashboard?id=org.sonarsource.slang%3Aslang)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=org.sonarsource.slang%3Aslang&metric=coverage)](https://sonarcloud.io/component_measures/domain/Coverage?id=org.sonarsource.slang%3Aslang)

SLang (Sonar Language) is a framework to quickly develop code analyzers for SonarQube to help developers write projects with integrated code quality and security.

SLang defines language-agnostic AST. Using this AST we can develop simple syntax-based rules. Then we use a parser for real language to create this AST.

## Have questions or feedback?

To provide feedback (request a feature, report a bug, etc.) use the [SonarQube Community Forum](https://community.sonarsource.com/). Please do not forget to specify the language, plugin version, and SonarQube version.

## Building

### Setup

Install Java 17.

### Build
Build and run Unit Tests:

    ./gradlew build


## License headers

License headers are automatically updated by the spotless plugin but only for Java files. 
Furthermore, there are files such as `package-info.java` and `module-info.java` that spotless ignores. For those files use a manual script like below to update the license. E.g. on Mac:

    `find . -type f -name "*-info.java" -exec sed -i '' 's/2018-2024/2018-2025/' "{}" \;`

## License

Copyright 2018-2026 SonarSource

SonarQube analyzers released after November 29, 2024, including patch fixes for prior versions, are published under the [Sonar Source-Available License Version 1 (SSALv1)](LICENSE).

See individual files for details that specify the license applicable to each file.
Files subject to the SSALv1 will be noted in their headers.
