/*
 * Part of NDLA image-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.db.migration

import no.ndla.imageapi.UnitSuite
import no.ndla.imageapi.db.migration.{TimeService, V3__AddUpdatedColoums, V3__DBImageMetaInformation}

class V3__AddUpdatedColoumsTest extends UnitSuite {

  class V3__MockedMigration extends V3__AddUpdatedColoums {
    override val timeService = mock[TimeService]
  }

  val migration = new V3__MockedMigration

  test("add updatedBy and updated on image object") {
    val before =
      """{"size":381667,"tags":[{"tags":["perforator","perforering","perforeringskanal"],"language":"nb"},{"tags":["shaped charge"]},{"tags":["shaped charge"],"language":"en"}],"titles":[{"title":"Perforeringssekvens"}],"alttexts":[{"alttext":"Avfyrings av skudd i brønn. Illustrasjon."}],"captions":[],"imageUrl":"avfyringssekvens.jpg","copyright":{"origin":"","authors":[{"name":"Baker Hughes Inc","type":"Opphavsmann"}],"license":{"url":"https://creativecommons.org/licenses/by-sa/2.0/","license":"by-sa","description":"Creative Commons Attribution-ShareAlike 2.0 Generic"}},"contentType":"image/jpeg"}"""
    val expectedAfter =
      """{"size":381667,"tags":[{"tags":["perforator","perforering","perforeringskanal"],"language":"nb"},{"tags":["shaped charge"]},{"tags":["shaped charge"],"language":"en"}],"titles":[{"title":"Perforeringssekvens"}],"alttexts":[{"alttext":"Avfyrings av skudd i brønn. Illustrasjon."}],"captions":[],"imageUrl":"avfyringssekvens.jpg","copyright":{"origin":"","authors":[{"name":"Baker Hughes Inc","type":"Opphavsmann"}],"license":{"url":"https://creativecommons.org/licenses/by-sa/2.0/","license":"by-sa","description":"Creative Commons Attribution-ShareAlike 2.0 Generic"}},"contentType":"image/jpeg","updatedBy":"content-import-client","updated":"2017-05-124T14:22:55+0200"}"""

    when(migration.timeService.nowAsString()).thenReturn("2017-05-124T14:22:55+0200")

    val image     = V3__DBImageMetaInformation(1, before)
    val converted = migration.convertImageUpdate(image)
    converted.document should equal(expectedAfter)

  }

}
