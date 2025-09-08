/*
 * Part of NDLA common
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.util

import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.model.api.search.SearchTrait
import no.ndla.common.model.domain.ArticleContent
import no.ndla.testbase.UnitTestSuiteBase

class TraitUtilTest extends UnitTestSuiteBase {

  val traitUtil = new TraitUtil

  test("That extracting attributes extracts data-title but not all attributes") {
    val html =
      s"""<section>Hei<p align="center">Heihei</p><$EmbedTagName class="testklasse" tulleattributt data-resource_id="55" data-title="For ei tittel"></$EmbedTagName>"""
    val result = traitUtil.getAttributes(html)
    result should be(List("For ei tittel"))
  }

  test("That traits are extracted correctly") {
    val content = Seq(
      ArticleContent(
        s"Sjekk denne h5p-en <$EmbedTagName data-resource=\"h5p\" data-path=\"/resource/id\"></$EmbedTagName>",
        "nb"
      ),
      ArticleContent(s"Fil <$EmbedTagName data-resource=\"file\" data-path=\"/file/path\"></$EmbedTagName>", "nn")
    )

    val result1 = traitUtil.getArticleTraits(content)
    result1 should be(List(SearchTrait.H5p))

    val content2 = Seq(
      ArticleContent(
        s"Skikkelig bra h5p: <$EmbedTagName data-resource=\"h5p\" data-path=\"/resource/id\"></$EmbedTagName>",
        "nb"
      ),
      ArticleContent(
        s"Fin video <$EmbedTagName data-resource=\"external\" data-url=\"https://youtu.be/id\"></$EmbedTagName>",
        "nn"
      ),
      ArticleContent(
        s"Movie trailer <$EmbedTagName data-resource=\"iframe\" data-url=\"https://www.imdb.com/video/vi3074735641\"></$EmbedTagName>",
        "en"
      )
    )

    val result2 = traitUtil.getArticleTraits(content2)
    result2 should be(List(SearchTrait.H5p, SearchTrait.Video))
  }

}
