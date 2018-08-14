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
import org.jruby.RubySymbol;
import org.sonarsource.slang.api.BlockTree;
import org.sonarsource.slang.api.ClassDeclarationTree;
import org.sonarsource.slang.api.FunctionDeclarationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.LiteralTree;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.sonarsource.slang.impl.BlockTreeImpl;
import org.sonarsource.slang.impl.ClassDeclarationTreeImpl;
import org.sonarsource.slang.impl.FunctionDeclarationTreeImpl;
import org.sonarsource.slang.impl.IdentifierTreeImpl;
import org.sonarsource.slang.impl.LiteralTreeImpl;
import org.sonarsource.slang.impl.NativeTreeImpl;
import org.sonarsource.slang.impl.TreeMetaDataProvider;

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
    TreeMetaData metaData = metaDataProvider.metaData(node.textRangeForAttribute("name"));
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
    return new ClassDeclarationTreeImpl(metaData(node), (IdentifierTree) children.get(0), createNativeTree(node, children));
  }

  private LiteralTree createLiteralTree(AstNode node, List<Object> children) {
    String value = String.valueOf(children.get(0));
    return new LiteralTreeImpl(metaData(node), value);
  }

  private IdentifierTree createIdentifierTree(AstNode node, List<Object> children) {
    String name = ((RubySymbol) children.get(1)).asJavaString();
    return new IdentifierTreeImpl(metaData(node), name);
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

  private static Stream<Tree> treeForChild(Object child, TreeMetaData treeMetaData) {
    if (child instanceof Tree) {
      return Stream.of((Tree) child);
    } else if (child instanceof RubySymbol) {
      return Stream.of(new NativeTreeImpl(treeMetaData, new RubyNativeKind(String.valueOf(child)), emptyList()));
    } else if (child instanceof String) {
      return Stream.of(new NativeTreeImpl(treeMetaData, new RubyNativeKind((String) child), emptyList()));
    } else {
      return Stream.empty();
    }
  }

  private TreeMetaData metaData(AstNode node) {
    return metaDataProvider.metaData(node.textRange());
  }
}
