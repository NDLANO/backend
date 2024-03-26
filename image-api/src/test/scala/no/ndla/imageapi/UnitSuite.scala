/*
 * Part of NDLA image-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import no.ndla.common.secrets.PropertyKeys
import no.ndla.scalatestsuite.UnitTestSuite

trait UnitSuite extends UnitTestSuite {
  setPropEnv("NDLA_ENVIRONMENT", "local")
  setPropEnv("SEARCH_SERVER", "search-server")
  setPropEnv("SEARCH_REGION", "some-region")
  setPropEnv("RUN_WITH_SIGNED_SEARCH_REQUESTS", "false")
  setPropEnv("SEARCH_INDEX_NAME", "image-integration-test-index")

  setPropEnv(PropertyKeys.MetaUserNameKey, "username")
  setPropEnv(PropertyKeys.MetaPasswordKey, "secret")
  setPropEnv(PropertyKeys.MetaResourceKey, "resource")
  setPropEnv(PropertyKeys.MetaServerKey, "server")
  setPropEnv(PropertyKeys.MetaPortKey, "1234")
  setPropEnv(PropertyKeys.MetaSchemaKey, "testschema")
}
