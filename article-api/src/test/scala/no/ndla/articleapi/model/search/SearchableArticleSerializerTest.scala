/*
 * Part of NDLA article-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.search

import no.ndla.articleapi.*
import no.ndla.common.CirceUtil
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.search.ArticleTrait.Video
import no.ndla.common.model.domain.{ArticleMetaImage, Availability}
import no.ndla.mapping.License
import no.ndla.search.model.{LanguageValue, SearchableLanguageList, SearchableLanguageValues}

class SearchableArticleSerializerTest extends UnitSuite with TestEnvironment {
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
    license = License.CC_BY_SA.toString,
    authors = Seq("Jonas Natty"),
    articleType = "standard",
    defaultTitle = Some("tjuppidu"),
    grepCodes = Some(Seq("testelitt", "testemye")),
    availability = Availability.everyone.toString,
    traits = List(Video)
  )

  test("That deserialization and serialization of SearchableArticle works as expected") {
    val json         = CirceUtil.toJsonString(searchableArticle1)
    val deserialized = CirceUtil.unsafeParseAs[SearchableArticle](json)

    deserialized should be(searchableArticle1)
  }

}
