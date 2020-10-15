/*
 * SonarSource SLang
 * Copyright (C) 2018-2019 SonarSource SA
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
package org.sonar.go.checks;

import org.sonar.go.converter.GoConverter;
import org.sonarsource.slang.api.ASTConverter;
import org.sonarsource.slang.checks.api.SlangCheck;

import java.nio.file.Path;
import java.nio.file.Paths;

public class GoVerifier {
    private static final Path BASE_DIR = Paths.get("src", "test", "resources", "checks");
    private static final ASTConverter CONVERTER = new GoConverter(Paths.get("build", "tmp").toFile());

    public static void verify(String fileName, SlangCheck check) {
        org.sonarsource.slang.testing.Verifier.verify(CONVERTER, BASE_DIR.resolve(fileName), check);
    }
}
