package com.socrata.util.io

import java.io._
import java.util.zip.CRC32
import java.nio.charset.StandardCharsets
import com.rojoma.simplearm.v2._

object MultipleFileInputStreamConsumer extends ((File, InputStream) => Unit) {
  @throws(classOf[IOException])
  def apply(targetDir: File, input: InputStream) = new MultipleFileInputStreamConsumer().consume(targetDir, input)
}

class MultipleFileInputStreamConsumer {
  private val checksum = new CRC32()
  private val buf = new Array[Byte](10240)

  @throws(classOf[IOException])
  def consume(targetDir: File, input: InputStream): Unit = {
    val in = new DataInputStream(input)
    while (true) {
      readFilename(in) match {
        case Some(filename) =>
          readFile(new File(targetDir, filename), in)
        case None =>
          return
      }
    }
  }

  def readFile(filename: File, in: DataInput): Unit = {
    Option(filename.getParentFile).foreach(_.mkdirs())
    using(new FileOutputStream(filename)) { out =>
      checksum.reset()
      var len = 0
      do {
        len = in.readInt()
        if (len != -1) copy(in, out, len)
      } while (len != -1)
      if (checksum.getValue.toInt != in.readInt()) throw new IOException("Bad hash on file contents")
    }
  }

  private def copy(in: DataInput, out: OutputStream, count: Int): Unit = {
    var remaining = count
    while (remaining != 0) {
      val toRead = math.min(remaining, buf.length)
      in.readFully(buf, 0, toRead)
      out.write(buf, 0, toRead)
      checksum.update(buf, 0, toRead)
      remaining -= toRead
    }
  }

  def readFilename(input: DataInput) = {
    input.readInt() match {
      case -1 =>
        None
      case n =>
        val buf = new Array[Byte](n)
        input.readFully(buf)
        checksum.reset()
        checksum.update(buf, 0, n)
        if (checksum.getValue.toInt != input.readInt()) throw new IOException("Bad hash on filename")
        Some(new String(buf, 0, n, StandardCharsets.UTF_8))
    }
  }

  def hashMatch(hash: Array[Byte], buf: Array[Byte], start: Int): Boolean = {
    var i = hash.length
    while (i != 0) {
      i -= 1
      if (hash(i) != buf(start + i)) return false
    }
    true
  }
}
