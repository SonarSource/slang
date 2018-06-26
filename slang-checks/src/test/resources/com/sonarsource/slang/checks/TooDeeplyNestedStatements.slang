if (false) { // Compliant
};

if (false) { // Compliant
};

if (false) { // Compliant
};

if (false) { // Compliant
};

if (true) {}
else if (true) {}
else if (true) {
  if (true) { // Compliant
    if (true) {
       if (true) { // Noncompliant {{Refactor this code to not nest more than 3 control flow statements.}}
       };
    };
  };
};

if (false) { // Compliant
  if (true) { // Compliant
  } else {
    if (false) { // Compliant
      if (true) { // Noncompliant {{Refactor this code to not nest more than 3 control flow statements.}}
        if (false) { // Compliant
        };
      } else if (true) { // Compliant
      } else {
        if (false) { // Compliant
        };
      };
    };
  };
};

if (false) {        // Compliant
} else if (false) { // Compliant
} else if (false) { // Compliant
} else if (false) { // Compliant
} else if (false) { // Compliant
};

if (false) // Compliant
  if (false) // Compliant
    if (false) // Compliant
      if (true) // Noncompliant
        println();

for (int var x = list) {      // Compliant
  for (int var o = objects) { // Compliant
    while (false) {           // Compliant

       for (int var p = list) { // Noncompliant
       };

       while (false){ // Noncompliant
       };

       do { } while (false); // Noncompliant

       if (false) { // Noncompliant
       };

       match (p) { // Noncompliant
       };

       try {  // Noncompliant

       } catch () { };

    };
  };
};


while (true) {
 if (true) {
 } else if (true) {
 } else {
   if (true) {
     if (true) { // Noncompliant
     };
   };
 };
};