/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import no.ndla.common.CirceUtil
import no.ndla.searchapi.TestData.*
import no.ndla.searchapi.{TestEnvironment, UnitSuite}

class SearchableTaxonomyContextTest extends UnitSuite with TestEnvironment {

  test(
    "That serializing a SearchableTaxonomyContext to json and deserializing back to object does not change content"
  ) {
    val json         = CirceUtil.toJsonString(searchableTaxonomyContexts)
    val deserialized = CirceUtil.unsafeParseAs[List[SearchableTaxonomyContext]](json)

    deserialized should be(searchableTaxonomyContexts)
  }
}
