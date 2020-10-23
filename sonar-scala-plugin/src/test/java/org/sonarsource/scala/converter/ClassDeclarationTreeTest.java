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
package org.sonarsource.scala.converter;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.sonarsource.slang.api.Annotation;
import org.sonarsource.slang.api.ClassDeclarationTree;
import org.sonarsource.slang.api.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarsource.slang.testing.TreeAssert.assertTree;

public class ClassDeclarationTreeTest extends AbstractScalaConverterTest {
  @Test
  public void empty_class() {
    Tree tree = scalaStatement("class Foo {}");
    assertTree(tree).isEquivalentTo(scalaStatement("class Foo {}"));
    assertTree(tree).isNotEquivalentTo(scalaStatement("class FooBar {}"));
    assertTree(tree).isNotEquivalentTo(scalaStatement("class Foo { def bar() {}}"));
    ClassDeclarationTree classTree = (ClassDeclarationTree) tree;
    assertThat(classTree.identifier().name()).isEqualTo("Foo");
  }

  @Test
  public void class_with_method() {
    Tree tree = scalaStatement("class Foo { def bar() {} }");
    assertTree(tree).isEquivalentTo(scalaStatement("class Foo { def bar() {} }"));
    assertTree(tree).isNotEquivalentTo(scalaStatement("class Foo { def foo() {} }"));
    ClassDeclarationTree classTree = (ClassDeclarationTree) tree;
    assertThat(classTree.identifier().name()).isEqualTo("Foo");
  }

  @Test
  public void class_with_annotation() {
    Tree tree = scalaStatement("@my.test.MyAnnotation(\"something\")\n" +
      "@MyAnnotation2\n" +
      "class A {}");

    List<Annotation> annotations = tree.metaData().annotations();
    assertThat(annotations).hasSize(2);
    Annotation firstAnnotation = annotations.get(0);
    assertThat(firstAnnotation.shortName()).isEqualTo("MyAnnotation");
    assertThat(firstAnnotation.argumentsText()).containsExactly("\"something\"");
    Annotation secondAnnotation = annotations.get(1);
    assertThat(secondAnnotation.shortName()).isEqualTo("MyAnnotation2");
    assertThat(secondAnnotation.argumentsText()).isEmpty();
  }

  @Test
  public void testClassWithComplexAnnotation() {
    Tree tree = scalaStatement("@my.test.MyAnnotation(value = \"something\", \"somethingElse\", otherValue = Array(\"a\", \"b\"))\n" +
      "class A {}");

    List<Annotation> annotations = tree.metaData().annotations();
    assertThat(annotations).hasSize(1);
    Annotation firstAnnotation = annotations.get(0);
    assertThat(firstAnnotation.shortName()).isEqualTo("MyAnnotation");
    assertThat(firstAnnotation.argumentsText()).containsExactly("value = \"something\"", "\"somethingElse\"", "otherValue = Array(\"a\", \"b\")");
  }

  @Test
  public void testClassWithAnnotatedMember() {
    Tree tree = scalaStatement("class A {\n" +
      "@MyAnnotation\n" +
      "def f(@MyAnnotation i: Int) = { }" +
      "}\n");

    assertThat(tree.metaData().annotations()).isEmpty();

    List<Tree> annotatedDescendants = tree.descendants().filter(d -> !d.metaData().annotations().isEmpty()).collect(Collectors.toList());
    // FunctionTree and ParameterTree + two annotations that are mapped to native trees.
    assertThat(annotatedDescendants).hasSize(4);
    annotatedDescendants.forEach(descendant -> {
      List<Annotation> annotations = descendant.metaData().annotations();
      assertThat(annotations).hasSize(1);
      Annotation annotation = annotations.get(0);
      assertThat(annotation.shortName()).isEqualTo("MyAnnotation");
      assertThat(annotation.argumentsText()).isEmpty();
    });
  }

}
