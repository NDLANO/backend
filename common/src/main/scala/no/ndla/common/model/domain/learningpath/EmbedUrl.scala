/*
 * Part of NDLA common
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.learningpath

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.errors.{ValidationException, ValidationMessage}
import no.ndla.language.model.LanguageField
import enumeratum._

case class EmbedUrl(url: String, language: String, embedType: EmbedType) extends LanguageField[String] {
  override def value: String    = url
  override def isEmpty: Boolean = url.isEmpty
}

object EmbedUrl {
  implicit val encoder: Encoder[EmbedUrl] = deriveEncoder
  implicit val decoder: Decoder[EmbedUrl] = deriveDecoder
}

sealed abstract class EmbedType(override val entryName: String) extends EnumEntry {
  override def toString: String = entryName
}

object EmbedType extends Enum[EmbedType] with CirceEnum[EmbedType] {
  case object OEmbed   extends EmbedType("oembed")
  case object LTI      extends EmbedType("lti")
  case object IFrame   extends EmbedType("iframe")
  case object External extends EmbedType("external")

  def valueOf(s: String): Option[EmbedType]        = EmbedType.values.find(_.entryName == s)
  def valueOfOrDefault(s: String): EmbedType       = valueOf(s).getOrElse(EmbedType.OEmbed)
  def valueOfOrError(embedType: String): EmbedType = {
    valueOf(embedType) match {
      case Some(s) => s
      case None    =>
        throw new ValidationException(
          errors = List(ValidationMessage("embedType", s"'$embedType' is not a valid embed type."))
        )
    }
  }

  override def values: IndexedSeq[EmbedType] = findValues
}
