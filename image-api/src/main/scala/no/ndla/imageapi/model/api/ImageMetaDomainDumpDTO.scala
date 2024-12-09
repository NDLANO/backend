/*
 * Part of NDLA image-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model.api

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.imageapi.model.domain.ImageMetaInformation
import sttp.tapir.Schema.annotations.description

@description("Information about image meta dump")
case class ImageMetaDomainDumpDTO(
    @description("The total number of images in the database") totalCount: Long,
    @description("For which page results are shown from") page: Int,
    @description("The number of results per page") pageSize: Int,
    @description("The search results") results: Seq[ImageMetaInformation]
)

object ImageMetaDomainDumpDTO {
  implicit val encoder: Encoder[ImageMetaDomainDumpDTO] = deriveEncoder
  implicit val decoder: Decoder[ImageMetaDomainDumpDTO] = deriveDecoder
}
