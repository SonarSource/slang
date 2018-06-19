fun noBody();

fun notEmpty() { bar() }

fun empty() {} // Noncompliant
//          ^^

fun containingOnlyAComment() { /* comment */ }
