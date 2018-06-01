package com.sonarsource.slang.api;

import java.io.File;
import java.util.Objects;

/**
 * Context injected in check classes and used to report issues.
 */
public interface SLangFileScannerContext {
  /**
   * Parsed tree of the current file.
   * @return ModuleTree ready for scan by checks.
   */
  ModuleTree getTree();

  /**
   * Report an issue at file level.
   * @param check The check raising the issue.
   * @param message Message to display to the user
   */
  void addIssueOnFile(SLangCheck check, String message);

  /**
   * Report an issue on a specific line.
   * @param line line on which to report the issue.
   * @param check The check raising the issue.
   * @param message Message to display to the user.
   */
  void addIssue(int line, SLangCheck check, String message);

  /**
   * Report an issue at a specific line of a given file.
   * This method is used for one.
   * @param file File on which to report.
   * @param check The check raising the issue.
   * @param line line on which to report the issue.
   * @param message Message to display to the user.
   */
  void addIssue(File file, SLangCheck check, int line, String message);

  /**
   * Report an issue.
   * @param check The check raising the issue.
   * @param tree syntax node on which to raise the issue.
   * @param message Message to display to the user.
   */
  void reportIssue(SLangCheck check, Tree tree, String message);

  /**
   * Report an issue.
   * @param check The check raising the issue.
   * @param startTree syntax node on which to start the highlighting of the issue.
   * @param endTree syntax node on which to end the highlighting of the issue.
   * @param message Message to display to the user.
   */
  void reportIssue(SLangCheck check, Tree startTree, Tree endTree, String message);

  /**
   * Checks if file has been parsed correctly.
   * @return true if parsing was successful
   */
  boolean fileParsed();


}
