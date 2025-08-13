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

case class NewOrUpdatedVisualElementDTO(`type`: String, id: String, alt: Option[String])
object NewOrUpdatedVisualElementDTO {
  implicit val encoder: Encoder[NewOrUpdatedVisualElementDTO] = deriveEncoder
  implicit val decoder: Decoder[NewOrUpdatedVisualElementDTO] = deriveDecoder
}
