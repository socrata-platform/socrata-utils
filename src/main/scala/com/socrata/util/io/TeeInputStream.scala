package com.socrata.util.io

import java.io._


/**
 * Write the contents of `source` to `target` as it is read.
 * This does not take ownership of `target`; it is up to the calling
 * code to make sure it gets closed appropriately.
 */
class TeeInputStream(source: InputStream, target: OutputStream) extends FilterInputStream(source) {
  override def read(): Int = {
    val b = in.read()
    if(b != -1) target.write(b)
    b
  }

  override def read(b: Array[Byte]): Int = read(b, 0, b.length)

  override def read(b: Array[Byte], offset: Int, len: Int): Int = {
    val count = in.read(b, offset, len)
    if(count != -1) target.write(b, offset, count)
    count
  }

  override def skip(n: Long): Long = {
    val buf = new Array[Byte](10240)
    var remaining = n
    while(remaining > 0) {
      val count = read(buf, 0, math.min(buf.length, remaining).toInt)
      if(count == -1) return n - remaining
      remaining -= count
    }
    n
  }

  override def markSupported = false
  override def mark(n: Int): Unit = {}
  override def reset(): Unit = {
    throw new IOException("mark/reset not supported")
  }

  @throws(classOf[IOException])
  def finish(): Unit = {
    skip(Long.MaxValue)
    target.flush()
  }
}
