package org.sonarsource.kotlin.plugin.surefire;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

@ScannerSide
public class KotlinResourcesLocator {
  private static final Logger LOGGER = Loggers.get(KotlinResourcesLocator.class);
  private final FileSystem fs;

  public KotlinResourcesLocator(FileSystem fs) {
    this.fs = fs;
  }
  
  public Optional<InputFile> findResourceByClassName(String className) {
    String fileName = className.replace(".", "/");
    LOGGER.info("Searching for {}", fileName);
    FilePredicates p = fs.predicates();
    FilePredicate fileNamePredicates =
      getFileNamePredicateFromSuffixes(p, fileName, new String[]{".kt"});
    if (fs.hasFiles(fileNamePredicates)) {
      return Optional.of(fs.inputFiles(fileNamePredicates).iterator().next());
    } else {
      return Optional.empty();
    }
  }

  private static FilePredicate getFileNamePredicateFromSuffixes(
    FilePredicates p, String fileName, String[] suffixes) {
    List<FilePredicate> fileNamePredicates = new ArrayList<>(suffixes.length);
    for (String suffix : suffixes) {
      fileNamePredicates.add(p.matchesPathPattern("**/" + fileName + suffix));
    }
    return p.or(fileNamePredicates);
  }
}
