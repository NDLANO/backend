/*
 * Part of NDLA backend.common.main
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.language

import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json}
import no.ndla.common.model.domain.language.OptionalLanguageValue.{NotWantedKey, NotWantedKeyT}
import no.ndla.language.model.WithLanguageAndValue

case class OptLanguageFields[T: Encoder: Decoder](
    internal: Map[String, Either[NotWantedKeyT, Option[T]]]
) {
  def get(language: String): Option[OptionalLanguageValue[T]] = {
    val res = internal.get(language)
    res match {
      case None                     => None
      case Some(Right(Some(value))) => Some(Exists(value))
      case Some(Right(None))        => None
      case Some(Left(_))            => Some(NotWanted())
    }
  }

  def withUnwanted(language: String): OptLanguageFields[T] = {
    val updated: Map[String, Either[NotWantedKeyT, Option[T]]] = internal.updated(language, Left(NotWantedKey))
    OptLanguageFields(updated)
  }
}

object OptLanguageFields {

  def fromFields[T](
      fields: Seq[WithLanguageAndValue[T]]
  )(implicit encoder: Encoder[T], decoder: Decoder[T]): OptLanguageFields[T] = {
    val underlyingMap = fields.map(f => f.language -> Right(Some(f.value))).toMap
    OptLanguageFields(underlyingMap)
  }

  implicit def eitherEncoder[T](implicit e: Encoder[T]): Encoder[Either[NotWantedKeyT, Option[T]]] = Encoder.instance {
    case Right(value) => value.asJson
    case Left(_)      => Json.obj(NotWantedKey -> Json.True)
  }

  implicit def eitherDecoder[T](implicit d: Decoder[T]): Decoder[Either[NotWantedKeyT, Option[T]]] = Decoder.instance {
    cursor =>
      val x              = cursor.downField(NotWantedKey)
      val notWantedField = x.as[Option[Boolean]]
      notWantedField match {
        case Right(Some(true)) =>
          Right(Left(NotWantedKey))
        case _ =>
          cursor.as[Option[T]].map(Right(_))
      }
  }

  implicit def encoder[T: Encoder]: Encoder[OptLanguageFields[T]] = Encoder.instance { lf =>
    lf.internal.asJson
  }

  implicit def decoder[T: Decoder: Encoder]: Decoder[OptLanguageFields[T]] = Decoder.instance { json =>
    json.as[Map[String, Either[NotWantedKeyT, Option[T]]]].map { m =>
      OptLanguageFields(m)
    }
  }
}
