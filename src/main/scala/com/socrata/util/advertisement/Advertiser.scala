package com.socrata.util.advertisement

trait Advertiser {
  /** Create an advertisement that will persist until it is explicitly de-advertised,
   * the JVM halts, or an implementation-specific shutdown procedure is invoked. */
  def advertise(name: String): AdvertisementRegistration
}
