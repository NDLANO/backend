/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common

import io.circe.DecodingFailure.Reason
import io.circe.{Decoder, DecodingFailure, Encoder}
import io.circe.syntax.EncoderOps

import scala.util.{Failure, Success, Try}
import scala.reflect.macros.blackbox

// TODO: Delete when we're on scala 3
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

  // NOTE: This is just a helper to make scala3 migration easier
  //       In scala 2 this does nothing at all, but in scala 3 it will be used to permit the use of .? without magic macros
  def permitTry[A](f: => A): A = { f }

  implicit class TryQuestionMark[A](private val self: Try[A]) extends AnyVal {

    /** See [[tryQuestionMarkOperator]] docs above */
    def ? : A = macro tryQuestionMarkOperator

    /** Same as [[?]] except that it returns Unit on success */
    def ?? : Unit = macro doubleTryQuestionMarkOperator

    def unit: Try[Unit] = self.map(_ => ())
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

  implicit def eitherEncoder[A: Encoder, B: Encoder]: Encoder[Either[A, B]] = Encoder.instance {
    case Left(value)  => value.asJson
    case Right(value) => value.asJson
  }

  implicit def eitherDecoder[A: Decoder, B: Decoder]: Decoder[Either[A, B]] = Decoder.instance { c =>
    c.value.as[B] match {
      case Right(value) => Right(Right(value))
      case Left(_) =>
        c.value.as[A] match {
          case Right(value) => Right(Left(value))
          case Left(_) => Left(DecodingFailure(Reason.CustomReason(s"Could not match ${c.value} to Either type"), c))
        }
    }
  }

}
