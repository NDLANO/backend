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
  val DEFAULT_PAGE_SIZE = 12
  val MAX_PAGE_SIZE = 548

  AudioApiProperties.setProperties(Map(
    "CONTACT_EMAIL" -> Some("user@host.com"),
    "APPLICATION_PORT" -> Some("80"),

    "META_USER_NAME" -> Some("username"),
    "META_PASSWORD" -> Some("password"),
    "META_RESOURCE" -> Some("resource"),
    "META_SCHEMA" -> Some("schema"),
    "META_SERVER" -> Some(""),
    "META_PORT" -> Some("1234"),
    "META_INITIAL_CONNECTIONS" -> Some("1"),
    "META_MAX_CONNECTIONS" -> Some("1"),

    "SEARCH_SERVER" -> Some("search-server"),
    "SEARCH_INDEX" -> Some("audios"),
    "SEARCH_REGION" -> Some("some-region"),
    "SEARCH_DOCUMENT" -> Some("audio"),
    "SEARCH_DEFAULT_PAGE_SIZE" -> Some(s"$DEFAULT_PAGE_SIZE"),
    "SEARCH_MAX_PAGE_SIZE" -> Some(s"$MAX_PAGE_SIZE"),
    "INDEX_BULK_SIZE" -> Some("500"),
    "RUN_WITH_SIGNED_SEARCH_REQUESTS" -> Some("false"),

    "MIGRATION_HOST" -> Some("migration.host"),
    "MIGRATION_USER" -> Some("user"),
    "MIGRATION_PASSWORD" -> Some("password"),

    "MAPPING_API_HOST" -> Some("mapping-host"),

    "STORAGE_NAME" -> Some("audio.storage")
  ))
}
