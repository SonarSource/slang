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
package com.sonarsource.slang.antlr;

import com.sonarsource.slang.api.NativeTree;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.impl.NativeTreeImpl;
import com.sonarsource.slang.parser.SLangBaseListener;
import com.sonarsource.slang.parser.SLangLexer;
import com.sonarsource.slang.parser.SLangParser;
import java.io.IOException;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SLangParserTest {

  @Test
  public void testFile() throws IOException {
    SLangLexer lexer = new SLangLexer(CharStreams.fromFileName("src/test/resources/binary.slang"));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    SLangParser parser = new SLangParser(tokens);
    SLangParser.SlangFileContext context = parser.slangFile();
    assertThat(context.children).isNotNull();
    assertThat(context.children.isEmpty()).isFalse();
  }

  @Test
  public void testBinaryExpression() {
    SLangLexer lexer = new SLangLexer(CharStreams.fromString("x = 1\n//comment\ny = 2 + \"1\""));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    SLangParser parser = new SLangParser(tokens);
    SLangParser.SlangFileContext tree = parser.slangFile();
    ParseTreeWalker walker = new ParseTreeWalker();

    for (Token token : tokens.getTokens()) {
      System.out.println(token.getLine() + ": " + token.getText());
    }

    assertThat(tree.children, notNullValue());
    assertThat(tree.children.isEmpty(), is(false));

  }
}
