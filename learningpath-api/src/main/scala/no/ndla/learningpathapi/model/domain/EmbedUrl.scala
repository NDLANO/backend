/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import no.ndla.common.errors.{ValidationException, ValidationMessage}
import no.ndla.language.model.LanguageField

case class EmbedUrl(url: String, language: String, embedType: EmbedType.Value) extends LanguageField[String] {
  override def value: String    = url
  override def isEmpty: Boolean = url.isEmpty
}

object EmbedType extends Enumeration {

  val OEmbed = Value("oembed")
  val LTI    = Value("lti")
  val IFrame = Value("iframe")

  def valueOf(s: String): Option[EmbedType.Value] = {
    EmbedType.values.find(_.toString == s)
  }

  def valueOfOrError(embedType: String): EmbedType.Value = {
    valueOf(embedType) match {
      case Some(s) => s
      case None =>
        throw new ValidationException(
          errors = List(ValidationMessage("embedType", s"'$embedType' is not a valid embed type."))
        )
    }
  }

  def valueOfOrDefault(s: String): EmbedType.Value = {
    valueOf(s).getOrElse(EmbedType.OEmbed)
  }
}
