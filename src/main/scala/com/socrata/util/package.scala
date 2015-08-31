package com.socrata

package object util {
  @inline def error(message: String) = sys.error(message)
}
