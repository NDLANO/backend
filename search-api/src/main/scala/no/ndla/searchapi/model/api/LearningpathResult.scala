/*
 * Part of NDLA search-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api

import sttp.tapir.Schema.annotations.description

@description("Search result for learningpath api")
case class LearningpathResult(
    @description("The unique id of this learningpath") id: Long,
    @description("The title of the learningpath") title: TitleWithHtml,
    @description("The introduction of the learningpath") introduction: LearningPathIntroduction,
    @description("List of supported languages") supportedLanguages: Seq[String]
)
