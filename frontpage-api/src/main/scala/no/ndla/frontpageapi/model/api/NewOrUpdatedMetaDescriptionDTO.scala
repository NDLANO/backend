/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.model.api

import io.circe.generic.semiauto.{deriveEncoder, deriveDecoder}
import io.circe.{Encoder, Decoder}

case class NewOrUpdatedMetaDescriptionDTO(metaDescription: String, language: String)
object NewOrUpdatedMetaDescriptionDTO {
  implicit val encoder: Encoder[NewOrUpdatedMetaDescriptionDTO] = deriveEncoder
  implicit val decoder: Decoder[NewOrUpdatedMetaDescriptionDTO] = deriveDecoder
}
