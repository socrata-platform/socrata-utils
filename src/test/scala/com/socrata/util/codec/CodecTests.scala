package com.socrata.util.codec

import org.scalatest.FunSuite
import org.scalatest.prop.Checkers

import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import org.scalacheck.Prop._

import scala.reflect.ClassTag

// "StringCodec" is also a top-level name in this package, which
// breaks implicit-resolution.
import Codecs.{StringCodec => SC, _}

class CodecTests extends FunSuite with Checkers {
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

  def roundtrip[T](x: T, codec: Codec[T]): T = {
    val baos = new java.io.ByteArrayOutputStream
    val dos = new java.io.DataOutputStream(baos)
    codec.encode(x, dos)
    dos.flush()
    val dis = new java.io.DataInputStream(new java.io.ByteArrayInputStream(baos.toByteArray))
    codec.decode(dis)
  }

  def codecRoundTrips[T: Arbitrary](codec: Codec[T]) = check(forAll { x: T => x == roundtrip(x, codec) })

  def arrayCodecRoundTrips[T: Arbitrary: ClassTag](codec: Codec[Array[T]]) = check(forAll { x: Array[T] =>
    x.toSeq == roundtrip(x, codec).toSeq
  })

  test("BoleanCodec roundtrips") { codecRoundTrips(BooleanCodec) }
  test("ByteCodec roundtrips") { codecRoundTrips(ByteCodec) }
  test("ShortCodec roundtrips") { codecRoundTrips(ShortCodec) }
  test("FixedWidthIntCodec roundtrips") { codecRoundTrips(FixedWidthIntCodec) }
  test("VariableWidthIntCodec roundtrips") { codecRoundTrips(VariableWidthIntCodec) }
  test("FixedWidthLongCodec roundtrips") { codecRoundTrips(FixedWidthLongCodec) }
  test("VariableWidthLongCodec roundtrips") { codecRoundTrips(VariableWidthLongCodec) }
  test("FloatCodec roundtrips") { codecRoundTrips(FloatCodec) }
  test("DoubleCodec roundtrips") { codecRoundTrips(DoubleCodec) }
  test("UnitCodec roundtrips") { codecRoundTrips(UnitCodec) }
  test("StringCodec roundtrips") { codecRoundTrips(SC) }
  test("ByteArrayCodec roundtrips") { arrayCodecRoundTrips(ByteArrayCodec) }
  test("ArrayCodec roundtrips") { arrayCodecRoundTrips(ArrayCodec[String](SC, implicitly)) }
  test("SeqCodec roundtrips") { codecRoundTrips(SeqCodec[String, List](SC, implicitly)) }
  test("MapCodec roundtrips") { codecRoundTrips(MapCodec[String, Long, Map](SC, implicitly, implicitly)) }
  test("OptionCodec roundtrips") { codecRoundTrips(OptionCodec[String](SC)) }
}
