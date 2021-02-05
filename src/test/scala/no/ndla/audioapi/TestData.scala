package no.ndla.audioapi

import no.ndla.audioapi.model.Sort
import no.ndla.audioapi.model.domain.SearchSettings

object TestData {

  val searchSettings: SearchSettings = SearchSettings(
    query = None,
    language = None,
    license = None,
    page = None,
    pageSize = None,
    sort = Sort.ByTitleAsc,
    shouldScroll = false,
    audioType = None
  )
}
