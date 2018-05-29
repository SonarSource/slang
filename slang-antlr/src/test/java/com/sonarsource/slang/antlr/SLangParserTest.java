package com.sonarsource.slang.antlr;

import com.sonarsource.slang.parser.SLangLexer;
import com.sonarsource.slang.parser.SLangParser;
import java.io.IOException;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class SLangParserTest {

  @Test
  public void testFile() throws IOException {
    SLangLexer lexer = new SLangLexer(CharStreams.fromFileName("src/test/resources/test.slang"));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    SLangParser parser = new SLangParser(tokens);
    SLangParser.SlangFileContext context = parser.slangFile();
    assertThat(context.children, notNullValue());
    assertThat(context.children.isEmpty(), is(false));
  }

}
