/*
 * Part of NDLA article-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import no.ndla.articleapi.model.api
import no.ndla.articleapi.model.api.ArticleSummaryV2DTO
import no.ndla.articleapi.model.domain.*
import no.ndla.articleapi.model.search.SearchResult
import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.errors.{AccessDeniedException, ValidationException}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.{FrontPageDTO, MenuDTO}
import no.ndla.common.model.domain.{
  ArticleContent,
  ArticleMetaImage,
  ArticleType,
  Availability,
  Description,
  Title,
  VisualElement
}
import no.ndla.network.clients.FeideExtendedUserInfo
import no.ndla.validation.{ResourceType, TagAttribute}
import org.mockito.ArgumentMatchers.{eq as eqTo, *}
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
    s"""<$EmbedTagName $resourceIdAttr="123" $resourceAttr="$imageType"></$EmbedTagName><$EmbedTagName $resourceIdAttr=1234 $resourceAttr="$imageType"></$EmbedTagName>"""

  val content2: String =
    s"""<$EmbedTagName $resourceIdAttr="321" $resourceAttr="$imageType"></$EmbedTagName><$EmbedTagName $resourceIdAttr=4321 $resourceAttr="$imageType"></$EmbedTagName>"""
  val articleContent1: ArticleContent = ArticleContent(content1, "und")

  val expectedArticleContent1: ArticleContent = articleContent1.copy(content =
    s"""<$EmbedTagName $resourceIdAttr="123" $resourceAttr="$imageType" $urlAttr="$externalImageApiUrl/123"></$EmbedTagName><$EmbedTagName $resourceIdAttr="1234" $resourceAttr="$imageType" $urlAttr="$externalImageApiUrl/1234"></$EmbedTagName>"""
  )

  val articleContent2: ArticleContent = ArticleContent(content2, "und")

  override val readService      = new ReadService
  override val converterService = new ConverterService

  override def beforeEach(): Unit = {
    reset(feideApiClient)
  }

  test("withId adds urls and ids on embed resources") {
    val visualElementBefore =
      s"""<$EmbedTagName data-align="" data-alt="" data-caption="" data-resource="image" data-resource_id="1" data-size=""></$EmbedTagName>"""
    val visualElementAfter =
      s"""<$EmbedTagName data-align="" data-alt="" data-caption="" data-resource="image" data-resource_id="1" data-size="" data-url="http://api-gateway.ndla-local/image-api/v2/images/1"></$EmbedTagName>"""
    val article = TestData.sampleArticleWithByNcSa.copy(
      content = Seq(articleContent1),
      visualElement = Seq(VisualElement(visualElementBefore, "nb"))
    )

    when(articleRepository.withId(eqTo(1L))(any)).thenReturn(Some(toArticleRow(article)))
    when(articleRepository.getExternalIdsFromId(any[Long])(any[DBSession])).thenReturn(List("54321"))

    val expectedResult: Try[Cachable[api.ArticleV2DTO]] = Cachable.yes(
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
      content = s"""<$EmbedTagName $resourceAttr="$h5pType" $urlAttr="http://some.h5p.org"></$EmbedTagName>"""
    )
    readService.addUrlOnResource(articleContent3.content) should equal(articleContent3.content)
  }

  test("addUrlOnResource adds url attribute on file embeds") {
    val filePath = "files/lel/fileste.pdf"
    val content  =
      s"""<div data-type="file"><$EmbedTagName $resourceAttr="${ResourceType.File}" ${TagAttribute.DataPath}="$filePath" ${TagAttribute.Title}="This fancy pdf"></$EmbedTagName><$EmbedTagName $resourceAttr="${ResourceType.File}" ${TagAttribute.DataPath}="$filePath" ${TagAttribute.Title}="This fancy pdf"></$EmbedTagName></div>"""
    val expectedResult =
      s"""<div data-type="file"><$EmbedTagName $resourceAttr="${ResourceType.File}" ${TagAttribute.DataPath}="$filePath" ${TagAttribute.Title}="This fancy pdf" $urlAttr="http://api-gateway.ndla-local/$filePath"></$EmbedTagName><$EmbedTagName $resourceAttr="${ResourceType.File}" ${TagAttribute.DataPath}="$filePath" ${TagAttribute.Title}="This fancy pdf" $urlAttr="http://api-gateway.ndla-local/$filePath"></$EmbedTagName></div>"""
    val result = readService.addUrlOnResource(content)
    result should equal(expectedResult)
  }

  test("addIdAndUrlOnResource adds urls on all content translations in an article") {
    val article =
      TestData.sampleArticleWithByNcSa.copy(content = Seq(articleContent1, articleContent2), visualElement = Seq.empty)
    val article1ExpectedResult = articleContent1.copy(content =
      s"""<$EmbedTagName $resourceIdAttr="123" $resourceAttr="$imageType" $urlAttr="$externalImageApiUrl/123"></$EmbedTagName><$EmbedTagName $resourceIdAttr="1234" $resourceAttr="$imageType" $urlAttr="$externalImageApiUrl/1234"></$EmbedTagName>"""
    )
    val article2ExpectedResult = articleContent1.copy(content =
      s"""<$EmbedTagName $resourceIdAttr="321" $resourceAttr="$imageType" $urlAttr="$externalImageApiUrl/321"></$EmbedTagName><$EmbedTagName $resourceIdAttr="4321" $resourceAttr="$imageType" $urlAttr="$externalImageApiUrl/4321"></$EmbedTagName>"""
    )

    val result = readService.addUrlsOnEmbedResources(article)
    result should equal(article.copy(content = Seq(article1ExpectedResult, article2ExpectedResult)))
  }

  test("addUrlOnResource adds url attribute on h5p embeds") {
    val h5pPath = "/resource/89734643-4006-4c65-a5de-34989ba7b2c8"
    val content =
      s"""<div><$EmbedTagName $resourceAttr="${ResourceType.H5P}" ${TagAttribute.DataPath}="$h5pPath" ${TagAttribute.Title}="This fancy h5p"></$EmbedTagName><$EmbedTagName $resourceAttr="${ResourceType.H5P}" ${TagAttribute.DataPath}="$h5pPath" ${TagAttribute.Title}="This fancy h5p"></$EmbedTagName></div>"""
    val expectedResult =
      s"""<div><$EmbedTagName $resourceAttr="${ResourceType.H5P}" ${TagAttribute.DataPath}="$h5pPath" ${TagAttribute.Title}="This fancy h5p" $urlAttr="https://h5p-test.ndla.no$h5pPath"></$EmbedTagName><$EmbedTagName $resourceAttr="${ResourceType.H5P}" ${TagAttribute.DataPath}="$h5pPath" ${TagAttribute.Title}="This fancy h5p" $urlAttr="https://h5p-test.ndla.no$h5pPath"></$EmbedTagName></div>"""
    val result = readService.addUrlOnResource(content)
    result should equal(expectedResult)
  }

  test("search should use size of id-list as page-size if defined") {
    val searchMock = mock[SearchResult[ArticleSummaryV2DTO]]
    when(articleSearchService.matchingQuery(any[SearchSettings])).thenReturn(Success(searchMock))
    when(feideApiClient.getFeideExtendedUser(any)).thenReturn(Failure(new AccessDeniedException("not allowed")))

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
    val teacherUser = FeideExtendedUserInfo("", eduPersonAffiliation = Seq("employee"), None, "", None)

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
    val teacherUser = FeideExtendedUserInfo("", eduPersonAffiliation = Seq("student"), None, "", None)

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

  test("That xml is generated correctly for frontpage articles") {
    val date          = NDLADate.now()
    val parentArticle = TestData.sampleDomainArticle.copy(
      id = Some(1),
      title = Seq(Title("Parent title", "nb")),
      metaDescription = Seq(Description("Parent description", "nb")),
      metaImage = Seq(ArticleMetaImage("1000", "alt", "nb")),
      slug = Some("some-slug"),
      published = date
    )

    val article1 = TestData.sampleDomainArticle.copy(
      id = Some(2),
      title = Seq(Title("Article1 title", "nb")),
      metaDescription = Seq(Description("Article1 description", "nb")),
      metaImage = Seq(ArticleMetaImage("1000", "alt", "nb")),
      slug = Some("slug-one"),
      published = date
    )

    val article2 = TestData.sampleDomainArticle.copy(
      id = Some(3),
      title = Seq(Title("Article2 title", "nb")),
      metaDescription = Seq(Description("Article2 description", "nb")),
      metaImage = Seq(),
      slug = Some("slug-two"),
      published = date
    )

    val frontPage = FrontPageDTO(
      100,
      List(MenuDTO(1, List(MenuDTO(2, List.empty, Some(true)), MenuDTO(3, List.empty, Some(true))), Some(false)))
    )

    val rowOne   = Some(ArticleRow(1, 1, 1, Some("some-slug"), Some(parentArticle)))
    val rowTwo   = Some(ArticleRow(2, 2, 2, Some("slug-one"), Some(article1)))
    val rowThree = Some(ArticleRow(3, 3, 3, Some("slug-two"), Some(article2)))

    when(articleRepository.getExternalIdsFromId(any)(any)).thenReturn(List.empty)
    when(frontpageApiClient.getFrontpage).thenReturn(Success(frontPage))
    when(articleRepository.withSlug("some-slug")).thenReturn(rowOne)
    when(articleRepository.withId(eqTo(1L))(any)).thenReturn(rowOne)
    when(articleRepository.withSlug("slug-one")).thenReturn(rowTwo)
    when(articleRepository.withId(eqTo(2L))(any)).thenReturn(rowTwo)
    when(articleRepository.withSlug("slug-two")).thenReturn(rowThree)
    when(articleRepository.withId(eqTo(3L))(any)).thenReturn(rowThree)

    val xml = readService.getArticleFrontpageRSS("some-slug").get.value
    xml should be(
      s"""<?xml version="1.0" encoding="utf-8"?>
        |<rss version="2.0">
        |  <channel>
        |    <title>Parent title</title>
        |    <link>http://localhost:30017/about/some-slug</link>
        |    <description>Parent description</description>
        |    <item>
        |      <title>Article1 title</title>
        |      <description>Article1 description</description>
        |      <link>http://localhost:30017/about/slug-one</link>
        |      <pubDate>${date.asString}</pubDate>
        |      <image>http://api-gateway.ndla-local/image-api/raw/id/1000</image>
        |    </item>
        |    <item>
        |      <title>Article2 title</title>
        |      <description>Article2 description</description>
        |      <link>http://localhost:30017/about/slug-two</link>
        |      <pubDate>${date.asString}</pubDate>
        |    </item>
        |  </channel>
        |</rss>""".stripMargin
    )

  }
}
