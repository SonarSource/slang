/*
 * SonarSource SLang
 * Copyright (C) 2018-2026 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.slang.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;
import org.sonarsource.slang.api.TextRange;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.TopLevelTree;
import org.sonarsource.slang.impl.TextRangeImpl;
import org.sonarsource.slang.impl.TokenImpl;

public class CpdVisitor extends PullRequestAwareVisitor {
  static final char ASCII_UNIT_SEPARATOR = 31;
  static final char ASCII_RECORD_SEPARATOR = 30;
  private static final Logger LOG = LoggerFactory.getLogger(CpdVisitor.class.getName());

  public CpdVisitor() {
    register(TopLevelTree.class, (ctx, tree) -> {
      NewCpdTokens cpdTokens = ctx.sensorContext.newCpdTokens().onFile(ctx.inputFile);
      List<Token> tokens = tree.metaData().tokens();
      List<Token> tokensToCache = new ArrayList<>(tokens.size());

      boolean foundFirstToken = (tree.firstCpdToken() == null);

      for (Token token : tokens) {
        foundFirstToken = foundFirstToken || (token == tree.firstCpdToken());
        if (foundFirstToken) {
          String text = substituteText(token);
          cpdTokens.addToken(ctx.textRange(token.textRange()), text);
          if (ctx.sensorContext.isCacheEnabled()) {
            tokensToCache.add(token);
          }
        }
      }
      cpdTokens.save();
      cacheNewTokens(ctx, tokensToCache);
    });
  }

  @Override
  public boolean reusePreviousResults(InputFileContext ctx) {
    if (canReusePreviousResults(ctx)) {
      NewCpdTokens reusedTokens = ctx.sensorContext.newCpdTokens().onFile(ctx.inputFile);
      // Load from the cache and skip parsing
      String fileKey = ctx.inputFile.key();
      LOG.debug("Looking up cached CPD tokens for {} ...", fileKey);
      ReadCache cache = ctx.sensorContext.previousCache();
      String key = computeCacheKey(ctx.inputFile);
      if (cache.contains(key)) {
        LOG.debug("Found cached CPD tokens for {}.", fileKey);
        LOG.debug("Loading cached CPD tokens for {} ...", fileKey);
        List<Token> tokens = null;
        try (InputStream in = cache.read(key)) {
          tokens = deserialize(in.readAllBytes());
        } catch (IllegalArgumentException | IOException e) {
          LOG.warn("Failed to load cached CPD tokens for input file %s.".formatted(fileKey));
          return false;
        }
        LOG.debug("Loaded cached CPD tokens for {}.", fileKey);
        for (Token token : tokens) {
          String text = substituteText(token);
          reusedTokens.addToken(ctx.textRange(token.textRange()), text);
        }
        try {
          ctx.sensorContext.nextCache().copyFromPrevious(key);
        } catch (IllegalArgumentException e) {
          LOG.warn("Failed to copy previous cached results for input file %s.".formatted(fileKey));
          return false;
        }
        reusedTokens.save();
        return true;
      }
    }
    return false;
  }

  private static void cacheNewTokens(InputFileContext ctx, List<Token> tokens) {
    if (ctx.sensorContext.isCacheEnabled()) {
      try {
        ctx.sensorContext.nextCache().write(
          computeCacheKey(ctx.inputFile),
          serialize(tokens)
        );
      } catch (IllegalArgumentException e) {
        LOG.warn("Failed to write CPD tokens to cache for input file {}: {}", ctx.inputFile.key(), e.getMessage());
      }
    }
  }

  /**
   * Computes a unique key for a file that can be used to store its CPD tokens in a cache.
   */
  // VisibleForTesting
  static String computeCacheKey(InputFile inputFile) {
    return "slang:cpd-tokens:%s".formatted(inputFile.key());
  }

  /**
   * Transforms a list of tokens into a byte array for caching.
   * Must be reversible by {@link #deserialize(byte[])}.
   */
  // VisibleForTesting
  static byte[] serialize(List<Token> tokens) {
    return tokens.stream()
      .map(CpdVisitor::serialize)
      .collect(Collectors.joining(String.valueOf(ASCII_RECORD_SEPARATOR)))
      .getBytes(StandardCharsets.UTF_8);
  }

  private static String serialize(Token token) {
    TextRange textRange = token.textRange();
    return String.format(
      "%d,%d,%d,%d%c%s%c%s",
      textRange.start().line(),
      textRange.start().lineOffset(),
      textRange.end().line(),
      textRange.end().lineOffset(),
      ASCII_UNIT_SEPARATOR,
      token.text(),
      ASCII_UNIT_SEPARATOR,
      token.type()
    );
  }

  /**
   * Deserialize a byte array, serialized by {@link #serialize(List)}, into a list of tokens.
   *
   * @throws IllegalArgumentException - when failing to deserialize (eg: unexpected format)
   */
  // VisibleForTesting
  static List<Token> deserialize(byte[] serialized) {
    if (serialized.length == 0) {
      return Collections.emptyList();
    }
    String str = new String(serialized, StandardCharsets.UTF_8);
    String[] tokensAsStrings = str.split(String.valueOf(ASCII_RECORD_SEPARATOR));
    try {
      return Arrays.stream(tokensAsStrings)
        .map(CpdVisitor::deserialize)
        .toList();
    } catch (IllegalArgumentException | IndexOutOfBoundsException | NoSuchElementException e) {
      throw new IllegalArgumentException(
        "Could not deserialize cached CPD tokens: %s".formatted(e.getMessage()),
        e
      );
    }
  }

  private static Token deserialize(String tokenAsString) {
    String[] fields = tokenAsString.split(String.valueOf(ASCII_UNIT_SEPARATOR));
    List<Integer> rangeIndices = Arrays.stream(fields[0].split(","))
      .map(Integer::valueOf)
      .toList();
    TextRange textRange = new TextRangeImpl(rangeIndices.get(0), rangeIndices.get(1), rangeIndices.get(2), rangeIndices.get(3));
    String text = fields[1];
    Token.Type type = Token.Type.valueOf(fields[2]);
    return new TokenImpl(textRange, text, type);
  }

  private static String substituteText(Token token) {
    return token.type() == Token.Type.STRING_LITERAL ? "LITERAL" : token.text();
  }
}
