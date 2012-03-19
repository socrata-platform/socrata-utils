package com.socrata.util.`concurrent-impl`

import com.socrata.util.concurrent.Timespan
import java.util.concurrent.TimeUnit._

class Timespanable(x: Long) {
  def nanoseconds = Timespan(x, NANOSECONDS)
  def microseconds = Timespan(x, MICROSECONDS)
  def milliseconds = Timespan(x, MILLISECONDS)
  def seconds = Timespan(x, SECONDS)
  def second = Timespan(x, SECONDS)
  def minutes = Timespan(x, MINUTES)
  def minute = Timespan(x, MINUTES)
  def hours = Timespan(x, HOURS)
  def hour = Timespan(x, HOURS)
  def days = Timespan(x, DAYS)
  def day = Timespan(x, DAYS)
}
