package com.socrata.util.concurrent

trait ExecutorService extends Executor {
  def submit[A](f: => A): Future[A]
  def isShutdown: Boolean
  def isTerminated: Boolean
  def awaitTermination(span: Timespan): Boolean
  def shutdown(): Unit
  def shutdownNow(): Seq[Function0[Unit]]
}
