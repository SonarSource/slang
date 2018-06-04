package com.sonarsource.slang.checks;

import com.sonarsource.slang.api.TextPointer;
import com.sonarsource.slang.api.TextRange;
import com.sonarsource.slang.api.TopLevelTree;
import com.sonarsource.slang.checks.api.InitContext;
import com.sonarsource.slang.checks.api.SlangCheck;
import com.sonarsource.slang.impl.TextPointerImpl;
import com.sonarsource.slang.impl.TextRangeImpl;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonarsource.analyzer.commons.TokenLocation;

public class TodoCommentCheck implements SlangCheck {

  private final Pattern todoPattern = Pattern.compile("(?i)(^|[^\\p{L}])(todo)");

  @Override
  public void initialize(InitContext init) {
    init.register(TopLevelTree.class, (ctx, tree) ->
      tree.allComments().forEach(comment -> {
        Matcher matcher = todoPattern.matcher(comment.textWithDelimiters());
        if (matcher.find()) {
          TextPointer start = comment.textRange().start();
          TokenLocation location = new TokenLocation(start.line(), start.lineOffset(), comment.textWithDelimiters().substring(0, matcher.start(2)));
          TextRange todoRange = new TextRangeImpl(
            new TextPointerImpl(location.endLine(), location.endLineOffset()),
            new TextPointerImpl(location.endLine(), location.endLineOffset() + 4)
          );
          ctx.reportIssue(todoRange, "Complete the task associated to this TODO comment.");
        }
      })
    );
  }

}
