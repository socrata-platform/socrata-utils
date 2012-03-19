package com.socrata.util.io

import java.net.URL
import java.io.{FileOutputStream, File}

object TemporaryFile {
  def withTemporaryFile[T](dir: File, prefix: String = "", suffix: String = ".tmp")(f: File => T): T = {
    val tmpFile = File.createTempFile(prefix, suffix, dir)
    try {
      f(tmpFile)
    } finally {
      tmpFile.delete()
    }
  }

  def streamURL(file: File, source: URL) {
    val inStream = source.openStream()
    try {
      val outStream = new FileOutputStream(file)
      try {
        val buf = new Array[Byte](10240)
        def loop() {
          inStream.read(buf) match {
            case -1 =>
              // done
            case n =>
              outStream.write(buf, 0, n)
              loop()
          }
        }
        loop()
      } finally {
        outStream.close()
      }
    } finally {
      inStream.close()
    }
  }
}
