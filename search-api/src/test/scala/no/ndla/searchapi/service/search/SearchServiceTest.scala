/*
 * Part of NDLA search-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import no.ndla.searchapi.{TestEnvironment, UnitSuite}
import no.ndla.searchapi.model.search.SearchType

class SearchServiceTest extends UnitSuite with TestEnvironment {

  override val draftIndexService: DraftIndexService = new DraftIndexService {
    override val indexShards = 1
  }
  override val learningPathIndexService: LearningPathIndexService = new LearningPathIndexService {
    override val indexShards = 1
  }

  val service: SearchService = new SearchService {
    override val searchIndex = List(SearchType.Drafts, SearchType.LearningPaths).map(props.SearchIndex)
    override val indexServices: List[IndexService[_]]     = List(draftIndexService, learningPathIndexService)
  }

}
