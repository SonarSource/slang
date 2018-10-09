// copyright
object Code {
  def method(param: Int): Int = {
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
        7
      case 3 =>
        13
      case 1 => // Noncompliant
        19
      case _ =>
        23
    }
  }
}
