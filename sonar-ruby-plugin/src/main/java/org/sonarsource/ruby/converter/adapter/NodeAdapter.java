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

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.sonarsource.slang.api.Tree;

@JRubyClass(name = "NodeAdapter")
public class NodeAdapter extends RubyObject {
  private static final String AST_NODE_CLASS = "Parser::AST::Node";
  private static RubyClass metaclass;

  private transient IRubyObject underlyingNode;

  private transient Tree tree;

  private NodeAdapter(Ruby runtime, IRubyObject underlyingNode) {
    super(runtime, metaclass);
    this.underlyingNode = underlyingNode;
  }

  public static NodeAdapter create(Ruby runtime, IRubyObject underlyingNode, Tree tree) {
    NodeAdapter nodeAdapter = new NodeAdapter(runtime, underlyingNode);
    nodeAdapter.tree = tree;
    return nodeAdapter;
  }

  public IRubyObject getUnderlyingNode() {
    return underlyingNode;
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

  public Tree getTree() {
    return tree;
  }

  @JRubyMethod
  public RubyFixnum hash(ThreadContext context) {
    return (RubyFixnum) JavaEmbedUtils.invokeMethod(getRuntime(), underlyingNode, "hash", null, RubyFixnum.class);
  }

}
