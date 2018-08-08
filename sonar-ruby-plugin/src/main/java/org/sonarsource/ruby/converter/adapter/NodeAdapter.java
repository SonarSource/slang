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
package org.sonarsource.ruby.converter.adapter;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyObject;
import org.jruby.RubySymbol;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.sonarsource.ruby.converter.RubyNativeKind;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.sonarsource.slang.impl.NativeTreeImpl;

@JRubyClass(name = "NodeAdapter")
public class NodeAdapter extends RubyObject {
  private static final String AST_NODE_CLASS = "Parser::AST::Node";
  private static RubyClass metaclass;

  private transient IRubyObject underlyingNode;
  private transient Tree tree;

  public NodeAdapter(Ruby runtime, IRubyObject underlyingNode) {
    super(runtime, metaclass);
    this.underlyingNode = underlyingNode;
  }

  public static void addToRuntime(final Ruby runtime) {
    if (metaclass == null) {
      RubyClass superClass = runtime.getObject().subclasses(true).stream()
        .filter(clazz -> AST_NODE_CLASS.equals(clazz.getName()))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(String.format("Could not find class %s in ruby runtime", AST_NODE_CLASS)));
      metaclass = runtime.getObject().defineClassUnder(NodeAdapter.class.getSimpleName(), superClass, NodeAdapter::new);
      metaclass.defineAnnotatedMethods(NodeAdapter.class);
    }
  }

  @Nullable
  public Tree getTree() {
    return tree;
  }

  public void setTree(Tree tree) {
    this.tree = tree;
  }

  @JRubyMethod
  public RubyFixnum hash(ThreadContext context) {
    return (RubyFixnum) JavaEmbedUtils.invokeMethod(getRuntime(), underlyingNode, "hash", null, RubyFixnum.class);
  }

  public IRubyObject nodeType() {
    return (IRubyObject) JavaEmbedUtils.invokeMethod(getRuntime(), underlyingNode, "type", null, IRubyObject.class);
  }

  public List<Tree> getChildren(TreeMetaData metaData) {
    List<Object> children = (List) JavaEmbedUtils.invokeMethod(getRuntime(), underlyingNode, "to_a", null, List.class);
    return children.stream()
      .filter(Objects::nonNull)
      .map(child -> {
        if (child instanceof NodeAdapter) {
          return ((NodeAdapter) child).getTree();
        }

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

  public Object getFirstChild() {
    return ((List) JavaEmbedUtils.invokeMethod(getRuntime(), underlyingNode, "to_a", null, List.class)).get(0);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    NodeAdapter that = (NodeAdapter) o;
    return Objects.equals(underlyingNode, that.underlyingNode) &&
      Objects.equals(tree, that.tree);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), underlyingNode, tree);
  }

}
