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
package org.sonar.go.converter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.slang.api.ASTConverter;
import org.sonarsource.slang.api.ParseException;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.persistence.JsonTree;

import static java.nio.charset.StandardCharsets.UTF_8;

public class GoConverter implements ASTConverter {

  private static final long MAX_SUPPORTED_SOURCE_FILE_SIZE = 1_500_000L;
  private static final Logger LOG = Loggers.get(GoConverter.class);
  private static final long PROCESS_TIMEOUT_MS = 5_000;

  private final ProcessBuilder processBuilder;
  private final ExternalProcessStreamConsumer errorConsumer;

  public GoConverter(File workDir) {
    this(new DefaultCommand(workDir));
  }

  GoConverter(Command command) {
    processBuilder = new ProcessBuilder(command.getCommand());
    errorConsumer = new ExternalProcessStreamConsumer();
  }

  @Override
  public Tree parse(String content) {
    if (content.length() > MAX_SUPPORTED_SOURCE_FILE_SIZE) {
      throw new ParseException("The file size is too big and should be excluded," +
        " its size is " + content.length() + " (maximum allowed is " + MAX_SUPPORTED_SOURCE_FILE_SIZE + " bytes)");
    }
    try {
      return JsonTree.fromJson(executeGoToJsonProcess(content));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ParseException("Go parser external process interrupted: " + e.getMessage(), null, e);
    } catch (IOException e) {
      throw new ParseException(e.getMessage(), null, e);
    }
  }

  private String executeGoToJsonProcess(String content) throws IOException, InterruptedException {
    Process process = processBuilder.start();
    errorConsumer.consumeStream(process.getErrorStream(), LOG::debug);
    try (OutputStream out = process.getOutputStream()) {
      out.write(content.getBytes(UTF_8));
    }
    String output;
    try (InputStream in = process.getInputStream()) {
      output = readAsString(in);
    }
    boolean exited = process.waitFor(PROCESS_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    if (exited && process.exitValue() != 0) {
      throw new ParseException("Go parser external process returned non-zero exit value: " + process.exitValue());
    }
    if (process.isAlive()) {
      process.destroyForcibly();
      throw new ParseException("Go parser external process took too long. External process killed forcibly");
    }
    return output;
  }

  private static String readAsString(InputStream in) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    copy(in, outputStream);
    return new String(outputStream.toByteArray(), UTF_8);
  }

  private static void copy(InputStream in, OutputStream out) throws IOException {
    byte[] buffer = new byte[8192];
    int read;
    while ((read = in.read(buffer)) >= 0) {
      out.write(buffer, 0, read);
    }
  }

  interface Command {
    List<String> getCommand();
  }

  static class DefaultCommand implements Command {

    private final String command;

    DefaultCommand(File workDir) {
      try {
        command = extract(workDir);
      } catch (IOException e) {
        throw new IllegalStateException(e.getMessage(), e);
      }
    }

    @Override
    public List<String> getCommand() {
      return Arrays.asList(command, "-");
    }

    private static String extract(File workDir) throws IOException {
      String executable = getExecutableForCurrentOS(System.getProperty("os.name"));
      byte[] executableData = getBytesFromResource(executable);
      File dest = new File(workDir, executable);
      if (!fileMatch(dest, executableData)) {
        Files.write(dest.toPath(), executableData);
        dest.setExecutable(true);
      }
      return dest.getAbsolutePath();
    }

    static boolean fileMatch(File dest, byte[] expectedContent) throws IOException {
      if (!dest.exists()) {
        return false;
      }
      byte[] actualContent = Files.readAllBytes(dest.toPath());
      return Arrays.equals(actualContent, expectedContent);
    }

    static byte[] getBytesFromResource(String executable) throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      try(InputStream in = GoConverter.class.getClassLoader().getResourceAsStream(executable)) {
        if (in == null) {
          throw new IllegalStateException(executable + " binary not found on class path");
        }
        copy(in, out);
      }
      return out.toByteArray();
    }

    static String getExecutableForCurrentOS(String osName) {
      String os = osName.toLowerCase(Locale.ROOT);
      if (os.contains("win")) {
        return "sonar-go-to-slang-windows-amd64.exe";
      } else if (os.contains("mac")) {
        return "sonar-go-to-slang-darwin-amd64";
      } else {
        return "sonar-go-to-slang-linux-amd64";
      }
    }
  }
}
