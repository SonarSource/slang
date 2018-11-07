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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.sonarsource.slang.stats.ComputeStats.KindByKind;

public class ReduceStats {

  private static final File slangForKotlin = new File("stats/out", "slang_for_kotlin.md");
  private static final File slangForScala = new File("stats/out", "slang_for_scala.md");
  private static final File slangForRuby = new File("stats/out", "slang_for_ruby.md");

  public static void main(String[] args) throws IOException {
    removeLines(slangForKotlin, "kotlin");
    removeLines(slangForScala, "scala");
    removeLines(slangForRuby, "ruby");
  }

  private static void removeLines(File file, String language) throws IOException {
    KindByKind kotlinData = new KindByKind();

    Files.readAllLines(file.toPath()).stream().skip(2).forEach(str -> {
      String[] parts = str.split("\\|");
      String[] parts2 = parts[1].split(";");
      Map<String, Double> percentages = new HashMap<>();
      for (String part : parts2) {
        if (part.trim().isEmpty()) {
          continue;
        }
        String[] parts3 = part.trim().split("\\(");
        percentages.put(parts3[0].trim(), Double.parseDouble(parts3[1].split("%")[0]));
      }
      kotlinData.data.put(parts[0].trim(), percentages);
    });

    new HashSet<>(kotlinData.data.keySet()).forEach(kotlinKind -> {
      Map<String, Double> slangKinds = kotlinData.data.get(kotlinKind);
      if (slangKinds.size() == 1) {
        kotlinData.data.remove(kotlinKind);
      }
      if (slangKinds.size() == 2 && max(slangKinds.values()) > 95) {
        kotlinData.data.remove(kotlinKind);
      }
    });

    ComputeStats.writeToFileForLang(language, "_part", kotlinData);
  }

  private static Double max(Collection<Double> values) {
    Double max = 0.;
    for (Double value : values) {
      if (value > max) {
        max = value;
      }
    }
    return max;
  }
}
