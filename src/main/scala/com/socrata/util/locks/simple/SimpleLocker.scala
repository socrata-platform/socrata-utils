package com.socrata.util.locks
package simple

import scala.{collection => sc}
import sc.{mutable => scm}
import org.joda.time.DateTime

private [simple] class LockStructure {
  var heldBy: Thread = null
  var holdCount: Int = 0
  var heldSince: DateTime = null
  val waiters = new scm.HashSet[Thread]

  def acquiredBy(newOwner: Thread): Unit = {
    // PRECONDITION: either I own the lock's monitor, or I hold the only reference to it.
    heldBy = newOwner
    holdCount += 1
    heldSince = new DateTime
    waiters.remove(newOwner)
  }
}

class SimpleLocker extends Locker {
  private val locks = new scm.HashMap[String, LockStructure]

  // Locking protocol:
  //   * If I hold both this object's monitor and a LockStructure's, I took this's first.
  //   * If I sleep on a lock, I do not hold this object's monitor

  def lock(lockId: String, maxWaitLength: Int, timeout: Long) = doLock(lockId, maxWaitLength, Locker.deadlineForTimeout(timeout))

  def lockHeld(lockId: String): Boolean = {
    validateLockId(lockId)
    synchronized { locks.contains(lockId) }
  }

  def lockHeldSince(lockId: String): Option[DateTime] = {
    validateLockId(lockId)
    synchronized { locks.get(lockId).map(_.heldSince) }
  }

  private def doLock(lockId: String, maxWaitLength: Int, deadline: Long): LockResult = {
    validateLockId(lockId)
    
    val self = Thread.currentThread()

    val lock = synchronized {
      locks.get(lockId) match {
        case None =>
          // it doesn't exist, so I need to create and take it all in one step
          val lock = new LockStructure
          locks(lockId) = lock
          lock.acquiredBy(self)
          return Locked(new SimpleUnlocker(lockId, lock))
        case Some(lock) =>
          lock.synchronized {
            if(!lock.waiters(self)) {
              if(lock.heldBy != self && lock.waiters.size >= maxWaitLength) return TooManyWaiters
              // prevent it from being removed from the hashset if someone else
              // unlocks it right this instant.
              lock.waiters.add(self)
            }
          }

          lock
      }
    }

    lock.synchronized {
      def canOwnLock() = lock.heldBy == null || self == lock.heldBy
      while(!canOwnLock() && (deadline - System.currentTimeMillis > 0))
        lock.wait(math.max(deadline - System.currentTimeMillis, 1L)) // what to do if interrupted...?
      if(!canOwnLock()) {
        lock.waiters -= self
        return TooLongWait
      }
      lock.acquiredBy(self)
    }

    Locked(new SimpleUnlocker(lockId, lock));
  }

  private def unlock(lockId: String, lock: LockStructure): Unit = {
    // I won't be sleeping, but I might be modifying the "locks" map, so I
    // need to take this object's monitor first
    synchronized {
      lock.synchronized {
        lock.holdCount -= 1
        if(lock.holdCount == 0) {
          if(lock.waiters.isEmpty) {
            locks -= lockId
          } else {
            lock.heldBy = null
            lock.notify()
          }
        }
      }
    }
  }

  private class SimpleUnlocker(lockId: String, var lock: LockStructure) extends Unlocker {
    def unlock(): Unit = {
      SimpleLocker.this.unlock(lockId, lock)
      lock = null // make improper (i.e., multiple) use of an Unlocker fail fast
    }
  }
}
