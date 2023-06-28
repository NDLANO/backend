/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi

import no.ndla.scalatestsuite.UnitTestSuite
import no.ndla.common.secrets.PropertyKeys

import scala.util.Properties.setProp

trait UnitSuite extends UnitTestSuite {
  setProp("NDLA_ENVIRONMENT", "local")

  setProp("SEARCH_SERVER", "search-server")
  setProp("SEARCH_REGION", "some-region")
  setProp("RUN_WITH_SIGNED_SEARCH_REQUESTS", "false")

  setProp("SEARCH_INDEX_NAME", "learning-integration-test-index")

  setProp(PropertyKeys.MetaUserNameKey, "postgres")
  setProp(PropertyKeys.MetaPasswordKey, "hemmelig")
  setProp(PropertyKeys.MetaResourceKey, "postgres")
  setProp(PropertyKeys.MetaServerKey, "127.0.0.1")
  setProp(PropertyKeys.MetaPortKey, "5432")
  setProp(PropertyKeys.MetaSchemaKey, "learningpathapi_test")
}
