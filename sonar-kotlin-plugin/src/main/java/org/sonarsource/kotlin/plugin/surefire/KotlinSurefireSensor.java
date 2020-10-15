package org.sonarsource.kotlin.plugin.surefire;

import java.io.File;
import java.util.List;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.kotlin.plugin.surefire.api.SurefireUtils;

public class KotlinSurefireSensor implements Sensor {
  private static final Logger LOGGER = Loggers.get(KotlinSurefireSensor.class);

  private final KotlinSurefireParser kotlinSurefireParser;
  private final Configuration settings;
  private final FileSystem fs;
  private final PathResolver pathResolver;

  public KotlinSurefireSensor(KotlinSurefireParser kotlinSurefireParser, Configuration settings, FileSystem fs, PathResolver pathResolver) {
    this.kotlinSurefireParser = kotlinSurefireParser;
    this.settings = settings;
    this.fs = fs;
    this.pathResolver = pathResolver;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.onlyOnLanguage("kotlin").name("KotlinSurefireSensor");
  }

  @Override
  public void execute(SensorContext context) {
    List<File> dirs = SurefireUtils.getReportsDirectories(settings, fs, pathResolver);
    collect(context, dirs);
  }

  protected void collect(SensorContext context, List<File> reportsDirs) {
    LOGGER.info("parsing {}", reportsDirs);
    kotlinSurefireParser.collect(context, reportsDirs, settings.hasKey(SurefireUtils.SUREFIRE_REPORT_PATHS_PROPERTY));
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
