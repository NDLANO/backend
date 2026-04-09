/*
 * Part of NDLA common
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api

import io.circe.{Decoder, Encoder, Json}
import sttp.tapir.Schema

sealed trait NullableOrValue[+T] {
  def toOption: Option[T]
}
final case class Value[T](value: T) extends NullableOrValue[T] {
  override def toOption: Option[T] = Some(value)
}
case object NullValue extends NullableOrValue[Nothing] {
  override def toOption: Option[Nothing] = None
}

object NullableOrValue {
  implicit def schema[T](implicit subschema: Schema[T]): Schema[NullableOrValue[T]] = subschema.nullable.as

  implicit def encodeNullable[A](implicit encodeA: Encoder[A]): Encoder[NullableOrValue[A]] = Encoder.instance {
    case Value(a)  => encodeA(a)
    case NullValue => Json.Null
  }

  implicit def decodeNullable[A](implicit decodeA: Decoder[A]): Decoder[NullableOrValue[A]] = Decoder.instance { c =>
    c.as[Option[A]]
      .map {
        case Some(a) => Value(a)
        case None    => NullValue
      }
  }
}
