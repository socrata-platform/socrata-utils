package com.socrata.util.`concurrent-impl`

import com.socrata.util.concurrent._

import scala.jdk.CollectionConverters._
import java.util.{concurrent => juc}
import annotation.unchecked.uncheckedVariance
import com.socrata.util.UnsafeMatch
import scala.collection.compat._

trait WrappedJavaExecutor extends Executor {
  def asJava: juc.Executor
  
  def execute[U](f: =>U): Unit = {
    asJava.execute(new Runnable {
      override def run(): Unit = {
        f
      }
    })
  }
}

trait WrappedScalaExecutor extends juc.Executor {
  def asScala: Executor

  def execute(f: Runnable): Unit = {
    asScala.execute {
      f.run()
    }
  }
}

class JavaExecutorWrapper(j: juc.Executor) {
  def asScala: Executor = j match {
    case unwrapped: WrappedScalaExecutor => unwrapped.asScala
    case _ => new WrappedJavaExecutor { def asJava = j }
  }
}

class ScalaExecutorWrapper(s: Executor) {
  def asJava: juc.Executor = s match {
    case wrapped: WrappedJavaExecutor => wrapped.asJava
    case _ => new WrappedScalaExecutor { def asScala = s }
  }
}

class WrappedNonRuntimeException(e: Throwable) extends RuntimeException(e)

trait WrappedJavaExecutorService extends WrappedJavaExecutor with ExecutorService {
  def asJava: juc.ExecutorService

  def submit[A](f: => A): Future[A] = {
    val future = asJava.submit(new juc.Callable[A] {
      def call() = try {
        f
      } catch {
        case e: RuntimeException => throw e
        case e: Throwable => throw new WrappedNonRuntimeException(e)
      }
    })

    new WrappedJavaFuture[A] { val asJava = future }
  }

  def isShutdown = asJava.isShutdown
  def isTerminated = asJava.isTerminated

  def awaitTermination(span: Timespan) = asJava.awaitTermination(span.count, span.unit)

  def shutdown() = asJava.shutdown()

  def shutdownNow() = asJava.shutdownNow().asScala.map(r => () => r.run()).to(Seq)
}

abstract class WrappedScalaExecutorService extends juc.AbstractExecutorService with WrappedScalaExecutor {
  def asScala: ExecutorService
  def isShutdown = asScala.isShutdown
  def isTerminated = asScala.isTerminated
  def awaitTermination(timeout: Long, unit: juc.TimeUnit) = asScala.awaitTermination(Timespan(timeout, unit))
  def shutdown() = asScala.shutdown()
  def shutdownNow() = asScala.shutdownNow().map(f => new Runnable() { def run() = f() }).asJava
}

class JavaExecutorServiceWrapper(j: juc.ExecutorService) {
  def asScala: ExecutorService = j match {
    case wrapped: WrappedScalaExecutorService => wrapped.asScala
    case _ => new WrappedJavaExecutorService { def asJava = j }
  }
}

class ScalaExecutorServiceWrapper(s: ExecutorService) {
  def asJava: juc.ExecutorService = s match {
    case wrapped: WrappedJavaExecutorService => wrapped.asJava
    case _ => new WrappedScalaExecutorService { def asScala = s }
  }
}

trait WrappedJavaFuture[+T] extends Future[T] {
  def asJava: juc.Future[T @uncheckedVariance]

  def cancel(mayInterruptIfRunning: Boolean) = asJava.cancel(mayInterruptIfRunning)

  def apply() =
    handlingExceptions(asJava.get())

  def apply(timespan: Timespan) =
    try {
      handlingExceptions(Some(asJava.get(timespan.count, timespan.unit)))
    } catch {
      case e: juc.TimeoutException =>
        None
    }
  
  private def handlingExceptions[A](x: => A) = {
    try {
      x
    } catch {
      case e: juc.ExecutionException =>
        if(e.getCause.isInstanceOf[WrappedNonRuntimeException])
          throw new ExecutionException(e.getMessage, e.getCause.getCause)
        else
          throw new ExecutionException(e.getMessage, e.getCause)
      case e: juc.CancellationException =>
        throw new CancellationException(e.getMessage)
    }
  }

  def isCancelled = asJava.isCancelled
  def isDone = asJava.isDone
}

trait WrappedScalaFuture[T] extends juc.Future[T] {
  def asScala: Future[T]

  def cancel(mayInterruptIfRunning: Boolean) = asScala.cancel(mayInterruptIfRunning)

  def get(): T =
    try {
      asScala()
    } catch {
      case CancellationException(msg) => throw new juc.CancellationException(msg)
      case ExecutionException(msg, cause) => throw new juc.ExecutionException(msg, cause)
    }

  def get(timeout: Long, unit: juc.TimeUnit): T = {
    val result = try {
      asScala(Timespan(timeout, unit))
    } catch {
      case CancellationException(msg) => throw new juc.CancellationException(msg)
      case ExecutionException(msg, cause) => throw new juc.ExecutionException(msg, cause)
    }

    result match {
      case Some(result) => result
      case None => throw new juc.TimeoutException()
    }
  }

  def isCancelled = asScala.isCancelled
  def isDone = asScala.isDone
}

class JavaFutureWrapper[A](j: juc.Future[A]) {
  val WrappedScalaFutureA = new UnsafeMatch[WrappedScalaFuture[A]]
  def asScala: Future[A] = j match {
    case WrappedScalaFutureA(wrapped) => wrapped.asScala
    case _ => new WrappedJavaFuture[A] { def asJava = j }
  }
}

class ScalaFutureWrapper[A](s: Future[A]) {
  val WrappedJavaFutureA = new UnsafeMatch[WrappedJavaFuture[A]]
  def asJava: juc.Future[A] = s match {
    case WrappedJavaFutureA(wrapped) => wrapped.asJava
    case _ => new WrappedScalaFuture[A] { def asScala = s }
  }
}
