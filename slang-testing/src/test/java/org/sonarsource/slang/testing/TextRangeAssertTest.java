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
package org.sonarsource.slang.testing;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonarsource.slang.testing.TextRangeAssert.assertTextRange;

public class TextRangeAssertTest {

  private TextRange range;

  @Before
  public void setup() {
    TextPointer start = mock(TextPointer.class);
    when(start.line()).thenReturn(1);
    when(start.lineOffset()).thenReturn(2);

    TextPointer end = mock(TextPointer.class);
    when(end.line()).thenReturn(3);
    when(end.lineOffset()).thenReturn(4);

    range = mock(TextRange.class);
    when(range.start()).thenReturn(start);
    when(range.end()).thenReturn(end);
  }

  @Test
  public void range_ok() {
    assertTextRange(range).hasRange(1, 2, 3, 4);
  }

  @Test(expected = AssertionError.class)
  public void range_failure() {
    assertTextRange(range).hasRange(1, 2, 3, 5);
  }

}
