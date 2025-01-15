/*
 * Part of NDLA backend.common.main
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.language

import io.circe.*
import io.circe.syntax.EncoderOps
import no.ndla.language.Language
import no.ndla.language.model.{BaseWithLanguageAndValue, WithLanguageAndValue}

case class LanguageFields[T: Encoder: Decoder](internal: Map[String, T]) {
  def getWithLanguageFields: Seq[WithLanguageAndValue[T]] = internal.map { case (language, value) =>
    BaseWithLanguageAndValue(language, value)
  }.toSeq
  def get(language: String): Option[WithLanguageAndValue[T]] =
    internal.get(language).map(BaseWithLanguageAndValue(language, _))
  def findByLanguageOrBestEffort(language: String): Option[WithLanguageAndValue[T]] =
    Language.findByLanguageOrBestEffort(getWithLanguageFields, language)
}

object LanguageFields {
  def empty[T: Encoder: Decoder]: LanguageFields[T] = LanguageFields(Map.empty)
  def fromFields[T](
      fields: Seq[WithLanguageAndValue[T]]
  )(implicit encoder: Encoder[T], decoder: Decoder[T]): LanguageFields[T] = {
    val underlyingMap = fields.map(f => f.language -> f.value).toMap
    LanguageFields(underlyingMap)
  }

  implicit def encoder[T: Encoder]: Encoder[LanguageFields[T]] = Encoder.instance { lf => lf.internal.asJson }
  implicit def decoder[T: Decoder: Encoder]: Decoder[LanguageFields[T]] = Decoder.instance { json =>
    json.as[Map[String, T]].map { m => LanguageFields(m) }

  }
}
