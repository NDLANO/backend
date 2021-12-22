/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.search

import no.ndla.searchapi.model.domain.article.LearningResourceType
import no.ndla.searchapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.searchapi.TestData._
import no.ndla.searchapi.model.domain.article.ArticleMetaImage
import org.json4s.native.Serialization.{read, write}
import org.json4s.Formats

class SearchableArticleTest extends UnitSuite with TestEnvironment {

  test("That serializing a SearchableArticle to json and deserializing back to object does not change content") {
    implicit val formats: Formats = SearchableLanguageFormats.JSonFormats

    val titles =
      SearchableLanguageValues(Seq(LanguageValue("nb", "Christian Tut"), LanguageValue("en", "Christian Honk")))

    val contents = SearchableLanguageValues(
      Seq(
        LanguageValue("nn", "Eg kjøyrar rundt i min fine bil"),
        LanguageValue("nb", "Jeg kjører rundt i tutut"),
        LanguageValue("en", "I'm in my mums car wroomwroom")
      ))

    val visualElements = SearchableLanguageValues(Seq(LanguageValue("nn", "image"), LanguageValue("nb", "image")))

    val introductions = SearchableLanguageValues(
      Seq(
        LanguageValue("en", "Wroom wroom")
      ))

    val metaDescriptions = SearchableLanguageValues(
      Seq(
        LanguageValue("nb", "Mammas bil")
      ))

    val tags = SearchableLanguageList(
      Seq(
        LanguageValue("en", Seq("Mum", "Car", "Wroom"))
      ))

    val embedAttrs = SearchableLanguageList(
      Seq(
        LanguageValue("nb", Seq("En norsk", "To norsk")),
        LanguageValue("en", Seq("One english"))
      ))

    val embedResourcesAndIds =
      List(EmbedValues(resource = Some("test resource 1"), id = List("test id 1"), language = "nb"))

    val metaImages = List(ArticleMetaImage("1", "alt", "nb"))

    val original = SearchableArticle(
      id = 100,
      title = titles,
      content = contents,
      visualElement = visualElements,
      introduction = introductions,
      metaDescription = metaDescriptions,
      tags = tags,
      lastUpdated = TestData.today,
      license = "by-sa",
      authors = List("Jonas", "Papi"),
      articleType = LearningResourceType.Article.toString,
      metaImage = metaImages,
      defaultTitle = Some("Christian Tut"),
      supportedLanguages = List("en", "nb", "nn"),
      contexts = searchableTaxonomyContexts,
      grepContexts =
        List(SearchableGrepContext("K123", Some("some title")), SearchableGrepContext("K456", Some("some title 2"))),
      traits = List.empty,
      embedAttributes = embedAttrs,
      embedResourcesAndIds = embedResourcesAndIds,
      availability = "everyone"
    )
    val json = write(original)
    val deserialized = read[SearchableArticle](json)

    deserialized should be(original)
  }

  test(
    "That serializing a SearchableArticle with null values to json and deserializing back does not throw an exception") {
    implicit val formats: Formats = SearchableLanguageFormats.JSonFormats

    val titles =
      SearchableLanguageValues(Seq(LanguageValue("nb", "Christian Tut"), LanguageValue("en", "Christian Honk")))

    val contents = SearchableLanguageValues(
      Seq(
        LanguageValue("nn", "Eg kjøyrar rundt i min fine bil"),
        LanguageValue("nb", "Jeg kjører rundt i tutut"),
        LanguageValue("en", "I'm in my mums car wroomwroom")
      ))

    val visualElements = SearchableLanguageValues(Seq(LanguageValue("nn", "image"), LanguageValue("nb", "image")))

    val introductions = SearchableLanguageValues(
      Seq(
        LanguageValue("en", "Wroom wroom")
      ))

    val metaDescriptions = SearchableLanguageValues(
      Seq(
        LanguageValue("nb", "Mammas bil")
      ))

    val tags = SearchableLanguageList(
      Seq(
        LanguageValue("en", Seq("Mum", "Car", "Wroom"))
      ))

    val embedAttrs = SearchableLanguageList(
      Seq(
        LanguageValue("nb", Seq("En norsk", "To norsk")),
        LanguageValue("en", Seq("One english"))
      ))

    val embedResourcesAndIds =
      List(EmbedValues(resource = Some("test resource 1"), id = List("test id 1"), language = "nb"))

    val embedResources = SearchableLanguageList(
      Seq(
        LanguageValue("nb", List("test resource 1", "test resource 2")),
      ))

    val embedIds = SearchableLanguageList(
      Seq(
        LanguageValue("nb", List("test id 1", "test id 2")),
      ))

    val metaImages = List(ArticleMetaImage("1", "alt", "nb"))

    val original = SearchableArticle(
      id = 100,
      title = titles,
      content = contents,
      visualElement = visualElements,
      introduction = introductions,
      metaDescription = metaDescriptions,
      tags = tags,
      lastUpdated = TestData.today,
      license = "by-sa",
      authors = List("Jonas", "Papi"),
      articleType = LearningResourceType.Article.toString,
      metaImage = metaImages,
      defaultTitle = Some("Christian Tut"),
      supportedLanguages = List("en", "nb", "nn"),
      contexts = List(singleSearchableTaxonomyContext),
      grepContexts =
        List(SearchableGrepContext("K123", Some("some title")), SearchableGrepContext("K456", Some("some title 2"))),
      traits = List.empty,
      embedAttributes = embedAttrs,
      embedResourcesAndIds = embedResourcesAndIds,
      availability = "everyone"
    )

    val json = write(original)
    val deserialized = read[SearchableArticle](json)

    val expected = original.copy(
      contexts = List(singleSearchableTaxonomyContext)
    )

    deserialized should be(expected)
  }

}
