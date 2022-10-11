/*
 * SonarSource SLang
 * Copyright (C) 2018-2022 SonarSource SA
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubySymbol;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.specialized.RubyArrayTwoObject;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.impl.TokenImpl;

public class TokenAdapter extends JRubyObjectAdapter<RubyArrayTwoObject> {
  private static final Set<String> ACCESS_MODIFIERS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("private", "protected", "public")));

  private static final Set<String> RUBY_STRING_TOKENS = Stream.of("tSTRING", "tSTRING_BEG", "tSTRING_CONTENT", "tSTRING_END")
    .collect(Collectors.toSet());

  public TokenAdapter(Ruby runtime, RubyArrayTwoObject underlyingRubyObject) {
    super(runtime, underlyingRubyObject);
  }

  public RubySymbol getTokenType() {
    return (RubySymbol) underlyingRubyObject.eltInternal(0);
  }

  @Nullable
  public String getText() {
    Object textElement = ((RubyArray) underlyingRubyObject.eltInternal(1)).get(0);
    if (textElement != null) {
      return textElement.toString();
    }
    return null;
  }

  public RangeAdapter getRange() {
    return new RangeAdapter(runtime, (IRubyObject) ((RubyArray) underlyingRubyObject.eltInternal(1)).get(1));
  }

  @Nullable
  public Token toSlangToken() {
    Token.Type type = Token.Type.OTHER;
    RubySymbol tokenType = getTokenType();
    String tokenString = tokenType.asJavaString();
    if (tokenString != null && (tokenString.startsWith("k") || isAccessModifier(tokenString))) {
      type = Token.Type.KEYWORD;
    } else if (RUBY_STRING_TOKENS.contains(tokenString)) {
      type = Token.Type.STRING_LITERAL;
    }

    String text = getText();
    // String literals are allowed to be empty here, as the parser consider only the content of the string as text.
    // So, although the string content is empty (empty string), there is actually some text here, which are the surrounding quotes.
    if (text == null || (text.length() == 0 && type != Token.Type.STRING_LITERAL)) {
      return null;
    }
    return new TokenImpl(getRange().toTextRange(), getText(), type);
  }

  private boolean isAccessModifier(String tokenString) {
    // Because the name of access modifiers can be used for all identifiers, we sometimes wrongly interpret variable identifiers as access modifiers.
    return tokenString.equals("tIDENTIFIER") && ACCESS_MODIFIERS.contains(this.getText());
  }

}
