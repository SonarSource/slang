## Rubocop

To re-generate rubocop's [`rules.json`](../../sonar-ruby-plugin/src/main/resources/org/sonar/l10n/ruby/rules/rubocop/rules.json):

  1. Update rubocop, the new version `rubocop --version` prints `0.58.2`
  1. Export rubocop rules `rubocop --show-cops | grep -v 'MaximumRangeSize: .inf' > utils/generate-rubocop-rules/src/main/resources/rubocop.yml`
  1. Run `mvn clean package exec:java`
