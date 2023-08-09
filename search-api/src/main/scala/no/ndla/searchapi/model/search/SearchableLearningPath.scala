/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import no.ndla.common.model.NDLADate
import no.ndla.search.model.{SearchableLanguageList, SearchableLanguageValues}
import no.ndla.searchapi.model.api.learningpath.Copyright

case class SearchableLearningPath(
    id: Long,
    title: SearchableLanguageValues,
    content: SearchableLanguageValues, // only for suggestions to work.
    description: SearchableLanguageValues,
    coverPhotoId: Option[String],
    duration: Option[Int],
    status: String,
    verificationStatus: String,
    lastUpdated: NDLADate,
    defaultTitle: Option[String],
    tags: SearchableLanguageList,
    learningsteps: List[SearchableLearningStep],
    license: String,
    copyright: Copyright,
    isBasedOn: Option[Long],
    supportedLanguages: List[String],
    authors: List[String],
    contexts: List[SearchableTaxonomyContext]
)
