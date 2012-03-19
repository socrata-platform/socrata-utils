package com.socrata.util.`implicits-impl`

import java.io.File

class EnhancedFile(file: File) {
  def /(subdir: String) = new File(file, subdir)
}
