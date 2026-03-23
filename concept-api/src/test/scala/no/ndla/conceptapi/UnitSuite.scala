/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi

import no.ndla.scalatestsuite.UnitTestSuite

trait UnitSuite extends UnitTestSuite {
  setPropEnv("NDLA_ENVIRONMENT", "local")
  setPropEnv("ENABLE_JOUBEL_H5P_OEMBED", "true")

  setPropEnv("SEARCH_SERVER", "some-server")
  setPropEnv("SEARCH_REGION", "some-region")
  setPropEnv("RUN_WITH_SIGNED_SEARCH_REQUESTS", "false")
  private val pid = ProcessHandle.current().pid()
  setPropEnv("SEARCH_INDEX_NAME", s"draft-integration-test-index-$pid")
  setPropEnv("CONCEPT_SEARCH_INDEX_NAME", s"concept-integration-test-index-$pid")

  setPropEnv("AUDIO_API_URL", "localhost:30014")
  setPropEnv("IMAGE_API_URL", "localhost:30001")

  setPropEnv("NDLA_BRIGHTCOVE_ACCOUNT_ID", "some-account-id")
  setPropEnv("NDLA_BRIGHTCOVE_PLAYER_ID", "some-player-id")
}
