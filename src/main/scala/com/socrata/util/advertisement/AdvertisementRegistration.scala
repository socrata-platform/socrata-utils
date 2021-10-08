package com.socrata.util.advertisement

trait AdvertisementRegistration {
  /** Remove an advertisement.  This call is idempotent, but may not be valid
   * to call if the advertisement implementation itself is shut down. */
  def stopAdvertising(): Unit
}
