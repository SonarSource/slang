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
package org.sonarsource.slang.kotlin;

import org.sonarsource.slang.checks.CommentedCodeCheck;
import org.sonarsource.slang.checks.CommonCheckList;
import java.util.ArrayList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;

public class KotlinRulesDefinition implements RulesDefinition {

  private static final String RESOURCE_FOLDER = "org/sonar/l10n/kotlin/rules/kotlin";

  @Override
  public void define(Context context) {
    NewRepository repository = context
      .createRepository(KotlinPlugin.KOTLIN_REPOSITORY_KEY, KotlinPlugin.KOTLIN_LANGUAGE_KEY)
      .setName(KotlinPlugin.REPOSITORY_NAME);
    RuleMetadataLoader ruleMetadataLoader = new RuleMetadataLoader(RESOURCE_FOLDER);
// TODO: Add logic for rules that require language specific configuration at construction time
    ArrayList<Class> checks = new ArrayList<>(CommonCheckList.checks());
    checks.add(CommentedCodeCheck.class);
    ruleMetadataLoader.addRulesByAnnotatedClass(repository, checks);
    repository.done();
  }

}
