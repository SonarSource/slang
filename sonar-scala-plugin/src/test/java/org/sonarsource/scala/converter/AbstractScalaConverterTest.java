/*
 * SonarSource SLang
 * Copyright (C) 2018-2019 SonarSource SA
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
package org.sonarsource.scala.converter;

import java.util.List;
import java.util.stream.Stream;
import org.sonarsource.slang.api.ASTConverter;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.parser.SLangConverter;
import org.sonarsource.slang.plugin.converter.ASTConverterValidation;
import org.sonarsource.slang.plugin.converter.ASTConverterValidation.ValidationMode;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractScalaConverterTest {

  private final ASTConverter converter = new ASTConverterValidation(new ScalaConverter(), ValidationMode.THROW_EXCEPTION);

  Tree parse(String scalaCode) {
    return converter.parse(scalaCode, null);
  }

  Tree scalaStatement(String scalaCode) {
    TopLevelTree topLevel = (TopLevelTree) converter.parse("object Main { def foo():Unit={ " + scalaCode + "} }");
    NativeTree objectDefn = (NativeTree) topLevel.children().get(0);
    NativeTree template = (NativeTree) objectDefn.children().get(1);
    FunctionDeclarationTree functionDefn = (FunctionDeclarationTree) template.children().get(template.children().size() - 1);
    return functionDefn.body().statementOrExpressions().get(0);
  }

  FunctionDeclarationTree scalaMethod(String scalaCode) {
    TopLevelTree topLevel = (TopLevelTree) parse("object Main { " + scalaCode + " }");
    NativeTree objectDefn = (NativeTree) topLevel.children().get(0);
    NativeTree template = (NativeTree) objectDefn.children().get(1);
    return (FunctionDeclarationTree) template.children().get(template.children().size() - 1);
  }

  Tree slangStatement(String innerCode) {
    List<Tree> statements = slangStatements(innerCode);
    assertThat(statements).hasSize(1);
    return statements.get(0);
  }

  List<Tree> slangStatements(String innerCode) {
    Tree tree = new SLangConverter().parse(innerCode);
    assertThat(tree).isInstanceOf(TopLevelTree.class);
    return tree.children();
  }

  static Stream<String> identifierDescendants(Tree tree) {
    return tree.descendants()
      .filter(IdentifierTree.class::isInstance)
      .map(i -> ((IdentifierTree) i).name());
  }
}
