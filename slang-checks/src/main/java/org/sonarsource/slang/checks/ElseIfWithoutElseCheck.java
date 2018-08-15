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

import org.sonar.check.Rule;
import org.sonarsource.slang.api.IfTree;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;
import org.sonarsource.slang.impl.TextRangeImpl;

@Rule(key = "S126")
public class ElseIfWithoutElseCheck implements SlangCheck {

  private static final String MESSAGE = "Add the missing \"else\" clause.";

  @Override
  public void initialize(InitContext init) {
    init.register(IfTree.class, (ctx, ifTree) -> {
      Tree elseBranch = ifTree.elseBranch();
      if (elseBranch instanceof IfTree) {
        IfTree nestedIfTree = (IfTree) elseBranch;
        if (nestedIfTree.elseBranch() == null) {
          Token elseToken = ifTree.elseKeyword();
          Token ifToken = nestedIfTree.ifKeyword();
          TextRange textRange = new TextRangeImpl(
            elseToken.textRange().start(),
            ifToken.textRange().end()
          );
          ctx.reportIssue(textRange, MESSAGE);
        }
      }
    });

  }
}
