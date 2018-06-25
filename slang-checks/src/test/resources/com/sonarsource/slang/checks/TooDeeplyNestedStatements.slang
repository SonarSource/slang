if (x > 0) {
  if (x > 1) {
    if (x > 2) {
      if (x > 3) {
        if (x > 4) { // Noncompliant {{Refactor this code to not nest more than 4 control flow statements.}}
        };
      };
    };
  };
};

if (x > 0) {
  if (x > 1) {
    if (x > 2) {
      if (x > 3) {
        try { // Noncompliant {{Refactor this code to not nest more than 4 control flow statements.}}
        };
      };
    };
  };
};
