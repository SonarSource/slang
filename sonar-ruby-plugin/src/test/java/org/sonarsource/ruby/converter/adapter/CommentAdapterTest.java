package org.sonarsource.ruby.converter.adapter;

import org.junit.jupiter.api.Test;
import org.sonarsource.analyzer.commons.TokenLocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.ruby.converter.adapter.CommentAdapter.getContentText;
import static org.sonarsource.ruby.converter.adapter.CommentAdapter.getContentTextRange;
import static org.sonarsource.slang.testing.RangeAssert.assertRange;

class CommentAdapterTest {
  @Test
  void get_content_text() {
    assertThat(getContentText("=begin\r\nXX\r\n=end\r\n")).isEqualTo("\r\nXX\r\n");
    assertThat(getContentText("=begin\nXX\n=end\n")).isEqualTo("\nXX\n");
    assertThat(getContentText("")).isEmpty();
  }

  @Test
  void get_content_text_range() {
    String text = "=begin\r\nABC\r\n=end\r\n";
    assertRange(getContentTextRange(text, new TokenLocation(1, 0, text), "\r\nABC\r\n")).hasRange(1, 6, 3, 0);
    text = "=begin\nA\n=end\n";
    assertRange(getContentTextRange(text, new TokenLocation(1, 0, text), "\nA\n")).hasRange(1, 6, 3, 0);
    text = "=begin\n=end\n";
    assertRange(getContentTextRange(text, new TokenLocation(1, 0, text), "\n")).hasRange(1, 6, 2, 0);
  }
}
