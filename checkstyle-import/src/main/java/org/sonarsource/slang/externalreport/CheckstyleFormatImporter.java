/*
 * SonarSource SLang
 * Copyright (C) 2018-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.slang.externalreport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonarsource.analyzer.commons.xml.SafeStaxParserFactory;

/**
 * Import external linter reports having a "Checkstyle" xml format into SonarQube
 */
public class CheckstyleFormatImporter {

  private static final Logger LOG = LoggerFactory.getLogger(CheckstyleFormatImporter.class);

  private static final Long DEFAULT_CONSTANT_DEBT_MINUTES = 5L;

  private static final QName CHECKSTYLE = new QName("checkstyle");
  private static final QName FILE = new QName("file");
  private static final QName ERROR = new QName("error");
  private static final QName NAME = new QName("name");
  private static final QName SEVERITY = new QName("severity");
  private static final QName SOURCE = new QName("source");
  private static final QName LINE = new QName("line");
  private static final QName MESSAGE = new QName("message");

  private final SensorContext context;

  protected final String linterKey;

  private int level = 0;

  @Nullable
  private InputFile inputFile = null;

  /**
   * @param context,   the context where issues will be sent
   * @param linterKey, used to specify the rule repository
   */
  public CheckstyleFormatImporter(SensorContext context, String linterKey) {
    this.context = context;
    this.linterKey = linterKey;
  }

  /**
   * "importFile" parses the given report file and imports the content into SonarQube
   *
   * @param reportPath, path of the xml file
   */
  public void importFile(File reportPath) {
    try (InputStream in = new FileInputStream(reportPath)) {
      XMLEventReader reader = SafeStaxParserFactory.createXMLInputFactory().createXMLEventReader(in);
      level = 0;
      while (reader.hasNext()) {
        XMLEvent event = reader.nextEvent();
        if (event.isStartElement()) {
          level++;
          onElement(event.asStartElement());
        } else if (event.isEndElement()) {
          level--;
        }
      }
    } catch (IOException | XMLStreamException | RuntimeException e) {
      LOG.error("No issue information will be saved as the report file '{}' can't be read.", reportPath, e);
    }
  }

  private void onElement(StartElement element) throws IOException {
    if (level == 1 && !CHECKSTYLE.equals(element.getName())) {
      throw new IOException("Unexpected document root '" + element.getName().getLocalPart() + "' instead of 'checkstyle'.");
    } else if (level == 2 && FILE.equals(element.getName())) {
      onFileElement(element);
    } else if (level == 3 && ERROR.equals(element.getName()) && inputFile != null) {
      onErrorElement(element);
    }
  }

  private void onFileElement(StartElement element) {
    String filePath = getAttributeValue(element, NAME);
    if (filePath.isEmpty()) {
      inputFile = null;
      return;
    }
    FilePredicates predicates = context.fileSystem().predicates();
    inputFile = context.fileSystem().inputFile(predicates.or(
      predicates.hasAbsolutePath(filePath),
      predicates.hasRelativePath(filePath)));
    if (inputFile == null) {
      LOG.warn("No input file found for {}. No {} issues will be imported on this file.", filePath, linterKey);
    }
  }

  private void onErrorElement(StartElement element) {
    String source = getAttributeValue(element, SOURCE);
    String line = getAttributeValue(element, LINE);
    // severity could be: error, warning, info
    String severity = getAttributeValue(element, SEVERITY);
    String message = getAttributeValue(element, MESSAGE);
    if (message.isEmpty()) {
      LOG.debug("Unexpected error without any message for rule: '{}'", source);
      return;
    }
    saveIssue(line, severity, source, message);
  }

  private void saveIssue(String line, String severity, String source, String message) {
    RuleType ruleType = ruleType(severity, source);
    Severity ruleSeverity = severity(severity);

    RuleKey ruleKey = createRuleKey(source, ruleType, ruleSeverity);

    NewExternalIssue newExternalIssue = context.newExternalIssue()
      .type(ruleType)
      .severity(ruleSeverity)
      .remediationEffortMinutes(effort(ruleKey.rule()));

    var impacts = impacts(severity, source);
    for (Impact impact : impacts) {
      newExternalIssue.addImpact(impact.softwareQuality(), impact.severity());
    }

    NewIssueLocation primaryLocation = newExternalIssue.newLocation()
      .message(message)
      .on(inputFile);

    if (!line.isEmpty()) {
      primaryLocation.at(inputFile.selectLine(Integer.parseInt(line)));
    }

    newExternalIssue
      .at(primaryLocation)
      .engineId(ruleKey.repository())
      .ruleId(ruleKey.rule())
      .save();
  }

  /**
   * Return a RuleKey based on the source, RuleType and Severity of an issue.
   */
  protected RuleKey createRuleKey(String source, RuleType ruleType, Severity ruleSeverity) {
    return RuleKey.of(linterKey, source);
  }

  /**
   * Return a RuleType equivalent based on the different parameters.
   *
   * @param severity "severity" attribute's value of the report. Ex: "info", "error".
   * @param source "source" attribute's value of the report. Ex: "gosec", "detekt.MagicNumber".
   * @return the RuleType defined by the given parameters.
   */
  protected RuleType ruleType(@Nullable String severity, String source) {
    return "error".equals(severity) ? RuleType.BUG : RuleType.CODE_SMELL;
  }

  /**
   * Return a Severity equivalent based on the different parameters.
   *
   * @param severity "severity" attribute's value of the report. Ex: "info", "error".
   * @return the Severity defined by the given parameters.
   */
  protected Severity severity(@Nullable String severity) {
    return "info".equals(severity) ? Severity.MINOR : Severity.MAJOR;
  }

  /**
   * Return an Effort value based on the ruleKey.
   *
   * @param ruleKey rule key of the current issue.
   * @return the Effort defined by the given ruleKey.
   */
  protected Long effort(String ruleKey) {
    return DEFAULT_CONSTANT_DEBT_MINUTES;
  }

  /**
   * Return list of {@link Impact}s. By default empty list is returned for backward compatibility.
   * @param severity "severity" attribute's value of the report. Ex: "info", "error".
   * @param source "source" attribute's value of the report. Ex: "gosec", "detekt.MagicNumber".
   * @return list of {@link Impact}s defined by the given parameters.
   */
  protected List<Impact> impacts(String severity, String source) {
    return List.of();
  }

  private static String getAttributeValue(StartElement element, QName attributeName) {
    Attribute attribute = element.getAttributeByName(attributeName);
    return attribute != null ? attribute.getValue() : "";
  }

  public record Impact(SoftwareQuality softwareQuality, org.sonar.api.issue.impact.Severity severity) {
  }
}
