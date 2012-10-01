package com.socrata.util.advertisement
package standalone

import com.socrata.util.concurrent.Executor

class StandaloneAdvertisements(executor: Executor) extends Advertiser with WatchAdvertisements {
  private var advertisements = Map.empty[String, Set[Registration]]
  private var onChange: WatchAdvertisements => Unit = null
  private var stopped = false

  def advertise(name: String): AdvertisementRegistration = {
    val (callback, result) = synchronized {
      val set = advertisements.getOrElse(name, Set.empty)

      val reg = new Registration(name)
      advertisements += name -> (set + reg)
      val cb = if(set.isEmpty) onChange else null
      (cb, reg)
    }
    if(callback != null) executor.execute { callback(this) }
    result
  }

  private def unregister(registration: Registration) {
    val callback = synchronized {
      advertisements.get(registration.name) match {
        case Some(set) =>
          val newSet = set - registration
          if(newSet.isEmpty) {
            advertisements -= registration.name
            onChange
          } else {
            advertisements += registration.name -> newSet
            null // already registered; no change
          }
        case None =>
          // ..ok, already unregistered
          null
      }
    }
    if(callback != null) executor.execute { callback(this) }
  }

  def start(changeCallback: WatchAdvertisements => Unit) {
    val doCallback = synchronized {
      if(stopped) throw new IllegalStateException("Cannot be restarted")
      if(onChange != null) throw new IllegalStateException("Already started")
      onChange = changeCallback
      advertisements.nonEmpty
    }

    if(doCallback) executor.execute { onChange(this) }
  }

  def current = synchronized { advertisements.keySet.toSet } // .toSet only necesary for 2.8

  def stop() = synchronized {
    onChange = null
    stopped = true
  }

  // Note: NOT A CASE CLASS.  We need identity-based hashcode and equals!
  private class Registration(val name: String) extends AdvertisementRegistration {
    def stopAdvertising() { unregister(this) }
  }
}
