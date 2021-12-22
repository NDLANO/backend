/*
 * Part of NDLA draft-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */
package db.migration

import no.ndla.draftapi.{TestEnvironment, UnitSuite}

class V15__RemoveVisualElementFromStandardTest extends UnitSuite with TestEnvironment {
  val migration = new V15__RemoveVisualElementFromStandard

  test("migration should update visualelement depending on articletype") {
    {
      val old =
        s"""{"articleType":"standard","content":[{"content":"<strong>hallo</strong>","language":"nb"}],"copyright":{"creators":[{"name":"For Fatter","type":"Writer"}],"license":"CC-BY-SA-4.0","processors":[],"rightsholders":[]},"created":"2011-05-03T19:32:43Z","introduction":[{"introduction":"Introduksjon.","language":"nb"}],"metaDescription":[{"content":"Meta.","language":"nb"}],"metaImage":[{"altText":"","imageId":"9","language":"nb"}],"notes":[],"requiredLibraries":[],"status":{"current":"PUBLISHED","other":["IMPORTED"]},"tags":[{"language":"nb","tags":["tag1","tag2","tag3"]}],"title":[{"language":"nb","title":"Tittel"}],"updated":"2017-03-04T04:49:05Z","updatedBy":"r0gHb9Xg3li4yyXv0QSGQczV3bviakrT","visualElement":[{"language":"nb","resource":"resource"}]}"""
      val expected =
        s"""{"articleType":"standard","content":[{"content":"<strong>hallo</strong>","language":"nb"}],"copyright":{"creators":[{"name":"For Fatter","type":"Writer"}],"license":"CC-BY-SA-4.0","processors":[],"rightsholders":[]},"created":"2011-05-03T19:32:43Z","introduction":[{"introduction":"Introduksjon.","language":"nb"}],"metaDescription":[{"content":"Meta.","language":"nb"}],"metaImage":[{"altText":"","imageId":"9","language":"nb"}],"notes":[],"requiredLibraries":[],"status":{"current":"PUBLISHED","other":["IMPORTED"]},"tags":[{"language":"nb","tags":["tag1","tag2","tag3"]}],"title":[{"language":"nb","title":"Tittel"}],"updated":"2017-03-04T04:49:05Z","updatedBy":"r0gHb9Xg3li4yyXv0QSGQczV3bviakrT","visualElement":[]}"""
      migration.convertArticleUpdate(old) should equal(expected)
    }
    {
      val old =
        s"""{"articleType":"topic-article","content":[{"content":"<strong>hallo</strong>","language":"nb"}],"copyright":{"creators":[{"name":"For Fatter","type":"Writer"}],"license":"CC-BY-SA-4.0","processors":[],"rightsholders":[]},"created":"2011-05-03T19:32:43Z","introduction":[{"introduction":"Introduksjon.","language":"nb"}],"metaDescription":[{"content":"Meta.","language":"nb"}],"metaImage":[{"altText":"","imageId":"9","language":"nb"}],"notes":[],"requiredLibraries":[],"status":{"current":"PUBLISHED","other":["IMPORTED"]},"tags":[{"language":"nb","tags":["tag1","tag2","tag3"]}],"title":[{"language":"nb","title":"Tittel"}],"updated":"2017-03-04T04:49:05Z","updatedBy":"r0gHb9Xg3li4yyXv0QSGQczV3bviakrT","visualElement":[{"language":"nb","resource":"resource"}]}"""
      val expected =
        s"""{"articleType":"topic-article","content":[{"content":"<strong>hallo</strong>","language":"nb"}],"copyright":{"creators":[{"name":"For Fatter","type":"Writer"}],"license":"CC-BY-SA-4.0","processors":[],"rightsholders":[]},"created":"2011-05-03T19:32:43Z","introduction":[{"introduction":"Introduksjon.","language":"nb"}],"metaDescription":[{"content":"Meta.","language":"nb"}],"metaImage":[{"altText":"","imageId":"9","language":"nb"}],"notes":[],"requiredLibraries":[],"status":{"current":"PUBLISHED","other":["IMPORTED"]},"tags":[{"language":"nb","tags":["tag1","tag2","tag3"]}],"title":[{"language":"nb","title":"Tittel"}],"updated":"2017-03-04T04:49:05Z","updatedBy":"r0gHb9Xg3li4yyXv0QSGQczV3bviakrT","visualElement":[{"language":"nb","resource":"resource"}]}"""
      migration.convertArticleUpdate(old) should equal(expected)
    }
  }
}
