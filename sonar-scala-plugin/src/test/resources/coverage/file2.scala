package com.sksamuel.scoverage.samples

object IfTestObject {
  def method(): Boolean = {
    val a = true || false
    if(a) {
      print("a")
    } else {
      print("b")
    }
    a
  }
}