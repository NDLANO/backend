/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api.draft

import no.ndla.common.model.api.draft.Comment
import no.ndla.searchapi.model.api.Title
import no.ndla.searchapi.model.api.article.{ArticleIntroduction, VisualElement}
import sttp.tapir.Schema.annotations.description

// format: off
@description("Short summary of information about the article")
case class DraftSummary(
    @description("The unique id of the article") id: Long,
    @description("The title of the article") title: Title,
    @description("A visual element article") visualElement: Option[VisualElement],
    @description("An introduction for the article") introduction: Option[ArticleIntroduction],
    @description("The full url to where the complete information about the article can be found") url: String,
    @description("Describes the license of the article") license: String,
    @description("The type of article this is. Possible values are topic-article,standard") articleType: String,
    @description("A list of available languages for this audio") supportedLanguages: Seq[String],
    @description("The notes for this draft article") notes: Seq[String],
    @description("Information about comments attached to the article") comments: Seq[Comment]
)
