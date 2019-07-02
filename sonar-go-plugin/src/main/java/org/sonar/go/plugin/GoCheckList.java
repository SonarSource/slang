package org.sonar.go.plugin;

import java.util.List;
import org.sonarsource.slang.checks.AllBranchesIdenticalCheck;
import org.sonarsource.slang.checks.BadClassNameCheck;
import org.sonarsource.slang.checks.BadFunctionNameCheck;
import org.sonarsource.slang.checks.BooleanInversionCheck;
import org.sonarsource.slang.checks.BooleanLiteralCheck;
import org.sonarsource.slang.checks.CheckList;
import org.sonarsource.slang.checks.CodeAfterJumpCheck;
import org.sonarsource.slang.checks.CollapsibleIfStatementsCheck;
import org.sonarsource.slang.checks.DuplicateBranchCheck;
import org.sonarsource.slang.checks.DuplicatedFunctionImplementationCheck;
import org.sonarsource.slang.checks.ElseIfWithoutElseCheck;
import org.sonarsource.slang.checks.EmptyBlockCheck;
import org.sonarsource.slang.checks.EmptyFunctionCheck;
import org.sonarsource.slang.checks.FunctionCognitiveComplexityCheck;
import org.sonarsource.slang.checks.HardcodedCredentialsCheck;
import org.sonarsource.slang.checks.IdenticalBinaryOperandCheck;
import org.sonarsource.slang.checks.IdenticalConditionsCheck;
import org.sonarsource.slang.checks.IfConditionalAlwaysTrueOrFalseCheck;
import org.sonarsource.slang.checks.MatchCaseTooBigCheck;
import org.sonarsource.slang.checks.MatchWithoutElseCheck;
import org.sonarsource.slang.checks.NestedMatchCheck;
import org.sonarsource.slang.checks.OneStatementPerLineCheck;
import org.sonarsource.slang.checks.RedundantParenthesesCheck;
import org.sonarsource.slang.checks.SelfAssignmentCheck;
import org.sonarsource.slang.checks.TabsCheck;
import org.sonarsource.slang.checks.TooComplexExpressionCheck;
import org.sonarsource.slang.checks.TooDeeplyNestedStatementsCheck;
import org.sonarsource.slang.checks.TooLongFunctionCheck;
import org.sonarsource.slang.checks.TooManyCasesCheck;
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
    AllBranchesIdenticalCheck.class,
    BadClassNameCheck.class,
    BadFunctionNameCheck.class,
    BooleanInversionCheck.class,
    BooleanLiteralCheck.class,
    CodeAfterJumpCheck.class,
    CollapsibleIfStatementsCheck.class,
    DuplicateBranchCheck.class,
    DuplicatedFunctionImplementationCheck.class,
    ElseIfWithoutElseCheck.class,
    EmptyBlockCheck.class,
    EmptyFunctionCheck.class,
    FunctionCognitiveComplexityCheck.class,
    HardcodedCredentialsCheck.class,
    IdenticalBinaryOperandCheck.class,
    IdenticalConditionsCheck.class,
    IfConditionalAlwaysTrueOrFalseCheck.class,
    MatchCaseTooBigCheck.class,
    MatchWithoutElseCheck.class,
    NestedMatchCheck.class,
    OneStatementPerLineCheck.class,
    RedundantParenthesesCheck.class,
    SelfAssignmentCheck.class,
    TabsCheck.class,
    TooComplexExpressionCheck.class,
    TooDeeplyNestedStatementsCheck.class,
    TooLongFunctionCheck.class,
    TooManyCasesCheck.class,
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
