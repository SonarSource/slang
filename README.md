# SLang

[![Build Status](https://api.cirrus-ci.com/github/SonarSource/slang.svg?branch=master)](https://cirrus-ci.com/github/SonarSource/slang)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=org.sonarsource.slang%3Aslang&metric=alert_status)](https://sonarcloud.io/dashboard?id=org.sonarsource.slang%3Aslang)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=org.sonarsource.slang%3Aslang&metric=coverage)](https://sonarcloud.io/component_measures/domain/Coverage?id=org.sonarsource.slang%3Aslang)

SLang (SonarSource Language) is a framework to quickly develop code analyzers for SonarQube to help developers write [Clean Code](https://www.sonarsource.com/solutions/clean-code/?utm_medium=referral&utm_source=github&utm_campaign=clean-code&utm_content=slang).

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
Furthermore, there are files such as `package-info.java` and `module-info.java` that spotless ignores. For those files use a manual script like below to update the license. E.g., for Go files (on Mac):

    `find . -type f -name "*.go" -exec sed -i '' 's/2018-2023/2018-2024/' "{}" \;`
