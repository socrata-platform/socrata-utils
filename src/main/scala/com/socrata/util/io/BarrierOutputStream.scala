package com.socrata.util.io

import java.io.{FilterOutputStream, OutputStream}

/** An OutputStream filter that prevents `close()` and/or `flush()` calls from reaching
  * the underlying stream. */
class BarrierOutputStream(underlying: OutputStream, preventFlush: Boolean = true, preventClose: Boolean = true) extends FilterOutputStream(underlying) {
  override def close() {
    if(!preventFlush) out.close()
  }
  
  override def flush() {
    if(!preventClose) out.flush()
  }

  // So.  We have a class (FilterOutputStream) whose WHOLE PURPOSE FOR EXISTING
  // is to just pass through to an underlying output stream so you can selectively
  // override methods.  And what does write(byte[], int, int) do?  DELEGATES
  // TO write(int) OF COURSE.  GRRR.
  //
  // write(byte[]) does the wrong thing too, but at least it does a reasonable
  // wrong thing (it calls this method instead of passing straight through to
  // out.write(byte[])
  override def write(bytes: Array[Byte], offset: Int, length: Int) = out.write(bytes, offset, length)
}
