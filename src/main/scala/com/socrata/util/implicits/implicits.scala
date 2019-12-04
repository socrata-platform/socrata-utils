package com.socrata.util

import scala.language.implicitConversions
import `implicits-impl`._
import java.io.{FileFilter, File, FilenameFilter}

package object implicits {
  implicit def enhancefile(f: java.io.File) = new EnhancedFile(f)
  implicit def enhancestring(s: String) = new EnhancedString(s)
  implicit def comparatorToOrdering[T](c: java.util.Comparator[T]): Ordering[T] = new ComparatorOrdering(c)
  implicit def comparableToOrdering[T <: Comparable[T]]: Ordering[T] = ComparableOrderingImpl.asInstanceOf[Ordering[T]]
  implicit def f2filefilter(f: File => Boolean) = new FileFilter {
    def accept(pathname: File) = f(pathname)
  }
  implicit def f2filenamefilter(f: (File, String) => Boolean) = new FilenameFilter {
    def accept(dir: File, name: String) = f(dir, name)
  }
  implicit val basicMurmurHash = new com.socrata.util.hashing.MurmurHash3(0)
}
