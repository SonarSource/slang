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

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.jruby.Ruby;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.exceptions.NoMethodError;
import org.jruby.exceptions.StandardError;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.specialized.RubyArrayTwoObject;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.ruby.converter.adapter.CommentAdapter;
import org.sonarsource.ruby.converter.adapter.NodeAdapter;
import org.sonarsource.ruby.converter.adapter.RangeAdapter;
import org.sonarsource.ruby.converter.adapter.TokenAdapter;
import org.sonarsource.slang.api.ASTConverter;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.TextPointer;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.sonarsource.slang.impl.TextRanges;
import org.sonarsource.slang.impl.TopLevelTreeImpl;
import org.sonarsource.slang.impl.TreeMetaDataProvider;
import org.sonarsource.slang.plugin.ParseException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class RubyConverter implements ASTConverter {

  private static final Logger LOG = Loggers.get(RubyConverter.class);
  private static final String SETUP_SCRIPT_PATH = "/whitequark_parser_init.rb";
  private static final String AST_RUBYGEM_PATH = "/ast-2.4.0/lib";
  private static final String PARSER_RUBYGEM_PATH = "/parser-2.5.1.2/lib";
  private static final RubyRuntimeAdapter rubyRuntimeAdapter = JavaEmbedUtils.newRuntimeAdapter();

  private final Ruby runtime;

  public RubyConverter() {
    try {
      runtime = initializeRubyRuntime();
    } catch (URISyntaxException | IOException e) {
      throw new IllegalStateException("Failed to initialized ruby runtime", e);
    }
  }

  @Override
  public void terminate() {
    // Shutdown and terminate ruby instance
    if (runtime != null) {
      JavaEmbedUtils.terminate(runtime);
    }
  }

  @Override
  public Tree parse(String content) {
    try {
      return parseContent(content);
    } catch (StandardError e) {
      throw new ParseException(e.getMessage(), getErrorLocation(e));
    }
  }

  private TextPointer getErrorLocation(StandardError e) {
    try {
      IRubyObject diagnostic = (IRubyObject) invokeMethod(e.getException(), "diagnostic", null);
      IRubyObject location = (IRubyObject) invokeMethod(diagnostic, "location", null);
      if (location != null) {
        return new RangeAdapter(runtime, location).toTextRange().start();
      }
    } catch (NoMethodError nme) {
      LOG.warn("No location information available for parse error");
    }
    return null;
  }

  private Tree parseContent(String content) {
    Object[] parameters = {content};
    List rubyParseResult = (List) invokeMethod(runtime.getObject(), "parse_with_tokens", parameters);
    if (rubyParseResult == null) {
      throw new ParseException("Unable to parse file content");
    }

    IRubyObject ast = (IRubyObject) rubyParseResult.get(0);
    List<IRubyObject> rubyComments = (List) rubyParseResult.get(1);
    List<IRubyObject> rubyTokens = (List) rubyParseResult.get(2);

    List<Comment> comments = rubyComments.stream()
      .map(rubyComment -> new CommentAdapter(runtime, rubyComment))
      .map(CommentAdapter::toSlangComment)
      .collect(Collectors.toList());
    List<Token> tokens = rubyTokens.stream()
      .map(rubyToken -> new TokenAdapter(runtime, (RubyArrayTwoObject) rubyToken))
      .map(TokenAdapter::toSlangToken)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
    TreeMetaDataProvider metaDataProvider = new TreeMetaDataProvider(comments, tokens);

    RubyProcessor rubyProcessor = getRubyProcessor(metaDataProvider);

    Object[] astProcessorParams = {ast};
    NodeAdapter node = (NodeAdapter) invokeMethod(rubyProcessor, "process", astProcessorParams);

    if (tokens.isEmpty() || node == null || node.getTree() == null) {
      throw new ParseException("No AST node found");
    }

    Tree originalTree = node.getTree();
    TextRange fullRange = TextRanges.merge(Arrays.asList(tokens.get(0).textRange(), tokens.get(tokens.size() - 1).textRange()));
    TreeMetaData topTreeMetaData = metaDataProvider.metaData(fullRange);
    if (originalTree.children().isEmpty()) {
      // singleton expression: we wrap it around a top level tree
      return new TopLevelTreeImpl(topTreeMetaData, Collections.singletonList(originalTree), comments);
    } else {
      // replace top level tree with correct range (including start and end comments)
      return new TopLevelTreeImpl(topTreeMetaData, originalTree.children(), comments);
    }
  }

  private RubyProcessor getRubyProcessor(TreeMetaDataProvider metaDataProvider) {
    Object[] constructorParams = {metaDataProvider};
    IRubyObject rubyProcessorClass = rubyRuntimeAdapter.eval(runtime, RubyProcessor.class.getSimpleName());
    return (RubyProcessor) invokeMethod(rubyProcessorClass, "new", constructorParams);
  }

  @Nullable
  private Object invokeMethod(@Nullable Object receiver, String methodName, @Nullable Object[] args) {
    return JavaEmbedUtils.invokeMethod(runtime, receiver, methodName, args, Object.class);
  }

  private static Ruby initializeRubyRuntime() throws URISyntaxException, IOException {
    URL astRubygem = RubyConverter.class.getResource(AST_RUBYGEM_PATH);
    URL parserRubygem = RubyConverter.class.getResource(PARSER_RUBYGEM_PATH);
    URL initParserScriptUrl = RubyConverter.class.getResource(SETUP_SCRIPT_PATH);

    Ruby runtime = JavaEmbedUtils.initialize(Arrays.asList(astRubygem.toString(), parserRubygem.toString()));
    String initParserScript = new String(Files.readAllBytes(Paths.get(initParserScriptUrl.toURI())), UTF_8);
    rubyRuntimeAdapter.eval(runtime, initParserScript);
    RubyProcessor.addToRuntime(runtime);
    NodeAdapter.addToRuntime(runtime);
    return runtime;
  }

}
