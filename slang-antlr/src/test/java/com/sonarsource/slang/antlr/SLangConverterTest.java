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

package com.sonarsource.slang.antlr;

import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.impl.IdentifierImpl;
import com.sonarsource.slang.impl.NativeTreeImpl;
import com.sonarsource.slang.parser.SLangConverter;
import com.sonarsource.slang.visitors.TreeContext;
import com.sonarsource.slang.visitors.TreeVisitor;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SLangConverterTest {
  @Test
  public void testFile() throws IOException {
    SLangConverter converter = new SLangConverter();
    Tree tree = converter.parse("src/test/resources/binary.slang");

    TreeVisitor<TreeContext> visitor = new TreeVisitor<>();
    visitor.register(NativeTreeImpl.class, (ctx, nativeTree) -> System.out.println("Native"));
    visitor.register(IdentifierImpl.class, (ctx, identifierTree) -> System.out.println(identifierTree.name()));
    visitor.scan(new TreeContext(), tree);

    assertThat(true, is(true));

  }
}
