package com.socrata.util.concurrent

trait Future[+A] extends Function0[A] with Function1[Timespan, Option[A]] {
  def cancel(mayInterruptIfRunning: Boolean): Boolean

  def apply(): A
  def apply(timespan: Timespan): Option[A]

  def isCancelled: Boolean
  def isDone: Boolean
}

class SimpleFuture[A](result: A) extends Future[A] with java.util.concurrent.Future[A] {
  def cancel(mayInterruptIfRunning: Boolean) = false
  def apply() = result
  def apply(timespan: Timespan): Option[A] = Some(result)
  def isCancelled = false
  def isDone = true

  // Java version...
  def get(interval: Long, timeunit: java.util.concurrent.TimeUnit) = result
  def get() = result
}
