package no.ndla.conceptapi.model.search

import no.ndla.conceptapi.ConceptApiProperties
import no.ndla.conceptapi.model.domain.{Language, Sort}

case class SearchSettings(
    withIdIn: List[Long],
    searchLanguage: String,
    page: Int,
    pageSize: Int,
    sort: Sort.Value,
    fallback: Boolean,
    subjects: Set[String],
    tagsToFilterBy: Set[String]
)

object SearchSettings {

  def empty: SearchSettings = {
    new SearchSettings(
      withIdIn = List.empty,
      searchLanguage = Language.AllLanguages,
      page = 1,
      pageSize = ConceptApiProperties.MaxPageSize,
      sort = Sort.ByRelevanceDesc,
      fallback = false,
      subjects = Set.empty,
      tagsToFilterBy = Set.empty
    )
  }
}
