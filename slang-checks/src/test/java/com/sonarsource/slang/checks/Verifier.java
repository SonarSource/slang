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
package com.sonarsource.slang.checks;

import com.sonarsource.checks.verifier.CommentParser;
import com.sonarsource.checks.verifier.SingleFileVerifier;
import com.sonarsource.slang.api.TextPointer;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.checks.api.CheckContext;
import com.sonarsource.slang.checks.api.InitContext;
import com.sonarsource.slang.checks.api.SlangCheck;
import com.sonarsource.slang.visitors.TreeContext;
import com.sonarsource.slang.visitors.TreeVisitor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Verifier {

  private static final Path BASE_DIR = Paths.get("src/test/resources/com/sonarsource/slang/checks/");

  public static void verify(String fileName, SlangCheck check) {
    createVerifier(fileName, check).assertOneOrMoreIssues();
  }

  private static SingleFileVerifier createVerifier(String fileName, SlangCheck check) {
    Path path = BASE_DIR.resolve(fileName);
    SingleFileVerifier verifier = SingleFileVerifier.create(path, UTF_8);

    Tree root = null; // TODO parse

    TestContext ctx = new TestContext(verifier);
    check.initialize(ctx);
    ctx.scan(root);

    CommentParser commentParser = CommentParser.create().addSingleLineCommentSyntax("//");
    commentParser.parseInto(path, verifier);
    return verifier;
  }

  private static class TestContext extends TreeContext implements InitContext, CheckContext {

    private final TreeVisitor<TestContext> visitor;
    private final SingleFileVerifier verifier;

    public TestContext(SingleFileVerifier verifier) {
      this.verifier = verifier;
      visitor = new TreeVisitor<>();
    }

    public void scan(@Nullable Tree root) {
      visitor.scan(this, root);
    }

    @Override
    public <T extends Tree> void register(Class<T> cls, BiConsumer<CheckContext, T> consumer) {
      visitor.register(cls, (ctx, node) -> consumer.accept(this, node));
    }

    @Override
    public void reportIssue(Tree tree, String message) {
      TextPointer start = tree.textRange().start();
      TextPointer end = tree.textRange().end();
      verifier.reportIssue(message).onRange(start.line(), start.lineOffset(), end.line(), end.lineOffset());
    }

  }

}
