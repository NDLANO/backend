/*
 * Part of NDLA search-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import no.ndla.searchapi.{TestEnvironment, UnitSuite}
import no.ndla.searchapi.model.search.SearchType

class SearchServiceTest extends UnitSuite with TestEnvironment {
  import props.SearchIndexes

  override val draftIndexService: DraftIndexService = new DraftIndexService {
    override val indexShards = 1
  }
  override val learningPathIndexService: LearningPathIndexService = new LearningPathIndexService {
    override val indexShards = 1
  }

  val service: SearchService = new SearchService {
    override val searchIndex = List(SearchIndexes(SearchType.Drafts), SearchIndexes(SearchType.LearningPaths))
    override val indexServices: List[IndexService[_]]     = List(draftIndexService, learningPathIndexService)
    override protected def scheduleIndexDocuments(): Unit = {}
  }

}
