package com.socrata.util.io

import java.nio.ByteBuffer
import java.io.{FileInputStream, File, InputStream}
import java.util.zip.CRC32

// Format:
//  ( FILENAME DATA )* EOF
// FILENAME ::=
//   {4 byte length} {filename} {4-byte checksum}
// DATA ::=
//   ( {4 byte length} {bytes} )* {4 byte -1} {4 byte checksum}
// EOF ::=
//   {4 byte -1}
class MultipleFileInputStream(base: File, files: Iterator[String]) extends InputStream {
  private val currentCheckSum = new CRC32()
  val digestLength = 4
  private val fileIterator = files.buffered
  private var currentFile: InputStream = _
  private var currentBlock = ByteBuffer.allocate(65536)
  private var currentState: State = _

  // kick things off
  moveToNextFile()

  @inline private def trace(s: => String) {
    // println(s)
  }

  trait State {
    def go(buf: Array[Byte], start: Int, len: Int): Int
  }

  abstract class ByteBufferState extends State {
    def go(buf: Array[Byte], start: Int, len: Int) = {
      if (!currentBlock.hasRemaining) {
        gotoNextState()
        trace(getClass + " : moving to state " + currentState.getClass)
        currentState.go(buf, start, len)
      } else {
        val toCopy = math.min(len, currentBlock.remaining)
        trace(getClass + " : writing " + toCopy + " byte(s)")
        currentBlock.get(buf, start, toCopy)
        toCopy
      }
    }

    def gotoNextState()
  }

  class HashState(hash: Int) extends ByteBufferState {
    currentBlock.clear()
    currentBlock.putInt(hash)
    currentBlock.flip()

    def gotoNextState() {
      moveToNextFile()
    }
  }

  class FilenameState(filename: String) extends ByteBufferState {
    val filenameBytes = filename.getBytes("UTF-8")
    val totalSpaceNeeded = filenameBytes.length + 4 + 4
    if (totalSpaceNeeded > currentBlock.capacity) { // this shouldn't happen for any reasonable filenames, but just in case
      currentBlock = ByteBuffer.allocate(totalSpaceNeeded)
    }
    currentBlock.clear()
    currentBlock.putInt(filenameBytes.length)
    currentBlock.put(filenameBytes)
    currentCheckSum.reset()
    currentCheckSum.update(filenameBytes)
    currentBlock.putInt(currentCheckSum.getValue.toInt)
    currentBlock.flip()

    def gotoNextState() {
      currentState = new FileContentsState(filename)
    }
  }

  class FileContentsState(filename: String) extends ByteBufferState {
    currentCheckSum.reset()
    currentBlock.clear().limit(0)
    currentFile = new FileInputStream(new File(base, filename))
    // but do not loadBlock(); this will force an immediate gotoNextState, but
    // if currentFile.read throws for some reason, doing the loadBlock here
    // will leak the file handle.

    def gotoNextState() {
      loadBlock()
      if (!currentBlock.hasRemaining) {
        currentState = new EOFState(currentCheckSum.getValue.toInt)
      }
    }

    def loadBlock() {
      currentBlock.clear().limit(0)
      // What we want is {length} {data}, so we'll read as much as we can
      // into the right place in the array, then go back and put the length
      // in the right place.
      currentFile.read(currentBlock.array, 4, currentBlock.capacity - 4) match {
        case -1 =>
          trace("Read EOF from " + filename)
          currentFile.close()
          currentFile = null
        case n =>
          trace("Read " + n + " bytes from " + filename)
          currentBlock.position(0).limit(4)
          currentBlock.putInt(n)
          currentCheckSum.update(currentBlock.array, 4, n)
          currentBlock.position(0).limit(n + 4)
      }
    }
  }

  class EOFState(digest: Int) extends ByteBufferState {
    currentBlock.clear()
    currentBlock.putInt(-1)
    currentBlock.flip()

    def gotoNextState() {
      currentState = new HashState(digest)
    }
  }

  class NoMoreFilesState extends ByteBufferState {
    currentBlock.clear()
    currentBlock.putInt(-1)
    currentBlock.flip()

    def gotoNextState() {
      currentState = DoneState
    }
  }

  object DoneState extends State {
    def go(buf: Array[Byte], start: Int, len: Int) = -1
  }

  override def close() {
    if (currentFile != null) currentFile.close()
    currentState = DoneState
  }

  def read() = {
    val buf = new Array[Byte](1)
    if (read(buf) == -1) -1
    else buf(0) & 0xff
  }

  private def moveToNextFile() {
    while (fileIterator.hasNext && !new File(base, fileIterator.head).isFile()) fileIterator.next()
    if (fileIterator.hasNext) currentState = new FilenameState(fileIterator.next())
    else currentState = new NoMoreFilesState
  }

  override def read(buf: Array[Byte], start: Int, len: Int): Int = {
    var total = currentState.go(buf, start, len)
    if (total == -1) return -1
    while (total < len) {
      val count = currentState.go(buf, start + total, len - total)
      if (count == -1) return total
      total += count
    }
    total
  }

  def writeTo(file: String) {
    val s = new java.io.FileOutputStream(file)
    try {
      val buf = new Array[Byte](100)
      def loop() {
        read(buf) match {
          case -1 => // done
          case n =>
            s.write(buf, 0, n)
            loop()
        }
      }
      loop()
    } finally {
      s.close()
    }
  }
}
