/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.language

import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json}
import no.ndla.common.model.domain.language.OptionalLanguageValue.{NotWantedKey, NotWantedKeyT}
import no.ndla.language.Language
import no.ndla.language.model.{BaseWithLanguageAndValue, WithLanguageAndValue}
import sttp.tapir.Schema

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

  def map[R](f: WithLanguageAndValue[Option[T]] => R): Seq[R] = internal.map { case (language, value) =>
    value match {
      case Right(Some(value)) => f(BaseWithLanguageAndValue(language, Some(value)))
      case _                  => f(BaseWithLanguageAndValue(language, None))
    }
  }.toSeq

  def mapExisting[R](f: WithLanguageAndValue[T] => R): Seq[R] = internal.flatMap { case (language, value) =>
    value match {
      case Right(Some(value)) => Some(f(BaseWithLanguageAndValue(language, value)))
      case _                  => None
    }
  }.toSeq

  def getWithLanguageFields: Seq[WithLanguageAndValue[T]] = internal.flatMap { case (language, value) =>
    value match {
      case Right(Some(value)) => Some(BaseWithLanguageAndValue(language, value))
      case _                  => None
    }
  }.toSeq

  def findByLanguageOrBestEffort(language: String): Option[WithLanguageAndValue[T]] = {
    get(language) match {
      case Some(Exists(value)) => Some(BaseWithLanguageAndValue(language, value))
      case Some(NotWanted())   => None
      case None                => Language.findByLanguageOrBestEffort(getWithLanguageFields, language)
    }
  }

  def withUnwanted(language: String): OptLanguageFields[T] = {
    val updated: Map[String, Either[NotWantedKeyT, Option[T]]] = internal.updated(language, Left(NotWantedKey))
    OptLanguageFields(updated)
  }

  def withValue(value: T, language: String): OptLanguageFields[T] = {
    val updated: Map[String, Either[NotWantedKeyT, Option[T]]] = internal.updated(language, Right(Some(value)))
    OptLanguageFields(updated)
  }

  def dropLanguage(language: String): OptLanguageFields[T] = {
    val newInternal = internal.removed(language)
    OptLanguageFields(newInternal)
  }
}

object OptLanguageFields {
  implicit val s1: Schema[OptLanguageFields[String]] = Schema.any

  def withUnwanted[T: Encoder: Decoder](language: String): OptLanguageFields[T] = {
    val underlyingMap = Map(language -> Left(NotWantedKey))
    OptLanguageFields(underlyingMap)
  }

  def withValue[T: Encoder: Decoder](value: T, language: String): OptLanguageFields[T] = {
    val underlyingMap = Map(language -> Right(Some(value)))
    OptLanguageFields(underlyingMap)
  }

  implicit class optStringLanguageFields(s: OptLanguageFields[String]) {
    def withOptValue(value: Option[String], language: String): OptLanguageFields[String] = {
      value match {
        case Some("") => s.withUnwanted(language)
        case Some(v)  => s.withValue(v, language)
        case None     => this.s
      }
    }

    def withOptValue(value: Option[String], language: Option[String]): OptLanguageFields[String] = language match {
      case None       => this.s
      case Some(lang) => this.withOptValue(value, lang)
    }
  }

  def fromMaybeString(value: Option[String], language: String): OptLanguageFields[String] = {
    value match {
      case Some("") => withUnwanted(language)
      case Some(v)  => withValue(v, language)
      case None     => empty
    }
  }

  def fromFields[T](
      fields: Seq[WithLanguageAndValue[T]]
  )(implicit encoder: Encoder[T], decoder: Decoder[T]): OptLanguageFields[T] = {
    val underlyingMap = fields.map(f => f.language -> Right(Some(f.value))).toMap
    OptLanguageFields(underlyingMap)
  }

  def empty[T: Encoder: Decoder]: OptLanguageFields[T] = OptLanguageFields(Map.empty)

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
