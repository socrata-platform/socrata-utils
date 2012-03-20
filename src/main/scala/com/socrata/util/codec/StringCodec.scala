package com.socrata.util
package codec

import java.io.{Reader, Writer}

trait StringCodec[T] {
  def encode(x: T, out: Writer)
  def decode(in: Reader): T
}

object StringCodec {
  import java.io.{StringReader, StringWriter}

  def asString[T](x: T)(implicit codec: StringCodec[T]): String =
    if(codec eq StringCodecs.StringStringCodec) {
      x.asInstanceOf[String]
    } else {
      val sw = new StringWriter
      codec.encode(x, sw)
      sw.toString
    }

  def fromString[T](str: String)(implicit codec: StringCodec[T]): T =
    if(codec eq StringCodecs.StringStringCodec) {
      str.asInstanceOf[T]
    } else {
      val sr = new StringReader(str)
      codec.decode(sr)
    }
}

object StringCodecs {
  implicit object StringStringCodec extends StringCodec[String] {
    def encode(x: String, out: Writer) {
      out.write(x)
    }

    def decode(in: Reader): String = {
      val result = new StringBuilder
      val buf = new Array[Char](4096)
      while(true) {
        val count = in.read(buf)
        if(count == -1) return result.toString
        result.appendAll(buf, 0, count)
      }
      error("Can't get here")
    }
  }
}
