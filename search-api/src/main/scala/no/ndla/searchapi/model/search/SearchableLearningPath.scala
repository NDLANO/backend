/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import no.ndla.searchapi.model.api.learningpath.Copyright
import org.joda.time.DateTime

case class SearchableLearningPath(
    id: Long,
    title: SearchableLanguageValues,
    content: SearchableLanguageValues, // only for suggestions to work.
    description: SearchableLanguageValues,
    coverPhotoId: Option[String],
    duration: Option[Int],
    status: String,
    verificationStatus: String,
    lastUpdated: DateTime,
    defaultTitle: Option[String],
    tags: SearchableLanguageList,
    learningsteps: List[SearchableLearningStep],
    license: String,
    copyright: Copyright,
    isBasedOn: Option[Long],
    supportedLanguages: List[String],
    authors: List[String],
    contexts: List[SearchableTaxonomyContext],
    embedResourcesAndIds: List[EmbedValues],
)
