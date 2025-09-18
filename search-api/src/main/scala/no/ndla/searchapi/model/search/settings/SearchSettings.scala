/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.search.settings

import no.ndla.common.model.api.search.{LearningResourceType, ArticleTrait, SearchType}
import no.ndla.common.model.domain.Availability
import no.ndla.common.model.taxonomy.NodeType
import no.ndla.language.Language
import no.ndla.network.tapir.NonEmptyString
import no.ndla.searchapi.model.domain.Sort

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
    learningResourceTypes: List[LearningResourceType],
    supportedLanguages: List[String],
    relevanceIds: List[String],
    grepCodes: List[String],
    traits: List[ArticleTrait],
    shouldScroll: Boolean,
    filterByNoResourceType: Boolean,
    aggregatePaths: List[String],
    embedResource: List[String],
    embedId: Option[String],
    availability: List[Availability],
    articleTypes: List[String],
    filterInactive: Boolean,
    resultTypes: Option[List[SearchType]],
    nodeTypeFilter: List[NodeType]
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
    traits = List.empty,
    shouldScroll = false,
    filterByNoResourceType = false,
    aggregatePaths = List.empty,
    embedResource = List.empty,
    embedId = None,
    availability = List.empty,
    articleTypes = List.empty,
    filterInactive = false,
    nodeTypeFilter = List(NodeType.SUBJECT),
    resultTypes = Some(
      List(
        SearchType.Articles,
        SearchType.LearningPaths
      )
    )
  )
}
