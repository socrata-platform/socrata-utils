package com.socrata.util.locks

import com.socrata.util.logging.LazyStringLogger
import java.util.concurrent.{Semaphore, CountDownLatch}
import scala.collection.{mutable => scm}
import org.joda.time.DateTime

private[locks] object LockerTestSupport {
  val log = new LazyStringLogger(getClass)

  def thread[U](name: String)(f: => U) = {
    val t = new Thread() {
      setName(name)
      override def run() {
        f
      }
    }
    t.start()
    t
  }

  def rwTooManyWaitersTest(locker: RWLocker) {
    tooManyWaitersTest(locker.readLocker, locker.writeLocker)
    tooManyWaitersTest(locker.writeLocker, locker.writeLocker)
    tooManyWaitersTest(locker.writeLocker, locker.readLocker)
  }

  def rwWaitTooLongTest(locker: RWLocker) {
    waitTooLongTest(locker.readLocker, locker.writeLocker)
    waitTooLongTest(locker.writeLocker, locker.writeLocker)
    waitTooLongTest(locker.writeLocker, locker.readLocker)
  }

  def rwCheckHeldTest(locker: RWLocker) {
    assert(checkLockTrueIfHeldTest(locker.readLocker, locker.writeLocker) == ((true, true, true)))
    assert(checkLockTrueIfHeldTest(locker.readLocker, locker.readLocker) == ((true, false, true)))
    assert(checkLockTrueIfHeldTest(locker.writeLocker, locker.writeLocker) == ((true, true, true)))
    assert(checkLockTrueIfHeldTest(locker.writeLocker, locker.readLocker) == ((true, true, true)))
  }

  def rwCheckHeldSinceTest(locker: RWLocker) {
    val lockName = "checkLockTime"
    val startTime = new DateTime

    Thread.sleep(10)

    locker.writeLocker.lock(lockName, Int.MaxValue, Long.MaxValue)

    Thread.sleep(10)

    val writeTime = locker.writeLocker.lockHeldSince(lockName).get

    assert(writeTime.compareTo(startTime) > 0)
  }

  def tooManyWaitersTest(aLocker: Locker, bLocker: Locker) {
    val locked = new Semaphore(0)
    val done = new CountDownLatch(1)

    val lockName = "toomanywaiters"

    val a = thread("a") {
      log.info("Locking")
      val Locked(u) = aLocker.lock(lockName, Int.MaxValue, Long.MaxValue)
      log.info("Locked; firing second thread")
      locked.release()
      done.await()
      log.info("Unlocking")
      u.unlock()
    }

    locked.acquire()

    val b = thread("b") {
      log.info("attempting")
      assert(bLocker.lock(lockName, 0, Long.MaxValue) == TooManyWaiters)
      log.info("done")
      done.countDown()
    }

    done.await()
    log.info("exiting")

    a.join()
    b.join()
  }

  def checkLockTrueIfHeldTest(aLocker: Locker, checkLocker: Locker) = {
    val lockName = "checkLock"
    val locked = new Semaphore(0)
    val unlocked = new Semaphore(0)
    val done = new CountDownLatch(1)
    val resultA = !checkLocker.lockHeld(lockName)
    val a = thread("a") {
      val Locked(u) = aLocker.lock(lockName, Int.MaxValue, Long.MaxValue)
      locked.release()
      done.await()
      u.unlock()
      unlocked.release()
    }
    locked.acquire()
    val resultB = checkLocker.lockHeld(lockName)
    done.countDown()
    unlocked.acquire()
    val resultC = !checkLocker.lockHeld(lockName)
    a.join()
    (resultA, resultB, resultC)
  }

  def waitTooLongTest(aLocker: Locker, bLocker: Locker) {
    val locked = new Semaphore(0)
    val done = new CountDownLatch(1)

    val lockName = "waittoolong"

    val a = thread("a") {
      log.info("Locking")
      val Locked(u) = aLocker.lock(lockName, Int.MaxValue, Long.MaxValue)
      log.info("Locked; firing second thread")
      locked.release()
      done.await()
      log.info("Unlocking")
      u.unlock()
    }

    locked.acquire()

    val b = thread("b") {
      log.info("attempting")
      assert(bLocker.lock(lockName, Int.MaxValue, 1000) == TooLongWait)
      log.info("done")
      done.countDown()
    }

    done.await()
    log.info("exiting")

    a.join()
    b.join()
  }

  def writerPrecedenceTest(locker: RWLocker) {
    val lockName = "writerprecedence"

    var record = new scm.Queue[String]

    // add reader, reader, writer, reader -- the last reader should not take the lock
    // until the writer has exited.  And the writer shouldn't exit until after both
    // the first two readers have.
    
    def lock(name: String, locker: Locker, releaseSignal: Semaphore, awaitLocked: Boolean = false) = {
      val doneLocking = new Semaphore(0)

      val t = thread(name) {
        val Locked(u) = locker.lock(lockName, Int.MaxValue, Long.MaxValue)
        record += name + " locked"
        doneLocking.release()
        releaseSignal.acquire()
        record += name + " unlocking"
        u.unlock()
      }

      if(awaitLocked) doneLocking.acquire()

      t
    }

    val aRelease = new Semaphore(0)
    val bRelease = new Semaphore(0)
    val cRelease = new Semaphore(0)
    val dRelease = new Semaphore(0)

    val a = lock("a", locker.readLocker, aRelease, awaitLocked = true)
    val b = lock("b", locker.readLocker, bRelease, awaitLocked = true)
    val c = lock("c", locker.writeLocker, cRelease)
    Thread.sleep(1000) // give "c" a full second to try to acquire the lock
    val d = lock("d", locker.readLocker, dRelease)
    Thread.sleep(1000) // give "d" a full second to try to acquire the lock

    cRelease.release()
    Thread.sleep(100)
    bRelease.release() // let second reader go first
    Thread.sleep(100)
    aRelease.release()
    Thread.sleep(100)
    dRelease.release()

    a.join()
    b.join()
    c.join()
    d.join()

    println(record.toList)
    assert(record.toList == List("a locked", "b locked", "b unlocking", "a unlocking", "c locked", "c unlocking", "d locked", "d unlocking"))
  }
}
