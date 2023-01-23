/*
 * Part of NDLA audio-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.db.migration

import no.ndla.audioapi.{TestEnvironment, UnitSuite}

class V16__AddSeriesRSSFieldTest extends UnitSuite with TestEnvironment {

  val migration = new V16__AddSeriesRSSField

  test("add hasRSS on audio object") {
    val before =
      """{"title":[{"title":"Hva vet jeg?","language":"nb"},{"title":"Kva veit eg?","language":"nn"}],"created":"2021-05-21T10:54:06Z","updated":"2022-03-25T13:10:10Z","coverPhoto":{"altText":"Podkast","imageId":"57363"},"description":[{"language": "nb", "description": "Hva vet egentlig jeg?"},{"language":"nn","description":"Kva veit egentlig eg?"}],"supportedLanguages":null}"""
    val expectedAfter =
      """{"title":[{"title":"Hva vet jeg?","language":"nb"},{"title":"Kva veit eg?","language":"nn"}],"created":"2021-05-21T10:54:06Z","updated":"2022-03-25T13:10:10Z","coverPhoto":{"altText":"Podkast","imageId":"57363"},"description":[{"language":"nb","description":"Hva vet egentlig jeg?"},{"language":"nn","description":"Kva veit egentlig eg?"}],"supportedLanguages":null,"hasRSS":true}"""

    val converted = migration.convertDocument(before)

    converted.toString should equal(expectedAfter)

  }

}
