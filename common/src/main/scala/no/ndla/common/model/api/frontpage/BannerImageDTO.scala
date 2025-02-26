/*
 * Part of NDLA common
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.api.frontpage

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class BannerImageDTO(
    mobileUrl: Option[String],
    mobileId: Option[Long],
    desktopUrl: String,
    desktopId: Long
)

object BannerImageDTO {
  implicit val encoder: Encoder[BannerImageDTO] = deriveEncoder
  implicit val decoder: Decoder[BannerImageDTO] = deriveDecoder
}
