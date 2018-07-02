/*
 * SonarSource SLang
 * Copyright (C) 2009-2018 SonarSource SA
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
package com.sonarsource.slang.externalreport.detekt;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.gitlab.arturbosch.detekt.api.Config;
import io.gitlab.arturbosch.detekt.api.MultiRule;
import io.gitlab.arturbosch.detekt.api.Rule;
import io.gitlab.arturbosch.detekt.api.RuleSetProvider;
import io.gitlab.arturbosch.detekt.api.Severity;
import io.gitlab.arturbosch.detekt.api.YamlConfig;
import io.gitlab.arturbosch.detekt.cli.ClasspathResourceConverter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.sonar.api.internal.apachecommons.lang.StringEscapeUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

public class DetektRuleDefinitionTest {

  private static final Path EXPECTED_RULES_FILE = Paths.get("target", "rules.json").toAbsolutePath();

  private static final Path SLANG_FOLDER = EXPECTED_RULES_FILE.getParent().getParent().getParent().getParent();

  private static final Path ACTUAL_RULES_FILE = SLANG_FOLDER.resolve(Paths.get("sonar-slang-plugin", "src", "main", "resources",
    "org", "sonar", "l10n", "kotlin", "rules", "detekt", "rules.json"));

  @Test
  public void check_rule_definition() throws IOException {

    String expected = DetektRuleDefinitionGenerator.generateRuleDefinitionJson();
    String actual = new String(Files.readAllBytes(ACTUAL_RULES_FILE), UTF_8);

    if (!expected.equals(actual)) {
      Files.write(EXPECTED_RULES_FILE, expected.getBytes(UTF_8));
    }

    assertEquals("\nThe file:\n" + EXPECTED_RULES_FILE + "\nIs not equals to:\n" + ACTUAL_RULES_FILE + "\n",
      expected, actual);
  }

  static final class DetektRuleDefinitionGenerator {

    private static final Logger LOG = Loggers.get(DetektRuleDefinitionGenerator.class);

    private static final String BASE_URL = "https://arturbosch.github.io/detekt/";
    private static final String BASE_PKG = "io.gitlab.arturbosch.detekt.rules.";

    private static final String STYLE = "style";

    private static final Map<String, String> PACKAGE_TO_URL = buildPackageToUrl();

    private static Map<String, String> buildPackageToUrl() {
      Map<String, String> map = new HashMap<>();
      map.put(BASE_PKG + "documentation", BASE_URL + "comments");
      map.put(BASE_PKG + "complexity", BASE_URL + "complexity");
      map.put(BASE_PKG + "empty", BASE_URL + "empty-blocks");
      map.put(BASE_PKG + "exceptions", BASE_URL + "exceptions");
      map.put(BASE_PKG + "naming", BASE_URL + "naming");
      map.put(BASE_PKG + "performance", BASE_URL + "performance");
      map.put(BASE_PKG + "bugs", BASE_URL + "potential-bugs");
      map.put(BASE_PKG + STYLE, BASE_URL + STYLE);
      map.put(BASE_PKG + "style.optional", BASE_URL + STYLE);
      return map;
    }

    private static final EnumMap<Severity, String> SEVERITY_TRANSLATIONS_MAP = buildSeverityTranslationsMap();

    private static EnumMap<Severity, String> buildSeverityTranslationsMap() {
      EnumMap<Severity, String> map = new EnumMap<>(Severity.class);
      map.put(Severity.CodeSmell, "MAJOR");
      map.put(Severity.Defect, "CRITICAL");
      map.put(Severity.Maintainability, "MAJOR");
      map.put(Severity.Minor, "MINOR");
      map.put(Severity.Security, "BLOCKER");
      map.put(Severity.Style, "INFO");
      map.put(Severity.Warning, "INFO");
      map.put(Severity.Performance, "CRITICAL");
      return map;
    }

    private DetektRuleDefinitionGenerator() {
      // Utility class
    }

    static String generateRuleDefinitionJson() {
      List<Rule> rules = new ArrayList<>();
      URL configUrl = new ClasspathResourceConverter().convert("default-detekt-config.yml");
      for (RuleSetProvider provider : ServiceLoader.load(RuleSetProvider.class)) {
        Config config = YamlConfig.Companion.loadResource(configUrl);
        rules.addAll(provider.instance(config).getRules().stream()
          .flatMap(rule -> rule instanceof MultiRule ? ((MultiRule) rule).getRules().stream() : Stream.of(rule))
          .filter(Rule.class::isInstance)
          .map(Rule.class::cast)
          .collect(Collectors.toList())
        );
      }
      ArrayList<ExternalRule> externalRules = new ArrayList<>();
      for (Rule rule : rules) {
        ExternalRule externalRule = new ExternalRule();
        externalRule.key = rule.getId();
        externalRule.name = pascalCaseToTitle(rule.getId());
        externalRule.description = StringEscapeUtils.escapeHtml(rule.getIssue().getDescription());
        externalRule.url = ruleDocumentation(rule);
        externalRule.tags = Collections.singleton(rule.getIssue().getSeverity().name().toLowerCase(Locale.ROOT));
        externalRule.type = "CODE_SMELL";
        externalRule.severity = SEVERITY_TRANSLATIONS_MAP.getOrDefault(rule.getIssue().getSeverity(), "MINOR");
        externalRule.constantDebtMinutes = Long.parseLong(rule.getIssue().getDebt().toString().replace("min",""));
        externalRules.add(externalRule);
      }
      LOG.info("Found " + externalRules.size() + " rules");
      externalRules.sort(Comparator.comparing(a -> a.key));
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      return gson.toJson(externalRules) + "\n";
    }

    private static String pascalCaseToTitle(String id) {
      return id.replaceAll("([^A-Z])([A-Z])", "$1 $2");
    }

    @Nullable
    private static String ruleDocumentation(Rule rule) {
      String packageName = rule.getClass().getPackage().getName();
      String url = PACKAGE_TO_URL.get(packageName);
      if (url != null) {
        return url + ".html#" + rule.getId().toLowerCase(Locale.ROOT);
      }
      return null;
    }

    private static class ExternalRule {
      String key;
      String name;
      String type;
      String severity;
      String description;
      String url;
      Set<String> tags;
      long constantDebtMinutes;
    }

  }
}
