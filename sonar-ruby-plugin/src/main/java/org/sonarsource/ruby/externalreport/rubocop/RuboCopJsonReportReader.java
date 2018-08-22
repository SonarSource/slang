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
package org.sonarsource.ruby.externalreport.rubocop;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import static java.nio.charset.StandardCharsets.UTF_8;

public class RuboCopJsonReportReader {

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

  static void read(InputStream in, Consumer<Issue> consumer) {
    new RuboCopJsonReportReader(consumer).read(in);
  }

  private void read(InputStream in) {
    Gson gson = new GsonBuilder()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .create();
    Report report = gson.fromJson(new InputStreamReader(in, UTF_8), Report.class);
    report.files.forEach(this::onFile);
  }

  private void onFile(Report.File file) {
    file.offenses.forEach(offense -> onOffense(file.path, offense));
  }

  private void onOffense(@Nullable String filePath, Report.Offense offense) {
    Issue issue = new Issue();
    issue.filePath = filePath;
    issue.ruleKey = offense.copName;
    issue.message = offense.message;
    if (offense.location != null) {
      issue.startLine = offense.location.startLine;
      issue.startColumn = offense.location.startColumn;
      issue.lastLine = offense.location.lastLine;
      issue.lastColumn = offense.location.lastColumn;
      if (issue.startLine == null) {
        issue.startLine = offense.location.line;
      }
    }
    consumer.accept(issue);
  }

  private static class Report {

    List<Report.File> files;

    private static class File {
      @Nullable
      String path;
      List<Report.Offense> offenses;
    }

    private static class Offense {
      @Nullable
      String message;
      @Nullable
      String copName;
      @Nullable
      Report.Location location;
    }

    private static class Location {
      @Nullable
      Integer line;
      @Nullable
      Integer startLine;
      @Nullable
      Integer startColumn;
      @Nullable
      Integer lastLine;
      @Nullable
      Integer lastColumn;
    }
  }

}
