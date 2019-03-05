/*
 * SonarSource SLang
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.ruby.converter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.jruby.RubySymbol;
import org.sonarsource.ruby.converter.impl.RubyPartialExceptionHandlingTree;
import org.sonarsource.slang.api.AssignmentExpressionTree;
import org.sonarsource.slang.api.BinaryExpressionTree;
import org.sonarsource.slang.api.BinaryExpressionTree.Operator;
import org.sonarsource.slang.api.BlockTree;
import org.sonarsource.slang.api.CatchTree;
import org.sonarsource.slang.api.ClassDeclarationTree;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.IfTree;
import org.sonarsource.slang.api.JumpTree.JumpKind;
import org.sonarsource.slang.api.LiteralTree;
import org.sonarsource.slang.api.LoopTree;
import org.sonarsource.slang.api.MatchCaseTree;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.TextPointer;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.sonarsource.slang.api.UnaryExpressionTree;
import org.sonarsource.slang.impl.AssignmentExpressionTreeImpl;
import org.sonarsource.slang.impl.BinaryExpressionTreeImpl;
import org.sonarsource.slang.impl.BlockTreeImpl;
import org.sonarsource.slang.impl.CatchTreeImpl;
import org.sonarsource.slang.impl.ClassDeclarationTreeImpl;
import org.sonarsource.slang.impl.ExceptionHandlingTreeImpl;
import org.sonarsource.slang.impl.FunctionDeclarationTreeImpl;
import org.sonarsource.slang.impl.IdentifierTreeImpl;
import org.sonarsource.slang.impl.IfTreeImpl;
import org.sonarsource.slang.impl.IntegerLiteralTreeImpl;
import org.sonarsource.slang.impl.JumpTreeImpl;
import org.sonarsource.slang.impl.LiteralTreeImpl;
import org.sonarsource.slang.impl.LoopTreeImpl;
import org.sonarsource.slang.impl.MatchCaseTreeImpl;
import org.sonarsource.slang.impl.MatchTreeImpl;
import org.sonarsource.slang.impl.NativeTreeImpl;
import org.sonarsource.slang.impl.ParameterTreeImpl;
import org.sonarsource.slang.impl.ParenthesizedExpressionTreeImpl;
import org.sonarsource.slang.impl.ReturnTreeImpl;
import org.sonarsource.slang.impl.StringLiteralTreeImpl;
import org.sonarsource.slang.impl.TextRangeImpl;
import org.sonarsource.slang.impl.TextRanges;
import org.sonarsource.slang.impl.ThrowTreeImpl;
import org.sonarsource.slang.impl.TreeMetaDataProvider;
import org.sonarsource.slang.impl.UnaryExpressionTreeImpl;
import org.sonarsource.slang.impl.VariableDeclarationTreeImpl;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class RubyVisitor {

  private static final String KEYWORD_ATTRIBUTE = "keyword";
  private static final List<String> EXCEPTION_BLOCK_TYPES = asList("resbody", "rescue", "ensure");
  private static final Map<String, Operator> BINARY_OPERATOR_MAP;
  private static final Map<String, UnaryExpressionTree.Operator> UNARY_OPERATOR_MAP;

  static {
    BINARY_OPERATOR_MAP = new HashMap<>();
    BINARY_OPERATOR_MAP.put("==", BinaryExpressionTree.Operator.EQUAL_TO);
    BINARY_OPERATOR_MAP.put("!=", BinaryExpressionTree.Operator.NOT_EQUAL_TO);
    BINARY_OPERATOR_MAP.put("<", BinaryExpressionTree.Operator.LESS_THAN);
    BINARY_OPERATOR_MAP.put(">", BinaryExpressionTree.Operator.GREATER_THAN);
    BINARY_OPERATOR_MAP.put("<=", BinaryExpressionTree.Operator.LESS_THAN_OR_EQUAL_TO);
    BINARY_OPERATOR_MAP.put(">=", BinaryExpressionTree.Operator.GREATER_THAN_OR_EQUAL_TO);
    BINARY_OPERATOR_MAP.put("||", BinaryExpressionTree.Operator.CONDITIONAL_OR);
    BINARY_OPERATOR_MAP.put("&&", BinaryExpressionTree.Operator.CONDITIONAL_AND);
    BINARY_OPERATOR_MAP.put("+", BinaryExpressionTree.Operator.PLUS);
    BINARY_OPERATOR_MAP.put("-", BinaryExpressionTree.Operator.MINUS);
    BINARY_OPERATOR_MAP.put("*", BinaryExpressionTree.Operator.TIMES);
    BINARY_OPERATOR_MAP.put("/", BinaryExpressionTree.Operator.DIVIDED_BY);

    UNARY_OPERATOR_MAP = new HashMap<>();
    UNARY_OPERATOR_MAP.put("!", UnaryExpressionTree.Operator.NEGATE);
    UNARY_OPERATOR_MAP.put("+@", UnaryExpressionTree.Operator.PLUS);
    UNARY_OPERATOR_MAP.put("-@", UnaryExpressionTree.Operator.MINUS);
    // Note: Ruby has no decrement/increment operator
  }

  private static final Set<String> LOCAL_SCOPE_TYPES = Collections.unmodifiableSet(
    new HashSet<>(Arrays.asList("def", "defs", "class")));

  private final TreeMetaDataProvider metaDataProvider;
  private Deque<String> nodeTypeStack = new ArrayDeque<>();
  private Deque<Set<String>> localVariables = new ArrayDeque<>();

  public RubyVisitor(TreeMetaDataProvider metaDataProvider) {
    this.metaDataProvider = metaDataProvider;
    localVariables.push(new HashSet<>());
  }

  public void beforeVisit(AstNode node) {
    nodeTypeStack.push(node.type());
    if (LOCAL_SCOPE_TYPES.contains(node.type())) {
      localVariables.push(new HashSet<>());
    }
  }

  public void afterVisit(AstNode node) {
    nodeTypeStack.pop();
    if (LOCAL_SCOPE_TYPES.contains(node.type())) {
      localVariables.pop();
    }
  }

  public Tree visitNode(AstNode node, List<?> children) {
    switch (node.type()) {
      case "and":
        return createLogicalOperation(node, children, Operator.CONDITIONAL_AND);
      case "arg":
      case "optarg":
      case "restarg":
      case "kwarg":
      case "kwoptarg":
      case "kwrestarg":
      case "blockarg":
      case "procarg0":
      case "shadowarg":
        // note obj-c arguments are not supported https://github.com/whitequark/parser/blob/master/doc/AST_FORMAT.md#objective-c-arguments
        return createParameterTree(node, children);
      case "begin":
        return createFromBeginNode(node, children);
      case "kwbegin":
        return createFromKwBeginNode(node, children);
      case "case":
        return createMatchTree(node, children);
      case "casgn":
        return createFromCasgn(node, children);
      case "const":
        return createFromConst(node, children);
      case "class":
        return createClassDeclarationTree(node, children);
      case "def":
      case "defs":
        return createFunctionDeclarationTree(node, children);
      case "cvasgn":
      case "gvasgn":
      case "ivasgn":
      case "lvasgn":
        return createFromAssign(node, children);
      case "if":
        return createIfTree(node, children);
      case "indexasgn":
        return createFromIndexasgn(node, children);
      case "int":
        return createIntegerLiteralTree(node);
      case "cvar":
      case "lvar":
      case "ivar":
        return createFromVar(node, children);
      case "masgn":
        return createFromMasgn(node, children);
      case "mlhs":
        return createFromMlhs(node, children);
      case "op_asgn":
        return createFromOpAsgn(node, children);
      case "break":
        return createJumpTree(node, children, JumpKind.BREAK);
      case "next":
        return createJumpTree(node, children, JumpKind.CONTINUE);
      case "return":
        return createReturnTree(node, children);
      case "or":
        return createLogicalOperation(node, children, Operator.CONDITIONAL_OR);
      case "send":
        return createFromSendNode(node, children);
      case "true":
      case "false":
        return new LiteralTreeImpl(metaData(node), node.type());
      case "when":
        return createCaseTree(node, children);
      case "str":
        return createStringLiteralTree(node, children);
      case "rescue":
        return createExceptionHandlingTree(node, children);
      case "resbody":
        return createCatchTree(node, children);
      case "ensure":
        return updateExceptionHandlingWithFinally(node, children);
      case "while_post":
      case "until_post":
        return createLoopTree(node, children, LoopTree.LoopKind.DOWHILE);
      case "while":
      case "until":
        return createLoopTree(node, children, LoopTree.LoopKind.WHILE);
      case "for":
        return createForLoopTree(node, children);
      default:
        return createNativeTree(node, children);
    }
  }

  private Tree createLoopTree(AstNode node, List<?> children, LoopTree.LoopKind kind) {
    Tree condition = (Tree) children.get(0);
    Tree body = (Tree) children.get(1);
    if (body == null) {
      TextRange endRange = getTokenByAttribute(node, "end").textRange();
      body = createEmptyBlockTree(condition.textRange().end(), endRange.start());
    }

    return new LoopTreeImpl(metaData(node),
      condition,
      body,
      kind,
      getTokenByAttribute(node, KEYWORD_ATTRIBUTE));
  }

  private Tree createForLoopTree(AstNode node, List<?> children) {
    Tree variables = (Tree) children.get(0);
    Tree expression = (Tree) children.get(1);
    TextRange conditionRange = TextRanges.merge(asList(variables.textRange(), expression.textRange()));
    RubyNativeKind nativeKind = new RubyNativeKind(node.type());

    Tree condition = new NativeTreeImpl(metaDataProvider.metaData(conditionRange), nativeKind, asList(variables, expression));
    Tree body = (Tree) children.get(2);
    if (body == null) {
      TextRange endRange = getTokenByAttribute(node, "end").textRange();
      body = createEmptyBlockTree(condition.textRange().end(), endRange.start());
    }

    return new LoopTreeImpl(metaData(node),
      condition,
      body,
      LoopTree.LoopKind.FOR,
      getTokenByAttribute(node, KEYWORD_ATTRIBUTE));
  }

  private Tree createFromKwBeginNode(AstNode node, List<?> children) {
    if (children.size() == 1 && children.get(0) instanceof RubyPartialExceptionHandlingTree) {
      // this begin is used as a "begin...rescue...end" or "begin...ensure...end" block
      RubyPartialExceptionHandlingTree partialExceptionTree = (RubyPartialExceptionHandlingTree) children.get(0);
      TreeMetaData treeMetaData = metaData(node);
      List<CatchTree> catchTrees = partialExceptionTree.catchBlocks();
      Tree tryBlock = partialExceptionTree.tryBlock();
      if (tryBlock == null) {
        List<Tree> exceptionChildren = partialExceptionTree.children();
        TextPointer to = exceptionChildren.isEmpty() ? treeMetaData.textRange().end() : exceptionChildren.get(0).textRange().start();
        tryBlock = createEmptyBlockTree(treeMetaData.textRange().start(), to);
      }

      Tree finallyBlock = partialExceptionTree.finallyBlock();
      if (!treeMetaData.commentsInside().isEmpty()) {
        // Update range for empty "rescue" and "ensure" clauses that have potential comments inside them
        TextRange endRange = getTokenByAttribute(node, "end").textRange();
        if (finallyBlock instanceof BlockTree && ((BlockTree) finallyBlock).statementOrExpressions().isEmpty()) {
          TextPointer from = finallyBlock.metaData().textRange().start();
          finallyBlock = createEmptyBlockTree(from, endRange.start());
          endRange = finallyBlock.textRange();
        }

        catchTrees = updateEmptyBlockRanges(
          catchTrees,
          endRange,
          catchTree -> catchTree.catchBlock() instanceof BlockTree && ((BlockTree) catchTree.catchBlock()).statementOrExpressions().isEmpty(),
          (catchTree, newBlockTree) -> new CatchTreeImpl(newBlockTree.metaData(), catchTree.catchParameter(), newBlockTree, catchTree.keyword()));
      }

      return new ExceptionHandlingTreeImpl(treeMetaData,
        tryBlock,
        treeMetaData.tokens().get(0),
        catchTrees,
        finallyBlock);
    }
    return createFromBeginNode(node, children);
  }


  private Tree updateExceptionHandlingWithFinally(AstNode node, List<?> children) {
    if (!isValidTryCatchBlock()) {
      // Ignore "ensure" clauses that are not part of a "begin...ensure...end" block
      return createNativeTree(node, children);
    }

    Tree finallyBlock = (Tree) children.get(1);
    if (finallyBlock == null) {
      Token keyword = getTokenByAttribute(node, KEYWORD_ATTRIBUTE);
      finallyBlock = createEmptyBlockTree(keyword.textRange().start(), metaData(node).textRange().end());
    }

    Tree body = (Tree) children.get(0);
    RubyPartialExceptionHandlingTree exceptionHandlingTree;
    if (body instanceof RubyPartialExceptionHandlingTree) {
      exceptionHandlingTree = (RubyPartialExceptionHandlingTree) body;
    } else {
      exceptionHandlingTree = new RubyPartialExceptionHandlingTree(body, emptyList());
    }
    exceptionHandlingTree.setFinallyBlock(finallyBlock);

    return exceptionHandlingTree;
  }

  private Tree createExceptionHandlingTree(AstNode node, List<?> children) {
    if (!isValidTryCatchBlock()) {
      // Ignore "rescue" clauses that are not part of "begin...rescue...end" or "begin...rescue...ensure...end" blocks
      return createNativeTree(node, children);
    }

    List<CatchTree> catchTrees = children.stream()
      .skip(1)
      .filter(tree -> tree instanceof CatchTree)
      .map(CatchTree.class::cast)
      .collect(Collectors.toList());

    TreeMetaData treeMetaData = metaData(node);
    lookForTokenByAttribute(node, "else")
      .map(elseToken -> {
          Tree lastClause = (Tree) children.get(children.size() - 1);
          if (lastClause == null) {
            lastClause = createEmptyBlockTree(elseToken.textRange().start(), treeMetaData.textRange().end());
          }
          TreeMetaData fullElseClauseMeta = metaDataProvider.metaData(TextRanges.merge(asList(elseToken.textRange(), lastClause.textRange())));
          return new CatchTreeImpl(fullElseClauseMeta, null, lastClause, elseToken);
        }
      )
      .ifPresent(catchTrees::add);

    Tree tryBlock = ((Tree) children.get(0));
    return new RubyPartialExceptionHandlingTree(tryBlock, catchTrees);
  }

  private Tree createCatchTree(AstNode node, List<?> children) {
    if (!isValidTryCatchBlock()) {
      // Ignore "rescue" clauses that are not part of "begin...rescue...end" or "begin...rescue...ensure...end" blocks
      return createNativeTree(node, children);
    }

    Token keyword = getTokenByAttribute(node, KEYWORD_ATTRIBUTE);

    List<Tree> catchParameterChildren = children.stream()
      .limit(2)
      .filter(Objects::nonNull)
      .map(Tree.class::cast)
      .collect(Collectors.toList());

    Tree catchParameter = null;
    if (catchParameterChildren.size() == 1) {
      catchParameter = catchParameterChildren.get(0);
    } else if (!catchParameterChildren.isEmpty()) {
      List<TextRange> textRanges = catchParameterChildren.stream().map(Tree::textRange).collect(Collectors.toList());
      TextRange catchParameterRange = TextRanges.merge(textRanges);
      catchParameter = new NativeTreeImpl(metaDataProvider.metaData(catchParameterRange), new RubyNativeKind(node.type()), catchParameterChildren);
    }

    Tree body = (Tree) children.get(2);
    if (body == null) {
      body = new BlockTreeImpl(metaData(node), emptyList());
    }

    return new CatchTreeImpl(metaData(node), catchParameter, body, keyword);
  }

  private Tree createFromAssign(AstNode node, List<?> children) {
    IdentifierTree identifier = identifierFromSymbol(node, (RubySymbol) children.get(0));

    if (children.size() == 2) {
      return assignmentOrDeclaration(node, identifier, (Tree) children.get(1));
    }

    return identifier;
  }

  private Tree createFromCasgn(AstNode node, List<?> children) {
    IdentifierTree identifier = identifierFromSymbol(node, (RubySymbol) children.get(1));

    if (children.get(0) == null) {
      if (children.size() == 3) {
        return assignmentOrDeclaration(node, identifier, (Tree) children.get(2));
      } else {
        return identifier;
      }
    }

    return createNativeTree(node, children);
  }

  private Tree assignmentOrDeclaration(AstNode node, IdentifierTree identifier, Tree rhs) {
    return assignmentOrDeclaration(metaData(node), identifier, rhs);
  }

  /**
   * @return if this is the first time we see this identifier in current scope we create {@link org.sonarsource.slang.api.VariableDeclarationTree}
   * otherwise {@link AssignmentExpressionTree} is created
   */
  private Tree assignmentOrDeclaration(TreeMetaData metaData, IdentifierTree identifier, Tree rhs) {
    if (isLocalVariable(identifier) && localVariables.peek().add(identifier.name())) {
      return new VariableDeclarationTreeImpl(
        metaData,
        identifier,
        null,
        rhs, false);
    } else {
      return new AssignmentExpressionTreeImpl(
        metaData,
        AssignmentExpressionTree.Operator.EQUAL,
        identifier,
        rhs);
    }
  }

  private static boolean isLocalVariable(IdentifierTree identifier) {
    return !identifier.name().startsWith("@")
      && !identifier.name().startsWith("$")
      // by convention unused variables are prefixed by "_", we want to ignore them
      && !identifier.name().startsWith("_");
  }


  private Tree createFromOpAsgn(AstNode node, List<?> children) {
    Token operatorToken = getTokenByAttribute(node, "operator");
    if (operatorToken.text().equals("+")) {
      return new AssignmentExpressionTreeImpl(
        metaData(node),
        AssignmentExpressionTree.Operator.PLUS_EQUAL,
        (Tree) children.get(0),
        (Tree) children.get(2));
    }

    return createNativeTree(node, children);
  }

  private Tree createFromMlhs(AstNode node, List<?> children) {
    // replacing native kind to support tree equivalence
    return createNativeTree(node, children, "array");
  }

  private Tree createFromMasgn(AstNode node, List<?> children) {
    List<IdentifierTree> lhsChild = getChildIfArray((Tree) children.get(0))
        .stream()
        .filter(child -> child instanceof IdentifierTree)
        .map(IdentifierTree.class::cast)
        .collect(Collectors.toList());

    List<Tree> rhsChild = getChildIfArray((Tree) children.get(1));

    if(lhsChild.isEmpty() || rhsChild.isEmpty() || lhsChild.size() != rhsChild.size()) {
      return new AssignmentExpressionTreeImpl(
          metaData(node),
          AssignmentExpressionTree.Operator.EQUAL,
          (Tree) children.get(0),
          (Tree) children.get(1));
    } else {
      List<Tree> assignments = new ArrayList<>();
      for (int i = 0; i < lhsChild.size(); i++) {
        IdentifierTree id = lhsChild.get(i);
        assignments.add(assignmentOrDeclaration(id.metaData(), id, rhsChild.get(i)));
      }
      return createNativeTree(node, assignments);
    }
  }

  private static List<Tree> getChildIfArray(Tree node) {
    if(node instanceof NativeTree) {
      NativeTree nativeNode = (NativeTree) node;
      if(nativeNode.nativeKind().equals(new RubyNativeKind("array"))) {
        return nativeNode.children();
      }
    }
    return Collections.emptyList();
  }

  private Tree createFromVar(AstNode node, List<?> children) {
    return identifierFromSymbol(node, (RubySymbol) children.get(0));
  }

  private Tree createFromIndexasgn(AstNode node, List<?> children) {
    if (children.size() > 2) {
      TreeMetaData metaData = metaData(node);
      Token firstToken = metaData.tokens().get(0);
      TextRange closeBracketRange = node.textRangeForAttribute("end");

      TextRange lhsRange = TextRanges.merge(Arrays.asList(firstToken.textRange(), closeBracketRange));
      TreeMetaData lhsMeta = metaDataProvider.metaData(lhsRange);

      // we will assume that all children are trees on LHS, this should always be the case
      // if not, we might miss some part of the tree, but it should not matter much
      List<Tree> lhsChildren = children.subList(0, children.size() - 1).stream()
        .filter(child -> child instanceof Tree)
        .map(child -> (Tree) child)
        .collect(Collectors.toList());

      // such ruby native kind is required to have tree equivalence
      Tree lhs = new NativeTreeImpl(lhsMeta, new RubyNativeKind("index"), lhsChildren);

      return new AssignmentExpressionTreeImpl(
        metaData,
        AssignmentExpressionTree.Operator.EQUAL,
        lhs,
        (Tree) children.get(children.size() - 1));
    }

    return createNativeTree(node, children, "index");
  }

  private Tree createParameterTree(AstNode node, List<?> children) {
    if (children.isEmpty() || !(children.get(0) instanceof RubySymbol)) {
      // unnamed splat argument
      return createNativeTree(node, children);
    }
    IdentifierTree identifierTree = identifierFromSymbol(node, (RubySymbol) children.get(0));

    Tree defaultValue = null;
    if ("optarg".equals(node.type()) || "kwoptarg".equals(node.type())) {
      defaultValue = (Tree) children.get(1);
    }
    return new ParameterTreeImpl(metaData(node), identifierTree, null, defaultValue);
  }

  private Tree createCaseTree(AstNode node, List<?> children) {
    Tree expression = ((Tree) children.get(0));
    Tree body = ((Tree) children.get(1));
    if (body == null) {
      body = new BlockTreeImpl(metaData(node), Collections.emptyList());
    }
    return new MatchCaseTreeImpl(metaData(node), expression, body);
  }

  private Tree createMatchTree(AstNode node, List<?> children) {
    Token caseKeywordToken = getTokenByAttribute(node, KEYWORD_ATTRIBUTE);

    List<MatchCaseTree> whens = children.stream()
      .filter(tree -> tree instanceof MatchCaseTree)
      .map(tree -> (MatchCaseTree) tree)
      .collect(Collectors.toList());

    TreeMetaData treeMetaData = metaData(node);

    lookForTokenByAttribute(node, "else")
      .map(elseKeywordToken -> {
        Tree elseBody = (Tree) children.get(children.size() - 1);
        if (elseBody == null) {
          elseBody = createEmptyBlockTree(elseKeywordToken.textRange().end(), treeMetaData.textRange().end());
        }
        TextRange textRange = new TextRangeImpl(elseKeywordToken.textRange().start(), elseBody.textRange().end());
        return new MatchCaseTreeImpl(metaDataProvider.metaData(textRange), null, elseBody);
      })
      .ifPresent(whens::add);

    if (!treeMetaData.commentsInside().isEmpty()) {
      // Update range for empty "when" clauses that have potential comments inside them
      TextRange endRange = node.textRangeForAttribute("end");
      whens = updateEmptyBlockRanges(
        whens,
        endRange,
        caseTree -> caseTree.body() instanceof BlockTree && ((BlockTree) caseTree.body()).statementOrExpressions().isEmpty(),
        (caseTree, newBlockTree) -> new MatchCaseTreeImpl(newBlockTree.metaData(), caseTree.expression(), newBlockTree));
    }

    return new MatchTreeImpl(treeMetaData, (Tree) children.get(0), whens, caseKeywordToken);
  }

  private Tree createStringLiteralTree(AstNode node, List<?> children) {
    if (hasDynamicStringParent()) {
      return createNativeTree(node, children);
    }
    String value = (String) children.get(0);
    // __FILE__ macro is resolved to filename we set when calling ruby parser
    if (RubyConverter.FILENAME.equals(value)) {
      return createNativeTree(node, children);
    }
    TextRange begin = node.textRangeForAttribute("begin");
    TextRange end = node.textRangeForAttribute("end");
    TreeMetaData treeMetaData;
    if (begin != null && end != null) {
      treeMetaData = metaDataProvider.metaData(new TextRangeImpl(begin.start(), end.end()));
    } else {
      treeMetaData = metaData(node);
    }
    return new StringLiteralTreeImpl(treeMetaData, node.source(), value);
  }

  private boolean hasDynamicStringParent() {
    return nodeTypeStack.stream().anyMatch("dstr"::equals);
  }

  private boolean isValidTryCatchBlock() {
    Iterator<String> iterator = nodeTypeStack.iterator();
    while (iterator.hasNext()) {
      String parentType = iterator.next();
      if (!EXCEPTION_BLOCK_TYPES.contains(parentType)) {
        return "kwbegin".equals(parentType);
      }
    }
    return false;
  }

  private <T extends Tree> List<T> updateEmptyBlockRanges(List<T> trees,
                                                          @Nullable TextRange endRange,
                                                          Predicate<T> hasEmptyBlock,
                                                          BiFunction<T, BlockTree, T> createNewTree) {
    TextRange nextRange = endRange;
    List<T> newTrees = new ArrayList<>(trees.size());
    ListIterator<T> iterator = trees.listIterator(trees.size());
    while (iterator.hasPrevious()) {
      T tree = iterator.previous();
      if (nextRange != null && hasEmptyBlock.test(tree)) {
        // update range of empty blocks to include potential following comments
        BlockTree newBlockTree = createEmptyBlockTree(tree.textRange().start(), nextRange.start());
        T newCatchTree = createNewTree.apply(tree, newBlockTree);
        newTrees.add(newCatchTree);
      } else {
        newTrees.add(tree);
      }
      nextRange = tree.textRange();
    }
    Collections.reverse(newTrees);
    return newTrees;
  }

  private Tree createLogicalOperation(AstNode node, List<?> children, Operator operator) {
    Tree left = (Tree) children.get(0);
    Tree right = (Tree) children.get(1);
    Token operatorToken = getTokenByAttribute(node, "operator");
    return new BinaryExpressionTreeImpl(metaData(node), operator, operatorToken, left, right);
  }

  private Tree createFromBeginNode(AstNode node, List<?> children) {
    Optional<Token> beginToken = lookForTokenByAttribute(node, "begin");
    Optional<Token> endToken = lookForTokenByAttribute(node, "end");
    if (beginToken.isPresent() && endToken.isPresent() && children.size() == 1 && beginToken.get().text().equals("(")) {
      return new ParenthesizedExpressionTreeImpl(metaData(node), ((Tree) children.get(0)), beginToken.get(), endToken.get());
    }

    List<Tree> nonNullChildren = convertChildren(node, children);
    return new BlockTreeImpl(metaData(node), nonNullChildren);
  }

  private Tree createFromSendNode(AstNode node, List<?> children) {
    Object callee = children.get(1);
    if (callee instanceof RubySymbol) {
      String calleeSymbol = ((RubySymbol) callee).asJavaString();
      if (UNARY_OPERATOR_MAP.containsKey(calleeSymbol)) {
        Tree argument = (Tree) children.get(0);
        return new UnaryExpressionTreeImpl(metaData(node), UNARY_OPERATOR_MAP.get(calleeSymbol), argument);
      } else if (BINARY_OPERATOR_MAP.containsKey(calleeSymbol)) {
        Tree left = (Tree) children.get(0);
        Tree right = (Tree) children.get(2);
        Token operatorToken = getTokenByAttribute(node, "selector");
        return new BinaryExpressionTreeImpl(metaData(node), BINARY_OPERATOR_MAP.get(calleeSymbol), operatorToken, left, right);
      } else if ("raise".equals(calleeSymbol)) {
        return createThrowTree(node, children);
      }
    }

    return createNativeTree(node, children);
  }

  private Tree createThrowTree(AstNode node, List<?> children) {
    Tree body = null;
    if (children.size() > 3) {
      List<Tree> raiseChildren = convertChildren(node, children.subList(2, children.size()));
      body = new NativeTreeImpl(metaData(node), new RubyNativeKind("raise"), raiseChildren);
    } else if (children.size() > 2) {
      body = (Tree) children.get(2);
    }
    return new ThrowTreeImpl(metaData(node), getTokenByAttribute(node, "selector"), body);
  }

  private FunctionDeclarationTree createFunctionDeclarationTree(AstNode node, List<?> children) {
    boolean isSingletonMethod = node.type().equals("defs");
    List<Tree> nativeChildren;
    if (isSingletonMethod) {
      nativeChildren = singletonList((Tree) children.get(0));
    } else {
      nativeChildren = emptyList();
    }

    int childrenIndexShift = isSingletonMethod ? 1 : 0;

    IdentifierTree name = identifierFromSymbol(node, (RubySymbol) children.get(0 + childrenIndexShift));

    List<Tree> parameters;
    Object args = children.get(1 + childrenIndexShift);
    if (args != null) {
      parameters = ((Tree) args).children();
    } else {
      parameters = emptyList();
    }

    BlockTree body;
    Tree rubyBodyBlock = (Tree) children.get(2 + childrenIndexShift);
    if (rubyBodyBlock instanceof BlockTree) {
      body = (BlockTree) rubyBodyBlock;
    } else if (rubyBodyBlock != null) {
      List<Tree> statements = singletonList(rubyBodyBlock);
      body = new BlockTreeImpl(rubyBodyBlock.metaData(), statements);
    } else {
      body = new BlockTreeImpl(metaData(node), emptyList());
    }
    return new FunctionDeclarationTreeImpl(metaData(node),
      emptyList(),
      null,
      name,
      parameters,
      body,
      nativeChildren);
  }

  private ClassDeclarationTree createClassDeclarationTree(AstNode node, List<?> children) {
    NativeTree nativeTree = createNativeTree(node, children);
    if (nativeTree == null) {
      throw new IllegalStateException("Failed to create ClassDeclarationTree for node " + node.asString());
    }

    Object nameChild = children.get(0);
    IdentifierTree classNameIdentifier;
    if (nameChild instanceof IdentifierTree) {
      classNameIdentifier = (IdentifierTree) nameChild;
    } else {
      List<Tree> nameChildren = ((Tree) nameChild).children();
      classNameIdentifier = (IdentifierTree) nameChildren.get(nameChildren.size() - 1);
    }

    return new ClassDeclarationTreeImpl(metaData(node), classNameIdentifier, nativeTree);
  }

  private LiteralTree createIntegerLiteralTree(AstNode node) {
    return new IntegerLiteralTreeImpl(metaData(node), node.source());
  }

  private Tree createFromConst(AstNode node, List<?> children) {
    IdentifierTree identifier = identifierFromSymbol(node, (RubySymbol) children.get(1));

    if (children.get(0) == null) {
      return identifier;
    } else {
      ArrayList<Object> newChildren = new ArrayList<>(children);
      newChildren.set(newChildren.size() - 1, identifier);
      return createNativeTree(node, newChildren);
    }
  }

  private Tree createIfTree(AstNode node, List<?> children) {
    Optional<Token> mainKeyword = lookForTokenByAttribute(node, KEYWORD_ATTRIBUTE);
    if (!mainKeyword.isPresent()) {
      // "ternary"
      Tree condition = (Tree) children.get(0);
      Tree thenBranch = (Tree) children.get(1);
      Tree elseBranch = (Tree) children.get(2);

      Token ifToken = previousToken(thenBranch.textRange(), "?");
      Token elseToken = previousToken(elseBranch.textRange(), ":");

      return new IfTreeImpl(metaData(node), condition, thenBranch, elseBranch, ifToken, elseToken);
    } else if (mainKeyword.get().text().equals("unless")) {
      // "unless" are not considered as "IfTree" for now
      return createNativeTree(node, children);
    }

    Optional<Token> elseKeywordOptional = lookForTokenByAttribute(node, "else");
    Token elseKeyword = elseKeywordOptional.orElse(null);
    Tree thenBranch = getThenBranch(node, mainKeyword.get(), elseKeyword, (Tree) children.get(1));
    Tree elseBranch = getElseBranch(node, elseKeyword, (Tree) children.get(2));

    return new IfTreeImpl(metaData(node), (Tree) children.get(0), thenBranch, elseBranch, mainKeyword.get(), elseKeyword);
  }

  private Token previousToken(TextRange textRange, String expectedTokenValue) {
    return metaDataProvider.previousToken(textRange, expectedTokenValue)
      .orElseThrow(() -> new IllegalStateException(
        String.format("Unable to locate token with value '%s' before position [Line:%d,Offset:%d].",
          expectedTokenValue, textRange.start().line(), textRange.start().lineOffset())));
  }

  private Tree getThenBranch(AstNode node, Token mainKeyword, @Nullable Token elseKeyword, @Nullable Tree thenBranch) {
    if (thenBranch != null) {
      return thenBranch;
    } else if (elseKeyword == null) {
      // empty "then" branch body and no "else" branch
      return new BlockTreeImpl(metaData(node), emptyList());
    } else {
      // empty "then" branch, with a "else" branch. Meta for empty "then" block will be "if..." until next "else" keyword
      return createEmptyBlockTree(mainKeyword.textRange().start(), elseKeyword.textRange().start());
    }
  }

  private Tree getElseBranch(AstNode node, @Nullable Token elseKeyword, @Nullable Tree elseBranch) {
    if (elseBranch != null) {
      // "else" branch has normal body
      boolean isFinalElseBranch = elseKeyword != null && elseKeyword.text().equals("else");
      if (isFinalElseBranch && elseBranch instanceof IfTree) {
        // This is not an "elsif" statement so we wrap the tree in a block to differentiate between "else; if" and "elsif"
        return new BlockTreeImpl(elseBranch.metaData(), singletonList(elseBranch));
      }
      return elseBranch;
    } else if (elseKeyword != null) {
      // "else" branch present but with empty body. Meta for empty "else" block will be "else...end" part
      return createEmptyBlockTree(elseKeyword.textRange().start(), metaData(node).textRange().end());
    } else {
      // no "else" branch
      return null;
    }
  }

  private BlockTree createEmptyBlockTree(TextPointer from, TextPointer to) {
    TextRange emptyBlockRange = new TextRangeImpl(from, to);
    return new BlockTreeImpl(metaDataProvider.metaData(emptyBlockRange), emptyList());
  }

  private Tree createJumpTree(AstNode node, List<?> children, JumpKind kind) {
    if (!children.isEmpty()) {
      return createNativeTree(node, children);
    }
    Token keyword = getTokenByAttribute(node, KEYWORD_ATTRIBUTE);
    return new JumpTreeImpl(metaData(node), keyword, kind, null);
  }

  private Tree createReturnTree(AstNode node, List<?> children) {
    Token keyword = getTokenByAttribute(node, KEYWORD_ATTRIBUTE);
    Tree expression = null;
    if (children.size() == 1) {
      expression = (Tree) children.get(0);
    } else if (!children.isEmpty()) {
      List<Tree> childTrees = convertChildren(node, children);
      TextRange childRange = TextRanges.merge(childTrees.stream().map(Tree::textRange).collect(Collectors.toList()));
      expression = new NativeTreeImpl(metaDataProvider.metaData(childRange), new RubyNativeKind("returnExpression"), childTrees);
    }
    return new ReturnTreeImpl(metaData(node), keyword, expression);
  }

  @CheckForNull
  private NativeTree createNativeTree(AstNode node, List<?> children, String type) {
    List<Tree> nonNullChildren = convertChildren(node, children);
    return new NativeTreeImpl(metaData(node), new RubyNativeKind(type), nonNullChildren);
  }

  private List<Tree> convertChildren(AstNode node, List<?> children) {
    return children.stream().flatMap(child -> treeForChild(node, child)).collect(Collectors.toList());
  }

  @CheckForNull
  private NativeTree createNativeTree(AstNode node, List<?> children) {
    // when node has no location it means that it is not present in the tree
    if (node.textRange() == null) {
      return null;
    }
    return createNativeTree(node, children, node.type());
  }

  private Stream<Tree> treeForChild(AstNode node, @Nullable Object child) {
    if (child instanceof Tree) {
      return Stream.of((Tree) child);
    } else if (child instanceof RubySymbol) {
      return Stream.of(identifierFromSymbol(node, (RubySymbol) child));
    } else if (child != null) {
      return Stream.of(createNativeTree(node, emptyList(), String.valueOf(child)));
    } else {
      return Stream.empty();
    }
  }

  private TreeMetaData metaData(AstNode node) {
    TextRange textRange = node.textRange();
    if (textRange == null) {
      throw new IllegalStateException("Attempt to retrieve metadata for null location. Node: " + node.asString());
    }
    return metaDataProvider.metaData(textRange);
  }

  private Optional<Token> lookForTokenByAttribute(AstNode node, String attribute) {
    TextRange mainKeywordTextRange = node.textRangeForAttribute(attribute);
    if (mainKeywordTextRange != null) {
      return Optional.of(metaDataProvider.metaData(mainKeywordTextRange).tokens().get(0));
    }
    return Optional.empty();
  }

  private Token getTokenByAttribute(AstNode node, String attribute) {
    Optional<Token> token = lookForTokenByAttribute(node, attribute);
    return token.orElseThrow(() ->
      new IllegalStateException(String.format("No attribute '%s' found for node of type '%s'", attribute, node.type())));
  }

  private IdentifierTree identifierFromSymbol(AstNode node, RubySymbol rubySymbol) {
    String name = rubySymbol.asJavaString();
    TextRange textRange = node.textRangeForAttribute("name");
    if (textRange == null) {
      textRange = node.textRange();
    }
    if (textRange == null) {
      throw new IllegalStateException("Missing range for identifier. Node: " + node.asString());
    }
    return new IdentifierTreeImpl(metaDataProvider.metaData(textRange), name);
  }

}
