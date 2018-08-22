## Android Lint

To re-generate android lint's [`rules.json`](../../sonar-kotlin-plugin/src/main/resources/org/sonar/l10n/android/rules/androidlint/rules.json):

  1. Update android lint, the new version `android-sdk/tools/bin/lint --version` prints `lint: version 26.1.1`
  1. Export android lint help `android-sdk/tools/bin/lint --show > generate-android-lint-rules/src/test/resources/android-lint-help.txt`
  1. Run `mvn clean package exec:java`
