object A {

  var name = "a"

  def doSomething() = {
    var name = "b"
    name = name
    this.name = name
    println(name)
  }
}
