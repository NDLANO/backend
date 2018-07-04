/*
 * Part of NDLA audio_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.audioapi.{TestEnvironment, UnitSuite}
import org.mockito.Mockito.when

class V2__AddUpdatedColoumsTest extends UnitSuite with TestEnvironment {

  val migration = new V2_Test

  class V2_Test extends V2__AddUpdatedColoums {
    override val timeService = mock[TimeService]
  }

  test("add updatedBy and updated on audio object") {
    val before =
      """{"tags":[{"tags":["leisure","german","hobby"],"language":"en"},{"tags":["fritid","tysk","hobby"],"language":"nb"},{"tags":["fritid","tysk","hobby"],"language":"nn"},{"tags":["爱好"],"language":"zh"},{"tags":["freizeit","deutsch","hobby"],"language":"de"},{"tags":["leisure","german","hobby"]}],"titles":[{"title":"Ich höre gern Musik","language":""}],"copyright":{"authors":[{"name":"NSI Lydproduksjon A/S","type":"Leverandør"}],"license":"by-sa"},"filePaths":[{"filePath":"lektion_2_text_21.mp3","fileSize":94922,"language":"","mimeType":"audio/mpeg"}]}"""
    val expectedAfter =
      """{"tags":[{"tags":["leisure","german","hobby"],"language":"en"},{"tags":["fritid","tysk","hobby"],"language":"nb"},{"tags":["fritid","tysk","hobby"],"language":"nn"},{"tags":["爱好"],"language":"zh"},{"tags":["freizeit","deutsch","hobby"],"language":"de"},{"tags":["leisure","german","hobby"]}],"titles":[{"title":"Ich höre gern Musik","language":""}],"copyright":{"authors":[{"name":"NSI Lydproduksjon A/S","type":"Leverandør"}],"license":"by-sa"},"filePaths":[{"filePath":"lektion_2_text_21.mp3","fileSize":94922,"language":"","mimeType":"audio/mpeg"}],"updatedBy":"content-import-client","updated":"2017-05-08T11:35:13Z"}"""

    when(migration.timeService.nowAsString()).thenReturn("2017-05-08T11:35:13Z")

    val audio = V2_DBAudioMetaInformation(1, before)
    val converted = migration.convertAudioUpdate(audio)

    converted.document should equal(expectedAfter)

  }

}
