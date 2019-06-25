/*
 * SonarSource SLang
 * Copyright (C) 2018-2019 SonarSource SA
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

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.persistence.JsonTestHelper;

import static org.assertj.core.api.Assertions.assertThat;

public class DeserializationContextTest extends JsonTestHelper {

  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();

  private DeserializationContext context = new DeserializationContext(JsonTreeConverter.POLYMORPHIC_CONVERTER)
    .withMetaDataProvider(metaDataProvider);

  @Test
  public void resolve_token() {
    Token token = otherToken(1, 0, "foo");
    JsonObject json = Json.object()
      .add("tokenReference", "1:0:1:3");

    assertThat(context.fieldToToken(json, "tokenReference")).isSameAs(token);
    assertThat(context.fieldToNullableToken(json, "tokenReference")).isSameAs(token);
    assertThat(context.fieldToNullableToken(json, "unknown")).isNull();
  }

  @Test
  public void resolve_token_not_found() {
    exceptionRule.expect(NoSuchElementException.class);
    exceptionRule.expectMessage("Token not found: 7:13:7:20");

    otherToken(1, 0, "foo");
    JsonObject json = Json.object()
      .add("tokenReference", "7:13:7:20");

    context.fieldToToken(json, "tokenReference");
  }

  @Test
  public void resolve_metadata() {
    Token token = otherToken(1, 0, "foo");
    JsonObject json = Json.object()
      .add("metaData", "1:0:1:3");

    assertThat(context.metaData(json)).isNotNull();
    assertThat(context.metaData(json).textRange()).isEqualTo(token.textRange());
  }

  @Test
  public void resolve_metadata_not_found() {
    exceptionRule.expect(IllegalStateException.class);
    exceptionRule.expectMessage("Missing non-null value for field 'metaData' at '' member: {\"field1\":\"42\"}");

    JsonObject json = Json.object()
      .add("field1", "42");

    context.metaData(json);
  }

  @Test
  public void object_list() {
    List<String> nodes = Arrays.asList("A", "B", "C");
    JsonArray array = Json.array();
    nodes.stream().map(value -> Json.object().add("value", value)).forEach(array::add);

    List<String> actual = context.objectList(array, (ctx, object) -> object.getString("value", null));
    assertThat(actual).containsExactly("A", "B", "C");

    assertThat(context.objectList(null, (ctx, object) -> object)).isEmpty();
    assertThat(context.objectList(Json.NULL, (ctx, object) -> object)).isEmpty();
  }

  @Test
  public void invalid_object_list() {
    exceptionRule.expect(IllegalStateException.class);
    exceptionRule.expectMessage("Expect Array instead of JsonNumber at 'root' member: 42");
    context.pushPath("root");
    context.objectList(Json.value(42), (ctx, object) -> object);
  }

  @Test
  public void field_to_nullable_string() {
    JsonObject json = Json.object()
      .add("field1", "abc");
    assertThat(context.fieldToNullableString(json, "field1")).isEqualTo("abc");
    assertThat(context.fieldToNullableString(json, "field2")).isNull();
  }

  @Test
  public void field_to_nullable_invalid_string() {
    exceptionRule.expect(IllegalStateException.class);
    exceptionRule.expectMessage("Expect String instead of 'JsonNumber' for field 'field1' at 'root' member: {\"field1\":42}");
    JsonObject json = Json.object()
      .add("field1", 42);
    context.pushPath("root");
    context.fieldToNullableString(json, "field1");
  }

  @Test
  public void field_to_string() {
    JsonObject json = Json.object()
      .add("field1", "abc");
    assertThat(context.fieldToString(json, "field1")).isEqualTo("abc");
  }

  @Test
  public void field_to_missing_string() {
    exceptionRule.expect(IllegalStateException.class);
    exceptionRule.expectMessage("Missing non-null value for field 'field2' at 'TopLevel/AssignmentExpression' member: {\"field1\":\"abc\"}");
    JsonObject json = Json.object()
      .add("field1", "abc");
    context.pushPath("TopLevel");
    context.pushPath("AssignmentExpression");
    context.fieldToString(json, "field2");
  }

  @Test
  public void field_to_null_string() {
    exceptionRule.expect(IllegalStateException.class);
    exceptionRule.expectMessage("Missing non-null value for field 'field1' at 'TopLevel/AssignmentExpression' member: {\"field1\":null}");
    JsonObject json = Json.object()
      .add("field1", Json.NULL);
    context.pushPath("TopLevel");
    context.pushPath("AssignmentExpression");
    context.fieldToString(json, "field1");
  }

  @Test
  public void field_to_range() {
    JsonObject json = Json.object()
      .add("field1", "1:2:3:4");
    TextRange range = context.fieldToRange(json, "field1");
    assertThat(range.start().line()).isEqualTo(1);
    assertThat(range.start().lineOffset()).isEqualTo(2);
    assertThat(range.end().line()).isEqualTo(3);
    assertThat(range.end().lineOffset()).isEqualTo(4);
  }

  @Test
  public void field_to_range_missing() {
    exceptionRule.expect(IllegalStateException.class);
    exceptionRule.expectMessage("Missing non-null value for field 'field2' at 'root' member: {\"field1\":\"1:2:3:4\"}");
    JsonObject json = Json.object()
      .add("field1", "1:2:3:4");
    context.pushPath("root");
    context.fieldToRange(json, "field2");
  }

}
