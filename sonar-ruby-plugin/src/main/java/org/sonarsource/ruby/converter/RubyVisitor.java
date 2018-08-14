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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNil;
import org.jruby.RubyObject;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.sonarsource.ruby.converter.adapter.NodeAdapter;
import org.sonarsource.ruby.converter.adapter.RangeAdapter;
import org.sonarsource.ruby.converter.adapter.SourceMapAdapter;
import org.sonarsource.slang.api.BlockTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.sonarsource.slang.impl.BlockTreeImpl;
import org.sonarsource.slang.impl.ClassDeclarationTreeImpl;
import org.sonarsource.slang.impl.FunctionDeclarationTreeImpl;
import org.sonarsource.slang.impl.IdentifierTreeImpl;
import org.sonarsource.slang.impl.LiteralTreeImpl;
import org.sonarsource.slang.impl.NativeTreeImpl;
import org.sonarsource.slang.impl.TreeMetaDataProvider;

@JRubyClass(name = "RubyVisitor")
public class RubyVisitor extends RubyObject {

  private static final String AST_PROCESSOR_CLASS = "Parser::AST::Processor";
  private static RubyClass superclass = null;

  private transient TreeMetaDataProvider metaDataProvider;

  public RubyVisitor(Ruby runtime, RubyClass metaClass) {
    super(runtime, metaClass);
  }

  public static void addToRuntime(final Ruby runtime) {
    superclass = runtime.getObject().subclasses(true).stream()
      .filter(clazz -> AST_PROCESSOR_CLASS.equals(clazz.getName()))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException(String.format("Could not find class %s in ruby runtime", AST_PROCESSOR_CLASS)));
    RubyModule rubyVisitor = runtime.getObject().defineClassUnder(RubyVisitor.class.getSimpleName(), superclass, RubyVisitor::new);
    rubyVisitor.defineAnnotatedMethods(RubyVisitor.class);
  }

  /**
   * Ruby constructor takes 2 parameters, metaDataProvider as first parameter, and superclass as second
   */
  @JRubyMethod(name = "new", required = 1, rest = true, meta = true)
  public static IRubyObject rbNew(ThreadContext context, IRubyObject klazz, IRubyObject[] args) {
    RubyVisitor rubyVisitor = (RubyVisitor) ((RubyClass) klazz).allocate();
    rubyVisitor.metaDataProvider = args[0].toJava(TreeMetaDataProvider.class);
    return rubyVisitor;
  }

  @JRubyMethod
  public IRubyObject process(ThreadContext context, IRubyObject node) {
    // some nodes are present in the AST but are empty (e.g. function parameters clause)
    // or node is nil (absent)
    if (noLocation(node) || node.isNil()) {
      return new RubyNil(getRuntime());
    }

    IRubyObject processedNode = visitNode(context, "process", node);

    if (processedNode instanceof NodeAdapter) {
      // A more specific tree was already created by a specialized method (on_${type}, ex: on_int)
      return processedNode;
    } else {
      return createNativeTreeAdapter(processedNode);
    }
  }

  @JRubyMethod(name = "on_class")
  public IRubyObject onClass(ThreadContext context, IRubyObject node) {
    IRubyObject updatedNode = visitNode(context, "on_class", node);

    IdentifierTree identifier = (IdentifierTree) ((NodeAdapter) getChild(updatedNode, 0)).getTree();
    return convertToNodeAdapter(updatedNode, metaData ->
      new ClassDeclarationTreeImpl(metaData, identifier, createNativeTree(updatedNode)));
  }

  @JRubyMethod(name = "on_def")
  public IRubyObject onDef(ThreadContext context, IRubyObject node) {
    return createFunctionDeclaration(context, node);
  }

  @JRubyMethod(name = "on_defs")
  public IRubyObject onDefs(ThreadContext context, IRubyObject node) {
    return createFunctionDeclaration(context, node);
  }

  private IRubyObject createFunctionDeclaration(ThreadContext context, IRubyObject node) {
    String type = nodeType(node);
    boolean isSingletonMethod = type.equals("defs");

    IRubyObject updatedNode = visitNode(context, "on_" + type, node);

    List<Tree> nativeChildren;
    if (isSingletonMethod) {
      nativeChildren = Collections.singletonList(((NodeAdapter) getChild(updatedNode, 0)).getTree());
    } else {
      nativeChildren = Collections.emptyList();
    }

    int childrenIndexShift = isSingletonMethod ? 1 : 0;

    Object name = getChild(updatedNode, 0 + childrenIndexShift);
    IdentifierTree identifier = new IdentifierTreeImpl(getMetaData(updatedNode, "name"), String.valueOf(name));

    List<Tree> parameters;
    IRubyObject args = (IRubyObject) getChild(updatedNode, 1 + childrenIndexShift);
    if (args != null) {
      parameters = getChildren(((NodeAdapter) args).getUnderlyingNode());
    } else {
      parameters = Collections.emptyList();
    }

    BlockTree body;
    IRubyObject rubyBodyBlock = (IRubyObject) getChild(updatedNode, 2 + childrenIndexShift);
    if (rubyBodyBlock != null) {
      List<Tree> statements = Collections.singletonList(((NodeAdapter) rubyBodyBlock).getTree());
      body = new BlockTreeImpl(getMetaData(((NodeAdapter) rubyBodyBlock).getUnderlyingNode()), statements);
    } else {
      body = new BlockTreeImpl(getMetaData(node), new ArrayList<>());
    }

    return convertToNodeAdapter(updatedNode, metaData ->
      new FunctionDeclarationTreeImpl(
        metaData,
        Collections.emptyList(),
        null,
        identifier,
        parameters,
        body,
        nativeChildren));
  }


  @JRubyMethod(name = "on_const")
  public IRubyObject onConst(ThreadContext context, IRubyObject node) {
    // FIXME add scope node child to current node
    Object name = getChild(node, 1);
    return convertToNodeAdapter(node, metaData -> new IdentifierTreeImpl(metaData, String.valueOf(name)));
  }

  @JRubyMethod(name = "on_int")
  public IRubyObject onInt(ThreadContext context, IRubyObject node) {
    Object value = getChild(node, 0);
    return convertToNodeAdapter(node, metaData -> new LiteralTreeImpl(metaData, String.valueOf(value)));
  }

  private NodeAdapter convertToNodeAdapter(IRubyObject node, Function<TreeMetaData, Tree> getTree) {
    TreeMetaData metaData = getMetaData(node);
    return NodeAdapter.create(getRuntime(), node, getTree.apply(metaData));
  }

  private NodeAdapter createNativeTreeAdapter(IRubyObject processedNode) {
    return NodeAdapter.create(getRuntime(), processedNode, createNativeTree(processedNode));
  }

  private Tree createNativeTree(IRubyObject node) {
    TreeMetaData metaData = getMetaData(node);
    return new NativeTreeImpl(metaData, new RubyNativeKind(nodeType(node)), getChildrenForNative(node, metaData));
  }

  private boolean noLocation(IRubyObject node) {
    if (!node.isNil()) {
      IRubyObject location = (IRubyObject) JavaEmbedUtils.invokeMethod(getRuntime(), node, "location", null, IRubyObject.class);
      SourceMapAdapter sourceMapAdapter = new SourceMapAdapter(getRuntime(), location);
      RangeAdapter rangeAdapter = sourceMapAdapter.getRange();

      return rangeAdapter.isNull();
    }

    return false;
  }

  private TreeMetaData getMetaData(IRubyObject node) {
    return getMetaData(node, "expression");
  }

  private TreeMetaData getMetaData(IRubyObject node, String attribute) {
    IRubyObject location = (IRubyObject) JavaEmbedUtils.invokeMethod(getRuntime(), node, "location", null, IRubyObject.class);
    SourceMapAdapter sourceMapAdapter = new SourceMapAdapter(getRuntime(), location);
    RangeAdapter rangeAdapter = sourceMapAdapter.getRange(attribute);
    return metaDataProvider.metaData(rangeAdapter.toTextRange());
  }

  private IRubyObject visitNode(ThreadContext context, String methodName, IRubyObject node) {
    DynamicMethod method = superclass.searchMethod(methodName);
    return method.call(context, this, superclass, methodName, node);
  }

  private Object getChild(IRubyObject node, int index) {
    return ((List) JavaEmbedUtils.invokeMethod(getRuntime(), node, "to_a", null, List.class)).get(index);
  }

  private String nodeType(IRubyObject node) {
    return (String) JavaEmbedUtils.invokeMethod(getRuntime(), node, "type", null, String.class);
  }

  private List<Tree> getChildrenForNative(IRubyObject node, TreeMetaData metaData) {
    List<Object> children = (List) JavaEmbedUtils.invokeMethod(getRuntime(), node, "to_a", null, List.class);
    return children.stream()
      .filter(Objects::nonNull)
      .map(child -> {
        if (child instanceof NodeAdapter) {
          return ((NodeAdapter) child).getTree();
        }

        // fixme: avoid this hack
        // The following node would normally not appear in the AST, as it represents the value of a specialized node (Ex: the string
        // value of a string literal, the operator name of a binary operation, ...). However we are dealing with a partially mapped
        // AST in SLang, and these nodes must appear syntactically different, so we add these values as children of native trees.
        String type = child.toString();
        if (child instanceof RubySymbol) {
          type = ((RubySymbol) child).asJavaString();
        }
        return new NativeTreeImpl(metaData, new RubyNativeKind(type), Collections.emptyList());
      })
      .collect(Collectors.toList());
  }

  private List<Tree> getChildren(IRubyObject node) {
    List<Object> children = (List) JavaEmbedUtils.invokeMethod(getRuntime(), node, "to_a", null, List.class);
    return children.stream()
      .filter(Objects::nonNull)
      .map(child -> ((NodeAdapter) child).getTree())
      .collect(Collectors.toList());
  }

}
