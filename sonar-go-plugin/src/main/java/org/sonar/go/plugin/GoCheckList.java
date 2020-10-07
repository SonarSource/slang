package org.sonar.go.plugin;

import java.util.List;
import org.sonarsource.slang.checks.BadClassNameCheck;
import org.sonarsource.slang.checks.CheckList;
import org.sonarsource.slang.checks.CollapsibleIfStatementsCheck;
import org.sonarsource.slang.checks.TabsCheck;
import org.sonarsource.slang.checks.UnusedFunctionParameterCheck;
import org.sonarsource.slang.checks.UnusedLocalVariableCheck;
import org.sonarsource.slang.checks.UnusedPrivateMethodCheck;

public class GoCheckList {

  private GoCheckList() {
    // utility class
  }

  private static final Class[] GO_CHECK_BLACK_LIST = {
    BadClassNameCheck.class,
    // Can not enable rule S1066, as Go if-trees are containing an initializer, not well handled by SLang
    CollapsibleIfStatementsCheck.class,
    TabsCheck.class,
    // Can not enable rule S1172 since it it not possible to identify overridden function with modifier (to avoid FP)
    UnusedFunctionParameterCheck.class,
    UnusedLocalVariableCheck.class,
    UnusedPrivateMethodCheck.class
  };

  public static List<Class<?>> checks() {
    return CheckList.excludeChecks(GO_CHECK_BLACK_LIST);
  }
}
