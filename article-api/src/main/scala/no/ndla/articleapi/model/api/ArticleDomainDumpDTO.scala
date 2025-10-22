/*
 * Part of NDLA article-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.api

import no.ndla.common.model.domain.article.Article
import sttp.tapir.Schema.annotations.description

@description("Information about articles")
case class ArticleDomainDumpDTO(
    @description("The total number of articles in the database")
    totalCount: Long,
    @description("For which page results are shown from")
    page: Int,
    @description("The number of results per page")
    pageSize: Int,
    @description("The search results")
    results: Seq[Article],
)
