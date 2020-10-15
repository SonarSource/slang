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
      case d: BigDecimal =>
        val decimal = Decimal(d)
        Literal(decimal, DecimalType.fromDecimal(decimal))
      case d: JavaBigDecimal =>
        val decimal = Decimal(d)
        Literal(decimal, DecimalType.fromDecimal(decimal))
      case _ =>
        val a: Int = value + 1 // Noncompliant {{This branch's code block is the same as the block for the branch on line 7.}}
        a
    }

    cond match {
      case None =>
        val a: Int = value + 1
        a
      case Some(3) =>
        val a: Int = value + 1 // Noncompliant
        a
      case Some(value) =>
        val a: Int = value + 1 // Ok
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
        val a: Int = value + 1
        a
      case Some(3) =>
        val a: Int = value + 1
        a
      case Some(value) =>
        val a: Int = value + 1
        a
      case _ =>
        val a: Int = value + 1
        a
    }
  }
}