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
package org.sonarsource.slang.testing;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PackageScanner {

  private PackageScanner() {
    // static usage only
  }

  /**
   * Returns the fully qualified names (FQNs) of the classes inside @packageName implementing SlangCheck.
   * @param packageName Used to filter classes - the FQN of a class contains the package name.
   * @return A list of slang checks (FQNs).
   */
  public static List<String> findSlangChecksInPackage(String packageName) {
    try (ScanResult scanResult = new ClassGraph().enableAllInfo().acceptPackages(packageName).scan()) {
      Map<String, ClassInfo> allClasses = scanResult.getAllClassesAsMap();
      List<String> testClassesInPackage = new ArrayList<>();
      for (Map.Entry<String, ClassInfo> classInfoEntry : allClasses.entrySet()) {
        String name = classInfoEntry.getKey();
        ClassInfo classInfo = classInfoEntry.getValue();
        if (name.startsWith(packageName) && classInfo.getInterfaces().stream().anyMatch(i -> i.getSimpleName().equals("SlangCheck"))) {
          testClassesInPackage.add(classInfo.getName());
        }
      }
      return testClassesInPackage;
    }
  }
}
