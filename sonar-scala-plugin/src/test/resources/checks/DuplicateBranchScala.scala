class MyClass {
  def myMethod(cond: Any) = {
    val value = 1

    cond match {
      case Some(33) => // is a pattern match, but no variable inside
        val a: Int = value + 1
        a
      case None =>
        val a: Int = value + 1 // Noncompliant {{This branch's code block is the same as the block for the branch on line 7.}}
        a
      case d: String =>
        val x = "" + d
        1
      case d: Boolean => // ok, we see a type pattern and ignore
        val x = "" + d
        1
      case _ =>
        val a: Int = value + 1 // Noncompliant {{This branch's code block is the same as the block for the branch on line 7.}}
        a
    }

    cond match {
      case None =>
        val a: String = "" + value + 1
        a
      case Some(3) =>
        val a: String = "" + value + 1 // Noncompliant
        a
      case Some(value) =>
        val a: String = "" + value + 1 // Ok
        a
      case _ =>
        1
    }

    value match {
      case 1 =>
        val b: Int = value + 1
        b
      case 2 =>
        val b: Int = value + 1000
        b
      case 3 =>
        val b: Int = value + 1 // Noncompliant {{This branch's code block is the same as the block for the branch on line 39.}}
        b
      case _ =>
        val b: Int = value + 1 // Noncompliant {{This branch's code block is the same as the block for the branch on line 39.}}
        b
    }

    if (value == 1) {
      val c: Int = value + 1
      c
    } else if (value == 2) {
      val c: Int = value + 1000
      c
    } else { // Noncompliant {{This branch's code block is the same as the block for the branch on line 52.}}
      val c: Int = value + 1
      c
    }
  }

  def allBranchesIdentical(cond: Any, value: Int) = {
    // S3923
    cond match {
      case None =>
        val a: String = "" + value + 1
        a
      case _ =>
        val a: String = "" + value + 1
        a
    }
  }

  def firstCaseIsPatternMatch(cond: Any, value: Int) : String = {
    cond match {
      case value: Boolean =>
        val a: String = "" + value + 1 // Ok
        a
      case None =>
        val a: String = "" + value + 1 // this is the original block
        a
      case 3 =>
        val a: String = "" + value + 1 // Noncompliant {{This branch's code block is the same as the block for the branch on line 82.}}
        a
      case _ =>
        ""
    }
  }

  def lastCaseIsPatternMatch(cond: Any, value: Int) : String = {
    cond match {
      case None =>
        val a: String = "" + value + 1
        a
      case Some(3) =>
        val a: String = "" + value + 1 // Noncompliant
        a
      case 2 => // this is just to not have all branches identical (S3923)
        "3"
      case Some(value) =>
        val a: String = "" + value + 1 // Ok
        a
    }
  }

  def patternMatchingShadowed(cond: Any, value : Any) : String = {
    cond match {
      case Some(value) =>
        val a: String = "" + value + 1
        a
      case value: Int =>
        val a: String = "" + value + 1 // Compliant, value is declared only in the case scope
        a
      case value: Float =>
        val a: String = "" + value + 1 // Compliant, value is declared only in the case scope
        a
      case 42 =>
        "case with a different content to trigger the rule S1871 and not S3923"
      case _ =>
        val a: String = "" + value + 1
        a
    }
  }

  def complexPatternMatching(cond: Any, x : Int) : String = {
    cond match {
      case 4 =>
        val a: String = "" + x + 1
        a
      case (Some(x: Int), y) => // Compliant, shadowing the 'x' variable
        val a: String = "" + x + 1
        a
      case (1 | 2) :: x :: tail => // Compliant, shadowing the 'x' variable
        val a: String = "" + x + 1
        a
      case x: Long if x == 0 => // Compliant, shadowing the 'x' variable
        val a: String = "" + x + 1
        a
      case _ =>
        "1"
    }
  }

  def variableNotUsedInTheCaseBody(cond: Any, x : Int) : String = {
    cond match {
      case x: Long if x == 0 => //  shadowing the 'x' variable but not used in the case body
        val a: String = ""
        a
      case x: String if x.isEmpty => //  shadowing the 'x' variable but not used in the case body
        val a: String = "" // Noncompliant
        a
      case _ =>
        "1"
    }
  }

  def currentRuleLimitation(cond: Any) : String = {
    cond match {
      case x: Long if x == 0 =>
        val a: String = "" + x
        a
      case x: Long if x == 5 => // false-negative, the two 'x' declaration have the same 'Long' type and could be merged
        val a: String = "" + x
        a
      case _ =>
        "1"
    }
  }

}
