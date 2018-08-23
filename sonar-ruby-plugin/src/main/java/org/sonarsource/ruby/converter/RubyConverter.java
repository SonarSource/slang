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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.sonarsource.ruby.converter.adapter.RangeAdapter;
import org.sonarsource.ruby.converter.adapter.TokenAdapter;
import org.sonarsource.slang.api.ASTConverter;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.ParseException;
import org.sonarsource.slang.api.TextPointer;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.sonarsource.slang.impl.TextRanges;
import org.sonarsource.slang.impl.TopLevelTreeImpl;
import org.sonarsource.slang.impl.TreeMetaDataProvider;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class RubyConverter implements ASTConverter {

  private static final Logger LOG = Loggers.get(RubyConverter.class);
  private static final String SETUP_SCRIPT_PATH = "/whitequark_parser_init.rb";
  private static final String AST_RUBYGEM_PATH = "/ast-2.4.0/lib";
  private static final String PARSER_RUBYGEM_PATH = "/parser-2.5.1.2/lib";
  private static final String COMMENT_TOKEN_TYPE = "tCOMMENT";
  static final String FILENAME = "(SonarRuby analysis)";

  private final RubyRuntimeAdapter rubyRuntimeAdapter;
  private final Ruby runtime;

  RubyConverter(RubyRuntimeAdapter rubyRuntimeAdapter) {
    this.rubyRuntimeAdapter = rubyRuntimeAdapter;
    try {
      runtime = initializeRubyRuntime();
    } catch (URISyntaxException | IOException e) {
      throw new IllegalStateException("Failed to initialized ruby runtime", e);
    }
  }

  public RubyConverter() {
    this(JavaEmbedUtils.newRuntimeAdapter());
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
      throw new ParseException(e.getMessage(), getErrorLocation(e), e);
    } catch (Exception e) {
      throw new ParseException(e.getMessage(), null, e);
    }
  }

  TextPointer getErrorLocation(StandardError e) {
    try {
      IRubyObject diagnostic = (IRubyObject) invokeMethod(e.getException(), "diagnostic", null);
      IRubyObject location = (IRubyObject) invokeMethod(diagnostic, "location", null);
      if (location != null) {
        return new RangeAdapter(runtime, location).toTextRange().start();
      }
    } catch (NoMethodError nme) {
      // location information could not be retrieved from ruby object
    }
    LOG.warn("No location information available for parse error");
    return null;
  }

  Tree parseContent(String content) {
    Object[] parameters = {content, FILENAME};
    List rubyParseResult = (List) invokeMethod(runtime.getObject(), "parse_with_tokens", parameters);
    if (rubyParseResult == null) {
      throw new ParseException("Unable to parse file content");
    }

    Object rubyAst = rubyParseResult.get(0);
    List<IRubyObject> rubyComments = (List<IRubyObject>) rubyParseResult.get(1);
    List<IRubyObject> rubyTokens = (List<IRubyObject>) rubyParseResult.get(2);

    List<Comment> comments = rubyComments.stream()
      .map(rubyComment -> new CommentAdapter(runtime, rubyComment))
      .map(CommentAdapter::toSlangComment)
      .collect(Collectors.toList());
    List<Token> tokens = rubyTokens.stream()
      .map(rubyToken -> new TokenAdapter(runtime, (RubyArrayTwoObject) rubyToken))
      .filter(tokenAdapter -> !COMMENT_TOKEN_TYPE.equals(tokenAdapter.getTokenType().asJavaString()))
      .map(TokenAdapter::toSlangToken)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
    TreeMetaDataProvider metaDataProvider = new TreeMetaDataProvider(comments, tokens);

    if (tokens.isEmpty() && comments.isEmpty()) {
      throw new ParseException("No AST node found");
    }

    Object[] visitParams = {rubyAst, new RubyVisitor(metaDataProvider)};
    Tree tree = (Tree) invokeMethod(runtime.getObject(), "visit", visitParams);

    TreeMetaData topTreeMetaData = metaDataProvider.metaData(getFullRange(tokens, comments));
    if (tree == null) {
      // only comments
      return new TopLevelTreeImpl(topTreeMetaData, emptyList(), comments);
    } else {
      // singleton expression: we wrap it around a top level tree
      return new TopLevelTreeImpl(topTreeMetaData, singletonList(tree), comments);
    }
  }

  private static TextRange getFullRange(List<Token> tokens, List<Comment> comments) {
    if (comments.isEmpty()) {
      return TextRanges.merge(Arrays.asList(tokens.get(0).textRange(), tokens.get(tokens.size() - 1).textRange()));
    } else if (tokens.isEmpty()) {
      return TextRanges.merge(Arrays.asList(comments.get(0).textRange(), comments.get(comments.size() - 1).textRange()));
    }
    return TextRanges.merge(Arrays.asList(
      tokens.get(0).textRange(),
      tokens.get(tokens.size() - 1).textRange(),
      comments.get(0).textRange(),
      comments.get(comments.size() - 1).textRange()));
  }

  @Nullable
  Object invokeMethod(@Nullable Object receiver, String methodName, @Nullable Object[] args) {
    return JavaEmbedUtils.invokeMethod(runtime, receiver, methodName, args, Object.class);
  }

  private Ruby initializeRubyRuntime() throws URISyntaxException, IOException {
    URL astRubygem = RubyConverter.class.getResource(AST_RUBYGEM_PATH);
    URL parserRubygem = RubyConverter.class.getResource(PARSER_RUBYGEM_PATH);
    URL initParserScriptUrl = RubyConverter.class.getResource(SETUP_SCRIPT_PATH);

    Ruby rubyRuntime = JavaEmbedUtils.initialize(Arrays.asList(astRubygem.toString(), parserRubygem.toString()));
    URI initParserScriptUri = initParserScriptUrl.toURI();

    System.setProperty("jruby.thread.pool.enabled", "true");
    if ("jar".equalsIgnoreCase(initParserScriptUri.getScheme())) {
      // Need to init ZipFileSystem to read file
      Map<String, String> env = new HashMap<>();
      env.put("create", "true");
      FileSystems.newFileSystem(initParserScriptUri, env);
    }

    Path initParserScriptPath = Paths.get(initParserScriptUri);
    String initParserScript = new String(Files.readAllBytes(initParserScriptPath), UTF_8);
    rubyRuntimeAdapter.eval(rubyRuntime, initParserScript);
    return rubyRuntime;
  }

}
