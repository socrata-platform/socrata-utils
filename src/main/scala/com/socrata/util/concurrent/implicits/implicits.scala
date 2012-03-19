package com.socrata.util.concurrent

import com.socrata.util.`concurrent-impl`._

package object implicits {
  implicit def java2scalaExecutor(j: java.util.concurrent.Executor) = new JavaExecutorWrapper(j)
  implicit def java2scalaExecutorService(j: java.util.concurrent.ExecutorService) = new JavaExecutorServiceWrapper(j)
  implicit def java2scalaFuture[T](j: java.util.concurrent.Future[T]) = new JavaFutureWrapper(j)

  implicit def scala2javaExecutor(s: Executor) = new ScalaExecutorWrapper(s)
  implicit def scala2javaExecutorService(s: ExecutorService) = new ScalaExecutorServiceWrapper(s)
  implicit def scala2javaFuture[T](s: Future[T]) = new ScalaFutureWrapper(s)

  implicit def long2timespanable(x: Long) = new Timespanable(x)
  implicit def int2timespanable(x: Int) = new Timespanable(x)
}
