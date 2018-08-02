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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.jruby.Ruby;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.exceptions.NoMethodError;
import org.jruby.exceptions.StandardError;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.ruby.converter.adapter.RangeAdapter;
import org.sonarsource.slang.api.ASTConverter;
import org.sonarsource.slang.api.TextPointer;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.impl.TopLevelTreeImpl;
import org.sonarsource.slang.plugin.ParseException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;

public class RubyConverter implements ASTConverter {

  private static final Logger LOG = Loggers.get(RubyConverter.class);

  private static final Path SETUP_SCRIPT_PATH = Paths.get("/whitequark_parser_init.rb");
  private static final Path AST_RUBYGEM_PATH = Paths.get("/ast-2.4.0", "lib");
  private static final Path PARSER_RUBYGEM_PATH = Paths.get("/parser-2.5.1.2", "lib");
  private static final RubyRuntimeAdapter rubyRuntimeAdapter = JavaEmbedUtils.newRuntimeAdapter();

  private final Ruby runtime;

  public RubyConverter() {
    try {
      runtime = initializeRubyRuntime();
    } catch (URISyntaxException | IOException e) {
      throw new IllegalStateException("Failed to initialized ruby runtime", e);
    }
  }

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
      if (diagnostic != null) {
        IRubyObject location = (IRubyObject) invokeMethod(diagnostic, "location", null);
        if (location != null) {
          return new RangeAdapter(runtime, location).toTextRange().start();
        }
      }
    } catch (NoMethodError nme) {
      LOG.warn("No location information available for parsing error");
    }
    return null;
  }

  private Tree parseContent(String content) {
    List rubyParseResult = callRubyParser(content);

    IRubyObject ast = (IRubyObject) rubyParseResult.get(0);
    List<IRubyObject> rubyComments = (List) rubyParseResult.get(1);
    List<IRubyObject> rubyTokens = (List) rubyParseResult.get(2);
    // TODO map to slang AST

    return new TopLevelTreeImpl(null, emptyList(), emptyList());
  }

  private List callRubyParser(String content) {
    Object[] parameters = {content};
    return (List) invokeMethod(runtime.getObject(), "parse_with_tokens", parameters);
  }

  private Object invokeMethod(Object receiver, String methodName, Object[] args) {
    return JavaEmbedUtils.invokeMethod(runtime, receiver, methodName, args, Object.class);
  }

  private static Ruby initializeRubyRuntime() throws URISyntaxException, IOException {
    URL astRubygem = RubyConverter.class.getResource(AST_RUBYGEM_PATH.toString());
    URL parserRubygem = RubyConverter.class.getResource(PARSER_RUBYGEM_PATH.toString());
    if (astRubygem == null || parserRubygem == null) {
      throw new IllegalStateException("Rubygems dependencies not found");
    }

    Ruby runtime = JavaEmbedUtils.initialize(Arrays.asList(astRubygem.toString(), parserRubygem.toString()));
    String initParserScript = new String(Files.readAllBytes(Paths.get(RubyConverter.class.getResource(SETUP_SCRIPT_PATH.toString()).toURI())), UTF_8);
    rubyRuntimeAdapter.eval(runtime, initParserScript);
    return runtime;
  }

}
