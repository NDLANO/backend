/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.search.settings

import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.search.{LearningResourceType, SearchTrait, SearchType}
import no.ndla.common.model.domain.draft.DraftStatus
import no.ndla.language.Language
import no.ndla.network.tapir.NonEmptyString
import no.ndla.searchapi.model.domain.Sort
import no.ndla.common.model.domain.Priority

case class MultiDraftSearchSettings(
    query: Option[NonEmptyString],
    noteQuery: Option[NonEmptyString],
    fallback: Boolean,
    language: String,
    license: Option[String],
    page: Int,
    pageSize: Int,
    sort: Sort,
    withIdIn: List[Long],
    subjects: List[String],
    topics: List[String],
    resourceTypes: List[String],
    learningResourceTypes: List[LearningResourceType],
    supportedLanguages: List[String],
    relevanceIds: List[String],
    statusFilter: List[DraftStatus],
    userFilter: List[String],
    grepCodes: List[String],
    traits: List[SearchTrait],
    shouldScroll: Boolean,
    searchDecompounded: Boolean,
    aggregatePaths: List[String],
    embedResource: List[String],
    embedId: Option[String],
    includeOtherStatuses: Boolean,
    revisionDateFilterFrom: Option[NDLADate],
    revisionDateFilterTo: Option[NDLADate],
    excludeRevisionHistory: Boolean,
    responsibleIdFilter: List[String],
    articleTypes: List[String],
    filterInactive: Boolean,
    priority: List[Priority],
    publishedFilterFrom: Option[NDLADate],
    publishedFilterTo: Option[NDLADate],
    resultTypes: Option[List[SearchType]]
)

object MultiDraftSearchSettings {
  def default: MultiDraftSearchSettings = MultiDraftSearchSettings(
    query = None,
    noteQuery = None,
    fallback = false,
    language = Language.AllLanguages,
    license = None,
    page = 1,
    pageSize = 10,
    sort = Sort.ByRelevanceDesc,
    withIdIn = List.empty,
    subjects = List.empty,
    topics = List.empty,
    resourceTypes = List.empty,
    learningResourceTypes = List.empty,
    supportedLanguages = List.empty,
    relevanceIds = List.empty,
    statusFilter = List.empty,
    userFilter = List.empty,
    grepCodes = List.empty,
    traits = List.empty,
    shouldScroll = false,
    searchDecompounded = false,
    aggregatePaths = List.empty,
    embedResource = List.empty,
    embedId = None,
    includeOtherStatuses = false,
    revisionDateFilterFrom = None,
    revisionDateFilterTo = None,
    excludeRevisionHistory = false,
    responsibleIdFilter = List.empty,
    articleTypes = List.empty,
    filterInactive = false,
    priority = List.empty,
    publishedFilterTo = None,
    publishedFilterFrom = None,
    resultTypes = Some(
      List(
        SearchType.Articles,
        SearchType.LearningPaths
      )
    )
  )
}
