package org.sonar.go.converter;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarsource.slang.api.ClassDeclarationTree;
import org.sonarsource.slang.api.IntegerLiteralTree;
import org.sonarsource.slang.api.LoopTree;
import org.sonarsource.slang.api.ParseException;
import org.sonarsource.slang.api.ReturnTree;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.api.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.go.converter.GoConverter.DefaultCommand.getExecutableForCurrentOS;

public class GoConverterTest {

  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();
  
  @Test
  public void test_parse_return() {
    GoConverter converter = new GoConverter(Paths.get("build", "tmp").toFile());
    Tree tree = converter.parse("package main\nfunc foo() {return 42}");
    List<ReturnTree> returnList = getReturnsList(tree);
    assertThat(returnList).hasSize(1);

    checkIntegerValue(returnList.get(0), "42");
  }
  
  @Test
  public void test_parse_binary_notation() {
    GoConverter converter = new GoConverter(Paths.get("build", "tmp").toFile());
    Tree tree = converter.parse("package main\nfunc foo() {return 0b_0010_1010}");
    List<ReturnTree> returnList = getReturnsList(tree);
    assertThat(returnList).hasSize(1);

    checkIntegerValue(returnList.get(0), "_0010_1010");
  }

  @Test
  public void test_parse_imaginary_literals() {
    GoConverter converter = new GoConverter(Paths.get("build", "tmp").toFile());
    Tree tree = converter.parse("package main\nfunc foo() {return 6.67428e-11i}");
    List<ReturnTree> returnList = getReturnsList(tree);
    assertThat(returnList).hasSize(1);
  }

  @Test
  public void test_parse_embed_overlapping_interfaces() {
    GoConverter converter = new GoConverter(Paths.get("build", "tmp").toFile());
    Tree tree = converter.parse("package main\ntype A interface{\n     DoX() string\n}\ntype B interface{\n     DoX() \n}\ntype AB interface{\n    A\n    B\n}");
    List<Tree> classList = tree.descendants()
      .filter(t -> t instanceof ClassDeclarationTree)
      .collect(Collectors.toList());
    assertThat(classList).hasSize(3);
  }
  
  @Test
  public void test_parse_infinite_for() {
    GoConverter converter = new GoConverter(Paths.get("build", "tmp").toFile());
    Tree tree = converter.parse("package main\nfunc foo() {for {}}");
    List<Tree> returnList = tree.descendants().filter(t -> t instanceof LoopTree).collect(Collectors.toList());
    assertThat(returnList).hasSize(1);
  }

  @Test
  public void parse_error() {
    exceptionRule.expect(ParseException.class);
    exceptionRule.expectMessage("Go parser external process returned non-zero exit value: 2");

    GoConverter converter = new GoConverter(Paths.get("build", "tmp").toFile());
    converter.parse("$!#@");
  }

  @Test
  public void invalid_command() {
    exceptionRule.expect(ParseException.class);
    exceptionRule.expectMessage(containsString("Cannot run program \"invalid-command\""));

    GoConverter.Command command = mock(GoConverter.Command.class);
    when(command.getCommand()).thenReturn(Collections.singletonList("invalid-command"));
    GoConverter converter = new GoConverter(command);
    converter.parse("package main\nfunc foo() {}");
  }

  @Test
  public void parse_accepted_big_file() {
    GoConverter converter = new GoConverter(Paths.get("build", "tmp").toFile());
    String code = "package main\n" +
      "func foo() {\n" +
      "}\n";
    String bigCode = code + new String(new char[700_000 - code.length()]).replace("\0", "\n");
    Tree tree = converter.parse(bigCode);
    assertThat(tree).isInstanceOf(TopLevelTree.class);
  }

  @Test
  public void parse_rejected_big_file() {
    exceptionRule.expect(ParseException.class);
    exceptionRule.expectMessage("The file size is too big and should be excluded, its size is 700028 (maximum allowed is 700000 bytes)");

    GoConverter converter = new GoConverter(Paths.get("build", "tmp").toFile());
    String code = "package main\n" +
      "func foo() {\n" +
      "}\n";
    String bigCode = code + new String(new char[700_000]).replace("\0", "\n");
    converter.parse(bigCode);
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

  private List<ReturnTree> getReturnsList(Tree tree) {
    return tree.descendants()
      .filter(t -> t instanceof ReturnTree)
      .map(ReturnTree.class::cast)
      .collect(Collectors.toList());
  }

  private void checkIntegerValue(ReturnTree returnTree, String s) {
    IntegerLiteralTree integerLiteralTree = (IntegerLiteralTree) returnTree.body();
    assertThat(integerLiteralTree.getNumericPart()).isEqualTo(s);
  }
}
