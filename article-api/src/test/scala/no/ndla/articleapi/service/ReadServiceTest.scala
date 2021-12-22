/*
 * Part of NDLA article-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import no.ndla.articleapi.ArticleApiProperties.{DefaultLanguage, externalApiUrls}
import no.ndla.validation.EmbedTagRules.ResourceHtmlEmbedTag
import no.ndla.articleapi.model.api
import no.ndla.articleapi.model.api.ArticleSummaryV2
import no.ndla.articleapi.model.domain.{
  ArticleContent,
  ArticleTag,
  ArticleType,
  Cachable,
  Language,
  SearchSettings,
  Sort,
  VisualElement
}
import no.ndla.articleapi.model.search.SearchResult
import no.ndla.validation.{ResourceType, TagAttributes}
import no.ndla.articleapi.{TestData, TestEnvironment, UnitSuite}
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import scalikejdbc.DBSession

import scala.util.{Success, Try}

class ReadServiceTest extends UnitSuite with TestEnvironment {

  val externalImageApiUrl: String = externalApiUrls("image")
  val resourceIdAttr = s"${TagAttributes.DataResource_Id}"
  val resourceAttr = s"${TagAttributes.DataResource}"
  val imageType = s"${ResourceType.Image}"
  val h5pType = s"${ResourceType.H5P}"
  val urlAttr = s"${TagAttributes.DataUrl}"

  val content1 =
    s"""<$ResourceHtmlEmbedTag $resourceIdAttr="123" $resourceAttr="$imageType"><$ResourceHtmlEmbedTag $resourceIdAttr=1234 $resourceAttr="$imageType">"""

  val content2 =
    s"""<$ResourceHtmlEmbedTag $resourceIdAttr="321" $resourceAttr="$imageType"><$ResourceHtmlEmbedTag $resourceIdAttr=4321 $resourceAttr="$imageType">"""
  val articleContent1 = ArticleContent(content1, "und")

  val expectedArticleContent1: ArticleContent = articleContent1.copy(content =
    s"""<$ResourceHtmlEmbedTag $resourceIdAttr="123" $resourceAttr="$imageType" $urlAttr="$externalImageApiUrl/123"><$ResourceHtmlEmbedTag $resourceIdAttr="1234" $resourceAttr="$imageType" $urlAttr="$externalImageApiUrl/1234">""")

  val articleContent2 = ArticleContent(content2, "und")

  val nbTags = ArticleTag(Seq("a", "b", "c", "a", "b", "a"), "nb")
  val enTags = ArticleTag(Seq("d", "e", "f", "d", "e", "d"), "en")
  when(articleRepository.allTags(any[DBSession])).thenReturn(Seq(nbTags, enTags))

  override val readService = new ReadService
  override val converterService = new ConverterService

  test("withId adds urls and ids on embed resources") {
    val visualElementBefore =
      s"""<$ResourceHtmlEmbedTag data-align="" data-alt="" data-caption="" data-resource="image" data-resource_id="1" data-size="">"""
    val visualElementAfter =
      s"""<$ResourceHtmlEmbedTag data-align="" data-alt="" data-caption="" data-resource="image" data-resource_id="1" data-size="" data-url="http://api-gateway.ndla-local/image-api/v2/images/1">"""
    val article = TestData.sampleArticleWithByNcSa.copy(content = Seq(articleContent1),
                                                        visualElement = Seq(VisualElement(visualElementBefore, "nb")))

    when(articleRepository.withId(1)).thenReturn(Option(article))
    when(articleRepository.getExternalIdsFromId(any[Long])(any[DBSession])).thenReturn(List("54321"))

    val expectedResult: Try[Cachable[api.ArticleV2]] = Cachable.yes(
      converterService.toApiArticleV2(article.copy(content = Seq(expectedArticleContent1),
                                                   visualElement = Seq(VisualElement(visualElementAfter, "nb"))),
                                      "nb"))
    readService.withIdV2(1, "nb") should equal(expectedResult)
  }

  test("addIdAndUrlOnResource adds an id and url attribute on embed-resoures with a data-resource_id attribute") {
    readService.addUrlOnResource(articleContent1.content) should equal(expectedArticleContent1.content)
  }

  test("addIdAndUrlOnResource adds id but not url on embed resources without a data-resource_id attribute") {
    val articleContent3 = articleContent1.copy(
      content = s"""<$ResourceHtmlEmbedTag $resourceAttr="$h5pType" $urlAttr="http://some.h5p.org">""")
    readService.addUrlOnResource(articleContent3.content) should equal(articleContent3.content)
  }

  test("addUrlOnResource adds url attribute on file embeds") {
    val filePath = "files/lel/fileste.pdf"
    val content =
      s"""<div data-type="file"><$ResourceHtmlEmbedTag $resourceAttr="${ResourceType.File}" ${TagAttributes.DataPath}="$filePath" ${TagAttributes.Title}="This fancy pdf"><$ResourceHtmlEmbedTag $resourceAttr="${ResourceType.File}" ${TagAttributes.DataPath}="$filePath" ${TagAttributes.Title}="This fancy pdf"></div>"""
    val expectedResult =
      s"""<div data-type="file"><$ResourceHtmlEmbedTag $resourceAttr="${ResourceType.File}" ${TagAttributes.DataPath}="$filePath" ${TagAttributes.Title}="This fancy pdf" $urlAttr="http://api-gateway.ndla-local/$filePath"><$ResourceHtmlEmbedTag $resourceAttr="${ResourceType.File}" ${TagAttributes.DataPath}="$filePath" ${TagAttributes.Title}="This fancy pdf" $urlAttr="http://api-gateway.ndla-local/$filePath"></div>"""
    val result = readService.addUrlOnResource(content)
    result should equal(expectedResult)
  }

  test("addIdAndUrlOnResource adds urls on all content translations in an article") {
    val article = TestData.sampleArticleWithByNcSa.copy(content = Seq(articleContent1, articleContent2))
    val article1ExpectedResult = articleContent1.copy(content =
      s"""<$ResourceHtmlEmbedTag $resourceIdAttr="123" $resourceAttr="$imageType" $urlAttr="$externalImageApiUrl/123"><$ResourceHtmlEmbedTag $resourceIdAttr="1234" $resourceAttr="$imageType" $urlAttr="$externalImageApiUrl/1234">""")
    val article2ExpectedResult = articleContent1.copy(content =
      s"""<$ResourceHtmlEmbedTag $resourceIdAttr="321" $resourceAttr="$imageType" $urlAttr="$externalImageApiUrl/321"><$ResourceHtmlEmbedTag $resourceIdAttr="4321" $resourceAttr="$imageType" $urlAttr="$externalImageApiUrl/4321">""")

    val result = readService.addUrlsOnEmbedResources(article)
    result should equal(article.copy(content = Seq(article1ExpectedResult, article2ExpectedResult)))
  }

  test("getNMostUsedTags should return the N most used tags") {
    val expectedResult1 = Some(api.ArticleTag(Seq("a", "b"), "nb"))
    val expectedResult2 = Some(api.ArticleTag(Seq("d", "e"), "en"))
    readService.getNMostUsedTags(2, "nb") should equal(expectedResult1)
    readService.getNMostUsedTags(2, "en") should equal(expectedResult2)
  }

  test("MostFrequentOccurencesList.getNMostFrequent returns the N most frequent entries in a list") {
    val tagsList = Seq("tag", "tag", "tag", "junk", "lol", "17. Mai", "is", "brus", "17. Mai", "is", "is", "tag")
    val occList = new readService.MostFrequentOccurencesList(tagsList)

    occList.getNMostFrequent(1) should equal(Seq("tag"))
    occList.getNMostFrequent(2) should equal(Seq("tag", "is"))
    occList.getNMostFrequent(3) should equal(Seq("tag", "is", "17. Mai"))
    occList.getNMostFrequent(4) should equal(Seq("tag", "is", "17. Mai", "lol"))
  }

  test("addUrlOnResource adds url attribute on h5p embeds") {
    val h5pPath = "/resource/89734643-4006-4c65-a5de-34989ba7b2c8"
    val content =
      s"""<div><$ResourceHtmlEmbedTag $resourceAttr="${ResourceType.H5P}" ${TagAttributes.DataPath}="$h5pPath" ${TagAttributes.Title}="This fancy h5p"><$ResourceHtmlEmbedTag $resourceAttr="${ResourceType.H5P}" ${TagAttributes.DataPath}="$h5pPath" ${TagAttributes.Title}="This fancy h5p"></div>"""
    val expectedResult =
      s"""<div><$ResourceHtmlEmbedTag $resourceAttr="${ResourceType.H5P}" ${TagAttributes.DataPath}="$h5pPath" ${TagAttributes.Title}="This fancy h5p" $urlAttr="https://h5p.ndla.no$h5pPath"><$ResourceHtmlEmbedTag $resourceAttr="${ResourceType.H5P}" ${TagAttributes.DataPath}="$h5pPath" ${TagAttributes.Title}="This fancy h5p" $urlAttr="https://h5p.ndla.no$h5pPath"></div>"""
    val result = readService.addUrlOnResource(content)
    result should equal(expectedResult)
  }

  test("search should use size of id-list as page-size if defined") {
    val searchMock = mock[SearchResult[ArticleSummaryV2]]
    when(articleSearchService.matchingQuery(any[SearchSettings])).thenReturn(Success(searchMock))

    readService.search(
      query = None,
      sort = None,
      language = "nb",
      license = None,
      page = 1,
      pageSize = 10,
      idList = List(1, 2, 3, 4),
      articleTypesFilter = Seq.empty,
      fallback = false,
      grepCodes = Seq.empty,
      shouldScroll = false,
      feideAccessToken = None
    )

    val expectedSettings = SearchSettings(
      None,
      List(1, 2, 3, 4),
      DefaultLanguage,
      None,
      1,
      4,
      Sort.ByIdAsc,
      ArticleType.all,
      fallback = false,
      grepCodes = Seq.empty,
      shouldScroll = false,
      availability = Seq.empty
    )

    verify(articleSearchService, times(1)).matchingQuery(expectedSettings)

  }
}
