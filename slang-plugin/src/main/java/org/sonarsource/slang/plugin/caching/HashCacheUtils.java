/*
 * SonarSource SLang
 * Copyright (C) 2018-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.slang.plugin.caching;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
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

  private HashCacheUtils() {
    /* Instances of this utility class should not be created. */
  }

  /**
   * Checks that a file matches its previous hash in the cache.
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
    } catch (IOException error) {
      LOG.warn(error.getMessage(), error);
      LOG.debug("File {} is considered changed: failed to read hash from the cache.", fileKey);
      return false;
    }
    String expected = Hex.encodeHexString(expectedHashAsBytes);
    String actual = inputFile.md5Hash();
    boolean matchesWithCache = expected.equals(actual);
    if (matchesWithCache) {
      LOG.debug("File {} is considered unchanged.", fileKey);
    } else {
      LOG.debug("File {} is considered changed: input file hash does not match cached hash ({} vs {}).", fileKey, actual, expected);
    }
    return matchesWithCache;
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
      nextCache.write(computeKey(inputFileContext.inputFile), Hex.decodeHex(inputFile.md5Hash()));
    } catch (IllegalArgumentException ignored) {
      LOG.warn("Failed to write hash for {} to cache.", inputFile.key());
      return false;
    } catch (DecoderException ignored) {
      LOG.warn("Failed to convert hash from hexadecimal string to bytes for {}.", inputFile.key());
      return false;
    }
    return true;
  }

  private static String computeKey(InputFile inputFile) {
    return "slang:hash:" + inputFile.key();
  }
}
