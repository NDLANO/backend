/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api.article

import no.ndla.searchapi.model.api.{MetaDescription, Title}
import sttp.tapir.Schema.annotations.description

@description("Short summary of information about the article")
case class ArticleSummary(
    @description("The unique id of the article") id: Long,
    @description("The title of the article") title: Title,
    @description("A visual element article") visualElement: Option[VisualElement],
    @description("An introduction for the article") introduction: Option[ArticleIntroduction],
    @description("A metaDescription for the article") metaDescription: Option[MetaDescription],
    @description("A meta image for the article") metaImage: Option[ArticleMetaImage],
    @description("The full url to where the complete information about the article can be found") url: String,
    @description("Describes the license of the article") license: String,
    @description("The type of article this is. Possible values are topic-article,standard") articleType: String,
    @description("A list of available languages for this article") supportedLanguages: Seq[String]
)
