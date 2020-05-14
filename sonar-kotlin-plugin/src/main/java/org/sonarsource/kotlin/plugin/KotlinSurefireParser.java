package org.sonarsource.kotlin.plugin;

import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.surefire.SurefireJavaParser;
import org.sonar.plugins.surefire.api.SurefireUtils;

import java.io.File;
import java.util.List;

@DependedUpon("surefire-java")
public class KotlinSurefireParser {

  private static final Logger LOGGER = Loggers.get(KotlinSensor.class);

  private final SurefireJavaParser surefireJavaParser;
  private final PathResolver pathResolver = new PathResolver();

  public KotlinSurefireParser(SurefireJavaParser surefireJavaParser) {
    this.surefireJavaParser = surefireJavaParser;
  }

  public void collect(SensorContext context) {
    List<File> reportsDirs = SurefireUtils.getReportsDirectories(context.config(), context.fileSystem(), pathResolver);
    LOGGER.info("parsing {}", reportsDirs);
    surefireJavaParser.collect(context, reportsDirs, context.config().hasKey(SurefireUtils.SUREFIRE_REPORT_PATHS_PROPERTY));
  }
}
