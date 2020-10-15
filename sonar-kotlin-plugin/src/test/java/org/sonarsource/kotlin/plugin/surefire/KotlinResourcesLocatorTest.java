package org.sonarsource.kotlin.plugin.surefire;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFilePredicates;
import org.sonar.api.batch.fs.internal.DefaultIndexedFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KotlinResourcesLocatorTest {

  private FileSystem fileSystem = mock(FileSystem.class);
  private KotlinResourcesLocator kotlinResourcesLocator = new KotlinResourcesLocator(fileSystem);
  private InputFile expected = new DefaultInputFile(new DefaultIndexedFile("", new File("/").toPath(), "",""), (x) -> {});
  
  @Before
  public void setUp() {
    when(fileSystem.predicates()).thenReturn(new DefaultFilePredicates(new File("/").toPath()));
    when(fileSystem.inputFiles(any())).thenReturn(Collections.singletonList(expected));
  }
  
  @Test
  public void findResourceByClassName() {
    when(fileSystem.hasFiles(any())).thenReturn(true);
    
    InputFile inputFile = kotlinResourcesLocator.findResourceByClassName("MyClass");
    
    assertEquals(expected, inputFile);
  }

  @Test
  public void findNoResourceByClassName() {
    when(fileSystem.hasFiles(any())).thenReturn(false);

    InputFile inputFile = kotlinResourcesLocator.findResourceByClassName("MyClass");

    assertNull(inputFile);
  }
}