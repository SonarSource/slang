// copyright
object Code {
  def method(param: Int): Unit = {
    if (param == 1) {
      openWindow
    } else if (param == 2) {
      closeWindow
    } else if (param == 1) { // Noncompliant
      moveWindowToTheBackground
    } else {
      moveWindow
    }
    param match {
      case 1 =>
      // ...
      case 3 =>
      // ...
      case 1 => // Noncompliant
      // ...
      case _ =>
      //...
    }
  }
}
