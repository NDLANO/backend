/*
 * Part of NDLA audio-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.db.migration

import no.ndla.audioapi.db.migration.V14__CreateMissingFilePaths
import no.ndla.audioapi.{TestEnvironment, UnitSuite}

class V14__CreateMissingFilePathsTest extends UnitSuite with TestEnvironment {
  val migration = new V14__CreateMissingFilePaths

  test("migration should create audioFiles for missing languages") {
    {
      val old =
        s"""{"tags":[{"tags":["fence"],"language":"unknown"},{"tags":["gjerde"],"language":"nb"}],"titles":[{"title":"Saemie saajv","language":"unknown"},{"title":"Norsk","language":"nb"}],"updated":"2017-12-12T11:12:08Z","copyright":{"origin":"Fylkesbiblioteket","license":"by-sa","creators":[{"name":"A","type":"Writer"},{"name":"B","type":"Redaksjonelt"},{"name":"C","type":"Originator"}],"processors":[],"rightsholders":[]},"filePaths":[{"filePath":"file1.mp3","fileSize":6326273,"language":"unknown","mimeType":"audio/mpeg"}],"updatedBy":"swagger-client","supportedLanguages":null}"""

      val expected =
        s"""{"tags":[{"tags":["fence"],"language":"unknown"},{"tags":["gjerde"],"language":"nb"}],"titles":[{"title":"Saemie saajv","language":"unknown"},{"title":"Norsk","language":"nb"}],"updated":"2017-12-12T11:12:08Z","copyright":{"origin":"Fylkesbiblioteket","license":"by-sa","creators":[{"name":"A","type":"Writer"},{"name":"B","type":"Redaksjonelt"},{"name":"C","type":"Originator"}],"processors":[],"rightsholders":[]},"filePaths":[{"filePath":"file1.mp3","fileSize":6326273,"language":"unknown","mimeType":"audio/mpeg"},{"filePath":"file1.mp3","fileSize":6326273,"language":"nb","mimeType":"audio/mpeg"}],"updatedBy":"swagger-client","supportedLanguages":null}"""

      migration.convertDocument(old) should equal(expected)
    }
  }

  test("migration should not create any audioFiles if list is empty") {
    {
      val old =
        s"""{"tags":[{"tags":["fence"],"language":"unknown"},{"tags":["gjerde"],"language":"nb"}],"titles":[{"title":"Saemie saajv","language":"unknown"},{"title":"Norsk","language":"nb"}],"updated":"2017-12-12T11:12:08Z","copyright":{"origin":"Fylkesbiblioteket","license":"by-sa","creators":[{"name":"A","type":"Writer"},{"name":"B","type":"Redaksjonelt"},{"name":"C","type":"Originator"}],"processors":[],"rightsholders":[]},"filePaths":[],"updatedBy":"swagger-client","supportedLanguages":null}"""

      val expected =
        s"""{"tags":[{"tags":["fence"],"language":"unknown"},{"tags":["gjerde"],"language":"nb"}],"titles":[{"title":"Saemie saajv","language":"unknown"},{"title":"Norsk","language":"nb"}],"updated":"2017-12-12T11:12:08Z","copyright":{"origin":"Fylkesbiblioteket","license":"by-sa","creators":[{"name":"A","type":"Writer"},{"name":"B","type":"Redaksjonelt"},{"name":"C","type":"Originator"}],"processors":[],"rightsholders":[]},"filePaths":[],"updatedBy":"swagger-client","supportedLanguages":null}"""

      migration.convertDocument(old) should equal(expected)
    }
  }

  test("migration should use nb as fallback if it exists") {
    {
      val old =
        s"""{"tags":[{"tags":["abc"],"language":"en"},{"tags":["abc"],"language":"nb"},{"tags":["abc"],"language":"unknown"},{"tags":["abc"],"language":"nn"}],"titles":[{"title":"123","language":"unknown"},{"title":"123","language":"nb"},{"title":"123","language":"nn"},{"title":"123","language":"en"}],"updated":"2017-12-12T11:12:08Z","copyright":{"origin":"Fylkesbiblioteket","license":"by-sa","creators":[{"name":"A","type":"Writer"},{"name":"B","type":"Redaksjonelt"},{"name":"C","type":"Originator"}],"processors":[],"rightsholders":[]},"filePaths":[{"filePath":"file1.mp3","fileSize":1000,"language":"en","mimeType":"audio/mpeg"},{"filePath":"file2.mp3","fileSize":2000,"language":"nb","mimeType":"audio/mpeg"},{"filePath":"file3.mp3","fileSize":3000,"language":"unknown","mimeType":"audio/mpeg"}],"updatedBy":"swagger-client","supportedLanguages":null}"""

      val expected =
        s"""{"tags":[{"tags":["abc"],"language":"en"},{"tags":["abc"],"language":"nb"},{"tags":["abc"],"language":"unknown"},{"tags":["abc"],"language":"nn"}],"titles":[{"title":"123","language":"unknown"},{"title":"123","language":"nb"},{"title":"123","language":"nn"},{"title":"123","language":"en"}],"updated":"2017-12-12T11:12:08Z","copyright":{"origin":"Fylkesbiblioteket","license":"by-sa","creators":[{"name":"A","type":"Writer"},{"name":"B","type":"Redaksjonelt"},{"name":"C","type":"Originator"}],"processors":[],"rightsholders":[]},"filePaths":[{"filePath":"file1.mp3","fileSize":1000,"language":"en","mimeType":"audio/mpeg"},{"filePath":"file2.mp3","fileSize":2000,"language":"nb","mimeType":"audio/mpeg"},{"filePath":"file3.mp3","fileSize":3000,"language":"unknown","mimeType":"audio/mpeg"},{"filePath":"file2.mp3","fileSize":2000,"language":"nn","mimeType":"audio/mpeg"}],"updatedBy":"swagger-client","supportedLanguages":null}"""

      migration.convertDocument(old) should equal(expected)
    }
  }

  test("migration should handle non-existing audioFiles list") {
    {
      val old =
        s"""{"tags":[{"tags":["fence"],"language":"unknown"},{"tags":["gjerde"],"language":"nb"}],"titles":[{"title":"Saemie saajv","language":"unknown"},{"title":"Norsk","language":"nb"}],"updated":"2017-12-12T11:12:08Z","copyright":{"origin":"Fylkesbiblioteket","license":"by-sa","creators":[{"name":"A","type":"Writer"},{"name":"B","type":"Redaksjonelt"},{"name":"C","type":"Originator"}],"processors":[],"rightsholders":[]},"updatedBy":"swagger-client","supportedLanguages":null}"""

      val expected =
        s"""{"tags":[{"tags":["fence"],"language":"unknown"},{"tags":["gjerde"],"language":"nb"}],"titles":[{"title":"Saemie saajv","language":"unknown"},{"title":"Norsk","language":"nb"}],"updated":"2017-12-12T11:12:08Z","copyright":{"origin":"Fylkesbiblioteket","license":"by-sa","creators":[{"name":"A","type":"Writer"},{"name":"B","type":"Redaksjonelt"},{"name":"C","type":"Originator"}],"processors":[],"rightsholders":[]},"updatedBy":"swagger-client","supportedLanguages":null}"""

      migration.convertDocument(old) should equal(expected)
    }
  }
}
