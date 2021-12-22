/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Information about the concept")
case class UpdatedConcept(
    // format: off
    @(ApiModelProperty @field)(description = "The language of this concept") language: String,
    @(ApiModelProperty @field)(description = "Available titles for the concept") title: Option[String],
    @(ApiModelProperty @field)(description = "The content of the concept") content: Option[String],
    @(ApiModelProperty @field)(description = "An image-api ID for the concept meta image") metaImage: Deletable[NewConceptMetaImage],
    @(ApiModelProperty @field)(description = "Describes the copyright information for the concept") copyright: Option[Copyright],
    @(ApiModelProperty @field)(description = "URL for the source of the concept'") source: Option[String],
    @(ApiModelProperty @field)(description = "A list of searchable tags") tags: Option[Seq[String]],
    @(ApiModelProperty @field)(description = "A list of taxonomy subject ids the concept is connected to") subjectIds: Option[Seq[String]],
    @(ApiModelProperty @field)(description = "Article id to which the concept is connected to") articleIds: Option[Seq[Long]],
    @(ApiModelProperty @field)(description = "The new status of the concept") status: Option[String],
    @(ApiModelProperty @field)(description = "A visual element for the concept. May be anything from an image to a video or H5P") visualElement: Option[String]
    // format: on
)
