package com.socrata.util.scalacollectionsfromjava


import scala.collection.JavaConverters._
object ScalaCollectionsFromJava {
  def scalaSeqAsJavaList[A](a:Seq[A]):java.util.List[A] = a.asJava
}
