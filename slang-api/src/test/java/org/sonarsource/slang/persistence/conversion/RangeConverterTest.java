/*
 * SonarSource SLang
 * Copyright (C) 2018-2020 SonarSource SA
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
package org.sonarsource.slang.persistence.conversion;

import java.util.NoSuchElementException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.sonarsource.slang.impl.LiteralTreeImpl;
import org.sonarsource.slang.impl.TextRangeImpl;
import org.sonarsource.slang.persistence.JsonTestHelper;

import static org.assertj.core.api.Assertions.assertThat;

public class RangeConverterTest extends JsonTestHelper {

  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();

  @Test
  public void format() {
    TextRange initialRange = new TextRangeImpl(3, 7, 4, 12);
    String actual = RangeConverter.format(initialRange);
    assertThat(actual).isEqualTo("3:7:4:12");

    assertThat(RangeConverter.format(null)).isNull();
  }

  @Test
  public void parse() {
    TextRange range = RangeConverter.parse("3:7:4:12");
    assertThat(range.start().line()).isEqualTo(3);
    assertThat(range.start().lineOffset()).isEqualTo(7);
    assertThat(range.end().line()).isEqualTo(4);
    assertThat(range.end().lineOffset()).isEqualTo(12);
  }

  @Test
  public void parse_null_string() {
    assertThat(RangeConverter.parse(null)).isNull();
  }

  @Test
  public void parse_invalid_string() {
    exceptionRule.expect(IllegalArgumentException.class);
    exceptionRule.expectMessage("Invalid TextRange '12345'");
    RangeConverter.parse("12345");
  }

  @Test
  public void token_reference() {
    Token token = otherToken(3, 7, "foo");
    assertThat(RangeConverter.tokenReference(token)).isEqualTo("3:7:3:10");
    assertThat(RangeConverter.tokenReference(null)).isNull();
  }

  @Test
  public void resolve_token() {
    Token token = otherToken(1, 0, "foo");
    Token actual = RangeConverter.resolveToken(metaDataProvider, "1:0:1:3");
    assertThat(actual).isSameAs(token);
    assertThat(RangeConverter.resolveToken(metaDataProvider, null)).isNull();
  }

  @Test
  public void resolve_invalid_token() {
    exceptionRule.expect(NoSuchElementException.class);
    exceptionRule.expectMessage("Token not found: 2:0:2:3");
    otherToken(1, 0, "foo");
    RangeConverter.resolveToken(metaDataProvider, "2:0:2:3");
  }

  @Test
  public void metadata_reference() {
    Token token = otherToken(1,0,"true");
    Tree tree = new LiteralTreeImpl(metaData(token), token.text());
    assertThat(RangeConverter.metaDataReference(tree)).isEqualTo("1:0:1:4");
  }

  @Test
  public void resolve_metadata() {
    Token token = otherToken(3,5,"true");
    TreeMetaData metaData = RangeConverter.resolveMetaData(metaDataProvider, "3:5:3:9");
    assertThat(metaData).isNotNull();
    assertThat(metaData.textRange()).isEqualTo(token.textRange());
  }

}
