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
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.sonarsource.scala.plugin.ScalaPlugin.COVERAGE_REPORT_PATHS_KEY;

public class ScoverageSensor implements Sensor {


  private final String XML_REPORT_FILENAME = "scoverage.xml";

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
      return; //Return if we don't have any files
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

  private void readReportFile(InputStream in, SensorContext context, Set<String> unresolvedInputFile) throws XMLStreamException {
    FilePredicates predicates = context.fileSystem().predicates();

    Map<String, Set<Integer>> linesHitPerFiles = new HashMap<>();

    XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(in);
    while (reader.hasNext()) {
      XMLEvent event = reader.nextEvent();
      if (event.isStartElement() && STATEMENT_ELEMENT.equals(event.asStartElement().getName())) {
        //We have a statement, read the attributes of the statement
        Iterator<Attribute> attributes = event.asStartElement().getAttributes();

        Integer line = null;
        Integer invocationCount = null;
        String source = null;

        while (attributes.hasNext()) {
          Attribute attribute = attributes.next();
          if (attribute.getName().equals(INVOCATION_COUNT_ATTRIBUTE)) {
            invocationCount = Integer.valueOf(attribute.getValue()); // Is always 0 or 1 in Scoverage
          }
          if (attribute.getName().equals(LINE_ATTRIBUTE)) {
            line = Integer.valueOf(attribute.getValue());
          }
          if (attribute.getName().equals(SOURCE_ATTRIBUTE)) {
            source = attribute.getValue();
          }
        }

        if(line != null && invocationCount != null && source != null) {
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
        } else {
          LOG.warn("Some attributes of statement at line {} of scoverage report are not present.", event.getLocation().getLineNumber());
        }
      }
    }
    //Add the lines hit to the context
    for(String sourcePath: linesHitPerFiles.keySet()) {
      InputFile inputFile = context.fileSystem().inputFile(predicates.hasAbsolutePath(sourcePath));
      if (inputFile == null) {
        unresolvedInputFile.add(sourcePath);
      } else {
        NewCoverage newCoverage = context.newCoverage().onFile(inputFile);
        for(Integer lineHit: linesHitPerFiles.get(sourcePath)){
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
