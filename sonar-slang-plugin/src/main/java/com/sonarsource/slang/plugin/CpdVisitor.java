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
package com.sonarsource.slang.plugin;

import com.sonarsource.slang.api.ImportTree;
import com.sonarsource.slang.api.Token;
import com.sonarsource.slang.api.TopLevelTree;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.kotlin.InputFileContext;
import com.sonarsource.slang.visitors.TreeVisitor;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;

public class CpdVisitor extends TreeVisitor<InputFileContext> {

  private NewCpdTokens cpdTokens;

  public CpdVisitor() {
    register(TopLevelTree.class, (ctx, tree) -> {

      Set<Token> importTokens = tree.descendants()
        .filter(ImportTree.class::isInstance)
        .flatMap(t -> t.metaData().tokens().stream())
        .collect(Collectors.toSet());

      for (Token token : tree.metaData().tokens()) {
        if (!importTokens.contains(token)) {
          String text = token.type() == Token.Type.STRING_LITERAL ? "LITERAL" : token.text();
          cpdTokens.addToken(ctx.textRange(token.textRange()), text);
        }
      }

    });
  }

  @Override
  protected void before(InputFileContext ctx, Tree root) {
    cpdTokens = ctx.sensorContext.newCpdTokens().onFile(ctx.inputFile);
  }

  @Override
  protected void after(InputFileContext ctx, Tree root) {
    cpdTokens.save();
  }
}
