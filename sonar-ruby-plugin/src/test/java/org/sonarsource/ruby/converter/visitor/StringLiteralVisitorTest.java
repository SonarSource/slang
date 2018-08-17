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
package org.sonarsource.ruby.converter.visitor;

import org.junit.Test;
import org.sonarsource.ruby.converter.AbstractRubyConverterTest;
import org.sonarsource.slang.api.BlockTree;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.StringLiteralTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;

public class StringLiteralVisitorTest extends AbstractRubyConverterTest {

  @Test
  public void plain_string_literal() {
    StringLiteralTree tree = (StringLiteralTree) rubyStatement("'foo'");
    assertTree(tree).isStringLiteral("foo");
  }

  @Test
  public void file_macro() {
    NativeTree tree = (NativeTree) rubyStatement("__FILE__");
    assertThat(((NativeTree) tree.children().get(0)).nativeKind()).isEqualTo(nativeKind("(SonarRuby analysis)"));
  }

  @Test
  public void heredoc_literal() {
    NativeTree tree =  (NativeTree) rubyStatement("<<-CODE\n" +
      "      get '/#{asset}' do\n" +
      "        redirect asset_path('#{asset}', protocol: 'http')\n" +
      "      end\n" +
      "    CODE\n");
    assertTree(tree).hasChildren(7);
    assertTree(tree).hasChildren(
      NativeTree.class,
      BlockTree.class,
      NativeTree.class,
      NativeTree.class,
      BlockTree.class,
      NativeTree.class,
      NativeTree.class);
  }

  @Test
  public void interpolated_string() {
    NativeTree tree =  (NativeTree) rubyStatement("\"foo#{bar}baz\"");
    assertTree(tree).hasChildren(3);
    assertTree(tree).hasChildren(NativeTree.class, BlockTree.class, NativeTree.class);
  }



}
