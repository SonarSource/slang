package com.sonarsource.slang.checks;

import com.sonarsource.slang.api.FunctionDeclarationTree;
import com.sonarsource.slang.checks.api.InitContext;
import com.sonarsource.slang.checks.api.SecondaryLocation;
import com.sonarsource.slang.checks.api.SlangCheck;
import java.util.List;
import java.util.stream.Collectors;

public class TooManyParametersCheck implements SlangCheck {

  private int threshold = 7;

  @Override
  public void initialize(InitContext init) {
    init.register(FunctionDeclarationTree.class, (ctx, tree) -> {
      if (tree.formalParameters().size() > threshold) {
        String message = String.format(
          "This function has %s parameters, which is greater than the %s authorized.",
          tree.formalParameters().size(),
          threshold);
        List<SecondaryLocation> secondaryLocations = tree.formalParameters().stream()
          .skip(threshold)
          .map(SecondaryLocation::new)
          .collect(Collectors.toList());
        ctx.reportIssue(tree.name(), message, secondaryLocations);
      }
    });
  }

}
