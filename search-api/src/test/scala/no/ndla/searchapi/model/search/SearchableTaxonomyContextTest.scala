/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import no.ndla.searchapi.TestData._
import no.ndla.searchapi.{TestEnvironment, UnitSuite}
import org.json4s.Formats
import org.json4s.native.Serialization.{read, write}

class SearchableTaxonomyContextTest extends UnitSuite with TestEnvironment {

  test("That serializing a SearchableTaxonomyContext to json and deserializing back to object does not change content") {
    implicit val formats: Formats = SearchableLanguageFormats.JSonFormats

    val json = write(searchableTaxonomyContexts)
    val deserialized = read[List[SearchableTaxonomyContext]](json)

    deserialized should be(searchableTaxonomyContexts)
  }
}
