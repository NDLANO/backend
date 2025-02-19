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
import sttp.tapir.Schema

sealed trait OptionalLanguageValue[T]
case class Exists[T](value: T) extends OptionalLanguageValue[T]
case class NotWanted[T]()      extends OptionalLanguageValue[T]

object OptionalLanguageValue {
  type NotWantedKeyT = "__notwanted__"
  final val NotWantedKey: NotWantedKeyT               = "__notwanted__"
  implicit val NotWantedSchema: Schema[NotWantedKeyT] = Schema.string
  implicit def encoder[T](implicit valueEncoder: Encoder[T]): Encoder[OptionalLanguageValue[T]] = Encoder.instance {
    case Exists(value) => Json.obj("value" -> value.asJson)
    case NotWanted()   => Json.obj(NotWantedKey -> Json.True)
  }

  implicit def decoder[T: Decoder]: Decoder[OptionalLanguageValue[T]] =
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
