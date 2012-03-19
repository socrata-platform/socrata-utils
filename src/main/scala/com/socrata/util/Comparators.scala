package com.socrata.util

import java.util.Comparator
import java.io.Serializable

object Comparators {
  def comparableComparator[T <: Comparable[T]] = ComparableComparator.asInstanceOf[Comparator[T]]

  object ComparableComparator extends Comparator[Comparable[AnyRef]] with Serializable {
    def compare(a: Comparable[AnyRef], b: Comparable[AnyRef]) = a.compareTo(b)
  }
}
