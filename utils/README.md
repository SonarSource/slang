# SLang - Utils

## Detekt

To check if [`rules.json`](sonar-slang-plugin/src/main/resources/org/sonar/l10n/kotlin/rules/detekt/rules.json)
should be updated according to a new version of detekt:

  1. Update `detekt.version` property in [`generate-detekt-rules/pom.xml`](generate-detekt-rules/pom.xml)
  1. Run `mvn test -Pgenerate-detekt-rules`
  1. If the test fails, follow instructions to replace `rules.json` by the one in `target` folder.
