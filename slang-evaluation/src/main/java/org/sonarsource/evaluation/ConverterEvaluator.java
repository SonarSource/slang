package org.sonarsource.evaluation;

import org.sonarsource.slang.api.ASTConverter;
import org.sonarsource.slang.api.NativeTree;
import org.sonarsource.slang.api.Tree;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

class ConverterEvaluator {
  private ASTConverter converter;
  private String sourceTestFilesFolder;
  private String sourceExtension;

  private double totalPercentage = 0;
  private int nFiles = 0;

  ConverterEvaluator(ASTConverter converter, String sourceTestFilesFolder, String sourceExtension) {
    this.converter = converter;
    this.sourceTestFilesFolder = sourceTestFilesFolder;
    this.sourceExtension = sourceExtension;
  }

  public void evaluate() throws IOException {
    String fileName = "slang-evaluation\\src\\main\\output\\" + sourceExtension + "_evaluation";
    try(BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
      try (Stream<Path> str = Files.walk(Paths.get(sourceTestFilesFolder))) {
        str.filter(p -> p.toString().endsWith(sourceExtension))
            .forEach(p -> {
              try {
                convertCode(p, writer);
              } catch (IOException e) {
                e.printStackTrace();
              }
            });
      } catch (IOException e) {
        e.printStackTrace();
      }
      writer.write("Total native percentage: " + totalPercentage/nFiles + "%");
    }
  }

  private void convertCode(Path p, BufferedWriter writer) throws IOException {
    byte[] encoded = Files.readAllBytes(p);
    String code = new String(encoded, Charset.defaultCharset());
    double percentage =  getNativeNodePercentage(code);
    writer.write(p.toString() + ": " + percentage + "%\n");
  }

  private Double getNativeNodePercentage(String code) {
    Tree convertedTree = converter.parse(code);
    long totalNodes = convertedTree.descendants().count();

    long nativeNodes = convertedTree.descendants()
        .filter(t -> t instanceof NativeTree).count();

    nFiles ++;

    if(totalNodes == 0){
      return 0.0;
    } else {
      double percentage = nativeNodes/(double)totalNodes;
      totalPercentage += percentage;
      return percentage;
    }
  }
}
