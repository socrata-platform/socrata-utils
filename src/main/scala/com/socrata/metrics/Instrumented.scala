package com.socrata.metrics

import com.yammer.metrics.Metrics
import com.yammer.metrics.core.Timer
import java.util.concurrent.TimeUnit

trait Instrumented {
  val metrics = new MetricsProvider(getClass)
}

class MetricsProvider(cls: Class[_]) {
  def timer(name: String, rateUnit: TimeUnit = TimeUnit.SECONDS, durationUnit: TimeUnit = TimeUnit.MILLISECONDS) =
    new WrappedTimer(Metrics.newTimer(cls, name))
  def meter(name: String, eventType: String, rateUnit: TimeUnit = TimeUnit.SECONDS) =
    Metrics.newMeter(cls, name, eventType, rateUnit)
}

class WrappedTimer(timer: Timer) {
  def time[T](f: => T): T = {
    val c = timer.time()
    try {
      f
    } finally {
      c.stop()
    }
  }
}
