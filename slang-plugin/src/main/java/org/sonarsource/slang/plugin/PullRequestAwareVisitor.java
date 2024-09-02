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
package org.sonarsource.slang.plugin;

import org.sonar.api.batch.fs.InputFile;
import org.sonarsource.slang.visitors.TreeVisitor;

/**
 * A type of Visitor that can leverage previous results rather than recompute findings from scratch.
 */
public abstract class PullRequestAwareVisitor extends TreeVisitor<InputFileContext> {
  /**
   * Tries to copy the cached results from a previous analysis into the cache for the next one.
   *
   * @param inputFileContext The input file and its context
   * @return true if successful, false otherwise.
   */
  public abstract boolean reusePreviousResults(InputFileContext inputFileContext);

  /**
   * The simplest logic to test that the cache can be used for a given file, without checking that the cache contains
   * any relevant data.
   * Should be called by callers of {@link #reusePreviousResults(InputFileContext)} or any overriding implementation.
   */
  public boolean canReusePreviousResults(InputFileContext inputFileContext) {
    return inputFileContext.sensorContext.canSkipUnchangedFiles() &&
      inputFileContext.sensorContext.isCacheEnabled() &&
      inputFileContext.inputFile.status() == InputFile.Status.SAME;
  }
}
