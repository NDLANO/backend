/*
 * Part of NDLA article-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.api

import no.ndla.common.model.NDLADate
import sttp.tapir.Schema.annotations.description

// format: off
@description("Short summary of information about the article")
case class ArticleSummaryV2(
    @description("The unique id of the article") id: Long,
    @description("The title of the article") title: ArticleTitle,
    @description("A visual element article") visualElement: Option[VisualElement],
    @description("An introduction for the article") introduction: Option[ArticleIntroduction],
    @description("A metaDescription for the article") metaDescription: Option[ArticleMetaDescription],
    @description("A meta image for the article") metaImage: Option[ArticleMetaImage],
    @description("The full url to where the complete information about the article can be found") url: String,
    @description("Describes the license of the article") license: String,
    @description("The type of article this is. Possible values are frontpage-article, standard, topic-article") articleType: String,
    @description("The time when the article was last updated") lastUpdated: NDLADate,
    @description("A list of available languages for this article") supportedLanguages: Seq[String],
    @description("A list of codes from GREP API attached to this article") grepCodes: Seq[String],
    @description("Value that dictates who gets to see the article. Possible values are: everyone/teacher") availability: String,
)
// format: on
