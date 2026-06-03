/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi

import no.ndla.scalatestsuite.UnitTestSuite

trait UnitSuite extends UnitTestSuite {

  setPropEnv("NDLA_ENVIRONMENT", "local")
  setPropEnv("ENABLE_JOUBEL_H5P_OEMBED", "true")

  setPropEnv("SEARCH_SERVER", "some-server")
  setPropEnv("SEARCH_REGION", "some-region")
  setPropEnv("RUN_WITH_SIGNED_SEARCH_REQUESTS", "false")
  private val pid = ProcessHandle.current().pid()
  setPropEnv("SEARCH_INDEX_NAME", s"draft-integration-test-index-$pid")
  setPropEnv("TAG_SEARCH_INDEX_NAME", s"draft-tags-$pid")
  setPropEnv("GREP_CODES_SEARCH_INDEX_NAME", s"draft-grepcodes-$pid")

  setPropEnv("AUDIO_API_URL", "localhost:30014")
  setPropEnv("IMAGE_API_URL", "localhost:30001")

  setPropEnv("BRIGHTCOVE_ACCOUNT_ID", "some-account-id")
  setPropEnv("BRIGHTCOVE_PLAYER_ID", "some-player-id")
  setPropEnv("BRIGHTCOVE_API_CLIENT_ID", "some-client-id")
  setPropEnv("BRIGHTCOVE_API_CLIENT_SECRET", "some-secret")
}
