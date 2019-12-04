package com.socrata.util
package codec

import scala.language.higherKinds
import scala.{collection => sc}
import scala.reflect.ClassTag

import java.io.{DataInput, DataOutput, IOException}
import scala.collection.mutable.ArrayBuilder

trait Codec[T] extends java.io.Serializable {
  @throws(classOf[IOException])
  def encode(x: T, out: DataOutput)

  @throws(classOf[IOException])
  def decode(in: DataInput): T
}

object Codec {
  import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
  import java.io.{DataInputStream, DataOutputStream}

  def toByteArray[T](x: T)(implicit codec: Codec[T]): Array[Byte] = {
    if(codec eq Codecs.ByteArrayCodec) {
      x.asInstanceOf[Array[Byte]]
    } else {
      val baos = new ByteArrayOutputStream
      val dos = new DataOutputStream(baos)
      codec.encode(x, dos)
      dos.flush()
      baos.toByteArray
    }
  }

  def fromByteArray[T](bytes: Array[Byte])(implicit codec: Codec[T]): T = {
    if(codec eq Codecs.ByteArrayCodec) {
      bytes.asInstanceOf[T]
    } else {
      val bais = new ByteArrayInputStream(bytes)
      val dis = new DataInputStream(bais)
      fromDataInput(dis)
    }
  }
  
  def fromDataInput[T](dataInput: DataInput)(implicit codec: Codec[T]): T =
    codec.decode(dataInput)

  def toDataOutput[T](dataOutput: DataOutput, x: T)(implicit codec: Codec[T]) =
    codec.encode(x, dataOutput)
}

object CodecsUtils { // want people to do "import Codecs._" without polluting their namespace with non-codecs
  type CB[A, B] = sc.generic.CanBuild[A, B]

  abstract class SingletonCodec[T] extends Codec[T] {
    @throws(classOf[java.io.ObjectStreamException])
    protected def readResolve(): AnyRef =
      getClass.getField("MODULE$").get(null)
  }

  val utf8 = java.nio.charset.Charset.forName("UTF-8")

  def writeSize(size: Int, o: DataOutput) = Codecs.VariableWidthIntCodec.encode(size, o)
  def readSize(i: DataInput) = Codecs.VariableWidthIntCodec.decode(i)

  def times[U](n: Int)(f: Int => U) {
    var i = 0
    while(i != n) {
      f(i)
      i += 1
    }
  }
}

object Codecs {
  import CodecsUtils._
  
  implicit object BooleanCodec extends SingletonCodec[Boolean] {
    def encode(b: Boolean, o: DataOutput) = o.writeBoolean(b)
    def decode(i: DataInput) = i.readBoolean()
  }

  implicit object ByteCodec extends SingletonCodec[Byte] {
    def encode(b: Byte, o: DataOutput) = o.writeByte(b)
    def decode(i: DataInput) = i.readByte()
  }

  implicit object ShortCodec extends SingletonCodec[Short] {
    def encode(s: Short, o: DataOutput) = o.writeShort(s)
    def decode(i: DataInput) = i.readShort()
  }

  object FixedWidthIntCodec extends SingletonCodec[Int] {
    def encode(i: Int, o: DataOutput) = o.writeInt(i)
    def decode(i: DataInput) = i.readInt()
  }

  implicit object VariableWidthIntCodec extends SingletonCodec[Int] { // from protocol buffers
    def encode(i: Int, o: DataOutput) {
      var value = zigzag(i)

      while ((value & ~0x7FL) != 0) {
        o.writeByte((value & 0x7F) | 0x80);
        value >>>= 7
      }
      o.writeByte(value)
    }

    def decode(i: DataInput): Int = {
      var shift = 0
      var result = 0
      while (shift < 32) {
        val b = i.readByte()
        result |= (b & 0x7F) << shift;
        if ((b & 0x80) == 0) {
          return unzigzag(result)
        }
        shift += 7
      }
      throw new IOException("Didn't find the terminator byte for a variable-length int") // FIXME: improve this
    }

    def zigzag(n: Int) = (n << 1) ^ (n >> 31)
    def unzigzag(n: Int) =(n >>> 1) ^ -(n & 1)
  }

  object FixedWidthLongCodec extends SingletonCodec[Long] {
    def encode(l: Long, o: DataOutput) = o.writeLong(l)
    def decode(i: DataInput) = i.readLong()
  }

  implicit object VariableWidthLongCodec extends SingletonCodec[Long] { // from protocol buffers
    def encode(l: Long, o: DataOutput) {
      var value = zigzag(l)

      while ((value & ~0x7FL) != 0) {
        o.writeByte((value.toInt & 0x7F) | 0x80);
        value >>>= 7
      }
      o.writeByte(value.toInt)
    }

    def decode(i: DataInput): Long = {
      var shift = 0
      var result = 0L
      while (shift < 64) {
        val b = i.readByte()
        result |= (b & 0x7F).toLong << shift;
        if ((b & 0x80) == 0) {
          return unzigzag(result)
        }
        shift += 7
      }
      throw new IOException("Didn't find the terminator byte for a variable-length long") // FIXME: improve this
    }

    def zigzag(n: Long) = (n << 1) ^ (n >> 63)
    def unzigzag(n: Long) = (n >>> 1) ^ -(n & 1)
  }

  implicit object FloatCodec extends SingletonCodec[Float] {
    def encode(f: Float, o: DataOutput) = o.writeFloat(f)
    def decode(i: DataInput) = i.readFloat()
  }

  implicit object DoubleCodec extends SingletonCodec[Double] {
    def encode(d: Double, o: DataOutput) = o.writeDouble(d)
    def decode(i: DataInput) = i.readDouble()
  }

  implicit object UnitCodec extends SingletonCodec[Unit] {
    def encode(u: Unit, o: DataOutput) = {}
    def decode(i: DataInput) = {}
  }

  object StringCodecOld extends SingletonCodec[String] {
    // FIXME: this format isn't suitable for general strings (in
    // particular, it can only represent strings <64k chars long)
    def encode(s: String, o: DataOutput) = o.writeUTF(s)
    def decode(i: DataInput) = i.readUTF()
  }

  implicit object StringCodec extends SingletonCodec[String] {
    // ..on the other hand, this codec requires well-formedness (i.e.,
    // correct surrogate pairs) in the strings it deals with.  It's a
    // bad idea to put random junk in strings, but should a codec be
    // the thing to decide it?  My current thinking is that code that
    // deals with Strings should be allowed to assume any surrogate
    // chars that appear are correct and have undefined behaviour if
    // they're not.  And this won't actually *break*, it'll just
    // replace the bad characters with the charset's default
    // replacement character (probably "?")
    def encode(s: String, o: DataOutput) = ByteArrayCodec.encode(s.getBytes(utf8), o)
    def decode(i: DataInput) = new String(ByteArrayCodec.decode(i), utf8)
  }

  // This is the only one it's really super-necessary that it be a
  // SingletonCodec, as {to,from}ByteArray tests for its identity.
  implicit object ByteArrayCodec extends SingletonCodec[Array[Byte]] {
    def encode(bs: Array[Byte], o: DataOutput) = {
      writeSize(bs.length, o)
      o.write(bs)
    }

    def decode(i: DataInput): Array[Byte] = {
      val bs = new Array[Byte](readSize(i))
      i.readFully(bs)
      bs
    }
  }

  implicit def ArrayCodec[T : Codec : ClassTag] = new Codec[Array[T]] {
    def encode(xs: Array[T], o: DataOutput) = {
      writeSize(xs.length, o)
      for(e <- xs) implicitly[Codec[T]].encode(e, o)
    }

    def decode(i: DataInput): Array[T] = {
      val count = readSize(i)
      val xs = ArrayBuilder.make[T]()
      xs.sizeHint(count)
      times(count) { _ => xs += implicitly[Codec[T]].decode(i) }
      xs.result
    }
  }

  implicit def SeqCodec[T, S[X] <: Seq[X]](implicit tCodec: Codec[T], buildFactory: CB[T, S[T]]) = new Codec[S[T]] {
    def encode(s: S[T], o: DataOutput) = {
      writeSize(s.size, o)
      for(e <- s) tCodec.encode(e, o)
    }

    def decode(i: DataInput): S[T] = {
      val b = buildFactory()
      val count = readSize(i)
      b.sizeHint(count)
      times(count) { _ => b += tCodec.decode(i)}
      b.result()
    }
  }
  
  implicit def MapCodec[K, V, M[A, B] <: sc.Map[A, B]](implicit kCodec: Codec[K], vCodec: Codec[V], buildFactory: CB[(K,V), M[K, V]]) = new Codec[M[K, V]] {
    def encode(x: M[K, V], o: DataOutput) = {
      writeSize(x.size, o)
      for((k, v) <- x) {
        kCodec.encode(k, o)
        vCodec.encode(v, o)
      }
    }
    
    def decode(i: DataInput): M[K, V] = {
      val b = buildFactory()
      val count = readSize(i)
      times(count) { _ =>
        val k = kCodec.decode(i)
        val v = vCodec.decode(i)
        b += (k -> v)
      }
      b.result()
    }
  }
  
  implicit def OptionCodec[T : Codec] = new Codec[Option[T]] {
    def encode(x: Option[T], o: DataOutput) = x match {
      case None => o.writeBoolean(false)
      case Some(v) => o.writeBoolean(true); implicitly[Codec[T]].encode(v, o)
    }
    
    def decode(i: DataInput) =
      if(i.readBoolean()) Some(implicitly[Codec[T]].decode(i))
      else None
  }
  
  implicit def Tuple2Codec[A: Codec, B: Codec] = new Codec[(A, B)] {
    def encode(x: (A,B), o: DataOutput) {
      implicitly[Codec[A]].encode(x._1, o)
      implicitly[Codec[B]].encode(x._2, o)
    }
    
    def decode(i: DataInput) = {
      val a = implicitly[Codec[A]].decode(i)
      val b = implicitly[Codec[B]].decode(i)
      (a, b)
    }
  }
}
