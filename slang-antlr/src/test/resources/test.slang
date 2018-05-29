public void foo(int a){
 x = a + 1;
 a = 2;

 // if as expressions
 b = if (x == a) 3 else (5 * 5);

 // if as statement
 if (x > 0) {
    if (x > 10)
      return 0;
 } else if (x > 1) {
     return 1;
 } else {
     return 2;
 }

 // pattern matching
 match (x) {
   1 -> {
    print(b);
    return b;
   }
   else -> return 0;
}

}