package no.ndla.common

import scala.util.{Failure, Success, Try}
import scala.reflect.macros.blackbox

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
    * def doStuff(): Try[String] = {
    *   val x: Try[Int] = theFunction().?
    *   Success(s"hello $x")
    * }
    * }}}
    */

  def tryQuestionMarkOperator(c: blackbox.Context): c.Tree = {
    import c.universe._
    c.prefix.tree match {
      case q"""$_[$_]($self)""" =>
        q"""
            import scala.util.{Failure, Success, Try}
            $self match {
              case Success(value) => value
              case Failure(ex)    => return Failure(ex)
            }
           """
      case _ => c.abort(c.enclosingPosition, "This is a bug with the tryQuestionMarkOperator macro")
    }
  }

  /** Same as [[tryQuestionMarkOperator]] except that it returns Unit on success */
  def doubleTryQuestionMarkOperator(c: blackbox.Context): c.Tree = {
    import c.universe._
    c.prefix.tree match {
      case q"""$_[$_]($self)""" =>
        q"""
            import scala.util.{Failure, Success, Try}
            $self match {
              case Success(value) => ()
              case Failure(ex)    => return Failure(ex)
            }
           """
      case _ => c.abort(c.enclosingPosition, "This is a bug with the tryQuestionMarkOperator macro")
    }
  }
  implicit class TryQuestionMark[A](private val self: Try[A]) extends AnyVal {

    /** See [[tryQuestionMarkOperator]] docs above */
    def ? : A = macro tryQuestionMarkOperator

    /** Same as [[?]] except that it returns Unit on success */
    def ?? : Unit = macro doubleTryQuestionMarkOperator
  }

  def optionQuestionMarkOperator(c: blackbox.Context): c.Tree = {
    import c.universe._

    c.prefix.tree match {
      case q"$_[$_]($self)" =>
        q"""
           $self match {
             case Some(value) => value
             case None        => return None
           }
           """
      case _ => c.abort(c.enclosingPosition, "This is a bug with the optionQuestionMarkOperator macro")
    }
  }
  implicit class OptionImplicit[T](private val self: Option[T]) extends AnyVal {
    def ? : T = macro optionQuestionMarkOperator
    def toTry(t: Throwable): Try[T] = {
      self match {
        case Some(v) => Success(v)
        case None    => Failure(t)
      }
    }
  }

  implicit class StringOption(private val self: Option[String]) {
    def emptySomeToNone: Option[String] = StringUtil.emptySomeToNone(self)
  }

}
