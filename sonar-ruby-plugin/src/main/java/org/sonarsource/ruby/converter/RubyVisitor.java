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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.jruby.RubySymbol;
import org.sonarsource.slang.api.BlockTree;
import org.sonarsource.slang.api.ClassDeclarationTree;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.IfTree;
import org.sonarsource.slang.api.LiteralTree;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.sonarsource.slang.impl.BlockTreeImpl;
import org.sonarsource.slang.impl.ClassDeclarationTreeImpl;
import org.sonarsource.slang.impl.FunctionDeclarationTreeImpl;
import org.sonarsource.slang.impl.IdentifierTreeImpl;
import org.sonarsource.slang.impl.IfTreeImpl;
import org.sonarsource.slang.impl.LiteralTreeImpl;
import org.sonarsource.slang.impl.NativeTreeImpl;
import org.sonarsource.slang.impl.TextRanges;
import org.sonarsource.slang.impl.TreeMetaDataProvider;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class RubyVisitor {

  private final TreeMetaDataProvider metaDataProvider;

  public RubyVisitor(TreeMetaDataProvider metaDataProvider) {
    this.metaDataProvider = metaDataProvider;
  }

  public Tree visitNode(AstNode node, List<Object> children) {
    switch (node.type()) {
      case "const":
        return createIdentifierTree(node, children);
      case "class":
        return createClassDeclarationTree(node, children);
      case "def":
      case "defs":
        return createFunctionDeclarationTree(node, children);
      case "int":
        return createLiteralTree(node, children);
      case "if":
        return createIfTree(node, children);
      default:
        return createNativeTree(node, children);
    }
  }

  private FunctionDeclarationTree createFunctionDeclarationTree(AstNode node, List<Object> children) {
    boolean isSingletonMethod = node.type().equals("defs");
    List<Tree> nativeChildren;
    if (isSingletonMethod) {
      nativeChildren = singletonList((Tree) children.get(0));
    } else {
      nativeChildren = emptyList();
    }

    int childrenIndexShift = isSingletonMethod ? 1 : 0;

    Object name = children.get(0 + childrenIndexShift);
    TextRange nameRange = node.textRangeForAttribute("name");
    if (nameRange == null) {
      throw new IllegalStateException("Missing range for function name. Node: " + node.asString());
    }
    TreeMetaData metaData = metaDataProvider.metaData(nameRange);
    IdentifierTree identifier = new IdentifierTreeImpl(metaData, String.valueOf(name));

    List<Tree> parameters;
    Object args = children.get(1 + childrenIndexShift);
    if (args != null) {
      parameters = ((Tree) args).children();
    } else {
      parameters = emptyList();
    }

    BlockTree body;
    Tree rubyBodyBlock = (Tree) children.get(2 + childrenIndexShift);
    if (rubyBodyBlock != null) {
      List<Tree> statements = singletonList(rubyBodyBlock);
      body = new BlockTreeImpl(rubyBodyBlock.metaData(), statements);
    } else {
      body = new BlockTreeImpl(metaData(node), emptyList());
    }
    return new FunctionDeclarationTreeImpl(metaData(node),
      emptyList(),
      null,
      identifier,
      parameters,
      body,
      nativeChildren);
  }

  private ClassDeclarationTree createClassDeclarationTree(AstNode node, List<Object> children) {
    NativeTree nativeTree = createNativeTree(node, children);
    if (nativeTree == null) {
      throw new IllegalStateException("Failed to create ClassDeclarationTree for node " + node.asString());
    }
    return new ClassDeclarationTreeImpl(metaData(node), (IdentifierTree) children.get(0), nativeTree);
  }

  private LiteralTree createLiteralTree(AstNode node, List<Object> children) {
    String value = String.valueOf(children.get(0));
    return new LiteralTreeImpl(metaData(node), value);
  }

  private IdentifierTree createIdentifierTree(AstNode node, List<Object> children) {
    // FIXME add scope node child to current node
    String name = ((RubySymbol) children.get(1)).asJavaString();
    return new IdentifierTreeImpl(metaData(node), name);
  }


  private Tree createIfTree(AstNode node, List<Object> children) {
    Token mainKeyword = tokenForAttribute(node, "keyword");
    if (mainKeyword == null || mainKeyword.text().equals("unless")) {
      // Ternary operator and "unless" are not considered as "IfTree" for now
      return createNativeTree(node, children);
    }

    Token elseKeyword = tokenForAttribute(node, "else");
    Tree condition = (Tree) children.get(0);
    Tree thenBranch = getThenBranch(node, mainKeyword, elseKeyword, (Tree) children.get(1));
    Tree elseBranch = getElseBranch(node, elseKeyword, (Tree) children.get(2));

    return new IfTreeImpl(metaData(node), condition, thenBranch, elseBranch, mainKeyword, elseKeyword);
  }

  private Tree getThenBranch(AstNode node, Token mainKeyword, @Nullable Token elseKeyword, @Nullable Tree thenBranch) {
    if (thenBranch != null) {
      return thenBranch;
    } else if (elseKeyword == null) {
      // empty "then" branch body and no "else" branch
      return new BlockTreeImpl(metaData(node), emptyList());
    } else {
      // empty "then" branch, with a "else" branch. Meta for empty "then" block will be "if...else" part
      TreeMetaData emptyIfMetadata = metaDataProvider.metaData(TextRanges.merge(asList(mainKeyword.textRange(), elseKeyword.textRange())));
      return new BlockTreeImpl(emptyIfMetadata, emptyList());
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
      TextRange endRange = node.textRangeForAttribute("end");
      TreeMetaData emptyElseMetadata = metaDataProvider.metaData(TextRanges.merge(asList(elseKeyword.textRange(), endRange)));
      return new BlockTreeImpl(emptyElseMetadata, emptyList());
    } else {
      // no "else" branch
      return null;
    }
  }

  @CheckForNull
  private NativeTree createNativeTree(AstNode node, List<Object> children) {
    // when node has no location it means that it is not present in the tree
    if (node.textRange() == null) {
      return null;
    }
    List<Tree> nonNullChildren = children.stream().flatMap(child -> treeForChild(child, metaData(node))).collect(Collectors.toList());
    return new NativeTreeImpl(metaData(node), new RubyNativeKind(node.type()), nonNullChildren);
  }

  private static Stream<Tree> treeForChild(@Nullable Object child, TreeMetaData treeMetaData) {
    if (child instanceof Tree) {
      return Stream.of((Tree) child);
    } else if (child instanceof RubySymbol) {
      String type = ((RubySymbol) child).asJavaString();
      return Stream.of(new NativeTreeImpl(treeMetaData, new RubyNativeKind(type), emptyList()));
    } else if (child instanceof String) {
      return Stream.of(new NativeTreeImpl(treeMetaData, new RubyNativeKind((String) child), emptyList()));
    } else if (child != null) {
      return Stream.of(new NativeTreeImpl(treeMetaData, new RubyNativeKind(String.valueOf(child)), emptyList()));
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

  @Nullable
  private Token tokenForAttribute(AstNode node, String attribute) {
    TextRange mainKeywordTextRange = node.textRangeForAttribute(attribute);
    if (mainKeywordTextRange != null) {
      return metaDataProvider.metaData(mainKeywordTextRange).tokens().get(0);
    }
    return null;
  }

}
