# SLang

[![Build Status](https://travis-ci.org/SonarSource/slang.svg?branch=master)](https://travis-ci.org/SonarSource/slang)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=org.sonarsource.slang%3Aslang&metric=alert_status)](https://sonarcloud.io/dashboard?id=org.sonarsource.slang%3Aslang) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=org.sonarsource.slang%3Aslang&metric=coverage)](https://sonarcloud.io/component_measures/domain/Coverage?id=org.sonarsource.slang%3Aslang)

This is a developer documentation. If you want to analyze source code in SonarQube read one of the following documentations:

* Kotlin language: [SonarKotlin documentation](https://docs.sonarqube.org/display/PLUG/SonarKotlin)
* Ruby language: [SonarRuby documentation](https://docs.sonarqube.org/display/PLUG/SonarRuby)
* Scala language: [SonarScala documentation](https://docs.sonarqube.org/display/PLUG/SonarRuby)

SLang (SonarSource Language) is a framework to quickly develop code analyzers for SonarQube. SLang defines language agnostic AST. Using this AST
we can develop simple syntax based rules. Then we use parser for real language to create this AST. Currently Kotlin, Ruby and Scala 
analyzers use this approach.

## Kotlin

We use [embeddable library](https://search.maven.org/artifact/org.jetbrains.kotlin/kotlin-compiler-embeddable/1.2.61/jar) of Kotlin compiler to create AST and [visitor](sonar-kotlin-plugin/src/main/java/org/sonarsource/kotlin/converter/KotlinTreeVisitor.java) to create SLang AST.

## Ruby

We use [whitequark parser](https://github.com/whitequark/parser) to parse Ruby language by embedding it using JRuby runtime.

* AST documentation for the parser can be found [here](https://github.com/whitequark/parser/blob/master/doc/AST_FORMAT.md)
* We use simple [Ruby script](sonar-ruby-plugin/src/main/resources/whitequark_parser_init.rb) to call the parser and invoke our [visitor](sonar-ruby-plugin/src/main/java/org/sonarsource/ruby/converter/RubyVisitor.java) written in Java 

## Scala

We use [Scalameta](https://scalameta.org/) to parse Scala language.

## Building

Build and run Unit Tests:

    ./gradlew build

## Integration Tests

By default, Integration Tests (ITs) are skipped during build.
If you want to run them, you need first to retrieve the related projects which are used as input:

    git submodule update --init its/sources

Then build and run the Integration Tests using the `its` property:

    ./gradlew build -Pits --info --no-daemon -Dsonar.runtimeVersion=7.4

## License headers

When adding a new source file, you will need to add license headers. Instead of copy-pasting blocks, the following command line can be used:

    ./gradlew licenseFormat
