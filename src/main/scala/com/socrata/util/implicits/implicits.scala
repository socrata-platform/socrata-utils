package com.socrata.util

import `implicits-impl`._

package object implicits {
  implicit def enhancefile(f: java.io.File) = new EnhancedFile(f)
  implicit def comparatorToOrdering[T](c: java.util.Comparator[T]): Ordering[T] = new ComparatorOrdering(c)
  implicit def comparableToOrdering[T <: Comparable[T]]: Ordering[T] = ComparableOrderingImpl.asInstanceOf[Ordering[T]]
}
