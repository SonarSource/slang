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
package org.sonarsource.ruby.checks;

import org.junit.Test;
import org.sonarsource.slang.checks.UnusedFunctionParameterCheck;

public class UnusedFunctionParameterCheckTest {

  @Test
  public void test(){
    RubyVerifier.verify("UnusedFunctionParameter.rb", new UnusedFunctionParameterRubyCheck());
  }

  @Test
  public void test_single_function(){
    RubyVerifier.verify("UnusedFunctionParameter.SingleFunction.rb", new UnusedFunctionParameterRubyCheck());
  }

  @Test
  public void test_single_class(){
    // SONARSLANG-520 access modifiers are not supported (we cannot see a method is private)
    RubyVerifier.verifyNoIssue("UnusedFunctionParameter.SingleClass.rb", new UnusedFunctionParameterRubyCheck());
  }
}
