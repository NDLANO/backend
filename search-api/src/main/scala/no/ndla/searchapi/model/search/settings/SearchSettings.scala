/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search.settings

import no.ndla.common.model.domain.Availability
import no.ndla.language.Language
import no.ndla.network.tapir.NonEmptyString
import no.ndla.searchapi.model.domain.{LearningResourceType, Sort}

case class SearchSettings(
    query: Option[NonEmptyString],
    fallback: Boolean,
    language: String,
    license: Option[String],
    page: Int,
    pageSize: Int,
    sort: Sort,
    withIdIn: List[Long],
    subjects: List[String],
    resourceTypes: List[String],
    learningResourceTypes: List[LearningResourceType.Value],
    supportedLanguages: List[String],
    relevanceIds: List[String],
    grepCodes: List[String],
    shouldScroll: Boolean,
    filterByNoResourceType: Boolean,
    aggregatePaths: List[String],
    embedResource: List[String],
    embedId: Option[String],
    availability: List[Availability.Value],
    articleTypes: List[String],
    filterInactive: Boolean
)

object SearchSettings {
  def default: SearchSettings = SearchSettings(
    query = None,
    fallback = false,
    language = Language.AllLanguages,
    license = None,
    page = 1,
    pageSize = 10,
    sort = Sort.ByRelevanceDesc,
    withIdIn = List.empty,
    subjects = List.empty,
    resourceTypes = List.empty,
    learningResourceTypes = List.empty,
    supportedLanguages = List.empty,
    relevanceIds = List.empty,
    grepCodes = List.empty,
    shouldScroll = false,
    filterByNoResourceType = false,
    aggregatePaths = List.empty,
    embedResource = List.empty,
    embedId = None,
    availability = List.empty,
    articleTypes = List.empty,
    filterInactive = false
  )
}
