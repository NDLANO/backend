/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import sttp.tapir.Schema.annotations.description

@description("Single failed result")
case class PartialPublishFailure(
    @description("Id of the article in question") id: Long,
    @description("Error message") message: String
)

@description("A list of articles that were partial published to article-api")
case class MultiPartialPublishResult(
    @description("Successful ids") successes: Seq[Long],
    @description("Failed ids with error messages") failures: Seq[PartialPublishFailure]
)
