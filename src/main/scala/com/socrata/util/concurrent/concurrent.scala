package com.socrata.util

package object concurrent {
  def thread[U](start: Boolean = true, name: String = null, daemon: Boolean = false)(f: => U): Thread = {
    val thread = new Thread() {
      override def run(): Unit = {
        f
      }
    }
    if(name != null) thread.setName(name)
    thread.setDaemon(daemon)
    if(start) thread.start()
    thread
  }
}
