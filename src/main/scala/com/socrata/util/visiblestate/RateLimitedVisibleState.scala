package com.socrata.util.visiblestate

/** A VisibleState that limits the frequency of updates */
class RateLimitedVisibleState(underlying: VisibleState, rateMS: Long) extends VisibleState {
  private var lastSetAt = 0L

  def get() = underlying.get()

  def set(value: => String): Unit = {
    val now = System.currentTimeMillis()
    if(lastSetAt + rateMS < now) {
      underlying.set(value)
      lastSetAt = now
    }
  }

  def clear(): Unit = {
    underlying.clear()
    lastSetAt = 0L
  }
}
