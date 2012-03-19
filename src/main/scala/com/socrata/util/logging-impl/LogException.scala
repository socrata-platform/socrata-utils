package com.socrata.util.`logging-impl`

sealed abstract class LogException

object LogException {
  // If there's an exception provided, this makes it available to
  // Loggers' logging methods' second argument lists.  Otherwise, the
  // NoThrowable object is passed to indicate "no causing exception"
  implicit def wrappedThrowable(t: Throwable): LogException = ThrowableWrapper(t)
  implicit case object NoThrowable extends LogException
}

final case class ThrowableWrapper(throwable: Throwable) extends LogException
