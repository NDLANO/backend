package no.ndla.common

import scala.util.{Failure, Success, Try}

package object implicits {

  /** Stealing the question mark operator from rust:
    * https://doc.rust-lang.org/rust-by-example/std/result/question_mark.html
    *
    * Basically it means that we can call .? on a `Try` in any function which returns `Try` If the `Try` is a `Failure`
    * it will be returned from the function. If the `Try` is a `Success` the contained value will be returned.
    *
    * Example:
    *
    * {{{
    * // In case `theFunction` returns Success(10) `doStuff` will return Success("hello 10").
    * // In case `theFunction` return Failure(RuntimeException("bad")), `doStuff` will return `Failure(RuntimeException("bad"))`
    *
    * def doStuff(): Try[String] = permitTry {
    *   val x: Try[Int] = theFunction().?
    *   Success(s"hello $x")
    * }
    * }}}
    */

  case class PermittedTryContext()

  case class ControlFlowException[EX](returnValue: Try[EX]) extends RuntimeException()
  def permitTry[A](f: PermittedTryContext ?=> Try[A]): Try[A] = {
    try{
      f(using PermittedTryContext())
    } catch {
      case x: ControlFlowException[A] => x.returnValue
      case throwable => throw throwable
    }
  }

  implicit class ctxctx[A](self: Try[A]) {
    def ?(using PermittedTryContext): A = {
      self match {
        case Failure(ex) => throw new ControlFlowException[A](Failure(ex))
        case Success(value) => value
      }
    }
  }

  extension [T](opt: Option[T]) {
    def toTry(throwable: Throwable): Try[T] = Failure(throwable)
  }
}
