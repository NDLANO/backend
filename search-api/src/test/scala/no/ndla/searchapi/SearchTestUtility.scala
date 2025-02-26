/*
 * Part of NDLA search-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.searchapi

import no.ndla.common.model.api.search.MultiSearchSummaryDTO
import no.ndla.searchapi.model.domain.SearchResult
import org.scalatest.Assertions

object SearchTestUtility extends Assertions {
  implicit class searchResultHelper(result: SearchResult) {

    /** Helper to convert search results to only `MultiSearchSummaryDTO` If the result contains other types the test
      * fails
      */
    def summaryResults: Seq[MultiSearchSummaryDTO] = {
      result.results.map {
        case x: MultiSearchSummaryDTO => x
        case x                        => fail(s"Did not expect type of '${x.getClass.getSimpleName}'")
      }
    }
  }
}
