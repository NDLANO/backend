/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.model.search

import no.ndla.conceptapi.Props
import no.ndla.conceptapi.model.domain.Sort
import no.ndla.language.Language.AllLanguages

case class SearchSettings(
    withIdIn: List[Long],
    searchLanguage: String,
    page: Int,
    pageSize: Int,
    sort: Sort,
    fallback: Boolean,
    tagsToFilterBy: Set[String],
    exactTitleMatch: Boolean,
    shouldScroll: Boolean,
    embedResource: List[String],
    embedId: Option[String],
    conceptType: Option[String],
    aggregatePaths: List[String]
)

object SearchSettings {
  def empty(using props: Props): SearchSettings = {
    new SearchSettings(
      withIdIn = List.empty,
      searchLanguage = AllLanguages,
      page = 1,
      pageSize = props.MaxPageSize,
      sort = Sort.ByRelevanceDesc,
      fallback = false,
      tagsToFilterBy = Set.empty,
      exactTitleMatch = false,
      shouldScroll = false,
      embedResource = List.empty,
      embedId = None,
      conceptType = None,
      aggregatePaths = List.empty
    )
  }
}
