package com.socrata.util

import scala.reflect.ClassTag

class UnsafeMatch[T: ClassTag] {
  def unapply(x: Any): Option[T] = {
    if(implicitly[ClassTag[T]].runtimeClass.isInstance(x.asInstanceOf[AnyRef])) Some(x.asInstanceOf[T])
    else None
  }
}
