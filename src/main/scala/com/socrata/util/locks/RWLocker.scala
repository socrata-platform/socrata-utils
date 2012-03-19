package com.socrata.util.locks

import org.joda.time.DateTime

trait RWLocker {
  val readLocker: Locker = new Locker {
    def lock(id: String, maxWaitLength: Int, timeout: Long) = readLock(id, maxWaitLength, timeout)
    def lockHeld(id: String) = checkWriteLock(id)
    def lockHeldSince(id: String) = checkWriteLockTime(id)
  }
  val writeLocker: Locker = new Locker {
    def lock(id: String, maxWaitLength: Int, timeout: Long) = writeLock(id, maxWaitLength, timeout)
    def lockHeld(id: String) = checkReadOrWriteLock(id)
    def lockHeldSince(id: String) = checkReadOrWriteLockTime(id)
  }

  def readLock(id: String, maxWaitLength: Int, timeout: Long): LockResult
  def writeLock(id: String, maxWaitLength: Int, timeout: Long): LockResult

  def checkWriteLock(id: String): Boolean
  def checkReadOrWriteLock(id: String): Boolean

  def checkWriteLockTime(id: String): Option[DateTime]
  def checkReadOrWriteLockTime(id: String): Option[DateTime]

  def validateLockId(id: String) {
    if(id.isEmpty || id.contains('/'))
      throw new IllegalArgumentException("A lock ID must be non-empty and must not contain a slash")
  }
}

class JRWLocker(val asScala: RWLocker) {
  def readLock(lockId: String, maxWaiters: Int, timeout: Long) = toJava(asScala.readLock(lockId, maxWaiters, timeout))
  def writeLock(lockId: String, maxWaiters: Int, timeout: Long) = toJava(asScala.writeLock(lockId, maxWaiters, timeout))

  private def toJava(lockResult: LockResult) = lockResult match {
    case Locked(unlocker) => unlocker
    case _ => null
  }
}
