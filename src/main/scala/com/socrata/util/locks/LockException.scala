package com.socrata.util.locks

sealed abstract class LockException(msg: String, cause: Throwable = null) extends Exception(msg, cause)

sealed abstract class RuntimeLockException(msg: String, cause: Throwable = null) extends RuntimeException(msg, cause)

case class LockInvariantFailedException(msg: String) extends RuntimeLockException(msg)
