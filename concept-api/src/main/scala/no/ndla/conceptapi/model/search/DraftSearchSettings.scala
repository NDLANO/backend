/*
 * Part of NDLA concept-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.search

import no.ndla.conceptapi.ConceptApiProperties
import no.ndla.conceptapi.model.domain.{Language, Sort}

case class DraftSearchSettings(
    withIdIn: List[Long],
    searchLanguage: String,
    page: Int,
    pageSize: Int,
    sort: Sort.Value,
    fallback: Boolean,
    subjects: Set[String],
    tagsToFilterBy: Set[String],
    statusFilter: Set[String],
    userFilter: Seq[String],
    shouldScroll: Boolean,
    embedResource: Option[String],
    embedId: Option[String]
)

object DraftSearchSettings {

  def empty: DraftSearchSettings = {
    new DraftSearchSettings(
      withIdIn = List.empty,
      searchLanguage = Language.AllLanguages,
      page = 1,
      pageSize = ConceptApiProperties.MaxPageSize,
      sort = Sort.ByRelevanceDesc,
      fallback = false,
      subjects = Set.empty,
      tagsToFilterBy = Set.empty,
      statusFilter = Set.empty,
      userFilter = Seq.empty,
      shouldScroll = false,
      embedResource = None,
      embedId = None
    )
  }
}
