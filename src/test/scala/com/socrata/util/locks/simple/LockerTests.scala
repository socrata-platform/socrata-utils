package com.socrata.util.locks
package simple

import org.scalatest.FunSuite
import org.scalatest.Assertions

class LockerTests extends FunSuite with Assertions {
  import LockerTestSupport._

  test("simple lock fires \"too many waiters\"") {
    val locker: Locker = new SimpleLocker
    tooManyWaitersTest(locker, locker)
  }

  test("simple lock fires \"wait too long\"") {
    val locker: Locker = new SimpleLocker
    waitTooLongTest(locker, locker)
  }

  test("simple rw lock fires \"too many waiters\"") {
    val locker = new SimpleRWLocker
    rwTooManyWaitersTest(locker)
  }

  test("simple locks can be checked") {
    val locker = new SimpleLocker
    assert(checkLockTrueIfHeldTest(locker, locker) === ((true, true, true)))
  }

  test("simple rw lock fires \"wait too long\"") {
    val locker = new SimpleRWLocker
    rwWaitTooLongTest(locker)
  }

  test("writers have precedence") {
    val locker = new SimpleRWLocker
    writerPrecedenceTest(locker)
  }

  test("simple rw locks can be checked") {
    val locker = new SimpleRWLocker
    rwCheckHeldTest(locker)
  }

  test("simple rw locks can be time-checked") {
    val locker = new SimpleRWLocker
    rwCheckHeldSinceTest(locker)
  }
}
