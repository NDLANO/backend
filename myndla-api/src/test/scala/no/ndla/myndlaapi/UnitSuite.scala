/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */
package no.ndla.myndlaapi

import no.ndla.common.secrets.PropertyKeys
import no.ndla.scalatestsuite.UnitTestSuite

import scala.util.Properties.setProp

trait UnitSuite extends UnitTestSuite {
  setProp("NDLA_ENVIRONMENT", "local")

  setProp(PropertyKeys.MetaUserNameKey, "postgres")
  setProp(PropertyKeys.MetaPasswordKey, "hemmelig")
  setProp(PropertyKeys.MetaResourceKey, "postgres")
  setProp(PropertyKeys.MetaServerKey, "127.0.0.1")
  setProp(PropertyKeys.MetaPortKey, "5432")
  setProp(PropertyKeys.MetaSchemaKey, "myndlaapi_test")
}
