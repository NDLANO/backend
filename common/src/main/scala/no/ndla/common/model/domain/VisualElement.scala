/*
 * Part of NDLA common
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.common.model.domain

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.language.model.LanguageField

case class VisualElement(resource: String, language: String) extends LanguageField[String] {
  override def value: String    = resource
  override def isEmpty: Boolean = resource.isEmpty
}

object VisualElement {
  implicit def encoder: Encoder[VisualElement] = deriveEncoder[VisualElement]
  implicit def decoder: Decoder[VisualElement] = deriveDecoder[VisualElement]
}
