/*
 * SonarSource SLang
 * Copyright (C) 2018-2021 SonarSource SA
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
package org.sonarsource.scala.checks;

import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonarsource.slang.api.ClassDeclarationTree;
import org.sonarsource.slang.api.IdentifierTree;
import org.sonarsource.slang.api.PackageDeclarationTree;
import org.sonarsource.slang.api.Tree;
import org.sonarsource.slang.checks.UnusedPrivateMethodCheck;
import org.sonarsource.slang.checks.api.CheckContext;
import org.sonarsource.slang.impl.NativeTreeImpl;

@Rule(key = "S1144")
public class UnusedPrivateMethodScalaCheck extends UnusedPrivateMethodCheck {

  // Serializable method should not raise any issue in Scala.
  private static final Set<String> IGNORED_METHODS = new HashSet<>(Arrays.asList(
    "writeObject",
    "readObject",
    "writeReplace",
    "readResolve",
    "readObjectNoData"));

  // companion objects may use methods in the main class
  private Set<String> usagesInCompanionObjects = new HashSet<>();

  @Override
  protected void processClassDeclaration(CheckContext context, ClassDeclarationTree classDeclarationTree) {
    // only verify the outermost class in the file, to avoid raising the same issue multiple times
    IdentifierTree identifier = classDeclarationTree.identifier();
    if (context.ancestors().stream().noneMatch(ClassDeclarationTree.class::isInstance) &&
        identifier != null) {
      collectUsagesInCompanionObject(identifier.name(), context.ancestors());
      reportUnusedPrivateMethods(context, classDeclarationTree);
    }
  }

  @Override
  protected boolean isUnusedMethod(IdentifierTree identifier, Set<String> usedIdentifierNames) {
    return super.isUnusedMethod(identifier, usedIdentifierNames) &&
      !IGNORED_METHODS.contains(identifier.name()) &&
      !usagesInCompanionObjects.contains(identifier.name());
  }

  private void collectUsagesInCompanionObject(String className, Deque<Tree> ancestors) {
    // a top-level class either has 1 ancestor (TopLevelTree) or could be inside a package
    if (ancestors.size() == 1 ||  isInsidePackage(ancestors)) {
      // search for the companion object and collect what's inside
      ancestors.getFirst().descendants()
        .filter(NativeTreeImpl.class::isInstance)
        .map(NativeTreeImpl.class::cast)
        .filter(n -> isObjectCompanionForClass(className, n))
        .forEach(n -> {
          MethodAndIdentifierCollector collector = new MethodAndIdentifierCollector(n.descendants());
          usagesInCompanionObjects = collector.getUsedUniqueIdentifiers();
        });
    }
  }

  private static boolean isObjectCompanionForClass(String className, NativeTreeImpl nativeTree) {
    return nativeTree.nativeKind().toString().contains("scala.meta.Defn$Object$DefnObjectImpl") &&
      nativeTree.children().stream().anyMatch(i -> i instanceof IdentifierTree && ((IdentifierTree)i).identifier().equals(className));
  }

  // Two ancestors: PackageDeclarationTree and TopLevelTree
  private static boolean isInsidePackage(Deque<Tree> ancestors) {
    return ancestors.size() == 2 && ancestors.getFirst() instanceof PackageDeclarationTree;
  }
}
