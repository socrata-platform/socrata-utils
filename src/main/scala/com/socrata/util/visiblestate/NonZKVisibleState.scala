package com.socrata.util.visiblestate

import NonZKVisibleState._

class NonZKVisibleState(name: String) extends VisibleState {
  def get() = Option(table.get(name))

  def set(value: =>String): Unit = {
    table.put(name, value)
  }

  def clear(): Unit = {
    table.remove(name)
  }
}

object NonZKVisibleState {
  val table = new java.util.concurrent.ConcurrentHashMap[String, String]
}
