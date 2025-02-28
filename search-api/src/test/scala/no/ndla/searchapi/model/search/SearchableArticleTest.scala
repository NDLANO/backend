/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.search

import no.ndla.common.CirceUtil
import no.ndla.common.model.api.search.LearningResourceType
import no.ndla.common.model.domain.ArticleMetaImage
import no.ndla.mapping.License
import no.ndla.search.model.domain.EmbedValues
import no.ndla.search.model.{LanguageValue, SearchableLanguageList, SearchableLanguageValues}
import no.ndla.searchapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.searchapi.TestData.*

class SearchableArticleTest extends UnitSuite with TestEnvironment {

  test("That serializing a SearchableArticle to json and deserializing back to object does not change content") {
    val titles =
      SearchableLanguageValues(Seq(LanguageValue("nb", "Christian Tut"), LanguageValue("en", "Christian Honk")))

    val contents = SearchableLanguageValues(
      Seq(
        LanguageValue("nn", "Eg kjøyrar rundt i min fine bil"),
        LanguageValue("nb", "Jeg kjører rundt i tutut"),
        LanguageValue("en", "I'm in my mums car wroomwroom")
      )
    )

    val introductions = SearchableLanguageValues(
      Seq(
        LanguageValue("en", "Wroom wroom")
      )
    )

    val metaDescriptions = SearchableLanguageValues(
      Seq(
        LanguageValue("nb", "Mammas bil")
      )
    )

    val tags = SearchableLanguageList(
      Seq(
        LanguageValue("en", Seq("Mum", "Car", "Wroom"))
      )
    )

    val embedAttrs = SearchableLanguageList(
      Seq(
        LanguageValue("nb", Seq("En norsk", "To norsk")),
        LanguageValue("en", Seq("One english"))
      )
    )

    val embedResourcesAndIds =
      List(EmbedValues(resource = Some("test resource 1"), id = List("test id 1"), language = "nb"))

    val metaImages = List(ArticleMetaImage("1", "alt", "nb"))

    val original = SearchableArticle(
      id = 100,
      title = titles,
      content = contents,
      introduction = introductions,
      metaDescription = metaDescriptions,
      tags = tags,
      lastUpdated = TestData.today,
      license = License.CC_BY_SA.toString,
      authors = List("Jonas", "Papi"),
      articleType = LearningResourceType.Article.toString,
      metaImage = metaImages,
      defaultTitle = Some("Christian Tut"),
      supportedLanguages = List("en", "nb", "nn"),
      context = searchableTaxonomyContexts.headOption,
      contexts = searchableTaxonomyContexts,
      contextids = searchableTaxonomyContexts.map(_.contextId),
      grepContexts =
        List(SearchableGrepContext("K123", Some("some title")), SearchableGrepContext("K456", Some("some title 2"))),
      traits = List.empty,
      embedAttributes = embedAttrs,
      embedResourcesAndIds = embedResourcesAndIds,
      availability = "everyone",
      learningResourceType = LearningResourceType.Article,
      domainObject = TestData.article1
    )
    val json         = CirceUtil.toJsonString(original)
    val deserialized = CirceUtil.unsafeParseAs[SearchableArticle](json)

    deserialized should be(original)
  }

  test(
    "That serializing a SearchableArticle with null values to json and deserializing back does not throw an exception"
  ) {
    val titles =
      SearchableLanguageValues(Seq(LanguageValue("nb", "Christian Tut"), LanguageValue("en", "Christian Honk")))

    val contents = SearchableLanguageValues(
      Seq(
        LanguageValue("nn", "Eg kjøyrar rundt i min fine bil"),
        LanguageValue("nb", "Jeg kjører rundt i tutut"),
        LanguageValue("en", "I'm in my mums car wroomwroom")
      )
    )

    val introductions = SearchableLanguageValues(
      Seq(
        LanguageValue("en", "Wroom wroom")
      )
    )

    val metaDescriptions = SearchableLanguageValues(
      Seq(
        LanguageValue("nb", "Mammas bil")
      )
    )

    val tags = SearchableLanguageList(
      Seq(
        LanguageValue("en", Seq("Mum", "Car", "Wroom"))
      )
    )

    val embedAttrs = SearchableLanguageList(
      Seq(
        LanguageValue("nb", Seq("En norsk", "To norsk")),
        LanguageValue("en", Seq("One english"))
      )
    )

    val embedResourcesAndIds =
      List(EmbedValues(resource = Some("test resource 1"), id = List("test id 1"), language = "nb"))

    val metaImages = List(ArticleMetaImage("1", "alt", "nb"))

    val original = SearchableArticle(
      id = 100,
      title = titles,
      content = contents,
      introduction = introductions,
      metaDescription = metaDescriptions,
      tags = tags,
      lastUpdated = TestData.today,
      license = License.CC_BY_SA.toString,
      authors = List("Jonas", "Papi"),
      articleType = LearningResourceType.Article.toString,
      metaImage = metaImages,
      defaultTitle = Some("Christian Tut"),
      supportedLanguages = List("en", "nb", "nn"),
      context = Some(singleSearchableTaxonomyContext),
      contexts = List(singleSearchableTaxonomyContext),
      contextids = List(singleSearchableTaxonomyContext.contextId),
      grepContexts =
        List(SearchableGrepContext("K123", Some("some title")), SearchableGrepContext("K456", Some("some title 2"))),
      traits = List.empty,
      embedAttributes = embedAttrs,
      embedResourcesAndIds = embedResourcesAndIds,
      availability = "everyone",
      learningResourceType = LearningResourceType.Article,
      domainObject = TestData.article1
    )

    val json         = CirceUtil.toJsonString(original)
    val deserialized = CirceUtil.unsafeParseAs[SearchableArticle](json)

    val expected =
      original.copy(context = Some(singleSearchableTaxonomyContext), contexts = List(singleSearchableTaxonomyContext))

    deserialized should be(expected)
  }

}
