package org.sonar.go.converter;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarsource.slang.api.ParseException;
import org.sonarsource.slang.api.ReturnTree;
import org.sonarsource.slang.api.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.go.converter.GoConverter.DefaultCommand.getExecutableForCurrentOS;

public class GoConverterTest {

  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();


  @Test
  public void test_parse_return() {
    GoConverter converter = new GoConverter(Paths.get("build", "tmp").toFile());
    Tree tree = converter.parse("package main\nfunc foo() {return 42}");
    List<Tree> returnList = tree.descendants().filter(t -> t instanceof ReturnTree).collect(Collectors.toList());
    assertThat(returnList).hasSize(1);
  }

  @Test
  public void parse_error() {
    exceptionRule.expect(ParseException.class);
    exceptionRule.expectMessage("Parser returned non-zero exit value: 2");

    GoConverter converter = new GoConverter(Paths.get("build", "tmp").toFile());
    converter.parse("$!#@");
  }

  @Test
  public void load_invalid_executable_path() throws IOException {
    exceptionRule.expect(IllegalStateException.class);
    exceptionRule.expectMessage("invalid-exe-path binary not found on class path");

    GoConverter.DefaultCommand.getBytesFromResource("invalid-exe-path");
  }

  @Test
  public void executable_for_current_os() {
    assertThat(getExecutableForCurrentOS("Linux")).isEqualTo("sonar-go-to-slang-linux-amd64");
    assertThat(getExecutableForCurrentOS("Windows 10")).isEqualTo("sonar-go-to-slang-windows-amd64.exe");
    assertThat(getExecutableForCurrentOS("Mac OS X")).isEqualTo("sonar-go-to-slang-darwin-amd64");
  }
}
