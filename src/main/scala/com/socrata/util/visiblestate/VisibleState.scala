package com.socrata.util.visiblestate

trait VisibleState {
  def get(): Option[String]

  def getOrElse(value: => String) = get().getOrElse(value)

  def set(value: => String): Unit
  def clear(): Unit
}
