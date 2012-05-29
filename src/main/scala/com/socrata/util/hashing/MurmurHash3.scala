package com.socrata.util.hashing

import java.nio.{ByteBuffer, ByteOrder}

class MurmurHash3(seed: Int) {
  def forString: (String => Int) = this(_: String)
  def forBytes: (Array[Byte] => Int) = this(_: Array[Byte])
  def forBytesSlice: ((Array[Byte], Int, Int) => Int) = this(_: Array[Byte], _, _)
  def forByteBuffer: (ByteBuffer => Int) = this(_: ByteBuffer)

  private final val c1 = 0xcc9e2d51
  private final val c2 = 0x1b873593

  private def fmix(h: Int): Int = {
    var h1 = h
    h1 ^= h1 >>> 16
    h1 *= 0x85ebca6b
    h1 ^= h1 >>> 13
    h1 *= 0xc2b2ae35
    h1 ^= h1 >>> 16
    h1
  }

  private def feed(hash: Int, block: Int): Int = {
    var k1 = block
    k1 *= c1
    k1 = java.lang.Integer.rotateLeft(k1, 15)
    k1 *= c2

    var h1 = hash ^ k1
    h1 = java.lang.Integer.rotateLeft(h1, 13)
    h1 * 5 + 0xe6546b64
  }

  private def finish(hash: Int, block: Int): Int = {
    var k1 = block
    k1 *= c1
    k1 = java.lang.Integer.rotateLeft(k1, 15)
    k1 *= c2
    hash ^ k1
  }

  def apply(datum: String): Int = {
    val len = datum.length
    val nblocks = len & ~1

    var h1 = seed

    var i = 0
    while(i != nblocks) {
      h1 = feed(h1, Integer.reverseBytes((datum.charAt(i).toInt << 16) + datum.charAt(i + 1)))
      i += 2
    }

    if(len != nblocks) {
      val c = Character.reverseBytes(datum.charAt(len - 1))
      h1 = finish(h1, c)
    }

    h1 ^= len * 2 // each char is 2 byte

    h1 = fmix(h1)

    h1
  }

  def apply(in: Array[Byte]): Int = this(ByteBuffer.wrap(in))
  def apply(in: Array[Byte], offset: Int, len: Int): Int = this(ByteBuffer.wrap(in, offset, len))

  /** Hash the available contents of the given ByteBuffer.  Duplicates
   * (and hence does not modify) the given buffer. */
  def apply(buffer: ByteBuffer): Int = {
    val in = buffer.duplicate.order(ByteOrder.LITTLE_ENDIAN) // LE because we're emulating MurmurHash_x86_32
    val len = in.remaining
    val nblocks = len / 4;

    var h1 = seed

    var i = 0
    while(i != nblocks) {
      h1 = feed(h1, in.getInt())
      i += 1
    }

    var remaining = len & 3
    if(remaining != 0) {
      var k1 = 0
      var offset = 0
      while(remaining != 0) {
        k1 |= (in.get() & 0xff) << offset
        offset += 8
        remaining -= 1
      }
      h1 = finish(h1, k1)
    }

    h1 ^= len

    h1 = fmix(h1)

    h1
  }
}
