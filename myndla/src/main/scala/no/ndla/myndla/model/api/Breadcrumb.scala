/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla.model.api

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

case class Breadcrumb(
    @(ApiModelProperty @field)(description = "UUID of the folder") id: String,
    @(ApiModelProperty @field)(description = "Folder name") name: String
)

object Breadcrumb {
  implicit val encoder: Encoder[Breadcrumb] = deriveEncoder[Breadcrumb]
  implicit val decoder: Decoder[Breadcrumb] = deriveDecoder[Breadcrumb]
}
