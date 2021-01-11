/*
 * SonarSource SLang
 * Copyright (C) 2018-2021 SonarSource SA
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
package org.sonarsource.scala.externalreport.scalastyle;

import java.io.IOException;
import java.io.InputStream;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.sonarsource.analyzer.commons.xml.SafeStaxParserFactory;

class ScalastyleXmlReportReader {

  private static final QName CHECKSTYLE_ELEMENT = new QName("checkstyle");
  private static final QName FILE_ELEMENT = new QName("file");
  private static final QName NAME_ATTRIBUTE = new QName("name");
  private static final QName ERROR_ELEMENT = new QName("error");
  private static final QName LINE_ATTRIBUTE = new QName("line");
  private static final QName MESSAGE_ATTRIBUTE = new QName("message");
  private static final QName SOURCE_ATTRIBUTE = new QName("source");

  private final IssueConsumer consumer;

  private int level = 0;
  private String file = "";

  @FunctionalInterface
  interface IssueConsumer {
    void onIssue(String file, String line, String source, String message);
  }

  private ScalastyleXmlReportReader(IssueConsumer consumer) {
    this.consumer = consumer;
  }

  static void read(InputStream in, IssueConsumer consumer) throws XMLStreamException, IOException {
    new ScalastyleXmlReportReader(consumer).read(in);
  }

  private void read(InputStream in) throws XMLStreamException, IOException {
    XMLEventReader reader = SafeStaxParserFactory.createXMLInputFactory().createXMLEventReader(in);
    while (reader.hasNext()) {
      XMLEvent event = reader.nextEvent();
      if (event.isStartElement()) {
        level++;
        onElement(event.asStartElement());
      } else if (event.isEndElement()) {
        level--;
      }
    }
  }

  private void onElement(StartElement element) throws IOException {
    if (level == 1 && !CHECKSTYLE_ELEMENT.equals(element.getName())) {
      throw new IOException("Unexpected document root '" + element.getName().getLocalPart() + "' instead of 'checkstyle'.");
    } else if (level == 2) {
      file = FILE_ELEMENT.equals(element.getName()) ? getAttributeValue(element, NAME_ATTRIBUTE) : "";
    } else if (level == 3 && ERROR_ELEMENT.equals(element.getName()) && !file.isEmpty()) {
      consumer.onIssue(
        file,
        getAttributeValue(element, LINE_ATTRIBUTE),
        getAttributeValue(element, SOURCE_ATTRIBUTE),
        getAttributeValue(element, MESSAGE_ATTRIBUTE));
    }
  }

  private static String getAttributeValue(StartElement element, QName attributeName) {
    Attribute attribute = element.getAttributeByName(attributeName);
    return attribute != null ? attribute.getValue() : "";
  }

}
