package org.sonarsource.slang.impl;

import org.junit.Test;
import org.sonarsource.slang.api.PlaceHolderTree;
import org.sonarsource.slang.api.Token;

import static org.assertj.core.api.Assertions.assertThat;

public class PlaceHolderTreeImplTest {

  @Test
  public void test_place_holder() {
    TokenImpl keyword = new TokenImpl(new TextRangeImpl(1, 0, 1, 1), "_", Token.Type.OTHER);
    PlaceHolderTree placeHolderTree = new PlaceHolderTreeImpl(null, keyword);
    assertThat(placeHolderTree.children()).isEmpty();
    assertThat(placeHolderTree.placeHolderToken().text()).isEqualTo("_");
    assertThat(placeHolderTree.placeHolderToken().type()).isEqualTo(Token.Type.OTHER);
  }
}
