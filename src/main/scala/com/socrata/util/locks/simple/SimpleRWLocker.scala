package com.socrata.util
package locks
package simple

import scala.{collection => sc}
import sc.{mutable => scm}
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.{CountDownLatch, TimeUnit}
import org.joda.time.DateTime
import util.Sorting

private [simple] class Ticket(val isRead: Boolean) {
  var acquireCount = 1
  private val latch = new CountDownLatch(1)
  var heldSince = new DateTime

  def release() = latch.countDown()
  def await(timeout: Long) = latch.await(timeout, TimeUnit.MILLISECONDS)
}

private [simple] class RWLockStructure {
  val ticketMap = new scm.LinkedHashMap[Thread, Ticket]
}

class SimpleRWLocker extends RWLocker {
  private val locks = new scm.HashMap[String, RWLockStructure]

  def checkWriteLock(lockId: String): Boolean = {
    validateLockId(lockId)
    synchronized {
      locks.get(lockId) match {
        case None =>
          false
        case Some(lock) =>
          lock.synchronized {
            lock.ticketMap.values.exists(!_.isRead)
          }
      }
    }
  }

  def checkReadOrWriteLock(lockId: String): Boolean = {
    validateLockId(lockId)
    synchronized {
      locks.contains(lockId)
    }
  }

  implicit object DateTimeOrdering extends Ordering[DateTime] {
    def compare(x: DateTime, y: DateTime) = x compareTo y
  }

  def checkWriteLockTime(lockId: String): Option[DateTime] = {
    validateLockId(lockId)
    synchronized {
      locks.get(lockId).flatMap { lock =>
        val lockTimes = lock.ticketMap.values.filter(!_.isRead).map(_.heldSince)
        if(lockTimes.isEmpty) None else Some(lockTimes.min)
      }
    }
  }

  def checkReadOrWriteLockTime(lockId: String): Option[DateTime] = {
    validateLockId(lockId)
    synchronized {
      locks.get(lockId).flatMap { lock =>
        val lockTimes = lock.ticketMap.values.map(_.heldSince)
        if(lockTimes.isEmpty) None else Some(lockTimes.min)
      }
    }
  }

  // This looks wildly inefficient but mirrors the behaviour of the zookeeper locker

  def readLock(lockId: String, maxWaitLength: Int, timeout: Long): LockResult = {
    val deadline = Locker.deadlineForTimeout(timeout)

    validateLockId(lockId)
    
    val self = Thread.currentThread()

    val (lock, ticket) = synchronized {
      locks.get(lockId) match {
        case None =>
          // it doesn't exist, so I need to create and take it all in one step
          val lock = new RWLockStructure
          locks(lockId) = lock
          val ticket = new Ticket(true)
          lock.ticketMap += (self -> ticket)
          return Locked(new SimpleUnlocker(lockId))
        case Some(lock) =>
          lock.synchronized {
            lock.ticketMap.get(self) match {
              case Some(ticket) =>
                if(ticket.isRead) {
                  ticket.acquireCount += 1
                  return Locked(new SimpleUnlocker(lockId))
                }
                throw new IllegalStateException("Tried to take a read-lock while owning the write half")
              case None =>
                if(lock.ticketMap.values.count(!_.isRead) >= maxWaitLength) return TooManyWaiters
                val ticket = new Ticket(true)
                lock.ticketMap += (self -> ticket)
                (lock, ticket)
            }
          }
      }
    }

    while(true) {
      var lastWriterTicket: Ticket = null

      lock.synchronized {
        // I want the last writer ahead of me...
        var foundMe = false
        val it = lock.ticketMap.iterator
        while(!foundMe) {
          val (thread, ticket) = it.next()
          if(thread == self) foundMe = true
          else if(!ticket.isRead) lastWriterTicket = ticket
        }
      }

      if(lastWriterTicket == null) return Locked(new SimpleUnlocker(lockId))
      else if(!lastWriterTicket.await(math.max(deadline - System.currentTimeMillis, 1))) {
        lock.synchronized {
          ticket.release()
          lock.ticketMap -= self
        }
        return TooLongWait
      }
      // otherwise, the last writer ticket was released.  This doesn't necessarily mean we're now at the head of the
      // line (it could have expired leaving another writer ahead of it) so go around and try again.
    }
    
    error("Can't get here")
  }

  def writeLock(lockId: String, maxWaitLength: Int, timeout: Long): LockResult = {
    val deadline = Locker.deadlineForTimeout(timeout)

    validateLockId(lockId)

    val self = Thread.currentThread()
    
    val (lock, ticket) = synchronized {
      locks.get(lockId) match {
        case None =>
          // it doesn't exist, so I need to create and take it all in one step
          val lock = new RWLockStructure
          locks(lockId) = lock
          val ticket = new Ticket(false)
          lock.ticketMap += (self -> ticket)
          return Locked(new SimpleUnlocker(lockId))
        case Some(lock) =>
          lock.synchronized {
            lock.ticketMap.get(self) match {
              case Some(ticket) =>
                if(!ticket.isRead) {
                  ticket.acquireCount += 1
                  return Locked(new SimpleUnlocker(lockId))
                }
                throw new IllegalStateException("Tried to take a write-lock while owning the read half")
              case None =>
                if(lock.ticketMap.size >= maxWaitLength) return TooManyWaiters
                val ticket = new Ticket(false)
                lock.ticketMap += (self -> ticket)
                (lock, ticket)
            }
          }
      }
    }

    while(true) {
      var lastTicket: Ticket = null

      lock.synchronized {
        // I want the last ticket ahead of me...
        var foundMe = false
        val it = lock.ticketMap.iterator
        while(!foundMe) {
          val (thread, ticket) = it.next()
          if(thread == self) foundMe = true
          else lastTicket = ticket
        }
      }

      if(lastTicket == null) return Locked(new SimpleUnlocker(lockId))
      else if(!lastTicket.await(math.max(deadline - System.currentTimeMillis, 1))) {
        lock.synchronized {
          ticket.release()
          lock.ticketMap -= self
        }
        return TooLongWait
      }
      // otherwise, the last ticket was released.  This doesn't necessarily mean we're now at the head of the
      // line so go around and try again.
    }

    error("Can't get here")
  }

  private def unlock(lockId: String) {
    // I won't be sleeping, but I might be modifying the "locks" map, so I
    // need to take this object's monitor first
    val self = Thread.currentThread
    synchronized {
      val lock = locks(lockId)
      lock.synchronized {
        val ticket = lock.ticketMap(self)
        if(ticket.acquireCount == 1) {
          lock.ticketMap(self).release()
          lock.ticketMap -= self
          if(lock.ticketMap.isEmpty) locks -= lockId
        } else {
          ticket.acquireCount -= 1
        }
      }
    }
  }

  private class SimpleUnlocker(var lockId: String) extends Unlocker {
    def unlock() {
      SimpleRWLocker.this.unlock(lockId)
      lockId = null // make improper use of this unlocker fail fast
    }
  }
}
