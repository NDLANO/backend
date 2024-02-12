/*
 * Part of NDLA article-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.db.migrationwithdependencies

import no.ndla.articleapi.db.migrationwithdependencies.{V8_Author, V9__TranslateUntranslatedAuthors}
import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import org.json4s.Formats
import org.json4s.ext.JavaTimeSerializers

class V9__TranslateUntranslatedAuthorsTest extends UnitSuite with TestEnvironment {
  val migration                 = new V9__TranslateUntranslatedAuthors(props)
  implicit val formats: Formats = org.json4s.DefaultFormats ++ JavaTimeSerializers.all

  test("That redaksjonelt is translated to editorial whilst still keeping correct authors") {
    val metaString =
      """{"tags":[{"tags":["oppsummering","sammendrag","økologi"],"language":"nb"}],"title":[{"title":"Sammendrag av \"Økologi – samspillet i naturen\"","language":"nb"}],"content":[{"content":"<section></section>","language":"nb"}],"created":"2017-08-24T12:29:51Z","updated":"2017-09-13T17:41:16Z","copyright":{"origin":"","license":"by-sa","creators":[{"name":"A","type":"Writer"},{"name":"B","type":"Redaksjonelt"}],"processors":[],"rightsholders":[{"name":"C","type":"Supplier"}]},"updatedBy":"swagger-client","articleType":"standard","introduction":[],"visualElement":[],"metaDescription":[{"content":"","language":"nb"}],"requiredLibraries":[]}"""
    val result = migration.convertArticleUpdate(5, 2, metaString)

    result.copyright.creators should equal(List(V8_Author("Writer", "A"), V8_Author("Editorial", "B")))
    result.copyright.processors should equal(List.empty)
    result.copyright.rightsholders should equal(List(V8_Author("Supplier", "C")))

  }
}
