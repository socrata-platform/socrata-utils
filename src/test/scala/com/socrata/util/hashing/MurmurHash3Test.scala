package com.socrata.util.hashing

import org.scalatest.FunSuite
import org.scalatest.MustMatchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.nio.{ByteBuffer, ByteOrder}
import java.nio.charset.StandardCharsets

class MurmurHash3Test extends FunSuite with ScalaCheckPropertyChecks with MustMatchers {
  test("MurmurHash3 passes the standard verification test") {
    // adapted from SMHasher's test of MurmurHash_x36_32
    val expected = 0xB0F57EE3
    val count = 256
    val key = new Array[Byte](count)
    val hashes = ByteBuffer.allocate(count * 4 /* four bytes per int */).order(ByteOrder.LITTLE_ENDIAN)
    for(i <- 0 until count) {
      key(i) = i.toByte
      val hash = new MurmurHash3(count - i)
      hashes.putInt(hash(key, 0, i))
    }
    hashes.flip()
    val actual = new MurmurHash3(0)(hashes)
    actual must equal (expected)
  }

  test("Hashing strings is equivalent to hashing their big-endian UTF-16 representations") {
    // ArbitraryValidString is from rojoma-json.  We need it because
    // in this test we're converting a string to an array of bytes
    // using a codec.  This only preserves complete information if the
    // string is well-formed.  ScalaCheck 1.9's String instance will
    // select only non-surrogate characters but ScalaCheck 1.8's will
    // choose any random Char at all, which will break the test.  So
    // if we'll be providing an Arbitrary instance, we might as well
    // provide one that is willing to cover the full range of Unicode.
    import org.scalacheck.{Arbitrary, Gen}
    implicit val ArbitraryValidString = Arbitrary[String] {
      val lowSurrogate = Gen.choose(Character.MIN_LOW_SURROGATE, Character.MAX_LOW_SURROGATE).map(_.toChar)

      val notLowSurrogate = Gen.frequency(
        (Character.MIN_LOW_SURROGATE - Char.MinValue, Gen.choose(Char.MinValue, Character.MIN_LOW_SURROGATE - 1)),
        (Char.MaxValue - Character.MAX_LOW_SURROGATE, Gen.choose(Character.MAX_LOW_SURROGATE + 1, Char.MaxValue))
      ).map(_.toChar)

      val validCodePoint = notLowSurrogate flatMap { a =>
        if(a.isHighSurrogate) lowSurrogate map { b => new String(Array(a, b)) }
        else a.toString
      }

      Gen.containerOf[List, String](validCodePoint) map (_.mkString)
    }

    forAll { (seed: Int, text: String) =>
      val hash = new MurmurHash3(seed)
      hash(text) must equal (hash(text.getBytes(StandardCharsets.UTF_16BE)))
    }
  }
}
