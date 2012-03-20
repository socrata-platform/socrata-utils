package com.socrata

package object util {
  @inline def error(message: String) = com.socrata.`util-impl`.ErrorImpl.error(message)
}
