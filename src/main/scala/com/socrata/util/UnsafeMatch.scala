package com.socrata.util

import scala.reflect.ClassTag

class UnsafeMatch[T: ClassTag] {
  def unapply(x: Any): Option[T] = x match {
    case t: T => Some(t)
    case _ => None
  }
}
