/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.api

import no.ndla.common.model.NDLADate
import sttp.tapir.Schema.annotations.description

// format: off
@description("Short summary of information about the article")
case class ArticleSummary(
    @description("The unique id of the article") id: Long,
    @description("The title of the article") title: ArticleTitle,
    @description("A visual element article") visualElement: Option[VisualElement],
    @description("An introduction for the article") introduction: Option[ArticleIntroduction],
    @description("The full url to where the complete information about the article can be found") url: String,
    @description("Describes the license of the article") license: String,
    @description("The type of article this is. Possible values are frontpage-article, standard, topic-article") articleType: String,
    @description("A list of available languages for this audio") supportedLanguages: Seq[String],
    @description("Searchable tags for the article") tags: Option[ArticleTag],
    @description("The notes for this draft article") notes: Seq[String],
    @description("The users saved for this draft article") users: Seq[String],
    @description("The codes from GREP API registered for this draft article") grepCodes: Seq[String],
    @description("The status of this article") status: Status,
    @description("When the article was last updated") updated: NDLADate
)
