/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi

import no.ndla.scalatestsuite.UnitTestSuite
import no.ndla.common.secrets.PropertyKeys

trait UnitSuite extends UnitTestSuite {
  setPropEnv("NDLA_ENVIRONMENT", "local")

  setPropEnv("SEARCH_SERVER", "search-server")
  setPropEnv("SEARCH_REGION", "some-region")
  setPropEnv("RUN_WITH_SIGNED_SEARCH_REQUESTS", "false")

  setPropEnv("SEARCH_INDEX_NAME", "learning-integration-test-index")

  setPropEnv(PropertyKeys.MetaUserNameKey, "postgres")
  setPropEnv(PropertyKeys.MetaPasswordKey, "hemmelig")
  setPropEnv(PropertyKeys.MetaResourceKey, "postgres")
  setPropEnv(PropertyKeys.MetaServerKey, "127.0.0.1")
  setPropEnv(PropertyKeys.MetaPortKey, "5432")
  setPropEnv(PropertyKeys.MetaSchemaKey, "learningpathapi_test")
}
