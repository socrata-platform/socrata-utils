package com.socrata.util.scalacollectionsfromjava


import org.scalatest.WordSpec
import org.scalatest.Matchers

class ScalaCollectionsFromJavaSpec extends WordSpec with Matchers {
  "A ScalaToJavaConverter" should {
    val b = new java.util.ArrayList[Int](3)
    b.add(1)
    b.add(2)
    b.add(3)
    "convert a scala list to a java list" in {
      val a = List(1,2,3)
      ScalaCollectionsFromJava.scalaSeqAsJavaList(a) should be (b)
    }
    "convert a scala array list to a java list" in {
      ScalaCollectionsFromJava.scalaSeqAsJavaList(scala.collection.mutable.ArrayBuffer(1,2,3)) should be (b)
    }
  }
}
