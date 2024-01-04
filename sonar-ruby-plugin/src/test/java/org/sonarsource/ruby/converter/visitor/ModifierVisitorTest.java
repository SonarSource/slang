/*
 * SonarSource SLang
 * Copyright (C) 2018-2024 SonarSource SA
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

import org.junit.jupiter.api.Test;
import org.sonarsource.ruby.converter.AbstractRubyConverterTest;
import org.sonarsource.slang.api.*;

import java.util.List;
import org.sonarsource.slang.impl.ClassDeclarationTreeImpl;

import static org.assertj.core.api.Assertions.assertThat;

class ModifierVisitorTest extends AbstractRubyConverterTest {

  @Test
  void test() {
    ClassDeclarationTree tree = (ClassDeclarationTree) rubyStatement("class Foo\n" +
      "  def public_function(a)\n" +
      "    puts \"Hello\"\n" +
      "  end\n" +

      "  private\n" +

      "  def is_no\n" +
      "    puts \"Hello!\"\n" +
      "  end\n" +
      "end");

    NativeTree nativeClassTree = (NativeTree) tree.children().get(0);
    assertThat(nativeClassTree.nativeKind()).isEqualTo(nativeKind("class"));
    BlockTree blockTree = (BlockTree) nativeClassTree.children().get(1);
    List<Tree> blockTreeChildren = blockTree.children();
    assertThat(blockTreeChildren).hasSize(3);
    Tree modifierTree = blockTreeChildren.get(1);
    assertThat(modifierTree).isInstanceOf(ModifierTree.class);
    assertModifierIsPrivate(modifierTree);
    FunctionDeclarationTree noModifierFnTree = (FunctionDeclarationTree) blockTreeChildren.get(0);
    assertThat(noModifierFnTree.modifiers()).isEmpty();
    assertFnIsPrivate(blockTreeChildren.get(2));
  }

  @Test
  void testInlinePrivate() {
    ClassDeclarationTree tree = (ClassDeclarationTree) rubyStatement("class Foo\n" +
            "  private def pick_coder(coder)\n" +
            "    case coder\n" +
            "    when nil, \"json\"\n" +
            "      ActiveSupport::JSON\n" +
            "    when \"custom\"\n" +
            "      DummyEncoder\n" +
            "    when \"none\"\n" +
            "      nil\n" +
            "    end\n" +
            "  end\n" +
            "end");

    NativeTree nativeClassTree = (NativeTree) tree.children().get(0);
    assertThat(nativeClassTree.nativeKind()).isEqualTo(nativeKind("class"));

    NativeTree modifierNativeTree = (NativeTree) nativeClassTree.children().get(1);
    assertThat(modifierNativeTree.nativeKind()).isEqualTo(nativeKind("modifier"));
    List<Tree> modifierNativeChildren = modifierNativeTree.children();
    assertThat(modifierNativeChildren).hasSize(2);

    assertModifierIsPrivate(modifierNativeChildren.get(0));
    assertFnIsPrivate(modifierNativeChildren.get(1));
  }

  @Test
  void testMultiplePrivate() {
    ClassDeclarationTree tree = (ClassDeclarationTree) rubyStatement("class Foo\n" +
      "  private\n" +

      "  def private1\n" +
      "    puts \"Hello!\"\n" +
      "  end\n" +

      "  public\n" +

      "  def public1\n" +
      "    puts \"Hello!\"\n" +
      "  end\n" +

      "  private\n" +

      "  def private2\n" +
      "    puts \"Hello!\"\n" +
      "  end\n" +

      "end");

    NativeTree nativeClassTree = (NativeTree) tree.children().get(0);
    BlockTree blockTree = (BlockTree) nativeClassTree.children().get(1);
    List<Tree> blockTreeChildren = blockTree.children();
    assertFnIsPrivate(blockTreeChildren.get(1));
    assertFnIsPrivate(blockTreeChildren.get(5));
  }

  @Test
  void testPublicPrivate() {
    ClassDeclarationTree tree = (ClassDeclarationTree) rubyStatement("class Foo\n" +
      "  private\n" +

      "  public\n" +

      "  def public\n" +
      "    puts \"Hello!\"\n" +
      "  end\n" +

      "end");

    NativeTree nativeClassTree = (NativeTree) tree.children().get(0);
    BlockTree blockTree = (BlockTree) nativeClassTree.children().get(1);
    List<Tree> blockTreeChildren = blockTree.children();
    assertFnIsPublic(blockTreeChildren.get(2));
  }

  @Test
  void testPrivateInInnerClass() {
    ClassDeclarationTree tree = (ClassDeclarationTree) rubyStatement("class Foo\n" +
      "  class Inner\n" +

      "    def inner_public(a)\n" +
      "      puts \"Hello!\"\n" +
      "    end\n" +

      "    private\n" +

      "    def inner_private(a)\n" +
      "      puts \"Hello!\"\n" +
      "    end\n" +

      "  end\n" +
      "end");

    NativeTree nativeClassTree = (NativeTree) tree.children().get(0);
    ClassDeclarationTree innerClassDeclarationTree = (ClassDeclarationTreeImpl) nativeClassTree.children().get(1);
    NativeTree innerClassTree = (NativeTree) innerClassDeclarationTree.children().get(0);
    BlockTree blockTree = (BlockTree) innerClassTree.children().get(1);
    List<Tree> blockTreeChildren = blockTree.children();
    assertThat(blockTreeChildren).hasSize(3);
    assertFnIsPrivate(blockTreeChildren.get(2));
  }

  @Test
  void testProtected() {
    ClassDeclarationTree tree = (ClassDeclarationTree) rubyStatement("class Foo\n" +
      "  protected\n" +

      "  def protected\n" +
      "    puts \"Hello!\"\n" +
      "  end\n" +

      "end");

    NativeTree nativeClassTree = (NativeTree) tree.children().get(0);
    BlockTree blockTree = (BlockTree) nativeClassTree.children().get(1);
    List<Tree> blockTreeChildren = blockTree.children();
    assertFnIsProtected(blockTreeChildren.get(1));
  }


  private void assertFnIsPrivate(Tree fnTree) {
    assertFnModifier(fnTree, ModifierTree.Kind.PRIVATE);
  }

  private void assertFnIsPublic(Tree fnTree) {
    assertFnModifier(fnTree, ModifierTree.Kind.PUBLIC);
  }

  private void assertFnIsProtected(Tree fnTree) {
    assertFnModifier(fnTree, ModifierTree.Kind.PROTECTED);
  }

  private void assertFnModifier(Tree fnTree, ModifierTree.Kind kind) {
    FunctionDeclarationTree tree = (FunctionDeclarationTree) fnTree;
    assertThat(((ModifierTree) tree.modifiers().get(0)).kind()).isEqualTo(kind);
  }

  private void assertModifierIsPrivate(Tree modifierTree) {
    assertThat(((ModifierTree) modifierTree).kind()).isEqualTo(ModifierTree.Kind.PRIVATE);
  }
}
