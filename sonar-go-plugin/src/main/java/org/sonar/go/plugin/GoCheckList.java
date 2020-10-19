package org.sonar.go.plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.sonarsource.slang.checks.BadClassNameCheck;
import org.sonarsource.slang.checks.CheckList;
import org.sonarsource.slang.checks.CodeAfterJumpCheck;
import org.sonarsource.slang.checks.CollapsibleIfStatementsCheck;
import org.sonarsource.slang.checks.OneStatementPerLineCheck;
import org.sonarsource.slang.checks.TabsCheck;
import org.sonarsource.slang.checks.UnusedFunctionParameterCheck;
import org.sonarsource.slang.checks.UnusedLocalVariableCheck;
import org.sonarsource.slang.checks.UnusedPrivateMethodCheck;

import org.sonar.go.checks.CodeAfterJumpGoCheck;
import org.sonar.go.checks.OneStatementPerLineGoCheck;

public class GoCheckList {

  private GoCheckList() {
    // utility class
  }

  static final Class[] GO_CHECK_BLACK_LIST = {
    BadClassNameCheck.class,
    // Can not enable rule S1066, as Go if-trees are containing an initializer, not well handled by SLang
    CollapsibleIfStatementsCheck.class,
    TabsCheck.class,
    // Can not enable rule S1172 since it it not possible to identify overridden function with modifier (to avoid FP)
    UnusedFunctionParameterCheck.class,
    UnusedLocalVariableCheck.class,
    UnusedPrivateMethodCheck.class,
    // Replaced by language specific test
    CodeAfterJumpCheck.class,
    OneStatementPerLineCheck.class
  };

  private static final Collection<Class<?>> GO_LANGUAGE_SPECIFIC_CHECKS = Arrays.asList(
    CodeAfterJumpGoCheck.class,
    OneStatementPerLineGoCheck.class
  );

  public static List<Class<?>> checks() {
    List<Class<?>> list = new ArrayList<>(CheckList.excludeChecks(GO_CHECK_BLACK_LIST));
    list.addAll(GO_LANGUAGE_SPECIFIC_CHECKS);
    return list;
  }
}
