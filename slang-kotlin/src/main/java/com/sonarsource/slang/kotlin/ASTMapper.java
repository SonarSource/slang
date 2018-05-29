package com.sonarsource.slang.kotlin;

import com.sonarsource.slang.api.Tree;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys;
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer;
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.com.intellij.openapi.project.Project;
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer;
import org.jetbrains.kotlin.com.intellij.psi.PsiFile;
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.idea.KotlinLanguage;

import static java.nio.file.Files.readAllBytes;

public class ASTMapper {

  public static Tree fromFile(String fileName) throws IOException {
    return fromString(readFile(fileName));
  }

  public static Tree fromString(String content) {
    PsiFile psiFile = compile(content);
    KotlinTreeVisitor kotlinTreeVisitor = new KotlinTreeVisitor();
    psiFile.accept(kotlinTreeVisitor);
    return kotlinTreeVisitor.getSLangAST();
  }

  private static String readFile(String fileName) throws IOException {
    if (!fileName.endsWith(".kt")) {
      throw new IllegalArgumentException("Trying to parse file with unknown extension");
    }

    return new String(readAllBytes(Paths.get(fileName)), Charset.forName("UTF-8"));
  }

  private static PsiFile compile(String content) {
    PsiFileFactory psiFileFactory = PsiFileFactory.getInstance(createKotlinCoreEnvironment());
    return psiFileFactory.createFileFromText(KotlinLanguage.INSTANCE, content);
  }

  private static Project createKotlinCoreEnvironment() {
    System.setProperty("idea.io.use.fallback", "true");
    CompilerConfiguration configuration = new CompilerConfiguration();
    configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, new PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false));
    return KotlinCoreEnvironment.createForProduction(Disposer.newDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES).getProject();
  }

}
