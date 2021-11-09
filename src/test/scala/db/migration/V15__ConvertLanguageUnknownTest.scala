/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package db.migration

import no.ndla.audioapi.{TestEnvironment, UnitSuite}

class V15__ConvertLanguageUnknownTest extends UnitSuite with TestEnvironment {
  val migration = new V15__ConvertLanguageUnknown

  test("migration should change unknown to und") {
    {
      val old =
        s"""{"tags":[{"tags":["fence"],"language":"unknown"},{"tags":["gjerde"],"language":"nb"}],"titles":[{"title":"Saemie saajv","language":"unknown"},{"title":"Norsk","language":"nb"}],"updated":"2017-12-12T11:12:08Z","copyright":{"origin":"Fylkesbiblioteket","license":"by-sa","creators":[{"name":"A","type":"Writer"},{"name":"B","type":"Redaksjonelt"},{"name":"C","type":"Originator"}],"processors":[],"rightsholders":[]},"filePaths":[{"filePath":"file1.mp3","fileSize":6326273,"language":"unknown","mimeType":"audio/mpeg"},{"filePath":"file1.mp3","fileSize":6326273,"language":"nb","mimeType":"audio/mpeg"}],"manuscript":[{"manuscript":"Manuscript","language":"unknown"},{"manuscript":"Manuscript","language":"nb"}],"updatedBy":"swagger-client","supportedLanguages":null}"""

      val expected =
        s"""{"tags":[{"tags":["fence"],"language":"und"},{"tags":["gjerde"],"language":"nb"}],"titles":[{"title":"Saemie saajv","language":"und"},{"title":"Norsk","language":"nb"}],"updated":"2017-12-12T11:12:08Z","copyright":{"origin":"Fylkesbiblioteket","license":"by-sa","creators":[{"name":"A","type":"Writer"},{"name":"B","type":"Redaksjonelt"},{"name":"C","type":"Originator"}],"processors":[],"rightsholders":[]},"filePaths":[{"filePath":"file1.mp3","fileSize":6326273,"language":"und","mimeType":"audio/mpeg"},{"filePath":"file1.mp3","fileSize":6326273,"language":"nb","mimeType":"audio/mpeg"}],"manuscript":[{"manuscript":"Manuscript","language":"und"},{"manuscript":"Manuscript","language":"nb"}],"updatedBy":"swagger-client","supportedLanguages":null}"""

      migration.convertDocument(old) should equal(expected)
    }
  }

}
