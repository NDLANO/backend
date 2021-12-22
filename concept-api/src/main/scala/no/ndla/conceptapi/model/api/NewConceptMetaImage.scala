/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.api
import org.scalatra.swagger.annotations.ApiModelProperty

import scala.annotation.meta.field

case class NewConceptMetaImage(
    @(ApiModelProperty @field)(description = "The image-api id of the meta image") id: String,
    @(ApiModelProperty @field)(description = "The alt text of the meta image") alt: String)
