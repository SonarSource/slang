package org.sonarsource.kotlin.plugin.surefire;

import java.io.File;
import java.util.Collections;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFilePredicates;
import org.sonar.api.batch.fs.internal.DefaultIndexedFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KotlinResourcesLocatorTest {

  private final FileSystem fileSystem = mock(FileSystem.class);
  private final KotlinResourcesLocator kotlinResourcesLocator = new KotlinResourcesLocator(fileSystem);
  private final InputFile expected = new DefaultInputFile(new DefaultIndexedFile("", new File("/").toPath(), "",""), (x) -> {});
  
  @Before
  public void setUp() {
    when(fileSystem.predicates()).thenReturn(new DefaultFilePredicates(new File("/").toPath()));
    when(fileSystem.inputFiles(any())).thenReturn(Collections.singletonList(expected));
  }
  
  @Test
  public void findResourceByClassName() {
    when(fileSystem.hasFiles(any())).thenReturn(true);
    
    Optional<InputFile> inputFile = kotlinResourcesLocator.findResourceByClassName("MyClass");
    
    assertEquals(Optional.of(expected), inputFile);
  }

  @Test
  public void findNoResourceByClassName() {
    when(fileSystem.hasFiles(any())).thenReturn(false);

    Optional<InputFile> inputFile = kotlinResourcesLocator.findResourceByClassName("MyClass");

    assertEquals(Optional.empty(), inputFile);
  }
}