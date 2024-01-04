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
package org.sonarsource.scala.externalreport.scapegoat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.rules.RuleType;
import org.sonarsource.slang.testing.ThreadLocalLogTester;
import org.sonarsource.scala.externalreport.scalastyle.ScalastyleSensorTest;

import static org.assertj.core.api.Assertions.assertThat;

class ScapegoatSensorTest {

  @RegisterExtension
  public ThreadLocalLogTester logTester = new ThreadLocalLogTester();

  @Test
  void scapegoat_issues_with_sonarqube() throws IOException {
    ScapegoatSensor sensor = new ScapegoatSensor(new ArrayList<String>()::add);
    List<ExternalIssue> externalIssues = ScalastyleSensorTest.executeSensorImporting(sensor, "scapegoat-scalastyle.xml");
    assertThat(externalIssues).hasSize(3);

    ExternalIssue first = externalIssues.get(0);
    assertThat(first.primaryLocation().inputComponent().key()).isEqualTo("project:HelloWorld.scala");
    assertThat(first.ruleKey()).hasToString("external_scapegoat:com.sksamuel.scapegoat.inspections.EmptyCaseClass");
    assertThat(first.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(first.severity()).isEqualTo(Severity.MINOR);
    assertThat(first.primaryLocation().message()).isEqualTo("Empty case class");
    assertThat(first.primaryLocation().textRange().start().line()).isEqualTo(9);

    ExternalIssue second = externalIssues.get(1);
    assertThat(second.primaryLocation().inputComponent().key()).isEqualTo("project:HelloWorld.scala");
    assertThat(second.ruleKey()).hasToString("external_scapegoat:com.sksamuel.scapegoat.inspections.FinalModifierOnCaseClass");
    assertThat(second.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(second.severity()).isEqualTo(Severity.MINOR);
    assertThat(second.primaryLocation().message()).isEqualTo("Missing final modifier on case class");
    assertThat(second.primaryLocation().textRange().start().line()).isEqualTo(9);

    ExternalIssue third = externalIssues.get(2);
    assertThat(third.primaryLocation().inputComponent().key()).isEqualTo("project:HelloWorld.scala");
    assertThat(third.ruleKey()).hasToString("external_scapegoat:com.sksamuel.scapegoat.inspections.unsafe.IsInstanceOf");
    assertThat(third.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(third.severity()).isEqualTo(Severity.MAJOR);
    assertThat(third.primaryLocation().message()).isEqualTo("Use of isInstanceOf");
    assertThat(third.primaryLocation().textRange().start().line()).isEqualTo(5);

    ScalastyleSensorTest.assertNoErrorWarnDebugLogs(logTester);
  }

}
