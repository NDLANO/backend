/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Id for a single Article")
case class ArticlePublishReport(@(ApiModelProperty @field)(description =
                                  "The ids of articles which was successfully (un)published") succeeded: Seq[Long],
                                @(ApiModelProperty @field)(
                                  description = "The ids of articles which failed to (un)publish") failed: Seq[
                                  FailedArticlePublish]) {
  def addFailed(fail: FailedArticlePublish): ArticlePublishReport = this.copy(failed = failed :+ fail)
  def addSuccessful(id: Long): ArticlePublishReport = this.copy(succeeded = succeeded :+ id)
}

case class FailedArticlePublish(
    @(ApiModelProperty @field)(description = "The id of an article which failed to be (un)published") id: Long,
    @(ApiModelProperty @field)(description = "A message describing the cause") message: String)
