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
package org.sonarsource.kotlin.externalreport.androidlint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.internal.apachecommons.lang.StringEscapeUtils;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Android lint source code: https://android.googlesource.com/platform/tools/base/+/studio-master-dev/lint
 */
public class AndroidLintRuleDefinition {

  private static final Path RULES_FILE = Paths.get("sonar-slang-plugin", "src", "main", "resources",
    "org", "sonar", "l10n", "android", "rules", "androidlint", "rules.json");

  private static final Path ANDROID_LINT_HELP = Paths.get("utils", "generate-android-lint-rules",
    "src", "main", "resources", "android-lint-help.txt");

  public static void main(String[] args) throws IOException {
    Path projectPath = Paths.get(".").toRealPath();
    while (!projectPath.resolve(RULES_FILE).toFile().exists()) {
      projectPath = projectPath.getParent();
    }
    String rules = AndroidLintDefinitionGenerator.generateRuleDefinitionJson(projectPath.resolve(ANDROID_LINT_HELP));
    Files.write(projectPath.resolve(RULES_FILE), rules.getBytes(UTF_8));
  }

  static final class AndroidLintDefinitionGenerator {

    private AndroidLintDefinitionGenerator() {
      // Utility class
    }

    static String generateRuleDefinitionJson(Path androidLintHelpPath) {
      ArrayList<ExternalRule> externalRules = new ArrayList<>();
      AndroidLintHelp help = new AndroidLintHelp(androidLintHelpPath);
      ExternalRule externalRule = help.read();
      while (externalRule != null) {
        externalRules.add(externalRule);
        externalRule = help.read();
      }
      externalRules.sort(Comparator.comparing(a -> a.key));
      Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
      return gson.toJson(externalRules) + "\n";
    }

    private static class ExternalRule {
      String key;
      String name;
      String type;
      String severity;
      String description;
      Set<String> tags;
      long constantDebtMinutes;
    }

    private static class AndroidLintHelp {

      private final List<String> lines;
      private int pos;

      private AndroidLintHelp(Path androidLintHelpPath) {
        try {
          lines = Files.readAllLines(androidLintHelpPath, UTF_8);
        } catch (IOException e) {
          throw new IllegalStateException("Can't load android-lint-help.txt", e);
        }
        if (lines.isEmpty() || !lines.get(0).equals("Available issues:")) {
          throw new IllegalStateException("Unexpected android-lint-help.txt first line: " + lines.get(0));
        }
        pos = 1;
      }

      @CheckForNull
      private ExternalRule read() {
        if (pos >= lines.size()) {
          return null;
        }
        consumeCategorySections();
        String ruleKey = consumeRuleSection();
        if (ruleKey == null) {
          return null;
        }
        ExternalRule rule = new ExternalRule();
        rule.key = ruleKey;
        rule.name = consumeHeader("Summary");
        String priority = consumeHeader("Priority");
        String severity = consumeHeader("Severity");
        String category = consumeHeader("Category");
        rule.description = consumeDescription();

        rule.type = mapType(severity, category, priority);
        rule.severity = mapSeverity(severity);
        rule.tags = new HashSet<>(mapTags(category));
        rule.constantDebtMinutes = 5L;
        return rule;
      }

      private static class AndroidLint {
        private static class Severity {
          private static final String INFORMATION = "Information";
          private static final String WARNING = "Warning";
          private static final String ERROR = "Error";
          private static final String FATAL = "Fatal";
        }
        private static class Category {
          private static final String ACCESSIBILITY = "Accessibility";
          private static final String CORRECTNESS = "Correctness";
          private static final String INTERNATIONALIZATION = "Internationalization";
          private static final String PERFORMANCE = "Performance";
          private static final String SECURITY = "Security";
          private static final String USABILITY = "Usability";
        }
      }

      private static String mapSeverity(String severity) {
        switch (severity) {
          case AndroidLint.Severity.INFORMATION:
            return "INFO";
          case AndroidLint.Severity.WARNING:
            return "MINOR";
          case AndroidLint.Severity.ERROR:
            return "MAJOR";
          case AndroidLint.Severity.FATAL:
            return "CRITICAL";
          default:
            throw new IllegalStateException("Unexpected severity: " + severity);
        }
      }

      private static String mapType(String severity, String category, String priority) {
        int priorityValue = Integer.parseInt(priority.substring(0, priority.indexOf(' ')));
        if (priorityValue >= 7 && category.equals(AndroidLint.Category.SECURITY) && severity.equals(AndroidLint.Severity.FATAL)) {
          return "VULNERABILITY";
        } else if (priorityValue >= 5 &&
          (category.equals(AndroidLint.Category.SECURITY) || category.startsWith(AndroidLint.Category.CORRECTNESS)) &&
          (severity.equals(AndroidLint.Severity.ERROR) || severity.equals(AndroidLint.Severity.FATAL))) {
          return "BUG";
        }
        return "CODE_SMELL";
      }

      private static List<String> mapTags(String category) {
        if (category.equals(AndroidLint.Category.ACCESSIBILITY)) {
          return Collections.singletonList("accessibility");
        } else if (category.startsWith(AndroidLint.Category.CORRECTNESS)) {
          return Collections.emptyList();
        } else if (category.startsWith(AndroidLint.Category.INTERNATIONALIZATION)) {
          return Collections.singletonList("i18n");
        } else if (category.equals(AndroidLint.Category.PERFORMANCE)) {
          return Collections.singletonList("performance");
        } else if (category.equals(AndroidLint.Category.SECURITY)) {
          return Collections.singletonList("security");
        } else if (category.startsWith(AndroidLint.Category.USABILITY)) {
          return Collections.singletonList("user-experience");
        } else {
          throw new IllegalStateException("Unexpected category: " + category);
        }
      }

      private void consumeCategorySections() {
        while (isSection("=+")) {
          pos += 3;
        }
      }

      @Nullable
      private String consumeRuleSection() {
        if (isSection("-+")) {
          String key = lines.get(pos + 1);
          pos += 3;
          return key;
        }
        return null;
      }

      private String consumeHeader(String name) {
        if (pos >= lines.size() || !lines.get(pos).startsWith(name + ": ")) {
          throw new IllegalStateException("Unexpected line at " + (pos + 1) +
            " instead of '" + name + ":' header: " + lines.get(pos));
        }
        String header = lines.get(pos).substring(lines.get(pos).indexOf(':') + 2);
        pos++;
        if (name.equals("Summary") && pos < lines.size() && !lines.get(pos).isEmpty()) {
          header += " " + lines.get(pos);
          pos++;
        }
        while (pos < lines.size() && lines.get(pos).isEmpty()) {
          pos++;
        }
        return header;
      }

      private String consumeDescription() {
        StringBuilder html = new StringBuilder();
        html.append("<p>\n");
        boolean lastLineWasMoreInformation = false;
        while (pos < lines.size() && !isSection("=+|-+")) {
          String line = StringEscapeUtils.escapeHtml(lines.get(pos));
          if (line.isEmpty()) {
            html.append(line).append("</p>\n<p>\n");
          } else if (lastLineWasMoreInformation && line.startsWith("http") && line.matches("\\S+")) {
            html.append("<a href=\"").append(line).append("\">").append(line).append("</a><br />\n");
          } else if (line.endsWith(":") || line.endsWith(": ")) {
            html.append(line).append("<br />\n");
          } else {
            html.append(line).append("\n");
          }
          lastLineWasMoreInformation = line.equals("More information: ");
          pos++;
        }
        html.append("</p>\n");
        return html.toString();
      }

      private boolean isSection(String underlineRegex) {
        return pos + 2 < lines.size() &&
          lines.get(pos).isEmpty() &&
          lines.get(pos + 1).matches("\\w+") &&
          lines.get(pos + 2).matches(underlineRegex) &&
          lines.get(pos + 1).length() == lines.get(pos + 2).length();
      }
    }
  }

}
