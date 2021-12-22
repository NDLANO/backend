/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi

import no.ndla.scalatestsuite.UnitTestSuite

trait UnitSuite extends UnitTestSuite {
  setPropEnv("NDLA_ENVIRONMENT", "local")
  setPropEnv("ENABLE_JOUBEL_H5P_OEMBED", "true")

  setPropEnv("SEARCH_SERVER", "some-server")
  setPropEnv("SEARCH_REGION", "some-region")
  setPropEnv("RUN_WITH_SIGNED_SEARCH_REQUESTS", "false")
  setPropEnv("SEARCH_INDEX_NAME", "draft-integration-test-index")
  setPropEnv("CONCEPT_SEARCH_INDEX_NAME", "concept-integration-test-index")
  setPropEnv("AGREEMENT_SEARCH_INDEX_NAME", "agreement-integration-test-index")

  setPropEnv("AUDIO_API_URL", "localhost:30014")
  setPropEnv("IMAGE_API_URL", "localhost:30001")

  setPropEnv("NDLA_BRIGHTCOVE_ACCOUNT_ID", "some-account-id")
  setPropEnv("NDLA_BRIGHTCOVE_PLAYER_ID", "some-player-id")
}
