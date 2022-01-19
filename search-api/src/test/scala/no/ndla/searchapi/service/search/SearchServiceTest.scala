/*
 * Part of NDLA search-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.service.search

import no.ndla.searchapi.{TestEnvironment, UnitSuite}
import com.sksamuel.elastic4s.ElasticDsl._
import no.ndla.searchapi.SearchApiProperties.SearchIndexes
import no.ndla.searchapi.model.search.SearchType

class SearchServiceTest extends UnitSuite with TestEnvironment {
  override val draftIndexService: DraftIndexService = new DraftIndexService {
    override val indexShards = 1
  }
  override val learningPathIndexService: LearningPathIndexService = new LearningPathIndexService {
    override val indexShards = 1
  }

  val service: SearchService = new SearchService {
    override val searchIndex = List(SearchIndexes(SearchType.Drafts), SearchIndexes(SearchType.LearningPaths))
    override val indexServices: List[IndexService[_]] = List(draftIndexService, learningPathIndexService)
    override protected def scheduleIndexDocuments(): Unit = {}
  }

  test("That building single termsAggregation works as expected") {
    val res1 = service.buildTermsAggregation(Seq("draftStatus.current"))
    res1 should be(
      Seq(
        termsAgg("draftStatus.current", "draftStatus.current")
          .size(50)
      ))
  }

  test("That building nested termsAggregation works as expected") {
    val res1 = service.buildTermsAggregation(Seq("contexts.contextType"))
    res1 should be(
      Seq(
        nestedAggregation("contexts", "contexts").subAggregations(
          termsAgg("contextType", "contexts.contextType").size(50)
        )))
  }

  test("That building nested multiple layers termsAggregation works as expected") {
    val res1 = service.buildTermsAggregation(Seq("contexts.resourceTypes.id"))
    res1 should be(
      Seq(
        nestedAggregation("contexts", "contexts")
          .subAggregations(
            nestedAggregation("resourceTypes", "contexts.resourceTypes")
              .subAggregations(
                termsAgg("id", "contexts.resourceTypes.id").size(50)
              )
          )
      )
    )
  }

  test("That aggregating paths that requires merging works as expected") {
    val res1 = service.buildTermsAggregation(Seq("contexts.contextType", "contexts.resourceTypes.id"))

    res1 should be(
      Seq(
        nestedAggregation("contexts", "contexts")
          .subAggregations(
            nestedAggregation("resourceTypes", "contexts.resourceTypes")
              .subAggregations(
                termsAgg("id", "contexts.resourceTypes.id").size(50)
              ),
            termsAgg("contextType", "contexts.contextType").size(50),
          )
      ))
  }

  test("That building multiple termsAggregation works as expected") {
    val res1 = service.buildTermsAggregation(
      Seq("draftStatus.current", "draftStatus.other", "contexts.contextType", "contexts.resourceTypes.id")
    )
    res1 should be(
      Seq(
        termsAgg("draftStatus.current", "draftStatus.current").size(50),
        termsAgg("draftStatus.other", "draftStatus.other").size(50),
        nestedAggregation("contexts", "contexts")
          .subAggregations(
            nestedAggregation("resourceTypes", "contexts.resourceTypes")
              .subAggregations(
                termsAgg("id", "contexts.resourceTypes.id").size(50)
              ),
            termsAgg("contextType", "contexts.contextType").size(50),
          )
      ))
  }

  test("that passing in an empty list does not crash even though it shouldnt happen") {
    val res1 = service.buildTermsAggregation(Seq.empty)
    res1 should be(Seq.empty)
  }

}
