/*
 * Part of NDLA search-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.service.search

import no.ndla.common.model.api.search.SearchType
import no.ndla.search.SearchLanguage
import no.ndla.searchapi.{TestEnvironment, UnitSuite}

class SearchServiceTest extends UnitSuite with TestEnvironment {

  override implicit lazy val searchLanguage: SearchLanguage = new SearchLanguage
  override lazy val draftIndexService: DraftIndexService    = new DraftIndexService {
    override val indexShards = 1
  }
  override lazy val learningPathIndexService: LearningPathIndexService = new LearningPathIndexService {
    override val indexShards = 1
  }

  val service: SearchService = new SearchService {
    override val searchIndex                          = List(SearchType.Drafts, SearchType.LearningPaths).map(props.SearchIndex)
    override val indexServices: List[IndexService[?]] = List(draftIndexService, learningPathIndexService)
  }

}
