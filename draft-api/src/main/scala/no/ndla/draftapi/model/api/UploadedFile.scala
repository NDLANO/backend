/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Information about the uploaded file")
case class UploadedFile(@(ApiModelProperty @field)(description = "Uploaded file's basename") filename: String,
                        @(ApiModelProperty @field)(description = "Uploaded file's mime type") mime: String,
                        @(ApiModelProperty @field)(description = "Uploaded file's file extension") extension: String,
                        @(ApiModelProperty @field)(description = "Full path of uploaded file") path: String)
