/*
 * SonarSource SLang
 * Copyright (C) 2018-2025 SonarSource SA
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.api.batch.sensor.cache.WriteCache;
import org.sonar.api.batch.sensor.cpd.internal.TokensLine;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonarsource.slang.api.Token;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.impl.TextRangeImpl;
import org.sonarsource.slang.impl.TokenImpl;
import org.sonarsource.slang.parser.SLangConverter;
import org.sonarsource.slang.plugin.caching.DummyReadCache;
import org.sonarsource.slang.plugin.caching.DummyWriteCache;
import org.sonarsource.slang.testing.ThreadLocalLogTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.sonarsource.slang.plugin.CpdVisitor.ASCII_RECORD_SEPARATOR;
import static org.sonarsource.slang.plugin.CpdVisitor.ASCII_UNIT_SEPARATOR;
import static org.sonarsource.slang.plugin.CpdVisitor.computeCacheKey;

class CpdVisitorTest {
  @RegisterExtension
  private ThreadLocalLogTester logTester = new ThreadLocalLogTester();

  @Test
  void test(@TempDir File tempFolder) throws Exception {
    File file = File.createTempFile("file", ".tmp", tempFolder);
    String content = "import util; foo(x\n * 42 \n+ \"abc\");";
    SensorContextTester sensorContext = SensorContextTester.create(tempFolder);
    DefaultInputFile inputFile = new TestInputFileBuilder("moduleKey", file.getName())
      .setContents(content)
      .build();
    Tree root = new SLangConverter().parse(content);
    InputFileContext ctx = new InputFileContext(sensorContext, inputFile);
    new CpdVisitor().scan(ctx, root);

    List<TokensLine> cpdTokenLines = sensorContext.cpdTokens(inputFile.key());
    assertThat(cpdTokenLines).hasSize(3);

    assertThat(cpdTokenLines.get(0).getValue()).isEqualTo("foo(x");
    assertThat(cpdTokenLines.get(0).getStartLine()).isEqualTo(1);
    assertThat(cpdTokenLines.get(0).getStartUnit()).isEqualTo(1);
    assertThat(cpdTokenLines.get(0).getEndUnit()).isEqualTo(3);

    assertThat(cpdTokenLines.get(1).getValue()).isEqualTo("*42");
    assertThat(cpdTokenLines.get(1).getStartLine()).isEqualTo(2);
    assertThat(cpdTokenLines.get(1).getStartUnit()).isEqualTo(4);
    assertThat(cpdTokenLines.get(1).getEndUnit()).isEqualTo(5);

    assertThat(cpdTokenLines.get(2).getValue()).isEqualTo("+LITERAL);");
    assertThat(cpdTokenLines.get(2).getStartLine()).isEqualTo(3);
    assertThat(cpdTokenLines.get(2).getStartUnit()).isEqualTo(6);
    assertThat(cpdTokenLines.get(2).getEndUnit()).isEqualTo(9);
  }

  @Nested
  class BranchAnalysisContext {
    private static final List<Token> EXPECTED_TOKENS = List.of(
      new TokenImpl(new TextRangeImpl(1, 0, 1, 6), "import", Token.Type.OTHER),
      new TokenImpl(new TextRangeImpl(1, 7, 1, 12), "hello", Token.Type.OTHER),
      new TokenImpl(new TextRangeImpl(1, 12, 1, 13), ";", Token.Type.OTHER)
    );

    private SensorContextTester sensorContext;
    private DefaultInputFile inputFile;
    private Tree root;
    private InputFileContext inputFileContext;
    private DummyWriteCache nextCache;

    @BeforeEach
    void setup(@TempDir File tempFolder) throws IOException {
      File file = File.createTempFile("file", ".tmp", tempFolder);
      String content = "import hello;";
      sensorContext = SensorContextTester.create(tempFolder);
      inputFile = new TestInputFileBuilder("moduleKey", file.getName())
        .setContents(content)
        .build();
      root = new SLangConverter().parse(content);
      inputFileContext = new InputFileContext(sensorContext, inputFile);
      // Set up the writing cache
      nextCache = new DummyWriteCache();
      sensorContext.setNextCache(nextCache);
      sensorContext.setCacheEnabled(true);
    }

    @Test
    void tokens_are_systematically_persisted_in_the_cache_when_caching_is_enabled() {
      // Produce tokens
      new CpdVisitor().scan(inputFileContext, root);

      String cacheKey = computeCacheKey(inputFile);
      assertThat(nextCache.persisted)
        .hasSize(1)
        .containsKey(cacheKey);
      byte[] serialized = nextCache.persisted.get(cacheKey);
      assertThat(serialized).isEqualTo(CpdVisitor.serialize(EXPECTED_TOKENS));
    }

    @Test
    void tokens_are_not_persisted_in_the_cache_when_caching_is_disabled() {
      // Disable the cache
      sensorContext.setCacheEnabled(false);

      new CpdVisitor().scan(inputFileContext, root);

      assertThat(nextCache.persisted).isEmpty();
    }

    @Test
    void tokens_are_not_persisted_when_the_cache_already_contains_an_entry_for_the_file() {
      // Set up the cache where will be writing but where an entry already exists
      var cache = new DummyWriteCache();
      String cacheKey = computeCacheKey(inputFile);
      cache.persisted.put(cacheKey, new byte[]{});
      sensorContext.setNextCache(cache);

      new CpdVisitor().scan(inputFileContext, root);

      assertThat(cache.persisted).containsOnlyKeys(cacheKey);
      String expectedWarningMessage = "Failed to write CPD tokens to cache for input file %s: ".formatted(inputFile.key()) +
        "The cache already contains the key: %s".formatted(cacheKey);
      assertThat(logTester.logs(Level.WARN)).containsOnly(expectedWarningMessage);
    }
  }

  @Nested
  class PullRequestAnalysisContext {
    private static final List<Token> EXPECTED_TOKENS = List.of(
      new TokenImpl(new TextRangeImpl(1, 0, 1, 6), "import", Token.Type.OTHER),
      new TokenImpl(new TextRangeImpl(1, 7, 1, 12), "hello", Token.Type.OTHER),
      new TokenImpl(new TextRangeImpl(1, 12, 1, 13), ";", Token.Type.OTHER)
    );

    private String cacheKey;
    private DefaultInputFile inputFile;
    private SensorContextTester sensorContext;
    private InputFileContext inputFileContext;
    private DummyReadCache previousCache;
    private DummyWriteCache nextCache;

    @BeforeEach
    /**
     * Sets up the happy path to reuse tokens.
     * - Skipping of unchanged files is enabled
     * - Caching is enabled
     * - The previous cache contains an entry for the input file with properly serialized tokens
     * - The previous and next caches are bound together
     */
    void setup(@TempDir File tempFolder) throws IOException {
      // Create file and set its status to something else than SAME
      File file = File.createTempFile("file", ".tmp", tempFolder);
      String content = "import hello;";
      inputFile = new TestInputFileBuilder("moduleKey", file.getName())
        .setContents(content)
        .setStatus(InputFile.Status.SAME)
        .build();
      // Set context for PR analysis
      sensorContext = spy(SensorContextTester.create(tempFolder));
      sensorContext.setCanSkipUnchangedFiles(true);
      sensorContext.setCacheEnabled(true);
      inputFileContext = new InputFileContext(sensorContext, inputFile);
      // Setup caches
      cacheKey = "slang:cpd-tokens:" + inputFile.key();
      previousCache = spy(new DummyReadCache());
      previousCache.persisted.put(cacheKey, CpdVisitor.serialize(EXPECTED_TOKENS));
      sensorContext.setPreviousCache(previousCache);
      nextCache = spy(new DummyWriteCache());
      nextCache.bind(previousCache);
      sensorContext.setNextCache(nextCache);
    }

    private void assertNoInteractionWithNextCache(WriteCache nextCache) {
      verify(nextCache, never()).write(any(String.class), any(byte[].class));
      verify(nextCache, never()).write(any(String.class), any(InputStream.class));
      verify(nextCache, never()).copyFromPrevious(any());
    }

    private void assertNoInteractionWithPreviousCache(ReadCache previousCache) {
      verify(previousCache, never()).contains(any());
      verify(previousCache, never()).read(any());
    }

    @Test
    void reuses_results_from_previous_analysis_when_available_in_cache() {
      CpdVisitor visitor = new CpdVisitor();

      assertThat(visitor.reusePreviousResults(inputFileContext)).isTrue();
      verify(previousCache, times(1)).contains(cacheKey);
      verify(previousCache, atLeastOnce()).read(cacheKey);
      verify(nextCache, times(1)).copyFromPrevious(cacheKey);

      assertThat(nextCache.persisted).containsAllEntriesOf(previousCache.persisted);
    }

    @Test
    void does_not_reuse_results_from_previous_analysis_when_cache_is_disabled() {
      // Disable the cache
      sensorContext.setCacheEnabled(false);

      CpdVisitor visitor = new CpdVisitor();

      assertThat(visitor.reusePreviousResults(inputFileContext)).isFalse();
      // Ensure there are no unexpected interactions with the caches
      verify(sensorContext, times(1)).isCacheEnabled();
      assertNoInteractionWithPreviousCache(previousCache);
      assertNoInteractionWithNextCache(nextCache);
      assertThat(nextCache.persisted).isEmpty();
    }

    @Test
    void does_not_reuse_results_from_previous_analysis_when_unchanged_files_cannot_be_skipped() {
      // Disable the skipping of unchanged files
      sensorContext.setCanSkipUnchangedFiles(false);

      CpdVisitor visitor = new CpdVisitor();

      assertThat(visitor.reusePreviousResults(inputFileContext)).isFalse();
      // Ensure there are no unexpected interactions with the caches
      verify(sensorContext, never()).isCacheEnabled();
      assertNoInteractionWithPreviousCache(previousCache);
      assertNoInteractionWithNextCache(nextCache);
      assertThat(nextCache.persisted).isEmpty();
    }

    @Test
    void does_not_reuse_results_from_previous_analysis_when_file_is_changed(@TempDir File tempFolder) throws IOException {
      // Prepare a file with a status different from InputFile.Status.SAME
      File file = File.createTempFile("file", ".tmp", tempFolder);
      String content = "import hello;";
      inputFile = new TestInputFileBuilder("moduleKey", file.getName())
        .setContents(content)
        .setStatus(InputFile.Status.CHANGED)
        .build();
      inputFileContext = new InputFileContext(sensorContext, inputFile);

      CpdVisitor visitor = new CpdVisitor();

      assertThat(visitor.reusePreviousResults(inputFileContext)).isFalse();
      // Ensure there are no unexpected interactions with the caches
      verify(sensorContext, times(1)).isCacheEnabled();
      assertNoInteractionWithPreviousCache(previousCache);
      assertNoInteractionWithNextCache(nextCache);
      assertThat(nextCache.persisted).isEmpty();
    }

    @Test
    void does_not_reuse_results_from_previous_analysis_when_cache_miss() {
      // Empty the previous cache
      previousCache.persisted.clear();

      CpdVisitor visitor = new CpdVisitor();

      assertThat(visitor.reusePreviousResults(inputFileContext)).isFalse();
      // Ensure there are no unexpected interactions with the caches
      verify(sensorContext, times(1)).isCacheEnabled();
      verify(previousCache, times(1)).contains(cacheKey);
      verify(previousCache, never()).read(any());
      assertNoInteractionWithNextCache(nextCache);
      assertThat(nextCache.persisted).isEmpty();
    }

    @Test
    void does_not_reuse_results_from_previous_analysis_when_failing_to_deserialize() {
      // Replace data in the cache with gibberish that cannot be deserialized
      previousCache.persisted.put(cacheKey, new byte[]{0xC, 0xA, 0xF, 0xE});

      CpdVisitor visitor = new CpdVisitor();

      assertThat(visitor.reusePreviousResults(inputFileContext)).isFalse();
      // Ensure there are no unexpected interactions with the caches
      verify(sensorContext, times(1)).isCacheEnabled();
      verify(previousCache, times(1)).contains(cacheKey);
      verify(previousCache, times(1)).read(cacheKey);
      assertNoInteractionWithNextCache(nextCache);
      assertThat(nextCache.persisted).isEmpty();

      assertThat(logTester.logs(Level.WARN)).contains(
        String.format("Failed to load cached CPD tokens for input file %s.", inputFile.key())
      );
    }

    @Test
    void does_not_reuse_results_from_previous_analysis_when_failing_to_read_stream_from_the_cache() throws IOException {
      // Replace the previous cache with a cache returning a faulty stream
      InputStream in = spy(new ByteArrayInputStream(new byte[]{}));
      doThrow(new IOException("This is expected")).when(in).readAllBytes();
      DummyReadCache cacheReturningFaultyStreams = new DummyReadCache() {
        @Override
        public InputStream read(String ignored) {
          return in;
        }
      };
      cacheReturningFaultyStreams.persisted.putAll(previousCache.persisted);
      nextCache.bind(cacheReturningFaultyStreams);
      sensorContext.setPreviousCache(cacheReturningFaultyStreams);

      CpdVisitor visitor = new CpdVisitor();

      assertThat(visitor.reusePreviousResults(inputFileContext)).isFalse();
      assertThat(logTester.logs(Level.WARN))
        .containsOnly("Failed to load cached CPD tokens for input file %s.".formatted(inputFile.key()));
    }

    @Test
    void does_not_reuse_results_from_previous_analysis_when_failing_to_read_from_the_cache() {
      // Replace the previous cache with an empty cache pretending it contains the key
      DummyReadCache lyingCache = new DummyReadCache() {
        @Override
        public boolean contains(String ignored) {
          return true;
        }
      };
      sensorContext.setPreviousCache(lyingCache);
      nextCache.bind(lyingCache);
      sensorContext.setPreviousCache(lyingCache);

      CpdVisitor visitor = new CpdVisitor();

      assertThat(visitor.reusePreviousResults(inputFileContext)).isFalse();
      assertThat(logTester.logs(Level.WARN))
        .containsOnly("Failed to load cached CPD tokens for input file %s.".formatted(inputFile.key()));
    }

    @Test
    void does_not_reuse_results_from_previous_analysis_when_failing_to_copy_from_previous_cache() {
      // Replace the next cache with a cache failing to copy from the previous analysis
      DummyWriteCache cacheFailingToCopyFromPrevious = new DummyWriteCache() {
        @Override
        public void copyFromPrevious(String key) {
          throw new IllegalArgumentException("This is expected");
        }
      };
      sensorContext.setNextCache(cacheFailingToCopyFromPrevious);
      cacheFailingToCopyFromPrevious.bind(previousCache);

      CpdVisitor visitor = new CpdVisitor();

      assertThat(visitor.reusePreviousResults(inputFileContext)).isFalse();
      String expectedWarningMessage = "Failed to copy previous cached results for input file %s.".formatted(inputFile.key());
      assertThat(logTester.logs(Level.WARN))
        .containsOnly(expectedWarningMessage);
    }
  }

  @Test
  void test_computeCacheKey(@TempDir File tempFolder) throws IOException {
    File file = File.createTempFile("file", ".tmp", tempFolder);
    InputFile inputFile = new TestInputFileBuilder("moduleKey", file.getName())
      .setContents("")
      .build();
    assertThat(CpdVisitor.computeCacheKey(inputFile)).isEqualTo("slang:cpd-tokens:" + inputFile.key());
  }

  @Test
  void serialize_produces_the_expected_format() {
    assertThat(CpdVisitor.serialize(Collections.emptyList())).isEmpty();
    Token token = new TokenImpl(new TextRangeImpl(1, 0, 1, 6), "import", Token.Type.KEYWORD);
    List<Token> singleToken = Collections.singletonList(token);
    byte[] serialized = CpdVisitor.serialize(singleToken);
    assertThat(new String(serialized, StandardCharsets.UTF_8))
      .isNotBlank()
      .isEqualTo("1,0,1,6" + ASCII_UNIT_SEPARATOR + "import" + ASCII_UNIT_SEPARATOR + "KEYWORD");

    List<Token> tokens = List.of(
      new TokenImpl(new TextRangeImpl(1, 0, 1, 7), "correct", Token.Type.KEYWORD),
      new TokenImpl(new TextRangeImpl(1, 9, 1, 13), "horse", Token.Type.STRING_LITERAL),
      new TokenImpl(new TextRangeImpl(1, 15, 1, 21), "battery", Token.Type.OTHER),
      new TokenImpl(new TextRangeImpl(1, 23, 1, 28), "staple", Token.Type.KEYWORD)
    );
    String tokensSerializedAsString = new String(CpdVisitor.serialize(tokens), StandardCharsets.UTF_8);
    String[] serializedTokens = tokensSerializedAsString.split(String.valueOf(ASCII_RECORD_SEPARATOR));
    assertThat(serializedTokens).hasSize(4);

    assertThat(serializedTokens[0]).isEqualTo("1,0,1,7" + ASCII_UNIT_SEPARATOR + "correct" + ASCII_UNIT_SEPARATOR + "KEYWORD");
    assertThat(serializedTokens[1]).isEqualTo("1,9,1,13" + ASCII_UNIT_SEPARATOR + "horse" + ASCII_UNIT_SEPARATOR + "STRING_LITERAL");
    assertThat(serializedTokens[2]).isEqualTo("1,15,1,21" + ASCII_UNIT_SEPARATOR + "battery" + ASCII_UNIT_SEPARATOR + "OTHER");
    assertThat(serializedTokens[3]).isEqualTo("1,23,1,28" + ASCII_UNIT_SEPARATOR + "staple" + ASCII_UNIT_SEPARATOR + "KEYWORD");
  }

  @Test
  void deserialize_produces_the_expected_tokens() {
    // No data
    assertThat(CpdVisitor.deserialize(new byte[]{})).isEmpty();

    byte[] singleSerializedToken = ("1,0,1,6" + ASCII_UNIT_SEPARATOR + "import" + ASCII_UNIT_SEPARATOR + "KEYWORD").getBytes(StandardCharsets.UTF_8);
    List<Token> singleToken = CpdVisitor.deserialize(singleSerializedToken);
    Token expectedToken = new TokenImpl(
      new TextRangeImpl(1, 0, 1, 6),
      "import",
      Token.Type.KEYWORD
    );
    assertThat(singleToken).containsExactly(expectedToken);

    byte[] serializedTokens = (
      "1,0,1,7" + ASCII_UNIT_SEPARATOR + "correct" + ASCII_UNIT_SEPARATOR + "KEYWORD" +
        ASCII_RECORD_SEPARATOR +
        "1,9,1,13" + ASCII_UNIT_SEPARATOR + "horse" + ASCII_UNIT_SEPARATOR + "STRING_LITERAL" +
        ASCII_RECORD_SEPARATOR +
        "1,15,1,21" + ASCII_UNIT_SEPARATOR + "battery" + ASCII_UNIT_SEPARATOR + "OTHER" +
        ASCII_RECORD_SEPARATOR +
        "1,23,1,28" + ASCII_UNIT_SEPARATOR + "staple" + ASCII_UNIT_SEPARATOR + "KEYWORD"
    ).getBytes(StandardCharsets.UTF_8);

    assertThat(CpdVisitor.deserialize(serializedTokens)).containsExactly(
      new TokenImpl(new TextRangeImpl(1, 0, 1, 7), "correct", Token.Type.KEYWORD),
      new TokenImpl(new TextRangeImpl(1, 9, 1, 13), "horse", Token.Type.STRING_LITERAL),
      new TokenImpl(new TextRangeImpl(1, 15, 1, 21), "battery", Token.Type.OTHER),
      new TokenImpl(new TextRangeImpl(1, 23, 1, 28), "staple", Token.Type.KEYWORD)
    );
  }

  @Test
  void deserialize_throws_an_IllegalArgumentException_when_deserialization_fails() {
    byte[] missingLineEndOffset = (
      "1,0,1," + ASCII_UNIT_SEPARATOR + "correct" + ASCII_UNIT_SEPARATOR + "KEYWORD"
    ).getBytes(StandardCharsets.UTF_8);
    assertThatThrownBy(() -> CpdVisitor.deserialize(missingLineEndOffset))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageStartingWith("Could not deserialize cached CPD tokens:");

    byte[] nonNumericLineEndOffset = (
      "1,0,1,vb" + ASCII_UNIT_SEPARATOR + "correct" + ASCII_UNIT_SEPARATOR + "KEYWORD"
    ).getBytes(StandardCharsets.UTF_8);
    assertThatThrownBy(() -> CpdVisitor.deserialize(nonNumericLineEndOffset))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageStartingWith("Could not deserialize cached CPD tokens:");

    byte[] unexpectedTokenType = (
      "1,0,1,7" + ASCII_UNIT_SEPARATOR + "correct" + ASCII_UNIT_SEPARATOR + "UNKNOWN"
    ).getBytes(StandardCharsets.UTF_8);

    assertThatThrownBy(() -> CpdVisitor.deserialize(unexpectedTokenType))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageStartingWith("Could not deserialize cached CPD tokens:");
  }
}
