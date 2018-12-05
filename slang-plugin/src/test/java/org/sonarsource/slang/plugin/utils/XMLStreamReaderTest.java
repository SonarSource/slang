package org.sonarsource.slang.plugin.utils;

import java.io.ByteArrayInputStream;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import org.junit.Assert;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;

public class XMLStreamReaderTest {

  @Test(expected = XMLStreamException.class)
  public void external_entity_should_be_disabled() throws XMLStreamException {
    String xml = "<?xml version=\"1.0\"?>\n" +
      "<!DOCTYPE foo [ <!ELEMENT foo ANY > <!ENTITY xxe SYSTEM \"file:src/test/resources/sensitive-data.txt\" >]>\n" +
      "<foo>&xxe;</foo>";
    XMLEventReader reader = XMLStreamReader.create(new ByteArrayInputStream(xml.getBytes(UTF_8)));
    while (reader.hasNext()) {
      XMLEvent event = reader.nextEvent();
      if (event.isCharacters()) {
        Assert.fail("We are able to read sensitive data: " + event.asCharacters().getData());
      }
    }
  }
}
