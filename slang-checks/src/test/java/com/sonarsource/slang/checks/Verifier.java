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
import com.sonarsource.slang.api.TextRange;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.checks.api.CheckContext;
import com.sonarsource.slang.checks.api.InitContext;
import com.sonarsource.slang.checks.api.SecondaryLocation;
import com.sonarsource.slang.checks.api.SlangCheck;
import com.sonarsource.slang.parser.SLangConverter;
import com.sonarsource.slang.visitors.TreeContext;
import com.sonarsource.slang.visitors.TreeVisitor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Verifier {

  private static final Path BASE_DIR = Paths.get("src/test/resources/com/sonarsource/slang/checks/");

  public static void verify(String fileName, SlangCheck check) {
    createVerifier(fileName, check).assertOneOrMoreIssues();
  }

  public static void verifyNoIssue(String fileName, SlangCheck check) {
    createVerifier(fileName, check).assertNoIssues();
  }

  private static SingleFileVerifier createVerifier(String fileName, SlangCheck check) {
    Path path = BASE_DIR.resolve(fileName);
    SingleFileVerifier verifier = SingleFileVerifier.create(path, UTF_8);

    String testFileContent = readFile(path);
    Tree root = new SLangConverter().parse(testFileContent);

    TestContext ctx = new TestContext(verifier, fileName, testFileContent);
    check.initialize(ctx);
    ctx.scan(root);

    CommentParser commentParser = CommentParser.create().addSingleLineCommentSyntax("//");
    commentParser.parseInto(path, verifier);
    return verifier;
  }

  private static String readFile(Path path) {
    try {
      return new String(Files.readAllBytes(path), UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read " + path, e);
    }
  }

  private static class TestContext extends TreeContext implements InitContext, CheckContext {

    private final TreeVisitor<TestContext> visitor;
    private final SingleFileVerifier verifier;
    private final String filename;
    private String testFileContent;

    public TestContext(SingleFileVerifier verifier, String filename, String testFileContent) {
      this.verifier = verifier;
      this.filename = filename;
      this.testFileContent = testFileContent;
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
      reportIssue(tree, message, Collections.emptyList());
    }

    @Override
    public void reportIssue(Tree tree, String message, SecondaryLocation secondaryLocation) {
      reportIssue(tree, message, Collections.singletonList(secondaryLocation));
    }

    @Override
    public String filename() {
      return filename;
    }

    @Override
    public String fileContent() {
      return testFileContent;
    }

    @Override
    public void reportIssue(TextRange textRange, String message) {
      reportIssue(textRange, message, Collections.emptyList(), null);
    }

    @Override
    public void reportIssue(Tree tree, String message, List<SecondaryLocation> secondaryLocations) {
      reportIssue(tree, message, secondaryLocations, null);
    }

    @Override
    public void reportIssue(Tree tree, String message, List<SecondaryLocation> secondaryLocations, @Nullable Double gap) {
      reportIssue(tree.metaData().textRange(), message, secondaryLocations, gap);
    }

    public void reportFileIssue(String message) {
      reportFileIssue(message, null);
    }

    @Override
    public void reportFileIssue(String message, @Nullable Double gap) {
      verifier.reportIssue(message).onFile().withGap(gap);
    }

    private void reportIssue(TextRange textRange, String message, List<SecondaryLocation> secondaryLocations, @Nullable Double gap) {
      TextPointer start = textRange.start();
      TextPointer end = textRange.end();
      SingleFileVerifier.Issue issue = verifier
        .reportIssue(message)
        .onRange(start.line(), start.lineOffset() + 1, end.line(), end.lineOffset())
        .withGap(gap);
      secondaryLocations.forEach(secondary -> issue.addSecondary(
        secondary.textRange.start().line(),
        secondary.textRange.start().lineOffset() + 1,
        secondary.textRange.end().line(),
        secondary.textRange.end().lineOffset(),
        secondary.message));
    }

  }

}
