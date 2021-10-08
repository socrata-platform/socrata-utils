package com.socrata.util.advertisement

trait WatchAdvertisements {
  /** Begin watching advertisements.  After this, `current` will return the
   * current set of advertisements until `stop` is called.
   *
   * @throws IllegalStateException if called more than once, or after `stop` */
  def start(onChange: WatchAdvertisements => Unit = WatchAdvertisements.noop): Unit

  /** The current set of advertisements.  Returns the empty set if
   * called before `start` or after `stop`. */
  def current: Set[String]

  /** Stops this watcher */
  def stop(): Unit
}

object WatchAdvertisements {
  private def noop(wa: WatchAdvertisements): Unit = {}
}
