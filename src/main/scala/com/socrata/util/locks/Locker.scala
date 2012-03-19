package com.socrata.util.locks

import org.joda.time.DateTime

sealed abstract class LockResult
case object TooManyWaiters extends LockResult
case object TooLongWait extends LockResult
case class Locked(unlocker: Unlocker) extends LockResult

trait TimeoutOnlyLocker {
  def lockWithTimeout(lockId: String, timeout: Long): Option[Unlocker]
  def lockHeld(lockId: String): Boolean
  def lockHeldSince(lockId: String): Option[DateTime]
}

trait Locker extends TimeoutOnlyLocker {
  def lock(lockId: String, maxWaitLength: Int, timeout: Long): LockResult

  def lockWithTimeout(lockId: String, timeout: Long) = lock(lockId, Int.MaxValue, timeout) match {
    case Locked(u) => Some(u)
    case _ => None
  }

  def validateLockId(id: String) {
    if(id.isEmpty || id.contains('/'))
      throw new IllegalArgumentException("A lock ID must be non-empty and must not contain a slash")
  }
}

object Locker {
  def deadlineForTimeout(timeout: Long) = {
    val realTimeout = math.max(timeout, 0L)
    val deadline = System.currentTimeMillis + realTimeout
    if(deadline < 0) Long.MaxValue // it wrapped around, just set it to the End of Time
    else deadline
  }
}

class JLocker(val asScala: Locker) {
  def lock(lockId: String, maxWaiters: Int, timeout: Long) =
    asScala.lock(lockId, maxWaiters, timeout) match {
      case Locked(unlocker) => unlocker
      case _ => null
    }

  def lockHeldSince(lockId: String): DateTime = asScala.lockHeldSince(lockId).orNull
}
