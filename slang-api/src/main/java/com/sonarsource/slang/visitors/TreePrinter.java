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
package com.sonarsource.slang.visitors;

import com.sonarsource.slang.api.AssignmentExpressionTree;
import com.sonarsource.slang.api.BinaryExpressionTree;
import com.sonarsource.slang.api.IdentifierTree;
import com.sonarsource.slang.api.LiteralTree;
import com.sonarsource.slang.api.NativeTree;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.api.VariableDeclarationTree;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TreePrinter {

  private TreePrinter() {
  }

  public static String tree2string(List<Tree> trees) {
    return trees.stream().map(TreePrinter::tree2string).collect(Collectors.joining("\n"));
  }

  public static String tree2string(Tree tree) {
    StringBuilder sb = new StringBuilder();
    TreeVisitor<TreeContext> visitor = new TreeVisitor<>();
    visitor.register(Tree.class, (ctx, t) -> {
      IntStream.range(0, ctx.ancestors().size()).forEach(i -> sb.append("  "));
      sb.append(t.getClass().getSimpleName());
      if (t instanceof BinaryExpressionTree) {
        sb.append(" ").append(((BinaryExpressionTree) t).operator().name());
      } else if (t instanceof AssignmentExpressionTree) {
        sb.append(" ").append(((AssignmentExpressionTree) t).operator().name());
      } else if (t instanceof LiteralTree) {
        sb.append(" ").append(((LiteralTree) t).value());
      } else if (t instanceof IdentifierTree) {
        sb.append(" ").append(((IdentifierTree) t).name());
      } else if (t instanceof NativeTree) {
        sb.append(" ").append(((NativeTree) t).nativeKind());
      } else if (t instanceof VariableDeclarationTree) {
        sb.append(" ").append(((VariableDeclarationTree) t).identifier().name());
      }
      sb.append("\n");
    });
    visitor.scan(new TreeContext(), tree);
    return sb.toString();
  }

}
