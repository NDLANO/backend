/*
 * Part of NDLA search-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi

import no.ndla.scalatestsuite.UnitTestSuite

trait UnitSuite extends UnitTestSuite {
  private val pid = ProcessHandle.current().pid()

  setPropEnv("NDLA_ENVIRONMENT", "local")
  setPropEnv("SEARCH_SERVER", "some-server")
  setPropEnv("SEARCH_REGION", "some-region")
  setPropEnv("RUN_WITH_SIGNED_SEARCH_REQUESTS", "false")

  setPropEnv("ARTICLE_SEARCH_INDEX_NAME", s"searchapi-tests-articles-$pid")
  setPropEnv("DRAFT_SEARCH_INDEX_NAME", s"searchapi-tests-drafts-$pid")
  setPropEnv("LEARNINGPATH_SEARCH_INDEX_NAME", s"searchapi-tests-learningpaths-$pid")
  setPropEnv("DRAFT_CONCEPT_SEARCH_INDEX_NAME", s"searchapi-tests-draftconcepts-$pid")
  setPropEnv("GREP_SEARCH_INDEX_NAME", s"searchapi-tests-greps-$pid")
  setPropEnv("NODE_SEARCH_INDEX_NAME", s"searchapi-tests-nodes-$pid")
}
