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

import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;

import static com.sonarsource.slang.externalreport.androidlint.AndroidLintRulesDefinition.isTextFile;
import static org.assertj.core.api.Assertions.assertThat;

public class AndroidLintRulesDefinitionTest {

  @Test
  public void external_repositories_not_supported() {
    RulesDefinition.Context context = new RulesDefinition.Context();
    AndroidLintRulesDefinition rulesDefinition = new AndroidLintRulesDefinition(false);
    rulesDefinition.define(context);
    assertThat(context.repositories()).isEmpty();
  }

  @Test
  public void android_lint_external_repository() {
    RulesDefinition.Context context = new RulesDefinition.Context();
    AndroidLintRulesDefinition rulesDefinition = new AndroidLintRulesDefinition(true);
    rulesDefinition.define(context);

    assertThat(context.repositories()).hasSize(3);
    RulesDefinition.Repository xmlRepository = context.repository("external_android-lint-xml");
    assertThat(xmlRepository.name()).isEqualTo("Android Lint");
    assertThat(xmlRepository.language()).isEqualTo("xml");
    assertThat(xmlRepository.isExternal()).isEqualTo(true);
    assertThat(xmlRepository.rules().size()).isEqualTo(313);

    RulesDefinition.Repository javaRepository = context.repository("external_android-lint-java");
    assertThat(javaRepository.name()).isEqualTo("Android Lint");
    assertThat(javaRepository.language()).isEqualTo("java");
    assertThat(javaRepository.isExternal()).isEqualTo(true);
    assertThat(javaRepository.rules().size()).isEqualTo(313);

    RulesDefinition.Repository kotlinRepository = context.repository("external_android-lint-kotlin");
    assertThat(kotlinRepository.name()).isEqualTo("Android Lint");
    assertThat(kotlinRepository.language()).isEqualTo("kotlin");
    assertThat(kotlinRepository.isExternal()).isEqualTo(true);
    assertThat(kotlinRepository.rules().size()).isEqualTo(313);

    RulesDefinition.Rule rule = xmlRepository.rule("AaptCrash");
    assertThat(rule).isNotNull();
    assertThat(rule.name()).isEqualTo("Potential AAPT crash");
    assertThat(rule.type()).isEqualTo(RuleType.BUG);
    assertThat(rule.severity()).isEqualTo("CRITICAL");
    assertThat(rule.htmlDescription()).isEqualTo(
        "<p>\n" +
        "Defining a style which sets android:id to a dynamically generated id can cause\n" +
        "many versions of aapt, the resource packaging tool, to crash. To work around\n" +
        "this, declare the id explicitly with &lt;item type=&quot;id&quot; name=&quot;...&quot; /&gt; instead.\n" +
        "</p>\n" +
        "<p>\n" +
        "More information: <br />\n" +
        "<a href=\"https://code.google.com/p/android/issues/detail?id=20479\">https://code.google.com/p/android/issues/detail?id=20479</a><br />\n" +
        "</p>");
    assertThat(rule.tags()).containsExactlyInAnyOrder("android");
    assertThat(rule.debtRemediationFunction().baseEffort()).isEqualTo("5min");
  }

  @Test
  public void rule_key_with_external_repository() {
    assertThat(AndroidLintRulesDefinition.ruleKey("xml", "S123")).isEqualTo(RuleKey.of("android-lint-xml", "S123"));
    assertThat(AndroidLintRulesDefinition.ruleKey("java", "S123")).isEqualTo(RuleKey.of("android-lint-java", "S123"));
    assertThat(AndroidLintRulesDefinition.ruleKey("kotlin", "S123")).isEqualTo(RuleKey.of("android-lint-kotlin", "S123"));
  }

  @Test
  public void rule_key_without_external_repository() {
    assertThat(AndroidLintRulesDefinition.ruleKey(null, "S123")).isEqualTo(RuleKey.of("android-lint", "S123"));
    assertThat(AndroidLintRulesDefinition.ruleKey("js", "S123")).isEqualTo(RuleKey.of("android-lint", "S123"));
  }

  @Test
  public void text_files() {
    assertThat(isTextFile("AndroidManifest.xml")).isTrue();
    assertThat(isTextFile("Main.java")).isTrue();
    assertThat(isTextFile("App.kt")).isTrue();
    assertThat(isTextFile("default.properties")).isTrue();
    assertThat(isTextFile("build.gradle")).isTrue();
    assertThat(isTextFile("proguard.cfg")).isTrue();
    assertThat(isTextFile("proguard-project.txt")).isTrue();
  }

  @Test
  public void binary_files() {
    assertThat(isTextFile("App.class")).isFalse();
    assertThat(isTextFile("button.png")).isFalse();

  }
}
