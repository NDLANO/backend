/*
 * Part of NDLA article-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import no.ndla.articleapi.model.api
import no.ndla.articleapi.model.api.ArticleSummaryV2
import no.ndla.articleapi.model.domain.*
import no.ndla.articleapi.model.search.SearchResult
import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.errors.ValidationException
import no.ndla.common.model.domain.{ArticleContent, ArticleType, Availability, VisualElement}
import no.ndla.network.clients.FeideExtendedUserInfo
import no.ndla.validation.{ResourceType, TagAttribute}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import scalikejdbc.DBSession

import scala.util.{Failure, Success, Try}

class ReadServiceTest extends UnitSuite with TestEnvironment {

  val externalImageApiUrl: String = props.externalApiUrls("image")
  val resourceIdAttr: String      = s"${TagAttribute.DataResource_Id}"
  val resourceAttr: String        = s"${TagAttribute.DataResource}"
  val imageType: String           = s"${ResourceType.Image}"
  val h5pType: String             = s"${ResourceType.H5P}"
  val urlAttr: String             = s"${TagAttribute.DataUrl}"

  val content1: String =
    s"""<$EmbedTagName $resourceIdAttr="123" $resourceAttr="$imageType" /><$EmbedTagName $resourceIdAttr=1234 $resourceAttr="$imageType" />"""

  val content2: String =
    s"""<$EmbedTagName $resourceIdAttr="321" $resourceAttr="$imageType" /><$EmbedTagName $resourceIdAttr=4321 $resourceAttr="$imageType" />"""
  val articleContent1: ArticleContent = ArticleContent(content1, "und")

  val expectedArticleContent1: ArticleContent = articleContent1.copy(content =
    s"""<$EmbedTagName $resourceIdAttr="123" $resourceAttr="$imageType" $urlAttr="$externalImageApiUrl/123" /><$EmbedTagName $resourceIdAttr="1234" $resourceAttr="$imageType" $urlAttr="$externalImageApiUrl/1234" />"""
  )

  val articleContent2: ArticleContent = ArticleContent(content2, "und")

  override val readService      = new ReadService
  override val converterService = new ConverterService

  override def beforeEach(): Unit = {
    reset(feideApiClient)
  }

  test("withId adds urls and ids on embed resources") {
    val visualElementBefore =
      s"""<$EmbedTagName data-align="" data-alt="" data-caption="" data-resource="image" data-resource_id="1" data-size="" />"""
    val visualElementAfter =
      s"""<$EmbedTagName data-align="" data-alt="" data-caption="" data-resource="image" data-resource_id="1" data-size="" data-url="http://api-gateway.ndla-local/image-api/v2/images/1" />"""
    val article = TestData.sampleArticleWithByNcSa.copy(
      content = Seq(articleContent1),
      visualElement = Seq(VisualElement(visualElementBefore, "nb"))
    )

    when(articleRepository.withId(1)).thenReturn(Some(toArticleRow(article)))
    when(articleRepository.getExternalIdsFromId(any[Long])(any[DBSession])).thenReturn(List("54321"))

    val expectedResult: Try[Cachable[api.ArticleV2]] = Cachable.yes(
      converterService.toApiArticleV2(
        article
          .copy(content = Seq(expectedArticleContent1), visualElement = Seq(VisualElement(visualElementAfter, "nb"))),
        "nb",
        false
      )
    )
    readService.withIdV2(1, "nb", fallback = false, None, None) should equal(expectedResult)
  }

  test("addIdAndUrlOnResource adds an id and url attribute on embed-resoures with a data-resource_id attribute") {
    readService.addUrlOnResource(articleContent1.content) should equal(expectedArticleContent1.content)
  }

  test("addIdAndUrlOnResource adds id but not url on embed resources without a data-resource_id attribute") {
    val articleContent3 = articleContent1.copy(
      content = s"""<$EmbedTagName $resourceAttr="$h5pType" $urlAttr="http://some.h5p.org" />"""
    )
    readService.addUrlOnResource(articleContent3.content) should equal(articleContent3.content)
  }

  test("addUrlOnResource adds url attribute on file embeds") {
    val filePath = "files/lel/fileste.pdf"
    val content =
      s"""<div data-type="file"><$EmbedTagName $resourceAttr="${ResourceType.File}" ${TagAttribute.DataPath}="$filePath" ${TagAttribute.Title}="This fancy pdf" /><$EmbedTagName $resourceAttr="${ResourceType.File}" ${TagAttribute.DataPath}="$filePath" ${TagAttribute.Title}="This fancy pdf" /></div>"""
    val expectedResult =
      s"""<div data-type="file"><$EmbedTagName $resourceAttr="${ResourceType.File}" ${TagAttribute.DataPath}="$filePath" ${TagAttribute.Title}="This fancy pdf" $urlAttr="http://api-gateway.ndla-local/$filePath" /><$EmbedTagName $resourceAttr="${ResourceType.File}" ${TagAttribute.DataPath}="$filePath" ${TagAttribute.Title}="This fancy pdf" $urlAttr="http://api-gateway.ndla-local/$filePath" /></div>"""
    val result = readService.addUrlOnResource(content)
    result should equal(expectedResult)
  }

  test("addIdAndUrlOnResource adds urls on all content translations in an article") {
    val article =
      TestData.sampleArticleWithByNcSa.copy(content = Seq(articleContent1, articleContent2), visualElement = Seq.empty)
    val article1ExpectedResult = articleContent1.copy(content =
      s"""<$EmbedTagName $resourceIdAttr="123" $resourceAttr="$imageType" $urlAttr="$externalImageApiUrl/123" /><$EmbedTagName $resourceIdAttr="1234" $resourceAttr="$imageType" $urlAttr="$externalImageApiUrl/1234" />"""
    )
    val article2ExpectedResult = articleContent1.copy(content =
      s"""<$EmbedTagName $resourceIdAttr="321" $resourceAttr="$imageType" $urlAttr="$externalImageApiUrl/321" /><$EmbedTagName $resourceIdAttr="4321" $resourceAttr="$imageType" $urlAttr="$externalImageApiUrl/4321" />"""
    )

    val result = readService.addUrlsOnEmbedResources(article)
    result should equal(article.copy(content = Seq(article1ExpectedResult, article2ExpectedResult)))
  }

  test("addUrlOnResource adds url attribute on h5p embeds") {
    val h5pPath = "/resource/89734643-4006-4c65-a5de-34989ba7b2c8"
    val content =
      s"""<div><$EmbedTagName $resourceAttr="${ResourceType.H5P}" ${TagAttribute.DataPath}="$h5pPath" ${TagAttribute.Title}="This fancy h5p" /><$EmbedTagName $resourceAttr="${ResourceType.H5P}" ${TagAttribute.DataPath}="$h5pPath" ${TagAttribute.Title}="This fancy h5p" /></div>"""
    val expectedResult =
      s"""<div><$EmbedTagName $resourceAttr="${ResourceType.H5P}" ${TagAttribute.DataPath}="$h5pPath" ${TagAttribute.Title}="This fancy h5p" $urlAttr="https://h5p.ndla.no$h5pPath" /><$EmbedTagName $resourceAttr="${ResourceType.H5P}" ${TagAttribute.DataPath}="$h5pPath" ${TagAttribute.Title}="This fancy h5p" $urlAttr="https://h5p.ndla.no$h5pPath" /></div>"""
    val result = readService.addUrlOnResource(content)
    result should equal(expectedResult)
  }

  test("search should use size of id-list as page-size if defined") {
    val searchMock = mock[SearchResult[ArticleSummaryV2]]
    when(articleSearchService.matchingQuery(any[SearchSettings])).thenReturn(Success(searchMock))
    when(feideApiClient.getFeideExtendedUser(any)).thenReturn(Failure(new RuntimeException))

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
      props.DefaultLanguage,
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

  test("that getArticlesByIds doesn't perform filter when every article has availability status everyone") {
    val feideId  = "asd"
    val ids      = List(1L, 2L, 3L)
    val article1 = TestData.sampleDomainArticle.copy(id = Some(1), availability = Availability.everyone)
    val article2 = TestData.sampleDomainArticle.copy(id = Some(2), availability = Availability.everyone)
    val article3 = TestData.sampleDomainArticle.copy(id = Some(3), availability = Availability.everyone)

    when(articleRepository.withIds(any, any, any)(any))
      .thenReturn(Seq(toArticleRow(article1), toArticleRow(article2), toArticleRow(article3)))
    when(articleRepository.getExternalIdsFromId(any)(any)).thenReturn(List(""), List(""), List(""))

    val Success(result) =
      readService.getArticlesByIds(
        articleIds = ids,
        language = "nb",
        fallback = true,
        page = 1,
        pageSize = 10,
        feideAccessToken = None
      )
    result.length should be(3)

    verify(feideApiClient, times(0)).getFeideExtendedUser(Some(feideId))
  }

  test("that getArticlesByIds performs filter and returns articles that can only be seen by teacher") {
    val feideId     = "asd"
    val ids         = List(1L, 2L, 3L)
    val article1    = TestData.sampleDomainArticle.copy(id = Some(1), availability = Availability.everyone)
    val article2    = TestData.sampleDomainArticle.copy(id = Some(2), availability = Availability.everyone)
    val article3    = TestData.sampleDomainArticle.copy(id = Some(3), availability = Availability.teacher)
    val teacherUser = FeideExtendedUserInfo("", eduPersonAffiliation = Seq("employee"), "", Seq.empty)

    when(feideApiClient.getFeideExtendedUser(any)).thenReturn(Success(teacherUser))
    when(articleRepository.withIds(any, any, any)(any))
      .thenReturn(Seq(toArticleRow(article1), toArticleRow(article2), toArticleRow(article3)))
    when(articleRepository.getExternalIdsFromId(any)(any)).thenReturn(List(""), List(""), List(""))

    val Success(result) =
      readService.getArticlesByIds(
        articleIds = ids,
        language = "nb",
        fallback = true,
        page = 1,
        pageSize = 10,
        feideAccessToken = Some(feideId)
      )
    result.length should be(3)

    verify(feideApiClient, times(1)).getFeideExtendedUser(Some(feideId))
  }

  test("that getArticlesByIds performs filter and returns articles that can only be seen by everyone") {
    val feideId     = "asd"
    val ids         = List(1L, 2L, 3L)
    val article1    = TestData.sampleDomainArticle.copy(id = Some(1), availability = Availability.everyone)
    val article2    = TestData.sampleDomainArticle.copy(id = Some(2), availability = Availability.everyone)
    val article3    = TestData.sampleDomainArticle.copy(id = Some(3), availability = Availability.teacher)
    val teacherUser = FeideExtendedUserInfo("", eduPersonAffiliation = Seq("student"), "", Seq.empty)

    when(feideApiClient.getFeideExtendedUser(any)).thenReturn(Success(teacherUser))
    when(articleRepository.withIds(any, any, any)(any))
      .thenReturn(Seq(toArticleRow(article1), toArticleRow(article2), toArticleRow(article3)))
    when(articleRepository.getExternalIdsFromId(any)(any)).thenReturn(List(""), List(""), List(""))

    val Success(result) =
      readService.getArticlesByIds(
        articleIds = ids,
        language = "nb",
        fallback = true,
        page = 1,
        pageSize = 10,
        feideAccessToken = Some(feideId)
      )
    result.length should be(2)
    result.map(res => res.availability).contains("teacher") should be(false)

    verify(feideApiClient, times(1)).getFeideExtendedUser(Some(feideId))
  }

  test("that getArticlesByIds performs filter if feideAccessToken is not set") {
    val feideId  = "asd"
    val ids      = List(1L, 2L, 3L)
    val article1 = TestData.sampleDomainArticle.copy(id = Some(1), availability = Availability.everyone)
    val article2 = TestData.sampleDomainArticle.copy(id = Some(2), availability = Availability.everyone)
    val article3 = TestData.sampleDomainArticle.copy(id = Some(3), availability = Availability.teacher)

    when(articleRepository.withIds(any, any, any)(any))
      .thenReturn(Seq(toArticleRow(article1), toArticleRow(article2), toArticleRow(article3)))
    when(articleRepository.getExternalIdsFromId(any)(any)).thenReturn(List(""), List(""), List(""))
    when(feideApiClient.getFeideExtendedUser(any)).thenReturn(Failure(new RuntimeException))

    val Success(result) =
      readService.getArticlesByIds(
        articleIds = ids,
        language = "nb",
        fallback = true,
        page = 1,
        pageSize = 10,
        feideAccessToken = None
      )
    result.length should be(2)
    result.map(res => res.availability).contains("teacher") should be(false)

    verify(feideApiClient, times(0)).getFeideAccessTokenOrFail(Some(feideId))
  }

  test("that getArticlesByIds fails if no ids were given") {
    reset(articleRepository)
    val Failure(result: ValidationException) =
      readService.getArticlesByIds(
        articleIds = List.empty,
        language = "nb",
        fallback = true,
        page = 1,
        pageSize = 10,
        feideAccessToken = None
      )
    result.errors.head.message should be("Query parameter 'ids' is missing")

    verify(articleRepository, times(0)).withIds(any, any, any)(any)
  }
}
