/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import sttp.tapir.Schema.annotations.description

@description("Id for a single Article")
case class ArticlePublishReport(
    @description("The ids of articles which was successfully (un)published") succeeded: Seq[Long],
    @description("The ids of articles which failed to (un)publish") failed: Seq[FailedArticlePublish]
) {
  def addFailed(fail: FailedArticlePublish): ArticlePublishReport = this.copy(failed = failed :+ fail)
  def addSuccessful(id: Long): ArticlePublishReport               = this.copy(succeeded = succeeded :+ id)
}

case class FailedArticlePublish(
    @description("The id of an article which failed to be (un)published") id: Long,
    @description("A message describing the cause") message: String
)
