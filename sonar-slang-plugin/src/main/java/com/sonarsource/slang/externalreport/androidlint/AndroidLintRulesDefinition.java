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
package com.sonarsource.slang.externalreport.androidlint;

import com.sonarsource.slang.kotlin.SlangPlugin;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonarsource.analyzer.commons.ExternalRuleLoader;

import static com.sonarsource.slang.externalreport.androidlint.AndroidLintSensor.LINTER_KEY;
import static com.sonarsource.slang.externalreport.androidlint.AndroidLintSensor.LINTER_NAME;

public class AndroidLintRulesDefinition implements RulesDefinition {

  private static final String RULES_JSON = "org/sonar/l10n/android/rules/androidlint/rules.json";

  /**
   * Android lint scopes could be: ".xml", ".java", ".kt", ".kts", ".properties", ".gradle", "proguard.cfg", "proguard-project.txt", ".png", ".class"
   * ( https://android.googlesource.com/platform/tools/base/+/studio-master-dev/lint/libs/lint-api/src/main/java/com/android/tools/lint/detector/api/Scope.kt )
   * But this sensor provides rule descriptions only for ".xml", ".java", ".kt"
   */
  private static final List<String> RULE_REPOSITORY_LANGUAGES = Arrays.asList("xml", "java", SlangPlugin.KOTLIN_LANGUAGE_KEY);

  private static final List<String> TEXT_FILE_EXTENSIONS = Arrays.asList(".xml", ".java", ".kt", ".kts", ".properties", ".gradle", ".cfg", ".txt");

  static final List<ExternalRuleLoader> RULE_LOADERS = RULE_REPOSITORY_LANGUAGES.stream()
    .map(language -> new ExternalRuleLoader(LINTER_KEY + "-" + language, LINTER_NAME, RULES_JSON, language))
    .collect(Collectors.toList());

  private static final String DEFAULT_REPOSITORY_KEY = LINTER_KEY;

  private final boolean externalIssuesSupported;

  public AndroidLintRulesDefinition(boolean externalIssuesSupported) {
    this.externalIssuesSupported = externalIssuesSupported;
  }

  @Override
  public void define(Context context) {
    if (externalIssuesSupported) {
      RULE_LOADERS.forEach(loader -> loader.createExternalRuleRepository(context));
    }
  }

  static RuleKey ruleKey(@Nullable String language, String ruleId) {
    if (language == null || !RULE_REPOSITORY_LANGUAGES.contains(language)) {
      return RuleKey.of(DEFAULT_REPOSITORY_KEY, ruleId);
    }
    return RuleKey.of(LINTER_KEY + "-" + language, ruleId);
  }

  static boolean isTextFile(String file) {
    return TEXT_FILE_EXTENSIONS.stream().anyMatch(file::endsWith);
  }
}
