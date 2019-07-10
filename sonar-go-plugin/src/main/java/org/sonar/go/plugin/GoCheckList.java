package org.sonar.go.plugin;

import java.util.List;
import org.sonarsource.slang.checks.BadClassNameCheck;
import org.sonarsource.slang.checks.CheckList;
import org.sonarsource.slang.checks.CodeAfterJumpCheck;
import org.sonarsource.slang.checks.CollapsibleIfStatementsCheck;
import org.sonarsource.slang.checks.OneStatementPerLineCheck;
import org.sonarsource.slang.checks.RedundantParenthesesCheck;
import org.sonarsource.slang.checks.TabsCheck;
import org.sonarsource.slang.checks.TooDeeplyNestedStatementsCheck;
import org.sonarsource.slang.checks.TooManyParametersCheck;
import org.sonarsource.slang.checks.UnusedFunctionParameterCheck;
import org.sonarsource.slang.checks.UnusedLocalVariableCheck;
import org.sonarsource.slang.checks.UnusedPrivateMethodCheck;
import org.sonarsource.slang.checks.VariableAndParameterNameCheck;
import org.sonarsource.slang.checks.WrongAssignmentOperatorCheck;

public class GoCheckList {

  private GoCheckList() {
    // utility class
  }

  private static final Class[] GO_CHECK_BLACK_LIST = {
    BadClassNameCheck.class,
    CodeAfterJumpCheck.class,
    // Can not enable rule S1066, as Go if-trees are containing an initializer, not well handled by SLang
    CollapsibleIfStatementsCheck.class,
    OneStatementPerLineCheck.class,
    RedundantParenthesesCheck.class,
    TabsCheck.class,
    TooDeeplyNestedStatementsCheck.class,
    TooManyParametersCheck.class,
    UnusedFunctionParameterCheck.class,
    UnusedLocalVariableCheck.class,
    UnusedPrivateMethodCheck.class,
    VariableAndParameterNameCheck.class,
    WrongAssignmentOperatorCheck.class
  };

  public static List<Class> checks() {
    return CheckList.excludeChecks(GO_CHECK_BLACK_LIST);
  }
}
