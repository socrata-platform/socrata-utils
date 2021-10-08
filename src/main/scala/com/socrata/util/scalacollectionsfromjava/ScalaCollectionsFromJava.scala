package com.socrata.util.scalacollectionsfromjava

import scala.jdk.CollectionConverters._
import scala.{collection => sc}

object ScalaCollectionsFromJava {
  def scalaSeqAsJavaList[A](a: sc.Seq[A]):java.util.List[A] = a.asJava
}
