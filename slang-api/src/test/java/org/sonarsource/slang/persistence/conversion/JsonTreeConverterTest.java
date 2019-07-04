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
import java.io.IOException;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarsource.slang.api.Comment;
import org.sonarsource.slang.api.JumpTree;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.api.TreeMetaData;
import org.sonarsource.slang.impl.BaseTreeImpl;
import org.sonarsource.slang.impl.TextRangeImpl;
import org.sonarsource.slang.impl.TokenImpl;
import org.sonarsource.slang.impl.TreeMetaDataProvider;
import org.sonarsource.slang.persistence.JsonTestHelper;
import org.sonarsource.slang.persistence.JsonTree;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.persistence.conversion.JsonTreeConverter.COMMENT_FROM_JSON;
import static org.sonarsource.slang.persistence.conversion.JsonTreeConverter.COMMENT_TO_JSON;
import static org.sonarsource.slang.persistence.conversion.JsonTreeConverter.CONTENT_RANGE;
import static org.sonarsource.slang.persistence.conversion.JsonTreeConverter.CONTENT_TEXT;
import static org.sonarsource.slang.persistence.conversion.JsonTreeConverter.TEXT;
import static org.sonarsource.slang.persistence.conversion.JsonTreeConverter.TOKEN_FROM_JSON;
import static org.sonarsource.slang.persistence.conversion.JsonTreeConverter.TOKEN_TO_JSON;
import static org.sonarsource.slang.persistence.conversion.JsonTreeConverter.TREE_METADATA_PROVIDER_FROM_JSON;
import static org.sonarsource.slang.persistence.conversion.JsonTreeConverter.TREE_METADATA_PROVIDER_TO_JSON;
import static org.sonarsource.slang.persistence.conversion.JsonTreeConverter.TYPE;

public class JsonTreeConverterTest extends JsonTestHelper {

  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();

  private SerializationContext writeContext = new SerializationContext(JsonTreeConverter.POLYMORPHIC_CONVERTER);
  private DeserializationContext readContext = new DeserializationContext(JsonTreeConverter.POLYMORPHIC_CONVERTER);

  @Test
  public void tree_metadata_provider() throws IOException {
    Comment initialComment = comment(1, 0, "// hello", 2, 0);

    TextRange tokenRange = new TextRangeImpl(2, 0, 2, 3);
    Token initialToken = new TokenImpl(tokenRange, "fun", Token.Type.KEYWORD);

    TreeMetaDataProvider provider = new TreeMetaDataProvider(
      singletonList(initialComment),
      singletonList(initialToken));

    String actual = indentedJson(TREE_METADATA_PROVIDER_TO_JSON.apply(writeContext, provider).toString());
    assertThat(actual).isEqualTo(indentedJsonFromFile("tree_metadata_provider.json"));

    provider = TREE_METADATA_PROVIDER_FROM_JSON.apply(readContext, Json.parse(actual).asObject());
    assertThat(provider.allComments()).hasSize(1);
    Comment comment = provider.allComments().get(0);
    assertThat(comment.text()).isEqualTo("// hello");
    assertThat(comment.contentText()).isEqualTo(" hello");
    assertThat(comment.textRange()).isEqualTo(initialComment.textRange());
    assertThat(comment.contentRange()).isEqualTo(initialComment.contentRange());

    assertThat(provider.allTokens()).hasSize(1);
    Token token = provider.allTokens().get(0);
    assertThat(token.textRange()).isEqualTo(initialToken.textRange());
    assertThat(token.text()).isEqualTo("fun");
    assertThat(token.type()).isEqualTo(Token.Type.KEYWORD);

    assertThat(methodNames(TreeMetaDataProvider.class))
      .containsExactlyInAnyOrder("allComments", "previousToken", "updateTokenType", "firstToken",
        "allTokens", "indexOfFirstToken", "keyword");
  }

  @Test
  public void comment() throws IOException {
    Comment initialComment = comment(3, 7, "// hello", 2, 0);
    String actual = indentedJson(COMMENT_TO_JSON.apply(writeContext, initialComment).toString());
    assertThat(actual).isEqualTo(indentedJsonFromFile("comment.json"));
    Comment comment = COMMENT_FROM_JSON.apply(readContext, Json.parse(actual).asObject());
    assertThat(comment.text()).isEqualTo("// hello");
    assertThat(comment.contentText()).isEqualTo(" hello");
    assertThat(comment.textRange()).isEqualTo(initialComment.textRange());
    assertThat(comment.contentRange()).isEqualTo(initialComment.contentRange());

    assertThat(methodNames(Comment.class))
      .containsExactlyInAnyOrder(TEXT, CONTENT_TEXT, CONTENT_RANGE);
  }

  @Test
  public void token() throws IOException {
    Token initialToken = otherToken(3, 7, "foo");
    String actual = indentedJson(TOKEN_TO_JSON.apply(writeContext, initialToken).toString());
    assertThat(actual).isEqualTo(indentedJsonFromFile("token.json"));
    Token token = TOKEN_FROM_JSON.apply(readContext, Json.parse(actual).asObject());
    assertThat(token.textRange()).isEqualTo(initialToken.textRange());
    assertThat(token.text()).isEqualTo("foo");
    assertThat(token.type()).isEqualTo(Token.Type.OTHER);

    assertThat(methodNames(Token.class))
      .containsExactlyInAnyOrder(TEXT, TYPE);
  }

  @Test
  public void error_missing_type() throws IOException {
    exceptionRule.expect(IllegalStateException.class);
    exceptionRule.expectMessage("Missing non-null value for field '@type' at 'tree/Return/body'" +
      " member: {\"invalid_type\":\"Literal\",\"metaData\":\"1:7:1:11\",\"value\":\"true\"}");
    String invalidJson = indentedJsonFromFile("error_missing_type.json");
    JsonTree.fromJson(invalidJson);
  }

  @Test
  public void error_invalid_json_tree() throws IOException {
    exceptionRule.expect(IllegalStateException.class);
    exceptionRule.expectMessage("Unexpected value for Tree at 'tree/Return/body' member: 1234");
    String invalidJson = indentedJsonFromFile("error_invalid_json_tree.json");
    JsonTree.fromJson(invalidJson);
  }

  @Test
  public void error_invalid_tree_type() throws IOException {
    exceptionRule.expect(IllegalStateException.class);
    exceptionRule.expectMessage("Invalid '@type' value at 'tree/Return/body/UnsupportedType' member: UnsupportedType");
    String invalidJson = indentedJsonFromFile("error_invalid_tree_type.json");
    JsonTree.fromJson(invalidJson);
  }

  @Test
  public void error_unsupported_tree_class() throws IOException {
    exceptionRule.expect(IllegalStateException.class);
    exceptionRule.expectMessage("Unsupported tree class: org.sonarsource.slang.persistence.conversion.JsonTreeConverterTest$UnsupportedTree");
    Token token = otherToken(1, 0, "x");
    UnsupportedTree tree = new UnsupportedTree(metaData(token));
    JsonTree.toJson(tree);
  }

  @Test
  public void error_unsupported_implementation_class() throws IOException {
    exceptionRule.expect(IllegalStateException.class);
    exceptionRule.expectMessage("Unsupported implementation class: org.sonarsource.slang.persistence.conversion.JsonTreeConverterTest$UnsupportedTree");
    Token token = otherToken(1, 0, "x");
    UnsupportedTree tree = new UnsupportedTree(metaData(token));
    SerializationContext ctx = new SerializationContext(JsonTreeConverter.POLYMORPHIC_CONVERTER);
    ctx.newTypedObject(tree);
  }

  class UnsupportedTree extends BaseTreeImpl {
    public UnsupportedTree(TreeMetaData metaData) {
      super(metaData);
    }

    @Override
    public List<Tree> children() {
      return emptyList();
    }
  }

  @Test
  public void error_unexpected_match_child_class() throws IOException {
    exceptionRule.expect(IllegalStateException.class);
    exceptionRule.expectMessage("Unexpected 'org.sonarsource.slang.impl.IntegerLiteralTreeImpl'" +
      " type for member 'cases[]' instead of" +
      " 'org.sonarsource.slang.api.MatchCaseTree'" +
      " at 'tree/Match/cases[]/IntegerLiteral'" +
      " member: {\"@type\":\"IntegerLiteral\",\"metaData\":\"1:17:1:19\",\"value\":\"42\"}");
    String invalidJson = indentedJsonFromFile("error_unexpected_match_child_class.json");
    JsonTree.fromJson(invalidJson);
  }

  @Test
  public void error_unary_expression_without_child() throws IOException {
    exceptionRule.expect(IllegalStateException.class);
    exceptionRule.expectMessage("Unexpected null value for field 'operand' at 'tree/UnaryExpression' member: null");
    String invalidJson = indentedJsonFromFile("error_unary_expression_without_child.json");
    JsonTree.fromJson(invalidJson);
  }

  @Test
  public void error_unary_expression_with_null_child() throws IOException {
    exceptionRule.expect(IllegalStateException.class);
    exceptionRule.expectMessage("Unexpected null value for field 'operand' at 'tree/UnaryExpression' member: null");
    String invalidJson = indentedJsonFromFile("error_unary_expression_with_null_child.json");
    JsonTree.fromJson(invalidJson);
  }

  @Test
  public void nullable_child_can_be_omitted() throws IOException {
    JumpTree jump = (JumpTree) JsonTree.fromJson(indentedJsonFromFile("nullable_child_can_be_omitted.json"));
    assertThat(jump.label()).isNull();
  }

}