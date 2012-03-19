package com.socrata.util.`implicits-impl`

import java.util.Comparator

class ComparatorOrdering[T](comparator: Comparator[T]) extends Ordering[T] {
  def compare(x: T, y: T) = comparator.compare(x, y)
}

class ComparableOrdering[T <: Comparable[T]] extends Ordering[T] {
  def compare(x: T, y: T) = x.compareTo(y)
}

object ComparableOrderingImpl extends ComparableOrdering[Nothing] // Evil type erasure exploit hack
