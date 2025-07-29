/*
 * SonarSource SLang
 * Copyright (C) 2018-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.slang.checks;

import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonarsource.slang.api.BinaryExpressionTree;
import org.sonarsource.slang.api.BlockTree;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.FunctionInvocationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.IfTree;
import org.sonarsource.slang.api.LoopTree;
import org.sonarsource.slang.api.ParameterTree;
import org.sonarsource.slang.api.ReturnTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.checks.api.CheckContext;
import org.sonarsource.slang.checks.api.InitContext;
import org.sonarsource.slang.checks.api.SlangCheck;

@Rule(key = "S-slice-equal")
public class SliceEqualSimplificationCheck implements SlangCheck {

  private static final String MESSAGE = "Use slices.Equal() instead of custom slice comparison";

  @Override
  public void initialize(InitContext init) {
    init.register(FunctionDeclarationTree.class, (ctx, functionDeclarationTree) -> {
      if (isSliceEqualityFunction(functionDeclarationTree)) {
        ctx.reportIssue(functionDeclarationTree.name(), MESSAGE);
      }
    });
  }

  private boolean isSliceEqualityFunction(FunctionDeclarationTree function) {
    // Check if function has exactly 2 parameters
    List<Tree> parameters = function.formalParameters();
    if (parameters.size() != 2) {
      return false;
    }

    // Check if function body contains slice equality pattern
    BlockTree body = function.body();
    if (body == null) {
      return false;
    }

    // Look for simple slice equality patterns
    String[] paramNames = getParameterNames(parameters);
    if (paramNames[0] == null || paramNames[1] == null) {
      return false;
    }

    // Check if the function contains len() calls with our parameters
    return containsLenComparison(body, paramNames);
  }

  private boolean looksLikeSliceParameter(Tree parameter) {
    // This is a simplified check - in a real implementation, we'd need to 
    // check the actual type information to confirm it's a slice type
    // For now, we'll use heuristics based on common naming patterns
    if (parameter instanceof ParameterTree) {
      ParameterTree paramTree = (ParameterTree) parameter;
      IdentifierTree identifier = paramTree.identifier();
      if (identifier != null) {
        String name = identifier.name();
        // Common slice parameter names
        return name.contains("slice") || name.contains("array") || 
               name.matches(".*[sS]lice.*") || name.matches(".*[aA]rray.*") ||
               name.length() == 1; // single letter parameters like 'a', 'b'
      }
    }
    return false;
  }

  private String[] getParameterNames(List<Tree> parameters) {
    String[] names = new String[2];
    for (int i = 0; i < 2 && i < parameters.size(); i++) {
      if (parameters.get(i) instanceof ParameterTree) {
        ParameterTree paramTree = (ParameterTree) parameters.get(i);
        IdentifierTree identifier = paramTree.identifier();
        if (identifier != null) {
          names[i] = identifier.name();
        }
      }
    }
    return names;
  }

  private boolean containsSliceEqualityPattern(BlockTree body, String[] paramNames) {
    List<Tree> statements = body.statementOrExpressions();
    
    // Look for common slice equality patterns:
    // 1. Length comparison: if len(a) != len(b) { return false }
    // 2. Element-wise comparison loop
    // 3. Return true at the end
    
    boolean hasLengthCheck = false;
    boolean hasElementLoop = false;
    boolean hasReturnTrue = false;

    for (Tree statement : statements) {
      if (isLengthComparisonStatement(statement, paramNames)) {
        hasLengthCheck = true;
      } else if (isElementComparisonLoop(statement, paramNames)) {
        hasElementLoop = true;
      } else if (isReturnTrueStatement(statement)) {
        hasReturnTrue = true;
      }
    }

    // A slice equality function typically has all three patterns
    return hasLengthCheck && hasElementLoop && hasReturnTrue;
  }

  private boolean isLengthComparisonStatement(Tree statement, String[] paramNames) {
    // Look for: if len(a) != len(b) { return false }
    if (statement instanceof IfTree) {
      IfTree ifTree = (IfTree) statement;
      Tree condition = ifTree.condition();
      
      if (condition instanceof BinaryExpressionTree) {
        BinaryExpressionTree binaryExpr = (BinaryExpressionTree) condition;
        if (binaryExpr.operator() == BinaryExpressionTree.Operator.NOT_EQUAL_TO) {
          // Check if both sides are len() calls on our parameters
          return isLenCall(binaryExpr.leftOperand(), paramNames) && 
                 isLenCall(binaryExpr.rightOperand(), paramNames);
        }
      }
    }
    return false;
  }

  private boolean isLenCall(Tree expression, String[] paramNames) {
    // Look for len(paramName) pattern
    if (expression instanceof FunctionInvocationTree) {
      FunctionInvocationTree funcCall = (FunctionInvocationTree) expression;
      Tree functionName = funcCall.memberSelect();
      
      if (functionName instanceof IdentifierTree) {
        IdentifierTree identifier = (IdentifierTree) functionName;
        if ("len".equals(identifier.name())) {
          // Check if the argument is one of our parameters
          List<Tree> arguments = funcCall.arguments();
          if (arguments.size() == 1 && arguments.get(0) instanceof IdentifierTree) {
            String argName = ((IdentifierTree) arguments.get(0)).name();
            return argName.equals(paramNames[0]) || argName.equals(paramNames[1]);
          }
        }
      }
    }
    return false;
  }

  private boolean isElementComparisonLoop(Tree statement, String[] paramNames) {
    // Look for loop that compares elements: for i := range a { if a[i] != b[i] { return false } }
    if (statement instanceof LoopTree) {
      LoopTree loop = (LoopTree) statement;
      Tree body = loop.body();
      
      if (body instanceof BlockTree) {
        BlockTree loopBody = (BlockTree) body;
        List<Tree> loopStatements = loopBody.statementOrExpressions();
        
        // Look for element comparison inside the loop
        for (Tree loopStatement : loopStatements) {
          if (isElementComparisonStatement(loopStatement, paramNames)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private boolean isElementComparisonStatement(Tree statement, String[] paramNames) {
    // Look for: if a[i] != b[i] { return false }
    if (statement instanceof IfTree) {
      IfTree ifTree = (IfTree) statement;
      Tree condition = ifTree.condition();
      
      if (condition instanceof BinaryExpressionTree) {
        BinaryExpressionTree binaryExpr = (BinaryExpressionTree) condition;
        if (binaryExpr.operator() == BinaryExpressionTree.Operator.NOT_EQUAL_TO) {
          // This is a simplified check - in a real implementation we'd need to
          // verify that both sides are array/slice access expressions with the same index
          return containsParameterReference(binaryExpr.leftOperand(), paramNames) &&
                 containsParameterReference(binaryExpr.rightOperand(), paramNames);
        }
      }
    }
    return false;
  }

  private boolean containsParameterReference(Tree expression, String[] paramNames) {
    // Recursively check if the expression contains a reference to one of our parameters
    if (expression instanceof IdentifierTree) {
      String name = ((IdentifierTree) expression).name();
      return name.equals(paramNames[0]) || name.equals(paramNames[1]);
    }
    
    // Check children recursively
    for (Tree child : expression.children()) {
      if (containsParameterReference(child, paramNames)) {
        return true;
      }
    }
    
    return false;
  }

  private boolean isReturnTrueStatement(Tree statement) {
    // Look for: return true
    if (statement instanceof ReturnTree) {
      ReturnTree returnTree = (ReturnTree) statement;
      Tree body = returnTree.body();
      
      if (body instanceof IdentifierTree) {
        return "true".equals(((IdentifierTree) body).name());
      }
    }
    return false;
  }

  private boolean containsLenComparison(BlockTree body, String[] paramNames) {
    // Simple check: look for any len() function calls with our parameter names
    List<Tree> statements = body.statementOrExpressions();
    
    for (Tree statement : statements) {
      if (containsLenCallWithParams(statement, paramNames)) {
        return true;
      }
    }
    return false;
  }

  private boolean containsLenCallWithParams(Tree tree, String[] paramNames) {
    // Recursively search for len() calls with our parameters
    if (tree instanceof FunctionInvocationTree) {
      FunctionInvocationTree funcCall = (FunctionInvocationTree) tree;
      Tree functionName = funcCall.memberSelect();
      
      if (functionName instanceof IdentifierTree) {
        IdentifierTree identifier = (IdentifierTree) functionName;
        if ("len".equals(identifier.name())) {
          // Check if any argument matches our parameter names
          List<Tree> arguments = funcCall.arguments();
          for (Tree arg : arguments) {
            if (arg instanceof IdentifierTree) {
              String argName = ((IdentifierTree) arg).name();
              if (argName.equals(paramNames[0]) || argName.equals(paramNames[1])) {
                return true;
              }
            }
          }
        }
      }
    }

    // Recursively check children
    for (Tree child : tree.children()) {
      if (containsLenCallWithParams(child, paramNames)) {
        return true;
      }
    }
    
    return false;
  }
} 