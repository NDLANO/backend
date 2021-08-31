package db.migration

import no.ndla.audioapi.{TestEnvironment, UnitSuite}

class V14__CreateMissingFilePathsTest extends UnitSuite with TestEnvironment {
  val migration = new V14__CreateMissingFilePaths

  test("migration should audioFiles for missing languages") {
    {
      val old =
        s"""{"tags":[{"tags":["fence"],"language":"unknown"},{"tags":["gjerde"],"language":"nb"}],"titles":[{"title":"Saemie saajv","language":"unknown"},{"title":"Norsk","language":"nb"}],"updated":"2017-12-12T11:12:08Z","copyright":{"origin":"Fylkesbiblioteket","license":"by-sa","creators":[{"name":"A","type":"Writer"},{"name":"B","type":"Redaksjonelt"},{"name":"C","type":"Originator"}],"processors":[],"rightsholders":[]},"filePaths":[{"filePath":"file1.mp3","fileSize":6326273,"language":"unknown","mimeType":"audio/mpeg"}],"updatedBy":"swagger-client","supportedLanguages":null}"""

      val expected =
        s"""{"tags":[{"tags":["fence"],"language":"unknown"},{"tags":["gjerde"],"language":"nb"}],"titles":[{"title":"Saemie saajv","language":"unknown"},{"title":"Norsk","language":"nb"}],"updated":"2017-12-12T11:12:08Z","copyright":{"origin":"Fylkesbiblioteket","license":"by-sa","creators":[{"name":"A","type":"Writer"},{"name":"B","type":"Redaksjonelt"},{"name":"C","type":"Originator"}],"processors":[],"rightsholders":[]},"filePaths":[{"filePath":"file1.mp3","fileSize":6326273,"language":"unknown","mimeType":"audio/mpeg"},{"filePath":"file1.mp3","fileSize":6326273,"language":"nb","mimeType":"audio/mpeg"}],"updatedBy":"swagger-client","supportedLanguages":null}"""

      migration.convertDocument(old) should equal(expected)
    }
  }
}
