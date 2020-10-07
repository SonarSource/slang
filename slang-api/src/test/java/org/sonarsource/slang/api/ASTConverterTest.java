package org.sonarsource.slang.api;

import org.junit.Test;
import org.sonarsource.slang.impl.IdentifierTreeImpl;

import static org.junit.Assert.assertSame;

public class ASTConverterTest {

  private static final ASTConverter DUMMY_CONVERTER = new ASTConverter() {
    private final Tree SIMPLE_TREE = new IdentifierTreeImpl(null, "name");
    @Override
    public Tree parse(String content) {
      return SIMPLE_TREE;
    }
  };

  @Test
  public void parse_with_file_has_no_effect_by_default() {
    assertSame(DUMMY_CONVERTER.parse(""), DUMMY_CONVERTER.parse("", "file name"));
  }

}
