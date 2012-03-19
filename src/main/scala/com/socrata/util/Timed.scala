package com.socrata.util

object Timed {
  def apply[T](block: =>T): (T, Long) = {
    val start = System.currentTimeMillis
    val result = block

    (result, System.currentTimeMillis - start)
  }
}