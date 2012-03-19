package com.socrata.util.concurrent

sealed abstract class FutureException(msg: String = null, cause: Throwable = null) extends RuntimeException(msg, cause)

case class ExecutionException(msg: String, cause: Throwable) extends FutureException(msg, cause)
case class CancellationException(msg: String) extends FutureException(msg)
