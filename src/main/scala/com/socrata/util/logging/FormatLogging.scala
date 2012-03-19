package com.socrata.util.logging

import org.slf4j.{Logger, LoggerFactory}
import com.socrata.util.`logging-impl`._
import java.util.IllegalFormatException

/** An SLF4J wrapper which accepts `printf`-style format strings and paramters.
  * All parameters are always evaluated, but the final string is only constructed
  * if the requested log level is enabled.
  *
  * Each logging method also takes an optional exception in a second parameter list.
  *
  * If an `IllegalFormatException` occurs when formatting the message the logger will still
  * log the raw format string and the parameters separately.  Any other exception is allowed
  * to propagate outwards.  If one of the arguments' `toString` methods throws that exception,
  * it will still be retried, so it will throw that exception on the second attempt. */
class FormatLogger(val slf4j: Logger) {
  def this(cls: Class[_]) = this(LoggerFactory.getLogger(cls))

  import LogException.NoThrowable

  def trace(message: String, args: Any*)(implicit exception: LogException): Unit =
    if(slf4j.isTraceEnabled) {
      val text = fmt(message, args)
      exception match {
        case ThrowableWrapper(ex) =>
          slf4j.trace(text, ex)
        case NoThrowable =>
          slf4j.trace(text)
      }
    }

  def debug(message: String, args: Any*)(implicit exception: LogException): Unit =
    if(slf4j.isDebugEnabled) {
      val text = fmt(message, args)
      exception match {
        case ThrowableWrapper(ex) =>
          slf4j.debug(text, ex)
        case NoThrowable =>
          slf4j.debug(text)
      }
    }

  def info(message: String, args: Any*)(implicit exception: LogException): Unit =
    if(slf4j.isInfoEnabled) {
      val text = fmt(message, args)
      exception match {
        case ThrowableWrapper(ex) =>
          slf4j.info(text, ex)
        case NoThrowable =>
          slf4j.info(text)
      }
    }

  def warn(message: String, args: Any*)(implicit exception: LogException): Unit =
    if(slf4j.isWarnEnabled) {
      val text = fmt(message, args)
      exception match {
        case ThrowableWrapper(ex) =>
          slf4j.warn(text, ex)
        case NoThrowable =>
          slf4j.warn(text)
      }
    }

  def error(message: String, args: Any*)(implicit exception: LogException): Unit =
    if(slf4j.isErrorEnabled) {
      val text = fmt(message, args)
      exception match {
        case ThrowableWrapper(ex) =>
          slf4j.error(text, ex)
        case NoThrowable =>
          slf4j.error(text)
      }
    }

  private def fmt(message: String, args: Seq[Any]): String =
    try {
      message.format(args: _*)
    } catch {
      case ex: IllegalFormatException =>
        message + " : " + args.mkString(", ")
    }
}

