/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi

import no.ndla.common.secrets.PropertyKeys
import no.ndla.scalatestsuite.UnitTestSuite
import org.scalatest._

trait UnitSuite extends UnitTestSuite with PrivateMethodTester {
  setPropEnv("NDLA_ENVIRONMENT", "local")
  setPropEnv(PropertyKeys.MetaUserNameKey, "username")
  setPropEnv(PropertyKeys.MetaPasswordKey, "password")
  setPropEnv(PropertyKeys.MetaResourceKey, "resource")
  setPropEnv(PropertyKeys.MetaServerKey, "server")
  setPropEnv(PropertyKeys.MetaPortKey, "1234")
  setPropEnv(PropertyKeys.MetaSchemaKey, "schema")
  setPropEnv("SEARCH_SERVER", "search-server")
  setPropEnv("SEARCH_REGION", "some-region")
  setPropEnv("RUN_WITH_SIGNED_SEARCH_REQUESTS", "false")
  private val pid = ProcessHandle.current().pid()
  setPropEnv("SEARCH_INDEX_NAME", s"audio-integration-test-index-$pid")
  setPropEnv("SERIES_SEARCH_INDEX_NAME", s"audio-series-$pid")
  setPropEnv("AUDIO_TAG_SEARCH_INDEX_NAME", s"audio-tags-$pid")
  setPropEnv("BRIGHTCOVE_API_CLIENT_ID", "client-id")
  setPropEnv("BRIGHTCOVE_API_CLIENT_SECRET", "client")
  setPropEnv("BRIGHTCOVE_ACCOUNT_ID", "312532")
}
