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
package org.sonarsource.slang.stats;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.jetbrains.kotlin.com.google.common.collect.Lists;
import org.sonarsource.kotlin.converter.KotlinConverter;
import org.sonarsource.ruby.converter.RubyConverter;
import org.sonarsource.scala.converter.ScalaConverter;
import org.sonarsource.slang.api.ASTConverter;
import org.sonarsource.slang.api.ParseException;
import org.sonarsource.slang.api.Tree;

public class ComputeStats {

  public static final List<LanguageMeta> LANGUAGES = Arrays.asList(
    new LanguageMeta("kotlin", "kotlin",".kt", new KotlinConverter()),
    new LanguageMeta("ruby", "ruby", ".rb", new RubyConverter()),
    new LanguageMeta("scala", "scala", ".scala", new ScalaConverter())
  );

  public static void main(String[] args) throws IOException {
    Map<String, List<ConversionPair>> pairsByLanguage = new HashMap<>();

    LANGUAGES.forEach(languageMeta -> pairsByLanguage.put(
      languageMeta.name,
      getKindPairs(new File("its/sources/", languageMeta.dir).getAbsoluteFile().toPath(), languageMeta)));

    Map<String, KindByKind> slangKindsByOriginal = new HashMap<>();
    pairsByLanguage.forEach((lang, pairs) -> slangKindsByOriginal.put(
      lang,
      aggregate(pairs, (ConversionPair pair) -> pair.originalTreeKind, (ConversionPair pair) -> pair.slangTreeKind)));

    Map<String, KindByKind> originalBySlangKind = new HashMap<>();
    pairsByLanguage.forEach((lang, pairs) -> originalBySlangKind.put(
      lang,
      aggregate(pairs, (ConversionPair pair) -> pair.slangTreeKind, (ConversionPair pair) -> pair.originalTreeKind)));

    writeToFileBySlang(originalBySlangKind);
    slangKindsByOriginal.forEach((key, value) -> writeToFileForLang(key, "", value));
  }

  public static void writeToFileForLang(String language, String fileSuffix, KindByKind kinds) {
    File file = new File("stats/out", "slang_for_" + language + fileSuffix + ".md");
    System.out.println("Writing to file " + file.getAbsolutePath());
    List<String> lines = new ArrayList<>();
    lines.add(language + "|slang");
    lines.add("--|--");

    List<String> originalKinds = Lists.newArrayList(kinds.data.keySet());
    Collections.sort(originalKinds);

    originalKinds.forEach(originalKind -> {
      lines.add(originalKind + "|" + concat(kinds.data.get(originalKind)));
    });

    try {
      Files.write(file.toPath(), lines);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void writeToFileBySlang(Map<String, KindByKind> originalBySlangKind) throws IOException {
    File file = new File("stats/out", "stat_by_slang.md");
    System.out.println("Writing to file " + file.getAbsolutePath());
    List<String> lines = new ArrayList<>();
    List<String> languages = Lists.newArrayList(originalBySlangKind.keySet());
    lines.add("slang|" + String.join("|", languages));
    String separator = "--";
    for (String language : languages) {
      separator += "|--";
    }
    lines.add(separator);

    Set<String> slangKindsSet = new HashSet<>();
    originalBySlangKind.forEach((lang, v) -> slangKindsSet.addAll(v.data.keySet()));
    List<String> slangKinds = Lists.newArrayList(slangKindsSet);
    Collections.sort(slangKinds);

    slangKinds.forEach(slangKind -> {
      StringBuilder sb = new StringBuilder();
      sb.append(slangKind).append("|");
      languages.forEach(lang -> sb.append(concat(originalBySlangKind.get(lang).data.get(slangKind))).append("|"));

      String result = sb.toString();
      result = result.substring(0, result.length() - 1);
      lines.add(result);
    });

    Files.write(file.toPath(), lines);
  }

  private static String concat(@Nullable Map<String, Double> kinds) {
    if (kinds == null) {
      return "-";
    }

    StringBuilder sb = new StringBuilder();

    kinds.entrySet().stream()
      .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
      .forEach(entry -> sb.append(entry.getKey()).append("(").append(Math.round(entry.getValue())).append("%); "));

    return sb.toString();
  }

  private static KindByKind aggregate(List<ConversionPair> pairs, Function<ConversionPair, String> getKey, Function<ConversionPair, String> getValue) {
    Map<String, Map<String, Integer>> intermediate = new HashMap<>();
    for (ConversionPair pair : pairs) {
      String key = getKey.apply(pair);
      String value = getValue.apply(pair);
      intermediate
        .computeIfAbsent(key, k -> new HashMap<>())
        .putIfAbsent(value, 0);
      intermediate.get(key).compute(value, (k, v) -> v + 1);
    }
    KindByKind aggregated = new KindByKind();
    ArrayList<String> kinds = Lists.newArrayList(intermediate.keySet());
    Collections.sort(kinds);
    for (String kind : kinds) {
      aggregated.data.put(kind, new HashMap<>());
      Map<String, Integer> matchingKinds = intermediate.get(kind);
      int sum = matchingKinds.values().stream().mapToInt(Integer::intValue).sum();
      matchingKinds.forEach((matchingKind, numberOfOccurrences) ->
        aggregated.data.get(kind).put(matchingKind, 1.0 * numberOfOccurrences / sum * 100));
    }

    return aggregated;
  }

  private static List<ConversionPair> getKindPairs(Path sourceTestFilesFolder, LanguageMeta language) {
    List<ConversionPair> conversionPairs = new ArrayList<>();

    try (Stream<Path> paths = Files.walk(sourceTestFilesFolder)) {
      System.out.println("Converting for " + language.name);
      int counter = 0;
      for (Path path : paths.filter(path -> path.toString().endsWith(language.extension)).collect(Collectors.toList())) {
        if (counter % 100 == 0) {
          System.out.println("Analyzed " + counter + " files");
        }
        convertFile(path, conversionPairs, language.converter);
        counter++;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    return conversionPairs;
  }

  private static void convertFile(Path path, List<ConversionPair> conversionPairs, ASTConverter converter) throws IOException {
    String code = String.join("\n", Files.readAllLines(path));
    try {
      Tree convertedTree = converter.parse(code);
      convertedTree.descendants().forEach(tree -> {
        if (tree.metaData() != null) {
          conversionPairs.add(new ConversionPair(tree.metaData().originalTreeKind(), tree.getClass().getSimpleName()));
        } else {
          System.out.println("Metadata is null for " + path);
        }
      });
    } catch (ParseException e) {
      System.out.println("Failed to parse " + path);
    }
  }

  static class LanguageMeta {
    String name;
    String dir;
    String extension;
    ASTConverter converter;

    LanguageMeta(String name, String dir, String extension, ASTConverter converter) {
      this.name = name;
      this.dir = dir;
      this.extension = extension;
      this.converter = converter;
    }
  }

  static class ConversionPair {
    String originalTreeKind;
    String slangTreeKind;

    ConversionPair(String originalTreeKind, String slangTreeKind) {
      this.originalTreeKind = originalTreeKind;
      this.slangTreeKind = slangTreeKind;
    }

    @Override
    public String toString() {
      return originalTreeKind + " -> " + slangTreeKind;
    }
  }

  public static class KindByKind {
    // Map<Kind1, Map<Kind2, percentage of occurrences of Kind2 matching Kind1>>
    Map<String, Map<String, Double>> data = new HashMap<>();
  }

}
