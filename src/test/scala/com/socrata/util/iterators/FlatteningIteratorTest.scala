package com.socrata.util.iterators

import scala.collection.JavaConverters._

import org.scalatest.FunSuite
import org.scalatest.MustMatchers
import org.scalatest.prop.PropertyChecks

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen

class FlatteningIteratorTest extends FunSuite with PropertyChecks with MustMatchers {
  test("Java flattening iterator produces the same results as Scala flatten") {
    forAll { xs: List[List[Int]] =>
      new FlatteningIterator(xs.map(_.iterator.asJava).iterator.asJava).asScala.toList must equal (xs.flatten)
    }
  }
}
