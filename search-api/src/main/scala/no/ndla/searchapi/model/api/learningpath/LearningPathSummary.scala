/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.api.learningpath

import no.ndla.searchapi.model.api.Title
import sttp.tapir.Schema.annotations.description

import java.time.LocalDateTime

@description("Summary of meta information for a learningpath")
case class LearningPathSummary(
    @description("The unique id of the learningpath") id: Long,
    @description("The titles of the learningpath") title: Title,
    @description("The descriptions of the learningpath") description: Description,
    @description("The introductions of the learningpath") introduction: Introduction,
    @description(
      "The full url to where the complete metainformation about the learningpath can be found"
    ) metaUrl: String,
    @description("Url to where a cover photo can be found") coverPhotoUrl: Option[String],
    @description("The duration of the learningpath in minutes") duration: Option[Int],
    @description("The publishing status of the learningpath.") status: String,
    @description("The date when this learningpath was last updated.") lastUpdated: LocalDateTime,
    @description("Searchable tags for the learningpath") tags: LearningPathTags,
    @description("The contributors of this learningpath") copyright: Copyright,
    @description("A list of available languages for this audio") supportedLanguages: Seq[String],
    @description("The id this learningpath is based on, if any") isBasedOn: Option[Long]
)
