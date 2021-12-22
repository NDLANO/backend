/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import no.ndla.scalatestsuite.UnitTestSuite

trait UnitSuite extends UnitTestSuite {
  setPropEnv("NDLA_ENVIRONMENT", "local")
  setPropEnv("SEARCH_SERVER", "some-server")
  setPropEnv("SEARCH_REGION", "some-region")
  setPropEnv("RUN_WITH_SIGNED_SEARCH_REQUESTS", "false")

  setPropEnv("ARTICLE_SEARCH_INDEX_NAME", "searchapi-tests-articles")
  setPropEnv("DRAFT_SEARCH_INDEX_NAME", "searchapi-tests-drafts")
  setPropEnv("LEARNINGPATH_SEARCH_INDEX_NAME", "searchapi-tests-learningpaths")
}
