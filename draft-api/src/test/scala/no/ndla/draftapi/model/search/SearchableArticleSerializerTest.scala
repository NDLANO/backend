/*
 * Part of NDLA draft-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.search

import no.ndla.draftapi.{TestEnvironment, UnitSuite}
import org.joda.time.{DateTime, DateTimeZone}
import org.json4s.Formats
import org.json4s.native.Serialization.{read, writePretty}

class SearchableArticleSerializerTest extends UnitSuite with TestEnvironment {
  implicit val formats: Formats = SearchableLanguageFormats.JSonFormats

  val searchableArticle1 = SearchableArticle(
    id = 10.toLong,
    title = SearchableLanguageValues(Seq(LanguageValue("nb", "tittel"), LanguageValue("en", "title"))),
    content = SearchableLanguageValues(Seq(LanguageValue("nb", "innhold"), LanguageValue("en", "content"))),
    visualElement =
      SearchableLanguageValues(Seq(LanguageValue("nb", "visueltelement"), LanguageValue("en", "visualelement"))),
    introduction = SearchableLanguageValues(List(LanguageValue("nb", "ingress"), LanguageValue("en", "introduction"))),
    tags = SearchableLanguageList(
      List(LanguageValue("nb", List("m", "e", "r", "k")), LanguageValue("en", List("t", "a", "g", "s")))),
    lastUpdated = new DateTime(2018, 2, 22, 13, 0, 51, DateTimeZone.UTC).withMillisOfSecond(0),
    license = Some("by-sa"),
    authors = Seq("Jonas Natty"),
    notes = Seq("jak"),
    articleType = "standard",
    defaultTitle = Some("tjuppidu"),
    users = Seq("ndalId54321"),
    previousNotes = Seq("Søte", "Jordbær"),
    grepCodes = Seq("KM1337", "KM5432")
  )

  test("That deserialization and serialization of SearchableArticle works as expected") {
    val json = writePretty(searchableArticle1)
    val deserialized = read[SearchableArticle](json)

    deserialized should be(searchableArticle1)
  }

}
