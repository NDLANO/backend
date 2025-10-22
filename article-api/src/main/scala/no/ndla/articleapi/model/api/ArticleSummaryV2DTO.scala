/*
 * Part of NDLA article-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.api

import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.search.ArticleTrait
import no.ndla.common.model.domain.article.ArticleMetaDescriptionDTO
import sttp.tapir.Schema.annotations.description

@description("Short summary of information about the article")
case class ArticleSummaryV2DTO(
    @description("The unique id of the article")
    id: Long,
    @description("The title of the article")
    title: ArticleTitleDTO,
    @description("A visual element article")
    visualElement: Option[VisualElementDTO],
    @description("An introduction for the article")
    introduction: Option[ArticleIntroductionDTO],
    @description("A metaDescription for the article")
    metaDescription: Option[ArticleMetaDescriptionDTO],
    @description("A meta image for the article")
    metaImage: Option[ArticleMetaImageDTO],
    @description("The full url to where the complete information about the article can be found")
    url: String,
    @description("Describes the license of the article")
    license: String,
    @description("The type of article this is. Possible values are frontpage-article, standard, topic-article")
    articleType: String,
    @description("The time when the article was last updated")
    lastUpdated: NDLADate,
    @description("A list of available languages for this article")
    supportedLanguages: Seq[String],
    @description("A list of codes from GREP API attached to this article")
    grepCodes: Seq[String],
    @description("Value that dictates who gets to see the article. Possible values are: everyone/teacher")
    availability: String,
    @description("Traits extracted from the article content")
    traits: List[ArticleTrait],
)
