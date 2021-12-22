/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi

import no.ndla.scalatestsuite.UnitTestSuite

import scala.util.Properties.setProp

trait UnitSuite extends UnitTestSuite {

  setProp("NDLA_ENVIRONMENT", "local")
  setProp("ENABLE_JOUBEL_H5P_OEMBED", "true")

  setProp("SEARCH_SERVER", "some-server")
  setProp("SEARCH_REGION", "some-region")
  setProp("RUN_WITH_SIGNED_SEARCH_REQUESTS", "false")
  setProp("SEARCH_INDEX_NAME", "draft-integration-test-index")
  setProp("AGREEMENT_SEARCH_INDEX_NAME", "agreement-integration-test-index")

  setProp("AUDIO_API_URL", "localhost:30014")
  setProp("IMAGE_API_URL", "localhost:30001")

  setProp("NDLA_BRIGHTCOVE_ACCOUNT_ID", "some-account-id")
  setProp("NDLA_BRIGHTCOVE_PLAYER_ID", "some-player-id")
  setProp("BRIGHTCOVE_API_CLIENT_ID", "some-client-id")
  setProp("BRIGHTCOVE_API_CLIENT_SECRET", "some-secret")
}
