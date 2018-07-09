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
package org.sonarsource.slang.checks;

import org.sonarsource.slang.api.AssignmentExpressionTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.StringLiteralTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.VariableDeclarationTree;
import org.sonarsource.slang.checks.api.CheckContext;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

@Rule(key = "S2068")
public class HardcodedCredentialsCheck implements SlangCheck {

  private static final String DEFAULT_VALUE = "password,passwd,pwd";

  @RuleProperty(
    key = "credentialWords",
    description = "Comma separated list of words identifying potential credentials",
    defaultValue = DEFAULT_VALUE)
  public String credentialWords = DEFAULT_VALUE;

  private List<Pattern> variablePatterns;
  private List<Pattern> literalPatterns;

  @Override
  public void initialize(InitContext init) {
    init.register(AssignmentExpressionTree.class, (ctx, tree) -> {
      Tree leftHandSide = tree.leftHandSide();
      if (leftHandSide instanceof IdentifierTree && tree.statementOrExpression() instanceof StringLiteralTree) {
        getPasswordVariableName(((IdentifierTree) leftHandSide).name())
          .ifPresent(passwordVariableName -> report(ctx, leftHandSide, passwordVariableName));
      }
    });

    init.register(VariableDeclarationTree.class, (ctx, tree) -> {
      if (tree.initializer() instanceof StringLiteralTree) {
        getPasswordVariableName(tree.identifier().name())
          .ifPresent(passwordVariableName -> report(ctx, tree.identifier(), passwordVariableName));
      }
    });

    init.register(StringLiteralTree.class, (ctx, tree) -> literalPatterns()
      .map(pattern -> pattern.matcher(tree.content()))
      .filter(Matcher::find)
      .map(matcher -> matcher.group(1))
      .forEach(credential -> report(ctx, tree, credential)));

  }

  private static void report(CheckContext ctx, Tree tree, String matchName) {
    String message = String.format("'%s' detected in this expression, review this potentially hardcoded credential.", matchName);
    ctx.reportIssue(tree, message);
  }

  private Optional<String> getPasswordVariableName(String name) {
    return variablePatterns()
      .map(pattern -> pattern.matcher(name))
      .filter(Matcher::find)
      .map(matcher -> matcher.group(1))
      .findAny();
  }

  private Stream<Pattern> variablePatterns() {
    if (variablePatterns == null) {
      variablePatterns = toPatterns("");
    }
    return variablePatterns.stream();
  }

  private Stream<Pattern> literalPatterns() {
    if (literalPatterns == null) {
      literalPatterns = toPatterns("=\\S");
    }
    return literalPatterns.stream();
  }

  private List<Pattern> toPatterns(String suffix) {
    return Stream.of(credentialWords.split(","))
      .map(String::trim)
      .map(word -> Pattern.compile("(" + word + ")" + suffix, Pattern.CASE_INSENSITIVE))
      .collect(Collectors.toList());
  }

}
