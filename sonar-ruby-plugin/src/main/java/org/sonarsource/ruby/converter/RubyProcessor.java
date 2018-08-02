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
import java.util.Objects;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.sonarsource.ruby.converter.adapter.NodeAdapter;
import org.sonarsource.ruby.converter.adapter.SourceMapAdapter;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.sonarsource.slang.impl.LiteralTreeImpl;
import org.sonarsource.slang.impl.NativeTreeImpl;
import org.sonarsource.slang.impl.TreeMetaDataProvider;

@JRubyClass(name = "RubyProcessor")
public class RubyProcessor extends RubyObject {

  private static final String AST_PROCESSOR_CLASS = "Parser::AST::Processor";
  private static RubyClass superclass = null;

  private transient TreeMetaDataProvider metaDataProvider;

  public RubyProcessor(Ruby runtime, RubyClass metaClass) {
    super(runtime, metaClass);
  }

  public static void addToRuntime(final Ruby runtime) {
    superclass = runtime.getObject().subclasses(true).stream()
      .filter(clazz -> AST_PROCESSOR_CLASS.equals(clazz.getName()))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException(String.format("Could not find class %s in ruby runtime", AST_PROCESSOR_CLASS)));
    RubyModule rubyProcessor = runtime.getObject().defineClassUnder(RubyProcessor.class.getSimpleName(), superclass, RubyProcessor::new);
    rubyProcessor.defineAnnotatedMethods(RubyProcessor.class);
  }

  /**
   * Ruby constructor takes 2 parameters, metaDataProvider as first parameter, and superclass as second
   */
  @JRubyMethod(name = "new", required = 1, rest = true, meta = true)
  public static IRubyObject rbNew(ThreadContext context, IRubyObject klazz, IRubyObject[] args) {
    RubyProcessor rubyProcessor = (RubyProcessor) ((RubyClass) klazz).allocate();
    rubyProcessor.metaDataProvider = args[0].toJava(TreeMetaDataProvider.class);
    return rubyProcessor;
  }

  @JRubyMethod
  public IRubyObject process(ThreadContext context, IRubyObject arg1) {
    IRubyObject rubyObject = callSuperMethod(context, "process", arg1);
    if (rubyObject instanceof NodeAdapter || rubyObject.isNil()) {
      // A more specific tree was already created by a specialized method (on_${type}, ex: on_int)
      return rubyObject;
    }

    // Create generic native tree
    NodeAdapter nodeAdapter = new NodeAdapter(getRuntime(), rubyObject);
    TreeMetaData metaData = getMetaData(rubyObject);
    nodeAdapter.setTree(createNativeTree(metaData, nodeAdapter.nodeType(), nodeAdapter.getChildren(metaData)));
    return nodeAdapter;
  }

  @JRubyMethod(name = "on_int")
  public IRubyObject onInt(ThreadContext context, IRubyObject arg1) {
    NodeAdapter nodeAdapter = new NodeAdapter(getRuntime(), arg1);
    Long value = nodeAdapter.getLiteralValue();
    nodeAdapter.setTree(new LiteralTreeImpl(getMetaData(arg1), String.valueOf(value)));
    return nodeAdapter;
  }

  private static Tree createNativeTree(TreeMetaData metaData, IRubyObject nodeType, List<Tree> children) {
    return new NativeTreeImpl(metaData, new RubyNativeKind(nodeType, nodeType.asJavaString()), children);
  }

  private TreeMetaData getMetaData(IRubyObject rubyObject) {
    IRubyObject location = (IRubyObject) JavaEmbedUtils.invokeMethod(getRuntime(), rubyObject, "location", null, IRubyObject.class);
    SourceMapAdapter sourceMapAdapter = new SourceMapAdapter(getRuntime(), location);
    TextRange textRange = sourceMapAdapter.getRange().toTextRange();
    return metaDataProvider.metaData(textRange);
  }

  private IRubyObject callSuperMethod(ThreadContext context, String methodName, IRubyObject arg1) {
    DynamicMethod method = superclass.searchMethod(methodName);
    return method.call(context, this, superclass, methodName, arg1);
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
    RubyProcessor that = (RubyProcessor) o;
    return Objects.equals(metaDataProvider, that.metaDataProvider);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), metaDataProvider);
  }

}
