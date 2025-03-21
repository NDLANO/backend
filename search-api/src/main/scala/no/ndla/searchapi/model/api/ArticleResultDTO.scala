/*
 * Part of NDLA search-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.api

import no.ndla.common.model.api.search.TitleWithHtmlDTO
import no.ndla.searchapi.model.api.article.ArticleIntroductionDTO
import sttp.tapir.Schema.annotations.description

@description("Search result for article api")
case class ArticleResultDTO(
    @description("The unique id of this article") id: Long,
    @description("The title of the article") title: TitleWithHtmlDTO,
    @description("The introduction of the article") introduction: Option[ArticleIntroductionDTO],
    @description("The type of the article") articleType: String,
    @description("List of supported languages") supportedLanguages: Seq[String]
)
