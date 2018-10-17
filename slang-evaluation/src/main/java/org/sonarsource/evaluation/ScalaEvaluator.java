package org.sonarsource.evaluation;

import org.sonarsource.scala.converter.ScalaConverter;

import java.io.IOException;


public class ScalaEvaluator {

  public static void main(String[] args) {
    String path = "its\\sources\\scala";
    ConverterEvaluator evaluator = new ConverterEvaluator(new ScalaConverter(), path, ".scala");
    try {
      evaluator.evaluate();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
