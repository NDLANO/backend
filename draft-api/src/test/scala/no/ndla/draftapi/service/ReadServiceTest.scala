/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service

import no.ndla.draftapi.DraftApiProperties.{externalApiUrls, resourceHtmlEmbedTag}
import no.ndla.draftapi.model.api
import no.ndla.draftapi.model.domain._
import no.ndla.draftapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.validation.{ResourceType, TagAttributes}
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import scalikejdbc.DBSession

import scala.util.Success

class ReadServiceTest extends UnitSuite with TestEnvironment {

  val externalImageApiUrl = externalApiUrls("image")
  val resourceIdAttr = s"${TagAttributes.DataResource_Id}"
  val resourceAttr = s"${TagAttributes.DataResource}"
  val imageType = s"${ResourceType.Image}"
  val h5pType = s"${ResourceType.H5P}"
  val urlAttr = s"${TagAttributes.DataUrl}"

  val content1 =
    s"""<$resourceHtmlEmbedTag $resourceIdAttr="123" $resourceAttr="$imageType"><$resourceHtmlEmbedTag $resourceIdAttr=1234 $resourceAttr="$imageType">"""

  val content2 =
    s"""<$resourceHtmlEmbedTag $resourceIdAttr="321" $resourceAttr="$imageType"><$resourceHtmlEmbedTag $resourceIdAttr=4321 $resourceAttr="$imageType">"""
  val articleContent1 = ArticleContent(content1, "und")

  val expectedArticleContent1 = articleContent1.copy(content =
    s"""<$resourceHtmlEmbedTag $resourceIdAttr="123" $resourceAttr="$imageType" $urlAttr="$externalImageApiUrl/123"><$resourceHtmlEmbedTag $resourceIdAttr="1234" $resourceAttr="$imageType" $urlAttr="$externalImageApiUrl/1234">""")

  val articleContent2 = ArticleContent(content2, "und")

  val nbTags = ArticleTag(Seq("a", "b", "c", "a", "b", "a"), "nb")
  val enTags = ArticleTag(Seq("d", "e", "f", "d", "e", "d"), "en")
  when(draftRepository.allTags(any[DBSession])).thenReturn(Seq(nbTags, enTags))

  override val readService = new ReadService
  override val converterService = new ConverterService

  test("withId adds urls and ids on embed resources") {
    val visualElementBefore =
      s"""<$resourceHtmlEmbedTag data-align="" data-alt="" data-caption="" data-resource="image" data-resource_id="1" data-size="">"""
    val visualElementAfter =
      s"""<$resourceHtmlEmbedTag data-align="" data-alt="" data-caption="" data-resource="image" data-resource_id="1" data-size="" data-url="http://api-gateway.ndla-local/image-api/v2/images/1">"""
    val article = TestData.sampleArticleWithByNcSa.copy(content = Seq(articleContent1),
                                                        visualElement = Seq(VisualElement(visualElementBefore, "nb")))

    when(draftRepository.withId(1)).thenReturn(Option(article))
    when(draftRepository.getExternalIdsFromId(any[Long])(any[DBSession])).thenReturn(List("54321"))

    val expectedResult = converterService
      .toApiArticle(article.copy(content = Seq(expectedArticleContent1),
                                 visualElement = Seq(VisualElement(visualElementAfter, "nb"))),
                    "nb")
      .get
    readService.withId(1, "nb") should equal(Success(expectedResult))
  }

  test("addIdAndUrlOnResource adds an id and url attribute on embed-resoures with a data-resource_id attribute") {
    readService.addUrlOnResource(articleContent1.content) should equal(expectedArticleContent1.content)
  }

  test("addIdAndUrlOnResource adds id but not url on embed resources without a data-resource_id attribute") {
    val articleContent3 = articleContent1.copy(
      content = s"""<$resourceHtmlEmbedTag $resourceAttr="$h5pType" $urlAttr="http://some.h5p.org">""")
    readService.addUrlOnResource(articleContent3.content) should equal(articleContent3.content)
  }

  test("addIdAndUrlOnResource adds urls on all content translations in an article") {
    val article = TestData.sampleArticleWithByNcSa.copy(content = Seq(articleContent1, articleContent2))
    val article1ExpectedResult = articleContent1.copy(content =
      s"""<$resourceHtmlEmbedTag $resourceIdAttr="123" $resourceAttr="$imageType" $urlAttr="$externalImageApiUrl/123"><$resourceHtmlEmbedTag $resourceIdAttr="1234" $resourceAttr="$imageType" $urlAttr="$externalImageApiUrl/1234">""")
    val article2ExpectedResult = articleContent1.copy(content =
      s"""<$resourceHtmlEmbedTag $resourceIdAttr="321" $resourceAttr="$imageType" $urlAttr="$externalImageApiUrl/321"><$resourceHtmlEmbedTag $resourceIdAttr="4321" $resourceAttr="$imageType" $urlAttr="$externalImageApiUrl/4321">""")

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

  test("addUrlOnResource adds url attribute on file embeds") {
    val filePath = "files/lel/fileste.pdf"
    val content =
      s"""<div data-type="file"><$resourceHtmlEmbedTag $resourceAttr="${ResourceType.File}" ${TagAttributes.DataPath}="$filePath" ${TagAttributes.Title}="This fancy pdf"><$resourceHtmlEmbedTag $resourceAttr="${ResourceType.File}" ${TagAttributes.DataPath}="$filePath" ${TagAttributes.Title}="This fancy pdf"></div>"""
    val expectedResult =
      s"""<div data-type="file"><$resourceHtmlEmbedTag $resourceAttr="${ResourceType.File}" ${TagAttributes.DataPath}="$filePath" ${TagAttributes.Title}="This fancy pdf" $urlAttr="http://api-gateway.ndla-local/$filePath"><$resourceHtmlEmbedTag $resourceAttr="${ResourceType.File}" ${TagAttributes.DataPath}="$filePath" ${TagAttributes.Title}="This fancy pdf" $urlAttr="http://api-gateway.ndla-local/$filePath"></div>"""
    val result = readService.addUrlOnResource(content)
    result should equal(expectedResult)
  }

  test("addUrlOnResource adds url attribute on h5p embeds") {
    val h5pPath = "/resource/89734643-4006-4c65-a5de-34989ba7b2c8"
    val content =
      s"""<div><$resourceHtmlEmbedTag $resourceAttr="${ResourceType.H5P}" ${TagAttributes.DataPath}="$h5pPath" ${TagAttributes.Title}="This fancy h5p"><$resourceHtmlEmbedTag $resourceAttr="${ResourceType.H5P}" ${TagAttributes.DataPath}="$h5pPath" ${TagAttributes.Title}="This fancy h5p"></div>"""
    val expectedResult =
      s"""<div><$resourceHtmlEmbedTag $resourceAttr="${ResourceType.H5P}" ${TagAttributes.DataPath}="$h5pPath" ${TagAttributes.Title}="This fancy h5p" $urlAttr="https://h5p.ndla.no$h5pPath"><$resourceHtmlEmbedTag $resourceAttr="${ResourceType.H5P}" ${TagAttributes.DataPath}="$h5pPath" ${TagAttributes.Title}="This fancy h5p" $urlAttr="https://h5p.ndla.no$h5pPath"></div>"""
    val result = readService.addUrlOnResource(content)
    result should equal(expectedResult)
  }
}
