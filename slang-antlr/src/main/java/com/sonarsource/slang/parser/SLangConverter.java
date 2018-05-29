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
package com.sonarsource.slang.parser;

import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.impl.IdentifierImpl;
import com.sonarsource.slang.impl.NativeTreeImpl;
import com.sun.istack.internal.NotNull;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class SLangConverter {

  public Tree parse(String slangFile) throws IOException {
    SLangLexer lexer = new SLangLexer(CharStreams.fromFileName(slangFile));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    SLangParser parser = new SLangParser(tokens);

    SLangParseTreeVisitor slangVisitor = new SLangParseTreeVisitor();
    Tree slangAST = slangVisitor.visit(parser.slangFile());
    return slangAST;
  }


  private static class SLangParseTreeVisitor extends SLangBaseVisitor<Tree> {
    @Override
    public Tree visitSlangFile(@NotNull SLangParser.SlangFileContext ctx) {
      SLangTypeVisitor typeVisitor = new SLangTypeVisitor();
      List<Tree> children = ctx.typeDeclaration()
          .stream()
          .map(type -> type.accept(typeVisitor))
          .collect(toList());

      return new NativeTreeImpl(null, new SNativeKind(), children);
    }
  }

  private static class SLangTypeVisitor extends SLangBaseVisitor<Tree> {
    @Override
    public Tree visitTypeDeclaration(SLangParser.TypeDeclarationContext ctx) {
      return new IdentifierImpl(null,ctx.getText());
    }
  }
}