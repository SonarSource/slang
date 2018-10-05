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
package org.sonarsource.scala.plugin;

import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.analyzer.commons.ExternalReportProvider;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.stream.Collectors;

import static org.sonarsource.scala.plugin.ScalaPlugin.COVERAGE_REPORT_PATHS_KEY;

public class ScoverageSensor implements Sensor {

  private static final QName STATEMENT_ELEMENT = new QName("statement");

  private static final QName INVOCATION_COUNT_ATTRIBUTE = new QName("invocation-count");
  private static final QName LINE_ATTRIBUTE= new QName("line");
  private static final QName SOURCE_ATTRIBUTE = new QName("source");

  private static final Logger LOG = Loggers.get(ScoverageSensor.class);

  private static final int MAX_LOGGED_FILE_NAMES = 20;

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
        .onlyWhenConfiguration(conf -> conf.hasKey(COVERAGE_REPORT_PATHS_KEY))
        .name("Scoverage sensor for Scala coverage");
  }

  @Override
  public void execute(SensorContext context) {
    List<File> reportFiles = ExternalReportProvider.getReportFiles(context, COVERAGE_REPORT_PATHS_KEY);

    if (reportFiles.isEmpty()) {
      return;
    }

    Set<String> unresolvedInputFile = new HashSet<>();

    for (File f : reportFiles) {
      LOG.info("Importing coverage from {}", f.getPath());
      try(InputStream in = new FileInputStream(f)){
        readReportFile(in, context, unresolvedInputFile);
      } catch (IOException | XMLStreamException | NumberFormatException e) {
        LOG.error("File '{}' can't be read. " + e.toString(), f, e);
      }
    }

    logUnresolvedInputFiles(unresolvedInputFile);
  }

  private static void readReportFile(InputStream in, SensorContext context, Set<String> unresolvedInputFile) throws XMLStreamException {
    Map<String, Map<Integer,Integer>> linesHitPerFiles = new HashMap<>();

    XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(in);
    while (reader.hasNext()) {
      XMLEvent event = reader.nextEvent();
      if (event.isStartElement() && STATEMENT_ELEMENT.equals(event.asStartElement().getName())) {
        parseStatementAttributes(linesHitPerFiles, event.asStartElement());
      }
    }
    addLineHitToContext(linesHitPerFiles, context, unresolvedInputFile);
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

  private static void addStatementToMap(Map<String, Map<Integer,Integer>> linesHitPerFiles, String source, Integer line, Integer invocationCount) {
    Map<Integer,Integer> hitCountPerLine = linesHitPerFiles.computeIfAbsent(source, key -> new HashMap<>());

    if(!hitCountPerLine.containsKey(line)) {
      hitCountPerLine.put(line, invocationCount);
    } else {
      int oldInvocationCount = hitCountPerLine.get(line);
      hitCountPerLine.put(line,oldInvocationCount + invocationCount);
    }
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
