package org.sonar.go.converter;

import org.sonarsource.slang.api.ASTConverter;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.sonarsource.slang.impl.TextRangeImpl;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * FIXME to be implemented
 */
public class GoConverter implements ASTConverter {

  @Override
  public Tree parse(String content) {
    return new Tree() {
      @Override
      public List<Tree> children() {
        return Collections.emptyList();
      }

      @Override
      public TreeMetaData metaData() {
        return new TreeMetaData() {
          @Override
          public TextRange textRange() {
            return new TextRangeImpl(1,1,2,4);
          }

          @Override
          public List<Comment> commentsInside() {
            return Collections.emptyList();
          }

          @Override
          public List<Token> tokens() {
            return Collections.emptyList();
          }

          @Override
          public Set<Integer> linesOfCode() {
            return Collections.emptySet();
          }
        };
      }
    };
  }
}
