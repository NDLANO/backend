/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *

 */

package no.ndla.common

import io.circe.DecodingFailure.Reason
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, DecodingFailure, Encoder}

import scala.annotation.unused
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

  case class ControlFlowException(returnValue: Throwable) extends RuntimeException()

  def permitTry[A](f: PermittedTryContext ?=> Try[A]): Try[A] = {
    try {
      f(using PermittedTryContext())
    } catch {
      case x: ControlFlowException => Failure(x.returnValue)
      case throwable               => throw throwable
    }
  }

  implicit class ctxctx[A](self: Try[A]) {
    def ?(using
        @unused(
          "This parameter is only to make sure we dont throw exceptions outside of a caught context"
        ) ctx: PermittedTryContext
    ): A = {
      self match {
        case Failure(ex)    => throw ControlFlowException(ex)
        case Success(value) => value
      }
    }

    def ??(using
        @unused(
          "This parameter is only to make sure we dont throw exceptions outside of a caught context"
        ) ctx: PermittedTryContext
    ): Unit = {
      self match {
        case Failure(ex) => throw ControlFlowException(ex)
        case Success(_)  => ()
      }
    }
  }

  extension [T](opt: Option[T]) {
    def toTry(throwable: Throwable): Try[T] = opt match {
      case Some(value) => Success(value)
      case None        => Failure(throwable)
    }
  }

  extension [T](t: Try[T]) {
    def unit: Try[Unit] = t.map(_ => ())
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
      case Left(_)      =>
        c.value.as[A] match {
          case Right(value) => Right(Left(value))
          case Left(_) => Left(DecodingFailure(Reason.CustomReason(s"Could not match ${c.value} to Either type"), c))
        }
    }
  }
}
