/*
 * SonarSource SLang
 * Copyright (C) 2018-2024 SonarSource SA
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
package org.sonarsource.slang.plugin.caching;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.api.batch.sensor.cache.WriteCache;
import org.sonarsource.slang.plugin.InputFileContext;

/**
 * A utility class that enables developers to check that a file has changed and commit its checksum for future analysis.
 */
public class HashCacheUtils {
  private static final Logger LOG = LoggerFactory.getLogger(HashCacheUtils.class);
  private static final String BYTES_TO_HEX_FORMAT = "%032X";

  private HashCacheUtils() {
    /* Instances of this utility class should not be created. */
  }

  /**
   * Checks that a file that matches it previous hash in the cache.
   *
   * @param inputFileContext The context joining both SensorContext and InputFile
   * @return True if the file has its status set to InputFile.Status.SAME and matches its MD5 hash in the cache.
   */
  public static boolean hasSameHashCached(InputFileContext inputFileContext) {
    InputFile inputFile = inputFileContext.inputFile;
    String fileKey = inputFile.key();
    if (inputFile.status() != InputFile.Status.SAME) {
      LOG.debug("File {} is considered changed: file status is {}.", fileKey, inputFile.status());
      return false;
    }
    SensorContext sensorContext = inputFileContext.sensorContext;
    if (!sensorContext.isCacheEnabled()) {
      LOG.debug("File {} is considered changed: hash cache is disabled.", fileKey);
      return false;
    }
    String hashKey = computeKey(inputFile);
    ReadCache previousCache = sensorContext.previousCache();
    if (!previousCache.contains(hashKey)) {
      LOG.debug("File {} is considered changed: hash could not be found in the cache.", fileKey);
      return false;
    }

    byte[] expectedHashAsBytes;
    try (InputStream in = previousCache.read(hashKey)) {
      expectedHashAsBytes = in.readAllBytes();
    } catch (IOException e) {
      LOG.debug("File {} is considered changed: failed to read hash from the cache.", fileKey);
      return false;
    }
    String expected = md5sumBytesToHex(expectedHashAsBytes);
    String actual = inputFile.md5Hash();
    return expected.equals(actual);
  }

  /**
   * Copies the hash from the previous analysis for the next one.
   * For consistency, this method should only be called if the hash matches (see hasSameHashCached).
   *
   * @param inputFileContext Context joining SensorContext and InputFile.
   * @return true if successfully copied. False if failing to copy because of a runtime caching issue or repeated call.
   */
  public static boolean copyFromPrevious(InputFileContext inputFileContext) {
    if (!inputFileContext.sensorContext.isCacheEnabled()) {
      return false;
    }
    InputFile inputFile = inputFileContext.inputFile;
    String cacheKey = computeKey(inputFile);
    WriteCache nextCache = inputFileContext.sensorContext.nextCache();
    try {
      nextCache.copyFromPrevious(cacheKey);
    } catch (IllegalArgumentException ignored) {
      LOG.warn("Failed to copy hash from previous analysis for {}.", inputFile.key());
      return false;
    }
    return true;
  }

  public static boolean writeHashForNextAnalysis(InputFileContext inputFileContext) {
    if (!inputFileContext.sensorContext.isCacheEnabled()) {
      return false;
    }
    InputFile inputFile = inputFileContext.inputFile;
    WriteCache nextCache = inputFileContext.sensorContext.nextCache();
    try {
      nextCache.write(computeKey(inputFileContext.inputFile), inputFile.md5Hash().getBytes(StandardCharsets.UTF_8));
    } catch (IllegalArgumentException ignored) {
      LOG.warn("Failed to write hash for {} to cache.", inputFile.key());
      return false;
    }
    return true;
  }

  private static String computeKey(InputFile inputFile) {
    return "slang:hash:" + inputFile.key();
  }

  private static String md5sumBytesToHex(byte[] bytes) {
    BigInteger bi = new BigInteger(1, bytes);
    return BYTES_TO_HEX_FORMAT.formatted(bi).toLowerCase(Locale.getDefault());
  }
}
