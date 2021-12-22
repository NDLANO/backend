/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Single failed result")
case class PartialPublishFailure(
    @(ApiModelProperty @field)(description = "Id of the article in question") id: Long,
    @(ApiModelProperty @field)(description = "Error message") message: String
)

@ApiModel(description = "A list of articles that were partial published to article-api")
case class MultiPartialPublishResult(
    @(ApiModelProperty @field)(description = "Successful ids") successes: Seq[Long],
    @(ApiModelProperty @field)(description = "Failed ids with error messages") failures: Seq[PartialPublishFailure]
)
