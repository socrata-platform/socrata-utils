package com.socrata.util

class UnsafeMatch[T: ClassManifest] {
  def unapply(x: Any): Option[T] = {
    if(classManifest[T].erasure.isInstance(x.asInstanceOf[AnyRef])) Some(x.asInstanceOf[T])
    else None
  }
}
