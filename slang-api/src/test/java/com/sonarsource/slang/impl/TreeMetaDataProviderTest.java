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
package com.sonarsource.slang.impl;

import com.sonarsource.slang.api.Comment;
import java.util.Collections;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TreeMetaDataProviderTest {

  @Test
  public void commentsInside() {
    Comment comment = new CommentImpl("comment1", "// comment1", new TextRangeImpl(2, 5, 2, 12));
    TreeMetaDataProvider provider = new TreeMetaDataProvider(Collections.singletonList(comment));
    assertThat(provider.metaData(new TextRangeImpl(1, 1, 1, 20)).commentsInside()).isEmpty();
    assertThat(provider.metaData(new TextRangeImpl(2, 1, 2, 20)).commentsInside()).containsExactly(comment);
  }


}
