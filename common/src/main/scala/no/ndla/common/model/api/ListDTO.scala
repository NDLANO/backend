/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.common.model.api

import io.circe.syntax.*
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema

/** Wrapper class to specify tapir schemas for lists without overriding every schema Mainly to make lists in output
  * types required in the openapi spec.
  */
case class ListDTO[T](underlying: List[T]) {
  def collect[B](pf: PartialFunction[T, B]): List[B]  = underlying.collect(pf)
  def map[B](f: T => B): ListDTO[B]                   = copy(underlying = underlying.map(f))
  def flatMap[B](f: T => IterableOnce[B]): ListDTO[B] = copy(underlying = underlying.flatMap(f))
}

object ListDTO {
  object implicits {
    implicit def fromIterable[T](s: Iterable[T]): ListDTO[T] = ListDTO(s.toList)
  }

  implicit def encoder[T: Encoder]: Encoder[ListDTO[T]] = Encoder.instance { case ListDTO(underlying) =>
    underlying.asJson
  }

  implicit def decoder[T: Decoder]: Decoder[ListDTO[T]] = Decoder.instance {
    _.as[List[T]].map(ListDTO(_))
  }

  implicit def schema[T](implicit subschema: Schema[T]): Schema[ListDTO[T]] = {
    subschema.asIterable.as[ListDTO[T]].copy(isOptional = false)
  }
}
