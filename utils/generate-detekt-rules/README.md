## Detekt

To re-generate detekt's [`rules.json`](../../sonar-kotlin-plugin/src/main/resources/org/sonar/l10n/kotlin/rules/detekt/rules.json):

  1. Update `detekt.version` property in [`pom.xml`](pom.xml)
  1. Run `mvn clean package exec:java`
