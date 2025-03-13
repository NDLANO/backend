/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain

import scala.util.{Failure, Success, Try}

/** Helper class to simplify working with [[Option]] inside [[Try]] types */
case class TryMaybe[T](value: Try[Option[T]]) {
  def flatMap[B](f: T => TryMaybe[B]): TryMaybe[B] = TryMaybe(
    value.flatMap {
      case Some(v) => f(v).value
      case None    => Success(None)
    }
  )

  def map[B](f: T => B): TryMaybe[B] = TryMaybe(value.map(_.map(f)))

  def unsafeGet: T = value.get.get

  /** Returns the value as a Try[T] if the value exists, and returns the supplied `default` argument if `None` */
  def recoverNoneWith(default: => Try[T]): Try[T] = {
    value match {
      case Failure(ex)          => Failure(ex)
      case Success(None)        => default
      case Success(Some(value)) => Success(value)
    }
  }
}

object TryMaybe {
  def from[T](value: T): TryMaybe[T] = TryMaybe(Success(Some(value)))

  implicit class TryMaybeImplicit[T](val self: Try[Option[T]]) extends AnyVal {
    def toTryMaybe: TryMaybe[T] = TryMaybe(self)
  }

  implicit class TryImplicit[T](val self: Try[T]) extends AnyVal {
    def toTrySome: TryMaybe[T] = self.map(Some(_)).toTryMaybe
  }
}
