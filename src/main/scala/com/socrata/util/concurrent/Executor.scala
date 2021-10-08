package com.socrata.util.concurrent

trait Executor {
  def execute[U](f: =>U): Unit
}
