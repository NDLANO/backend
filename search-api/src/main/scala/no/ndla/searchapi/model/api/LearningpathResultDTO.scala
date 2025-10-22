/*
 * Part of NDLA search-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.api

import no.ndla.common.model.api.search.TitleDTO
import sttp.tapir.Schema.annotations.description

@description("Search result for learningpath api")
case class LearningpathResultDTO(
    @description("The unique id of this learningpath")
    id: Long,
    @description("The title of the learningpath")
    title: TitleDTO,
    @description("The introduction of the learningpath")
    introduction: LearningPathIntroductionDTO,
    @description("List of supported languages")
    supportedLanguages: Seq[String],
)
