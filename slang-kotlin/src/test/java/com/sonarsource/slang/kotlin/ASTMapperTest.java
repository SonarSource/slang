package com.sonarsource.slang.kotlin;

import com.sonarsource.slang.api.BinaryExpressionTree;
import com.sonarsource.slang.api.IdentifierTree;
import com.sonarsource.slang.api.LiteralTree;
import com.sonarsource.slang.api.NativeTree;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.visitors.TreeContext;
import com.sonarsource.slang.visitors.TreeVisitor;
import java.io.IOException;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class ASTMapperTest {

  @Test
  public void testFile() throws IOException {
    Tree tree = ASTMapper.fromFile("src/test/resources/test.kt");
    assertThat(tree, notNullValue());
  }

  @Test
  public void testString() {
    Tree tree = ASTMapper.fromString("fun function1() = 2 >= 1");
    assertThat(tree, notNullValue());
  }

  @Test
  public void printTree() {
    TreeVisitor<TreeContext> visitor = new TreeVisitor<>();
    visitor.register(Tree.class, (ctx, tree) -> System.out.println(formatTree(tree)));
    visitor.scan(new TreeContext(), ASTMapper.fromString("fun function1() = 2 >= 1"));
    assertThat(true, is(true));
  }

  private String formatTree(Tree tree) {
    if (tree instanceof BinaryExpressionTree) {
      return "BinaryOp: " + ((BinaryExpressionTree) tree).operator().name() + " [children count: " + tree.children().size() + "]";
    } else if (tree instanceof IdentifierTree) {
      return "- IdentifierTree: " + ((IdentifierTree) tree).name();
    } else if (tree instanceof LiteralTree) {
      return "- Literal: " + ((LiteralTree) tree).value();
    } else if (tree instanceof NativeTree) {
      return "Native: [children count: " + tree.children().size() + "]";
    } else {
      return "Unknown";
    }
  }

}
