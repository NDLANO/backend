/*
 * Part of NDLA common
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.frontpage

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.language.model.LanguageField

case class AboutSubject(title: String, description: String, language: String, visualElement: VisualElement)
    extends LanguageField[(String, String, VisualElement)] {
  override def value: (String, String, VisualElement) = (title, description, visualElement)
  override def isEmpty: Boolean =
    title.isEmpty && description.isEmpty && visualElement.id.isEmpty && visualElement.alt.isEmpty
}

object AboutSubject {
  implicit val encoder: Encoder[AboutSubject] = deriveEncoder
  implicit val decoder: Decoder[AboutSubject] = deriveDecoder
}
