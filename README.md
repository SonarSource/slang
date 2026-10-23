# SLang

[![CI](https://github.com/SonarQubeCommunity/slang/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/SonarQubeCommunity/slang/actions/workflows/ci.yml)

SLang (Sonar Language) is a framework to quickly develop code analyzers for SonarQube to help developers write projects with integrated code quality and security.

SLang defines language-agnostic AST. Using this AST we can develop simple syntax-based rules. Then we use a parser for real language to create this AST.

## Community Maintenance

SLang was developed by SonarSource and is now maintained by the community in the [SonarQubeCommunity](https://github.com/SonarQubeCommunity) organization. It is the framework behind the community [Ruby](https://github.com/SonarQubeCommunity/sonar-ruby) and [Scala](https://github.com/SonarQubeCommunity/sonar-scala) analyzers.

## Feedback and Contributions

Report bugs and request features in [GitHub issues](https://github.com/SonarQubeCommunity/slang/issues). Contributions are welcome: see [CONTRIBUTING.md](CONTRIBUTING.md).

## Building

You will need Java 21 installed.

### Initialize submodules

    git submodule update --init --recursive

### Build
Build and run Unit Tests:

    ./gradlew build


## Releasing

Releases are published from GitHub:

1. Create and publish a GitHub release with a tag such as `v1.24.0`.
2. The [Release](.github/workflows/release.yml) workflow builds and tests the project with the version from the tag, and attaches the artifacts to the release as a Maven repository (`slang-<version>-maven-repository.zip`, with POMs and checksums), together with the module JARs.
3. Set `version` in `gradle.properties` to the next development version, such as `1.24.1-SNAPSHOT`.

## License headers

License headers are automatically updated by the spotless plugin but only for Java files. 
Furthermore, there are files such as `package-info.java` and `module-info.java` that spotless ignores. For those files use a manual script like below to update the license. E.g. on Mac:

    `find . -type f -name "*-info.java" -exec sed -i '' 's/2018-2024/2018-2025/' "{}" \;`

## License

Copyright 2018-2026 SonarSource

SonarQube analyzers released after November 29, 2024, including patch fixes for prior versions, are published under the [Sonar Source-Available License Version 1 (SSALv1)](LICENSE.txt).

See individual files for details that specify the license applicable to each file.
Files subject to the SSALv1 will be noted in their headers.
