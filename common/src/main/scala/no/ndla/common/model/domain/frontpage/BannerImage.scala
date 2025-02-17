/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.frontpage

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class BannerImage(mobileImageId: Option[Long], desktopImageId: Long)

object BannerImage {
  implicit val encoder: Encoder[BannerImage] = deriveEncoder
  implicit val decoder: Decoder[BannerImage] = deriveDecoder
}
