# SLang

[![Build Status](https://travis-ci.org/SonarSource/slang.svg?branch=master)](https://travis-ci.org/SonarSource/slang)

## Building

Run:

    mvn clean install

By default, Integration Tests (ITs) are skipped during build.
If you want to run them, you need first to retrieve the related projects which are used as input:

    git submodule update --init

Then run the ITs

    cd its
    mvn clean install -Dsonar.runtimeVersion=7.1
