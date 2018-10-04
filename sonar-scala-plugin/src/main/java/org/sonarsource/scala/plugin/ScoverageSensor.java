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
import java.util.Iterator;
import java.util.stream.Collectors;

import static org.sonarsource.scala.plugin.ScalaPlugin.COVERAGE_REPORT_PATHS_KEY;

public class ScoverageSensor implements Sensor {


  private static final String XML_REPORT_FILENAME = "scoverage.xml";

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
      //Return if we don't have any files
      return;
    }

    Set<String> unresolvedInputFile = new HashSet<>();

    for (File f : reportFiles) {
      if(f.getName().endsWith(XML_REPORT_FILENAME)) {
        //Read the scoverage.xml file
        try(InputStream in = new FileInputStream(f)){
          readReportFile(in, context, unresolvedInputFile);
        } catch (IOException | XMLStreamException e ) {
          LOG.error("File '{}' can't be read. " + e.getMessage(), f, e);
        }
      }
    }

    logUnresolvedInputFiles(unresolvedInputFile);
  }

  private static void readReportFile(InputStream in, SensorContext context, Set<String> unresolvedInputFile) throws XMLStreamException {
    Map<String, Set<Integer>> linesHitPerFiles = new HashMap<>();

    XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(in);
    while (reader.hasNext()) {
      XMLEvent event = reader.nextEvent();
      if (event.isStartElement() && STATEMENT_ELEMENT.equals(event.asStartElement().getName())) {
        //We have a statement, read the attributes of the statement
        parseStatementAttributes(linesHitPerFiles, event.asStartElement());
      }
    }
    //Once we have parsed all this report, add the statements to the context
    addLineHitToContext(linesHitPerFiles, context, unresolvedInputFile);
  }


  private static void parseStatementAttributes(Map<String, Set<Integer>> linesHitPerFiles, StartElement currentEvent){
    Integer line = null;
    Integer invocationCount = null;
    String source = null;
    Iterator<Attribute> attributes = currentEvent.getAttributes();
    while (attributes.hasNext()) {
      Attribute attribute = attributes.next();
      if (attribute.getName().equals(INVOCATION_COUNT_ATTRIBUTE)) {
        invocationCount = Integer.valueOf(attribute.getValue());
      }
      if (attribute.getName().equals(LINE_ATTRIBUTE)) {
        line = Integer.valueOf(attribute.getValue());
      }
      if (attribute.getName().equals(SOURCE_ATTRIBUTE)) {
        source = attribute.getValue();
      }
    }
    if(line != null && invocationCount != null && source != null) {
      addStatementToMap(linesHitPerFiles, source, line, invocationCount);
    } else {
      LOG.warn("Some attributes of statement at line {} of scoverage report are not present.", currentEvent.getLocation().getLineNumber());
    }
  }

  private static void addStatementToMap(Map<String, Set<Integer>> linesHitPerFiles, String source, Integer line, Integer invocationCount) {
    //Store the new statement if invoked
    if (invocationCount > 0) {
      if (!linesHitPerFiles.containsKey(source)) {
        Set<Integer> newFileLineHit = new HashSet<>();
        newFileLineHit.add(line);
        linesHitPerFiles.put(source, newFileLineHit);
      } else {
        linesHitPerFiles.get(source).add(line);
      }
    }
  }

  private static void addLineHitToContext(Map<String, Set<Integer>> linesHitPerFiles, SensorContext context, Set<String> unresolvedInputFile){
    FilePredicates predicates = context.fileSystem().predicates();
    for (Map.Entry<String,Set<Integer>> entry : linesHitPerFiles.entrySet()) {
      String sourcePath = entry.getKey();

      InputFile inputFile = context.fileSystem().inputFile(predicates.hasAbsolutePath(sourcePath));
      if (inputFile == null) {
        unresolvedInputFile.add(sourcePath);
      } else {
        NewCoverage newCoverage = context.newCoverage().onFile(inputFile);
        for(Integer lineHit: entry.getValue()){
          newCoverage.lineHits(lineHit,1);
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
