/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi

import no.ndla.scalatestsuite.UnitTestSuite

trait UnitSuite extends UnitTestSuite {
  setPropEnv("NDLA_ENVIRONMENT", "local")
  setPropEnv("META_SCHEMA", "test")
  setPropEnv("BRIGHTCOVE_ACCOUNT", "123")
  setPropEnv("BRIGHTCOVE_PLAYER", "player")
}
