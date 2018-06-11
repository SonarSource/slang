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
package com.sonarsource.slang.kotlin;

import com.sonarsource.slang.api.ASTConverter;
import com.sonarsource.slang.api.Tree;
import com.sonarsource.slang.impl.TreeMetaDataProvider;
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys;
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer;
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.com.intellij.openapi.editor.Document;
import org.jetbrains.kotlin.com.intellij.openapi.project.Project;
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer;
import org.jetbrains.kotlin.com.intellij.psi.PsiFile;
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.idea.KotlinLanguage;

public class KotlinConverter implements ASTConverter {
  private static final PsiFileFactory psiFileFactory = PsiFileFactory.getInstance(createKotlinCoreEnvironment());

  @Override
  public Tree parse(String content) {
    PsiFile psiFile = psiFileFactory.createFileFromText(KotlinLanguage.INSTANCE, content);
    Document document;
    try {
      document = psiFile.getViewProvider().getDocument();
    } catch (AssertionError e) {
      // A KotlinLexerException may occur when attempting to read invalid files
      throw new ParseException("Cannot correctly map AST with a null Document object");
    }

    CommentVisitor commentVisitor = new CommentVisitor(document);
    psiFile.accept(commentVisitor);
    KotlinTreeVisitor kotlinTreeVisitor = new KotlinTreeVisitor(psiFile, new TreeMetaDataProvider(commentVisitor.getAllComments()));
    return kotlinTreeVisitor.getSLangAST();
  }

  private static Project createKotlinCoreEnvironment() {
    System.setProperty("idea.io.use.fallback", "true");
    CompilerConfiguration configuration = new CompilerConfiguration();
    configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, new PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false));
    return KotlinCoreEnvironment.createForProduction(Disposer.newDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES).getProject();
  }

}
