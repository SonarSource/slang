# Contributing

Thank you for helping to improve SLang.

## Reporting Issues

Report bugs and request features in [GitHub issues](https://github.com/SonarQubeCommunity/slang/issues). For a problem in an analyzer built on SLang, include the analyzer, its version, and a minimal code sample.

## Submitting Changes

1. Fork the repository and create a branch from `master`.
2. Follow the build instructions in the [README](README.md#building), including the submodule initialization.
3. Add or update tests that cover your change.
4. Run `./gradlew spotlessApply` to format the code and update the license headers, then `./gradlew build` to make sure that everything passes.
5. Open a pull request against `master` and describe what the change does and why.

The [CI](.github/workflows/ci.yml) workflow builds the project and runs the tests on every pull request.

## License

By contributing, you agree that your contributions are licensed under the [Sonar Source-Available License Version 1 (SSALv1)](LICENSE.txt), like the rest of the project.
