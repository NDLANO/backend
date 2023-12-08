/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */
package no.ndla.myndlaapi

import no.ndla.scalatestsuite.UnitTestSuite

import scala.util.Properties.setProp

trait UnitSuite extends UnitTestSuite {
  setProp("NDLA_ENVIRONMENT", "local")
  setProp("LP_META_USER_NAME", "some-user")
  setProp("LP_META_PASSWORD", "some-pw")
  setProp("LP_META_SCHEMA", "some-schema")
  setProp("LP_META_SERVER", "some-server")
  setProp("LP_META_PORT", "5432")
  setProp("LP_META_RESOURCE", "some-db")
  setProp("LP_MIGRATE", "false")
}
