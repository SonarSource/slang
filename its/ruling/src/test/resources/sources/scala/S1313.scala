// copyright
object Main {
  private def f(ip: String = "192.168.12.42"): String = {
    ip
  }
  private def g(implicit id : Int, @transient ip: String = "192.168.12.42"): String = {
    if (id == 0) "" else ip
  }
}
