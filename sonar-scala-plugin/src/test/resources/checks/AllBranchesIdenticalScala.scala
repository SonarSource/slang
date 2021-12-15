class MyClass {
  def myMethod(x : Any) = {
    val cond: Option[Int] = Some(3)
    val value = 1

    cond match {
      case Some(value) => value
      case _ => value
    }

    cond match { // Noncompliant
      case Some(123) => value
      case _ => value
    }

    x match {
      case value: Int => value
      case value: Long => value
      case _ => value
    }

    value match { // Noncompliant
      case 1 => value
      case 2 => value
      case _ => value
    }

    value match { // Compliant
      case _ => value
    }

    if (value == 1) { // Noncompliant
      value
    } else if (value == 2) {
      value
    } else {
      value
    }
  }
}
