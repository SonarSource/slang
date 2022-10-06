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
package org.sonarsource.ruby.externalreport.rubocop;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonarsource.analyzer.commons.internal.json.simple.JSONArray;
import org.sonarsource.analyzer.commons.internal.json.simple.JSONObject;
import org.sonarsource.analyzer.commons.internal.json.simple.parser.JSONParser;
import org.sonarsource.analyzer.commons.internal.json.simple.parser.ParseException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class RuboCopJsonReportReader {

  private final JSONParser jsonParser = new JSONParser();
  private final Consumer<Issue> consumer;

  public static class Issue {
    @Nullable
    String filePath;
    @Nullable
    String ruleKey;
    @Nullable
    String message;
    @Nullable
    Integer startLine;
    @Nullable
    Integer startColumn;
    @Nullable
    Integer lastLine;
    @Nullable
    Integer lastColumn;
  }

  private RuboCopJsonReportReader(Consumer<Issue> consumer) {
    this.consumer = consumer;
  }

  static void read(InputStream in, Consumer<Issue> consumer) throws IOException, ParseException {
    new RuboCopJsonReportReader(consumer).read(in);
  }

  private void read(InputStream in) throws IOException, ParseException {
    JSONObject rootObject = (JSONObject) jsonParser.parse(new InputStreamReader(in, UTF_8));
    JSONArray files = (JSONArray) rootObject.get("files");
    if (files != null) {
      ((Stream<JSONObject>) files.stream()).forEach(this::onFile);
    }
  }

  private void onFile(JSONObject file) {
    String filePath = (String) file.get("path");
    JSONArray offenses = (JSONArray) file.get("offenses");
    if (offenses != null) {
      ((Stream<JSONObject>) offenses.stream()).forEach(offense -> onOffense(filePath, offense));
    }
  }

  private void onOffense(@Nullable String filePath, JSONObject offense) {
    Issue issue = new Issue();
    issue.filePath = filePath;
    issue.ruleKey = (String) offense.get("cop_name");
    issue.message = (String) offense.get("message");
    JSONObject location = (JSONObject) offense.get("location");
    if (location != null) {
      issue.startLine = toInteger(location.get("start_line"));
      issue.startColumn = toInteger(location.get("start_column"));
      issue.lastLine = toInteger(location.get("last_line"));
      issue.lastColumn = toInteger(location.get("last_column"));
      if (issue.startLine == null) {
        issue.startLine = toInteger(location.get("line"));
      }
    }
    consumer.accept(issue);
  }

  private static Integer toInteger(Object value) {
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    return null;
  }

}
