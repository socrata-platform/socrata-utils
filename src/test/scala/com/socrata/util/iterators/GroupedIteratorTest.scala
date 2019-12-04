package com.socrata.util.iterators

import scala.collection.JavaConverters._

import org.scalatest.FunSuite
import org.scalatest.MustMatchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen

class GroupedIteratorTest extends FunSuite with ScalaCheckPropertyChecks with MustMatchers {
  test("Java grouped iterator produces the same results as Scala grouped iterator") {
    val listAndBound = for {
      xs <- arbitrary[List[Int]]
      s <- if(xs.isEmpty) Gen.choose(1, 3)
           else Gen.choose(1, xs.length * 3)
    } yield (xs, s)

    forAll(listAndBound) { case (s, groupSize) =>
      whenever(groupSize > 0 && groupSize < s.length * 3) {
        new GroupedIterator(s.iterator.asJava, groupSize).asScala.map(_.asScala).toList must equal (s.iterator.grouped(groupSize).toList)
      }
    }
  }
}
