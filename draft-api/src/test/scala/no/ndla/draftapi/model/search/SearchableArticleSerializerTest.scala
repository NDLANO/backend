/*
 * Part of NDLA draft-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.search

import enumeratum.Json4s
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.draft.DraftStatus
import no.ndla.draftapi.{TestEnvironment, UnitSuite}
import no.ndla.search.model.{LanguageValue, SearchableLanguageFormats, SearchableLanguageList, SearchableLanguageValues}
import org.json4s.Formats
import org.json4s.native.Serialization.{read, writePretty}

class SearchableArticleSerializerTest extends UnitSuite with TestEnvironment {
  implicit val formats: Formats = SearchableLanguageFormats.JSonFormats + Json4s.serializer(DraftStatus)

  val searchableArticle1: SearchableArticle = SearchableArticle(
    id = 10.toLong,
    title = SearchableLanguageValues(Seq(LanguageValue("nb", "tittel"), LanguageValue("en", "title"))),
    content = SearchableLanguageValues(Seq(LanguageValue("nb", "innhold"), LanguageValue("en", "content"))),
    visualElement =
      SearchableLanguageValues(Seq(LanguageValue("nb", "visueltelement"), LanguageValue("en", "visualelement"))),
    introduction = SearchableLanguageValues(List(LanguageValue("nb", "ingress"), LanguageValue("en", "introduction"))),
    tags = SearchableLanguageList(
      List(LanguageValue("nb", List("m", "e", "r", "k")), LanguageValue("en", List("t", "a", "g", "s")))
    ),
    lastUpdated = NDLADate.of(2018, 2, 22, 13, 0, 51),
    license = Some("by-sa"),
    authors = Seq("Jonas Natty"),
    notes = Seq("jak"),
    articleType = "standard",
    defaultTitle = Some("tjuppidu"),
    users = Seq("ndalId54321"),
    previousNotes = Seq("Søte", "Jordbær"),
    grepCodes = Seq("KM1337", "KM5432"),
    status = SearchableStatus(DraftStatus.PUBLISHED, Set.empty)
  )

  test("That deserialization and serialization of SearchableArticle works as expected") {
    val json         = writePretty(searchableArticle1)
    val deserialized = read[SearchableArticle](json)

    deserialized should be(searchableArticle1)
  }

}
