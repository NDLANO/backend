/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi

import org.scalatest._
import org.scalatest.mock.MockitoSugar

object IntegrationTest extends Tag("no.ndla.IntegrationTest")

abstract class UnitSuite extends FunSuite with Matchers with OptionValues with Inside with Inspectors with MockitoSugar with BeforeAndAfterEach with BeforeAndAfterAll with PrivateMethodTester {
  AudioApiProperties.setProperties(Map(
    "NDLA_ENVIRONMENT" -> Some("local"),

    "META_USER_NAME" -> Some("username"),
    "META_PASSWORD" -> Some("password"),
    "META_RESOURCE" -> Some("resource"),
    "META_SCHEMA" -> Some("schema"),
    "META_SERVER" -> Some(""),
    "META_PORT" -> Some("1234"),

    "RUN_WITH_SIGNED_SEARCH_REQUESTS" -> Some("false"),

    "MIGRATION_HOST" -> Some("migration.host"),
    "MIGRATION_USER" -> Some("user"),
    "MIGRATION_PASSWORD" -> Some("password")
  ))
}
