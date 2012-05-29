package com.socrata.util.hashing

import org.scalatest.FunSuite
import org.scalatest.matchers.MustMatchers
import org.scalatest.prop.PropertyChecks

import java.nio.{ByteBuffer, ByteOrder}

class MurmurHash3Test extends FunSuite with PropertyChecks with MustMatchers {
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
    val hash = new MurmurHash3(1)
    forAll { (text: String) =>
      hash(text) must equal (hash(text.getBytes("UTF-16BE")))
    }
  }
}
