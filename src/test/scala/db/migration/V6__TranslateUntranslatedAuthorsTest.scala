package db.migration

import no.ndla.audioapi.{TestEnvironment, UnitSuite}

class V6__TranslateUntranslatedAuthorsTest extends UnitSuite with TestEnvironment {
  val migration = new V6__TranslateUntranslatedAuthors
  implicit val formats = org.json4s.DefaultFormats

  test("That redaksjonelt is translated to editorial whilst still keeping correct authors") {
    val metaString =
      """{"tags":[{"tags":["fence"],"language":"unknown"}],"titles":[{"title":"Saemie saajv","language":"unknown"}],"updated":"2017-12-12T11:12:08Z","copyright":{"origin":"Fylkesbiblioteket","license":"by-sa","creators":[{"name":"A","type":"Writer"},{"name":"B","type":"Redaksjonelt"},{"name":"C","type":"Originator"}],"processors":[],"rightsholders":[]},"filePaths":[{"filePath":"saemie_saajvi_luvnie.mp3","fileSize":6326273,"language":"unknown","mimeType":"audio/mpeg"}],"updatedBy":"swagger-client","supportedLanguages":null}"""
    val result = migration.updateAuthorFormat(5, 2, metaString)

    result.copyright.creators should equal(
      List(V4_Author("Writer", "A"), V4_Author("Editorial", "B"), V4_Author("Originator", "C")))
    result.copyright.processors should equal(List.empty)
    result.copyright.rightsholders should equal(List.empty)

  }

}
