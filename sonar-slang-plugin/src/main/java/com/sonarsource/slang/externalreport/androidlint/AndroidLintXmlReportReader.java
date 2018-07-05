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
package com.sonarsource.slang.externalreport.androidlint;

import java.io.IOException;
import java.io.InputStream;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.analyzer.commons.ExternalRuleLoader;

class AndroidLintXmlReportReader {

  private static final Logger LOG = Loggers.get(AndroidLintXmlReportReader.class);

  private static final QName ISSUES_ELEMENT = new QName("issues");
  private static final QName ISSUE_ELEMENT = new QName("issue");
  private static final QName ID_ATTRIBUTE = new QName("id");
  private static final QName MESSAGE_ATTRIBUTE = new QName("message");
  private static final QName LOCATION_ELEMENT = new QName("location");
  private static final QName FILE_ATTRIBUTE = new QName("file");
  private static final QName LINE_ATTRIBUTE = new QName("line");

  private final SensorContext context;

  private int level = 0;

  private String id = "";
  private String message = "";
  private String file = "";
  private String line = "";

  private AndroidLintXmlReportReader(SensorContext context) {
    this.context = context;
  }

  static void read(SensorContext context, InputStream in) throws XMLStreamException, IOException {
    new AndroidLintXmlReportReader(context).read(in);
  }

  private void read(InputStream in) throws XMLStreamException, IOException {
    XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(in);
    while (reader.hasNext()) {
      XMLEvent event = reader.nextEvent();
      if (event.isStartElement()) {
        level++;
        onElement(event.asStartElement());
      } else if (event.isEndElement()) {
        level--;
        onEndElement();
      }
    }
  }

  private void onEndElement() {
    if (level == 1) {
      saveIssue();
      id = "";
      message = "";
      file = "";
      line = "";
    }
  }

  private void onElement(StartElement element) throws IOException {
    if (level == 1 && !ISSUES_ELEMENT.equals(element.getName())) {
      throw new IOException("Unexpected document root '" + element.getName().getLocalPart() + "' instead of 'issues'.");
    } else if (level == 2 && ISSUE_ELEMENT.equals(element.getName())) {
      id = getAttributeValue(element, ID_ATTRIBUTE);
      message = getAttributeValue(element, MESSAGE_ATTRIBUTE);
    } else if (level == 3 && LOCATION_ELEMENT.equals(element.getName()) && file.isEmpty()) {
      file = getAttributeValue(element, FILE_ATTRIBUTE);
      line = getAttributeValue(element, LINE_ATTRIBUTE);
    }
  }

  private void saveIssue() {
    if (id.isEmpty() || message.isEmpty() || file.isEmpty() || !AndroidLintRulesDefinition.isTextFile(file)) {
      return;
    }
    FilePredicates predicates = context.fileSystem().predicates();
    InputFile inputFile = context.fileSystem().inputFile(predicates.or(
      predicates.hasAbsolutePath(file),
      predicates.hasRelativePath(file)));

    if (inputFile == null) {
      LOG.warn("No input file found for {}. No android lint issues will be imported on this file.", file);
      return;
    }
    RuleKey ruleKey = AndroidLintRulesDefinition.ruleKey(inputFile.language(), id);
    NewExternalIssue newExternalIssue = context.newExternalIssue();
    setRulesDefinitionProperties(newExternalIssue, ruleKey.rule());

    NewIssueLocation primaryLocation = newExternalIssue.newLocation()
      .message(message)
      .on(inputFile);

    if (!line.isEmpty()) {
      primaryLocation.at(inputFile.selectLine(Integer.parseInt(line)));
    }

    newExternalIssue
      .at(primaryLocation)
      .forRule(ruleKey)
      .save();
  }

  private static void setRulesDefinitionProperties(NewExternalIssue newExternalIssue, String ruleKey) {
    ExternalRuleLoader externalRuleLoader = AndroidLintRulesDefinition.RULE_LOADERS.get(0);
    newExternalIssue
      .type(externalRuleLoader.ruleType(ruleKey))
      .severity(externalRuleLoader.ruleSeverity(ruleKey))
      .remediationEffortMinutes(externalRuleLoader.ruleConstantDebtMinutes(ruleKey));
  }

  private static String getAttributeValue(StartElement element, QName attributeName) {
    Attribute attribute = element.getAttributeByName(attributeName);
    return attribute != null ? attribute.getValue() : "";
  }

}
