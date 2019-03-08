// copyright
object Main {
  def m(): Unit = {
    param.sortWith(_._1 < _._1) // Compliant
    param.sortWith(_._2._1 < _._2._1) // Compliant
    param.sortWith(_._2._1 < a._2._1) // Compliant
    param.sortWith(a._2._1 < a._2._1) // Noncompliant
    v filter (_.size == _.size) // Compliant
    (_ || _) // Compliant
    (a || a) // Noncompliant

    case class Person(name: String, age: Int)
    val personList = List(Person("Santi", 25), Person("Robert", 12))
    val f: Person => Int = x => x.age
    personList.sortWith(f(_) < f(_)) // Compliant
  }
}
