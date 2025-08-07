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

case class NewOrUpdateBannerImageDTO(mobileImageId: Option[Long], desktopImageId: Long)
object NewOrUpdateBannerImageDTO {
  implicit val encoder: Encoder[NewOrUpdateBannerImageDTO] = deriveEncoder
  implicit val decoder: Decoder[NewOrUpdateBannerImageDTO] = deriveDecoder
}
