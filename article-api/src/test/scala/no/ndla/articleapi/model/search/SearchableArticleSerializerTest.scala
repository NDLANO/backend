/*
 * Part of NDLA article-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.model.search

import no.ndla.articleapi._
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.{ArticleMetaImage, Availability}
import no.ndla.search.model.{LanguageValue, SearchableLanguageFormats, SearchableLanguageList, SearchableLanguageValues}
import org.json4s.Formats
import org.json4s.native.Serialization.{read, writePretty}

class SearchableArticleSerializerTest extends UnitSuite with TestEnvironment {
  implicit val formats: Formats = SearchableLanguageFormats.JSonFormats

  val searchableArticle1: SearchableArticle = SearchableArticle(
    id = 10.toLong,
    title = SearchableLanguageValues(Vector(LanguageValue("nb", "tittel"), LanguageValue("en", "title"))),
    content = SearchableLanguageValues(Vector(LanguageValue("nb", "innhold"), LanguageValue("en", "content"))),
    visualElement =
      SearchableLanguageValues(Vector(LanguageValue("nb", "visueltelement"), LanguageValue("en", "visualelement"))),
    introduction =
      SearchableLanguageValues(Vector(LanguageValue("nb", "ingress"), LanguageValue("en", "introduction"))),
    metaDescription = SearchableLanguageValues(
      Vector(LanguageValue("nb", "meta beskrivelse"), LanguageValue("en", "meta description"))
    ),
    metaImage = Vector(ArticleMetaImage("nb", "alt", "1")),
    tags = SearchableLanguageList(
      Vector(LanguageValue("nb", List("m", "e", "r", "k")), LanguageValue("en", List("t", "a", "g", "s")))
    ),
    lastUpdated = NDLADate.of(2018, 2, 22, 14, 0, 51),
    license = "by-sa",
    authors = Seq("Jonas Natty"),
    articleType = "standard",
    defaultTitle = Some("tjuppidu"),
    grepCodes = Seq("testelitt", "testemye"),
    availability = Availability.everyone.toString
  )

  test("That deserialization and serialization of SearchableArticle works as expected") {
    val json         = writePretty(searchableArticle1)
    val deserialized = read[SearchableArticle](json)

    deserialized should be(searchableArticle1)
  }

}
