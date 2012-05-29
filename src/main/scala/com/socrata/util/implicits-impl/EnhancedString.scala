package com.socrata.util.`implicits-impl`

import com.socrata.util.hashing.MurmurHash3

class EnhancedString(s: String) {
  def murmurHash(implicit hasher: MurmurHash3) = hasher(s)
}
