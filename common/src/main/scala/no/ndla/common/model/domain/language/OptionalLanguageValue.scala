/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.language

import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, HCursor, Json}

sealed trait OptionalLanguageValue[T]
case class Exists[T: Encoder: Decoder](value: T) extends OptionalLanguageValue[T]
case class NotWanted[T]()                        extends OptionalLanguageValue[T]

object OptionalLanguageValue {
  type NotWantedKeyT = "__notwanted__"
  final val NotWantedKey = "__notwanted__"
  implicit def encoder[T](implicit valueEncoder: Encoder[T]): Encoder[OptionalLanguageValue[T]] = Encoder.instance {
    case Exists(value) => Json.obj("value" -> value.asJson)
    case NotWanted()   => Json.obj(NotWantedKey -> Json.True)
  }

  implicit def decoder[T: Encoder: Decoder]: Decoder[OptionalLanguageValue[T]] =
    (c: HCursor) => {
      c.downField(NotWantedKey).as[Option[Boolean]].flatMap {
        case Some(true) => Right(NotWanted())
        case _ =>
          val field  = c.downField("value")
          val parsed = field.as[T]
          parsed.map(value => Exists(value))
      }
    }
}
