/*
 * Part of NDLA draft-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

object PartialArticleFields extends Enumeration {
  val availability, grepCodes, license, metaDescription, relatedContent, tags = Value
}

// format: off
@ApiModel(description = "Partial data about articles to publish in bulk")
case class PartialBulkArticles(
    @(ApiModelProperty @field)(description = "A list of article ids to partially publish") articleIds: Seq[Long],
    @(ApiModelProperty @field)(description = "A list of fields that should be partially published") fields: Seq[PartialArticleFields.Value],
)
