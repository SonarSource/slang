/*
 * SonarSource SLang
 * Copyright (C) 2018-2021 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.slang.externalreport.rubocop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.apache.commons.text.StringEscapeUtils;

import static java.nio.charset.StandardCharsets.UTF_8;

public class RuboCopRuleDefinition {

  private static final Path RULES_FILE = Paths.get("sonar-ruby-plugin", "src", "main", "resources",
    "org", "sonar", "l10n", "ruby", "rules", "rubocop", "rules.json");

  private static final Path RUBOCOP_RULES_YAML_FILE = Paths.get("utils", "generate-rubocop-rules",
    "src", "main", "resources", "rubocop.yml");

  public static void main(String[] args) throws IOException {
    Path rulesSourceYaml = resolve(RUBOCOP_RULES_YAML_FILE);
    Path rulesDestinationJson = resolve(RULES_FILE);
    System.out.println("Convert : " + rulesSourceYaml);
    System.out.println("Into    : " + rulesDestinationJson);
    String rules = RuboCopRuleDefinitionGenerator.generateRuleDefinitionJson(rulesSourceYaml);
    Files.write(rulesDestinationJson, rules.getBytes(UTF_8));
  }

  private static Path resolve(Path path) throws IOException {
    Path projectPath = Paths.get(".").toRealPath();
    while (!projectPath.resolve(path).toFile().exists()) {
      projectPath = projectPath.getParent();
    }
    return projectPath.resolve(path);
  }

  static final class RuboCopRuleDefinitionGenerator {

    private RuboCopRuleDefinitionGenerator() {
      // Utility class
    }

    static String generateRuleDefinitionJson(Path ruboCopRulesYamlFilePath) throws IOException {
      RuboCopRulesYamlFile help = new RuboCopRulesYamlFile(ruboCopRulesYamlFilePath);
      help.rules.sort(Comparator.comparing(a -> a.key));
      Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
      return gson.toJson(help.rules) + "\n";
    }
  }

  private static class ExternalRule {
    String key;
    String name;
    String type;
    String severity;
    String description;
    String url;
  }

  private static class RuboCopRulesYamlFile {

    private static final String BASE_URL = "https://www.rubydoc.info/gems/rubocop/RuboCop/Cop/";

    private List<ExternalRule> rules;

    private RuboCopRulesYamlFile(Path yamlFilePath) throws IOException {
      this.rules = new ArrayList<>();
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      mapper.readTree(yamlFilePath.toFile()).fields().forEachRemaining(this::addRule);
    }

    private void addRule(Map.Entry<String, JsonNode> entry) {
      ExternalRule rule = new ExternalRule();
      rule.key = entry.getKey();
      String category = ruleCategory(rule.key);
      String pascalCaseName = rule.key.substring(category.length() + 1).replaceAll("([^A-Z])([A-Z])", "$1 $2");
      rule.name = pascalCaseName + " (" + category + ")";
      rule.description = StringEscapeUtils.escapeHtml4(entry.getValue().get("Description").asText());
      rule.url = BASE_URL + rule.key;
      switch (category) {
        case "Naming":
        case "Layout":
        case "Migration":
          rule.type = "CODE_SMELL";
          rule.severity = "INFO";
          break;
        case "Bundler":
        case "Gemspec":
        case "Metrics":
        case "Style":
          rule.type = "CODE_SMELL";
          rule.severity = "MINOR";
          break;
        case "Lint":
        case "Performance":
        case "Rails":
          rule.type = "CODE_SMELL";
          rule.severity = "MAJOR";
          break;
        case "Security":
          rule.type = "VULNERABILITY";
          rule.severity = "MAJOR";
          break;
        default:
          throw new IllegalStateException("Unsupported category: " + category);
      }
      rules.add(rule);
    }

    private static String ruleCategory(String key) {
      return key.substring(0, key.indexOf('/'));
    }

  }

}
