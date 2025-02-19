/*
 * Part of NDLA concept-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.model.search

import no.ndla.conceptapi.Props
import no.ndla.conceptapi.model.domain.Sort
import no.ndla.language.Language.AllLanguages

case class DraftSearchSettings(
    withIdIn: List[Long],
    searchLanguage: String,
    page: Int,
    pageSize: Int,
    sort: Sort,
    fallback: Boolean,
    tagsToFilterBy: Set[String],
    statusFilter: Set[String],
    userFilter: Seq[String],
    shouldScroll: Boolean,
    embedResource: List[String],
    embedId: Option[String],
    responsibleIdFilter: List[String],
    conceptType: Option[String],
    aggregatePaths: List[String]
)

trait DraftSearchSettingsHelper {
  this: Props =>
  object draftSearchSettings {
    def empty: DraftSearchSettings = {
      DraftSearchSettings(
        withIdIn = List.empty,
        searchLanguage = AllLanguages,
        page = 1,
        pageSize = props.MaxPageSize,
        sort = Sort.ByRelevanceDesc,
        fallback = false,
        tagsToFilterBy = Set.empty,
        statusFilter = Set.empty,
        userFilter = Seq.empty,
        shouldScroll = false,
        embedResource = List.empty,
        embedId = None,
        responsibleIdFilter = List.empty,
        conceptType = None,
        aggregatePaths = List.empty
      )
    }
  }
}
