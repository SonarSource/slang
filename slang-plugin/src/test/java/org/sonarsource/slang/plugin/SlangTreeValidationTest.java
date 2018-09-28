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
package org.sonarsource.slang.plugin;

import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarsource.slang.api.ParseException;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.impl.IdentifierTreeImpl;
import org.sonarsource.slang.impl.ImportDeclarationTreeImpl;
import org.sonarsource.slang.impl.PackageDeclarationTreeImpl;
import org.sonarsource.slang.parser.SLangConverter;
import org.sonarsource.slang.plugin.SlangTreeValidation.TokenValidationBuilder;

import static org.sonarsource.slang.plugin.SlangTreeValidation.validateTree;

public class SlangTreeValidationTest {

  private SLangConverter parser = new SLangConverter();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void no_exception_when_source_code_matches() {
    String code = "package /* comment1 */ abc; // comment2";
    Tree tree = parser.parse(code);
    validateTree(tree, code, validation()
        .patternFor("package|;", PackageDeclarationTreeImpl.class)
        .anyFor(IdentifierTreeImpl.class)
        .build());
  }

  @Test
  public void allow_space_at_end_of_line_and_file() {
    Tree tree = parser.parse("import a;\nimport b;");
    validateTree(tree, "import a;  \nimport b;  \n  \n", validation()
        .patternFor("import|;", ImportDeclarationTreeImpl.class)
        .anyFor(IdentifierTreeImpl.class)
        .build());
  }

  @Test
  public void unexpected_token() {
    thrown.expect(ParseException.class);
    thrown.expectMessage("Token(s) 'import', ';' unexpected in ImportDeclarationTreeImpl");
    String code = "import a;";
    Tree tree = parser.parse(code);
    validateTree(tree, code, validation()
      .anyFor(IdentifierTreeImpl.class)
      .build());
  }

  @Test
  public void unexpected_additional_source_code_line() {
    thrown.expect(ParseException.class);
    thrown.expectMessage("Unexpected AST number of lines actual: 1, expected: 2");
    Tree tree = parser.parse("import a;");
    validateTree(tree, "import a;\n import b;", validation()
        .patternFor("import|;", ImportDeclarationTreeImpl.class)
        .anyFor(IdentifierTreeImpl.class)
        .build());
  }

  @Test
  public void unexpected_missing_source_code_line() {
    thrown.expect(ParseException.class);
    thrown.expectMessage("Unexpected AST number of lines actual: 2, expected: 1");
    Tree tree = parser.parse("import a;\n import b;");
    validateTree(tree, "import a;", validation()
        .patternFor("import|;", ImportDeclarationTreeImpl.class)
        .anyFor(IdentifierTreeImpl.class)
        .build());
  }

  @Test
  public void source_code_does_not_match() {
    thrown.expect(ParseException.class);
    thrown.expectMessage("Unexpected AST difference at line: 1\n" +
      "Actual   : import a;\n" +
      "Expected : import b;");
    Tree tree = parser.parse("import a;");
    validateTree(tree, "import b;", validation()
        .patternFor("import|;", ImportDeclarationTreeImpl.class)
        .anyFor(IdentifierTreeImpl.class)
        .build());
  }

  private static TokenValidationBuilder validation() {
    return new TokenValidationBuilder();
  }

  @Test
  public void redundant_token_in_children() {
    thrown.expect(ParseException.class);
    thrown.expectMessage("Token 'import' missing from parent tokens or already used by another child.");
    String code = "import a;";
    Tree validTree = parser.parse(code);
    List<Tree> children = validTree.children();
    Tree invalidTree = new ImportDeclarationTreeImpl(validTree.metaData(), Arrays.asList(children.get(0), children.get(0)));
    validateTree(invalidTree, code, validation()
      .patternFor("import|;", ImportDeclarationTreeImpl.class)
      .anyFor(IdentifierTreeImpl.class)
      .build());
  }

}
