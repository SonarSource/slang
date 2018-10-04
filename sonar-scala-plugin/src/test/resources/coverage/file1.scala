package com.sksamuel.scoverage.samples

object ListTestObject {
  def listM(l : List[Int]):Unit = {
    l match {
        case x :: y :: z :: xs => print("Should not be called !")
        case x :: xs => print(x)
        case Nil => print("End of list !")
    }
  }
}