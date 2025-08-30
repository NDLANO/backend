/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi

import no.ndla.common.configuration.BaseProps
import no.ndla.common.secrets.PropertyKeys
import no.ndla.scalatestsuite.UnitTestSuite

trait UnitSuite extends UnitTestSuite {
  lazy val props: BaseProps

  setPropEnv("NDLA_ENVIRONMENT", "local")

  setPropEnv(PropertyKeys.MetaUserNameKey, "postgres")
  setPropEnv(PropertyKeys.MetaPasswordKey, "hemmelig")
  setPropEnv(PropertyKeys.MetaResourceKey, "postgres")
  setPropEnv(PropertyKeys.MetaServerKey, "127.0.0.1")
  setPropEnv(PropertyKeys.MetaPortKey, "5432")
  setPropEnv(PropertyKeys.MetaSchemaKey, "myndlaapi_test")
}
