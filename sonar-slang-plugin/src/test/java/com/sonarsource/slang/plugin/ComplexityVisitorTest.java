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

import com.sonarsource.slang.api.BlockTree;
import com.sonarsource.slang.api.ClassDeclarationTree;
import com.sonarsource.slang.api.FunctionDeclarationTree;
import com.sonarsource.slang.api.HasTextRange;
import com.sonarsource.slang.api.LoopTree;
import com.sonarsource.slang.api.MatchCaseTree;
import com.sonarsource.slang.api.Token;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.kotlin.InputFileContext;
import com.sonarsource.slang.parser.SLangConverter;
import java.io.File;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.duplications.internal.pmd.TokensLine;

import static org.assertj.core.api.Assertions.assertThat;

public class ComplexityVisitorTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();
  private InputFileContext ctx;

  @Before
  public void setUp() throws Exception {
    File file = tempFolder.newFile();
    SensorContextTester sensorContext = SensorContextTester.create(tempFolder.getRoot());
    DefaultInputFile inputFile = new TestInputFileBuilder("moduleKey", file.getName())
      .setContents("")
      .build();
    ctx = new InputFileContext(sensorContext, inputFile);
  }

  @Test
  public void test_matchCases() throws Exception {
    String content = "match (a) {" +
      "      0 -> return \"none\";" +
      "      1 -> return \"one\";" +
      "      2 -> return \"many\";" +
      "      else -> return \"it's complicated\";" +
      "    };";
    Tree root = new SLangConverter().parse(content);
    List<HasTextRange> trees = new ComplexityVisitor().complexityTrees(ctx, root);
    assertThat(trees).hasSize(4);
    assertThat(trees).allMatch(tree -> tree instanceof MatchCaseTree);
  }

  @Test
  public void test_functions_with_conditional() throws Exception {
    String content = "void fun foo (a) {" +
      "      if (a == 2) {" +
      "        print(a + 1);" +
      "      } else {" +
      "        print(a);" +
      "      };" +
      "    }";
    Tree root = new SLangConverter().parse(content);
    List<HasTextRange> trees = new ComplexityVisitor().complexityTrees(ctx, root);
    assertThat(trees).hasSize(3);
    assertThat(trees.get(0)).isInstanceOf(FunctionDeclarationTree.class);
    assertThat(trees.get(1)).isInstanceOf(Token.class);
    assertThat(trees.get(2)).isInstanceOf(Token.class);
  }

  @Test
  public void test_class() throws Exception {
    String content = "class foo {}";
    Tree root = new SLangConverter().parse(content);
    List<HasTextRange> trees = new ComplexityVisitor().complexityTrees(ctx, root);
    assertThat(trees).hasSize(1);
    assertThat(trees.get(0)).isInstanceOf(ClassDeclarationTree.class);
  }

  @Test
  public void test_loops() throws Exception {
    String content =
      "for (var x = list) { " +
      "  while (x > y) { " +
      "    x = x-1;" +
      "  };" +
      "};";
    Tree root = new SLangConverter().parse(content);
    List<HasTextRange> trees = new ComplexityVisitor().complexityTrees(ctx, root);
    assertThat(trees).hasSize(2);
    assertThat(trees).allMatch(tree -> tree instanceof LoopTree);

    content = "do { x = x-1; } while (x > y);";
    root = new SLangConverter().parse(content);
    trees = new ComplexityVisitor().complexityTrees(ctx, root);
    assertThat(trees).hasSize(1);
    assertThat(trees).allMatch(tree -> tree instanceof LoopTree);

  }

}
