/*
 * SonarSource SLang
 * Copyright (C) 2018-2022 SonarSource SA
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
package org.sonarsource.scala.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.analyzer.commons.xml.SafeStaxParserFactory;
import org.sonarsource.slang.plugin.AbstractPropertyHandlerSensor;

import static org.sonarsource.scala.plugin.ScalaPlugin.COVERAGE_REPORT_PATHS_KEY;

public class ScoverageSensor extends AbstractPropertyHandlerSensor {

  private static final QName STATEMENT_ELEMENT = new QName("statement");

  private static final QName INVOCATION_COUNT_ATTRIBUTE = new QName("invocation-count");
  private static final QName LINE_ATTRIBUTE= new QName("line");
  private static final QName SOURCE_ATTRIBUTE = new QName("source");

  private static final Logger LOG = Loggers.get(ScoverageSensor.class);

  private static final int MAX_LOGGED_FILE_NAMES = 20;
  private final Set<String> unresolvedInputFile = new HashSet<>();

  public ScoverageSensor(AnalysisWarnings analysisWarnings) {
    super(analysisWarnings, "scoverage", "Scoverage", COVERAGE_REPORT_PATHS_KEY, ScalaPlugin.SCALA_LANGUAGE_KEY);
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
        .onlyOnLanguage(ScalaPlugin.SCALA_LANGUAGE_KEY)
        .onlyWhenConfiguration(conf -> conf.hasKey(COVERAGE_REPORT_PATHS_KEY))
        .name("Scoverage sensor for Scala coverage");
  }

  @Override
  public Consumer<File> reportConsumer(SensorContext context) {
    return file -> readReportFile(file, context, unresolvedInputFile);
  }

  @Override
  public void execute(SensorContext context) {
    unresolvedInputFile.clear();
    super.execute(context);
    logUnresolvedInputFiles(unresolvedInputFile);
  }

  private static void readReportFile(File file, SensorContext context, Set<String> unresolvedInputFile) {
    Map<String, Map<Integer,Integer>> linesHitPerFiles = new HashMap<>();

    try (InputStream in = new FileInputStream(file)) {
      XMLEventReader reader = SafeStaxParserFactory.createXMLInputFactory().createXMLEventReader(in);
      while (reader.hasNext()) {
        XMLEvent event = reader.nextEvent();
        if (event.isStartElement() && STATEMENT_ELEMENT.equals(event.asStartElement().getName())) {
          parseStatementAttributes(linesHitPerFiles, event.asStartElement());
        }
      }
      addLineHitToContext(linesHitPerFiles, context, unresolvedInputFile);
    } catch (IOException | XMLStreamException | NumberFormatException e) {
      LOG.error("File '{}' can't be read. " + e.toString(), file, e);
    }
  }


  private static void parseStatementAttributes(Map<String, Map<Integer,Integer>> linesHitPerFiles, StartElement currentEvent){
    Integer line = Integer.valueOf(getAttributeValue(currentEvent, LINE_ATTRIBUTE));
    Integer invocationCount = Integer.valueOf(getAttributeValue(currentEvent, INVOCATION_COUNT_ATTRIBUTE));
    String source = getAttributeValue(currentEvent, SOURCE_ATTRIBUTE);

    if(source == null){
      throw new IllegalStateException("Source attribute is null.");
    }

    linesHitPerFiles.computeIfAbsent(source, key -> new HashMap<>()).merge(line, invocationCount, Integer::sum);
  }

  private static String getAttributeValue(StartElement element, QName attributeName) {
    Attribute attribute = element.getAttributeByName(attributeName);
    return attribute != null ? attribute.getValue() : null;
  }

  private static void addLineHitToContext(Map<String, Map<Integer,Integer>> linesHitPerFiles, SensorContext context, Set<String> unresolvedInputFile){
    FilePredicates predicates = context.fileSystem().predicates();
    for (Map.Entry<String,Map<Integer,Integer>> entry : linesHitPerFiles.entrySet()) {
      String sourcePath = entry.getKey();

      InputFile inputFile = context.fileSystem().inputFile(predicates.hasAbsolutePath(sourcePath));
      if (inputFile == null) {
        unresolvedInputFile.add(sourcePath);
      } else {
        NewCoverage newCoverage = context.newCoverage().onFile(inputFile);
        for(Map.Entry<Integer, Integer> hitCount: entry.getValue().entrySet()){
          newCoverage.lineHits(hitCount.getKey(), hitCount.getValue());
        }
        newCoverage.save();
      }
    }
  }

  private static void logUnresolvedInputFiles(Set<String> unresolvedInputFile) {
    if (unresolvedInputFile.isEmpty()) {
      return;
    }
    String fileList = unresolvedInputFile.stream().sorted().limit(MAX_LOGGED_FILE_NAMES).collect(Collectors.joining(";"));
    if (unresolvedInputFile.size() > MAX_LOGGED_FILE_NAMES) {
      fileList += ";...";
    }
    LOG.warn("Fail to resolve {} file(s). No coverage data will be imported on the following file(s): {}", unresolvedInputFile.size(), fileList);
  }

}
