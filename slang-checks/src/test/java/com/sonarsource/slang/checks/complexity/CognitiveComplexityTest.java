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
package com.sonarsource.slang.checks.complexity;

import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.parser.SLangConverter;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CognitiveComplexityTest {

  private SLangConverter parser = new SLangConverter();

  @Test
  public void unrelated_statement() {
    assertThat(complexity("42;").value()).isEqualTo(0);
  }

  @Test
  public void if_statements() {
    assertThat(complexity("if (x) { 42 };").value()).isEqualTo(1);
    assertThat(complexity("if (x) { 42 } else { 43 };").value()).isEqualTo(2);
    assertThat(complexity("if (x) { 42 } else if (y) { 43 };").value()).isEqualTo(2);
    assertThat(complexity("if (x) { 42 } else if (y) { 43 } else { 44 };").value()).isEqualTo(3);
  }

  @Test
  public void loop_statements() {
    assertThat(complexity("while (x) { 42 };").value()).isEqualTo(1);
  }

  @Test
  public void match_statements() {
    assertThat(complexity("match (x) { else -> 42; };").value()).isEqualTo(1);
    assertThat(complexity("match (x) { 'a' -> 0; else -> 42; };").value()).isEqualTo(1);
  }

  @Test
  public void try_catch_statements() {
    assertThat(complexity("try { foo; };").value()).isEqualTo(0);
    assertThat(complexity("try { foo; } catch (e1) { bar; };").value()).isEqualTo(1);
    assertThat(complexity("try { foo; } catch (e1) { bar; } catch (e2) { baz; };").value()).isEqualTo(2);
    assertThat(complexity("try { foo; } finally { bar; };").value()).isEqualTo(0);
  }

  @Test
  public void functions() {
    assertThat(complexity("fun foo() { 42 }").value()).isEqualTo(0);
    assertThat(complexity("fun foo() { f = fun() { 42 }; }").value()).isEqualTo(0);
  }

  @Test
  public void binary_operators() {
    assertThat(complexity("a == b;").value()).isEqualTo(0);
    assertThat(complexity("a && b;").value()).isEqualTo(1);
    assertThat(complexity("a || b;").value()).isEqualTo(1);
    assertThat(complexity("a && b && c;").value()).isEqualTo(1);
    assertThat(complexity("a || b || c;").value()).isEqualTo(1);
    assertThat(complexity("a || b && c;").value()).isEqualTo(2);
    assertThat(complexity("a || b && c || d;").value()).isEqualTo(3);
  }

  @Test
  public void nesting() {
    assertThat(complexity("if (x) a && b;").value()).isEqualTo(2);
    assertThat(complexity("if (x) if (y) 42;").value()).isEqualTo(3);
    assertThat(complexity("while (x) if (y) 42;").value()).isEqualTo(3);
    assertThat(complexity("match (x) { else -> if (y) 42; };").value()).isEqualTo(3);
    assertThat(complexity("try { x } catch (e) { if (y) 42; };").value()).isEqualTo(3);
    assertThat(complexity("try { if (y) 42; } catch (e) { x };").value()).isEqualTo(2);
    assertThat(complexity("fun foo() { if (x) 42; }").value()).isEqualTo(1);
    assertThat(complexity("fun foo() { f = fun() { if (x) 42; }; }").value()).isEqualTo(2);
    assertThat(complexity("if (x) { f = fun() { if (x) 42; }; };").value()).isEqualTo(4);
  }

  private CognitiveComplexity complexity(String code) {
    Tree tree = parser.parse(code);
    return new CognitiveComplexity(tree);
  }
}
