package com.socrata.util.logging

import org.slf4j.{Logger, LoggerFactory}

import scala.reflect.ClassTag

/** An SLF4J wrapper which uses a lazily-evaluated string for message contents.
  * The string is evaluated only if the requested log level is enabled.
  * 
  * All methods can also take a second exception parameter which is evaluated
  * normally.
  *
  * If an exception occurs while evaluating the string, the exception is allowed
  * to propagate outward. */
class LazyStringLogger(val slf4j: Logger) {
  def this(cls: Class[_]) = this(LoggerFactory.getLogger(cls))
  def this(name: String) = this(LoggerFactory.getLogger(name))
  
  def trace(message: => String): Unit =
    if(slf4j.isTraceEnabled) {
      slf4j.trace(fmt(message))
    }

  def trace(message: => String, exception : Throwable): Unit =
    if(slf4j.isTraceEnabled) {
      slf4j.trace(fmt(message), exception)
    }

  def debug(message: => String): Unit =
    if(slf4j.isDebugEnabled) {
      slf4j.debug(fmt(message))
    }

  def debug(message: => String, exception : Throwable): Unit =
    if(slf4j.isDebugEnabled) {
      slf4j.debug(fmt(message), exception)
    }

  def info(message: => String): Unit =
    if(slf4j.isInfoEnabled) {
      slf4j.info(fmt(message))
    }

  def info(message: => String, exception : Throwable): Unit =
    if(slf4j.isInfoEnabled) {
      slf4j.info(fmt(message), exception)
    }

  def warn(message: => String): Unit =
    if(slf4j.isWarnEnabled) {
      slf4j.warn(fmt(message))
    }

  def warn(message: => String, exception : Throwable): Unit =
    if(slf4j.isWarnEnabled) {
      slf4j.warn(fmt(message), exception)
    }

  def error(message: => String): Unit =
    if(slf4j.isErrorEnabled) {
      slf4j.error(fmt(message))
    }

  def error(message: => String, exception : Throwable): Unit =
    if(slf4j.isErrorEnabled) {
      slf4j.error(fmt(message), exception)
    }

  private def fmt(message: => String): String =
    try {
      message
    } catch {
      case e: Exception =>
        slf4j.warn("Exception while formatting log message", e)
        throw e
    }
}

object LazyStringLogger {
  def apply(cls: Class[_]): LazyStringLogger = new LazyStringLogger(cls)
  def apply(name: String): LazyStringLogger = new LazyStringLogger(name)
  def apply(logger: Logger): LazyStringLogger = new LazyStringLogger(logger)
}
