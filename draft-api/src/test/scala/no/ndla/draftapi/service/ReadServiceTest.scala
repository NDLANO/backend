/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.service

import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.errors.ValidationException
import no.ndla.common.model.TagAttribute
import no.ndla.common.model.domain.{ArticleContent, Description, VisualElement}
import no.ndla.draftapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.validation.EmbedType
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import scalikejdbc.DBSession

import scala.util.{Failure, Success}

class ReadServiceTest extends UnitSuite with TestEnvironment {
  override implicit lazy val readService: ReadService           = new ReadService
  override implicit lazy val converterService: ConverterService = new ConverterService

  val externalImageApiUrl: String = props.externalApiUrls("image")
  val resourceIdAttr: String      = s"${TagAttribute.DataResource_Id}"
  val resourceAttr: String        = s"${TagAttribute.DataResource}"
  val imageType: String           = s"${EmbedType.Image}"
  val h5pType: String             = s"${EmbedType.H5P}"
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

  test("withId adds urls and ids on embed resources") {
    val visualElementBefore =
      s"""<$EmbedTagName data-align="" data-alt="" data-caption="" data-resource="image" data-resource_id="1" data-size=""></$EmbedTagName>"""
    val visualElementAfter =
      s"""<$EmbedTagName data-align="" data-alt="" data-caption="" data-resource="image" data-resource_id="1" data-size="" data-url="http://api-gateway.ndla-local/image-api/v2/images/1"></$EmbedTagName>"""
    val article = TestData
      .sampleArticleWithByNcSa
      .copy(content = Seq(articleContent1), visualElement = Seq(VisualElement(visualElementBefore, "nb")))

    when(draftRepository.withId(eqTo(1L))(using any)).thenReturn(Success(Some(article)))
    when(draftRepository.getExternalIdsFromId(any[Long])(using any[DBSession])).thenReturn(Success(List("54321")))

    val expectedResult = converterService
      .toApiArticle(
        article.copy(
          content = Seq(expectedArticleContent1),
          visualElement = Seq(VisualElement(visualElementAfter, "nb")),
        ),
        "nb",
      )
      .get
    readService.withId(1, "nb") should equal(Success(expectedResult))
  }

  test("addIdAndUrlOnResource adds an id and url attribute on embed-resoures with a data-resource_id attribute") {
    readService.addUrlOnResource(articleContent1.content) should equal(expectedArticleContent1.content)
  }

  test("addIdAndUrlOnResource adds id but not url on embed resources without a data-resource_id attribute") {
    val articleContent3 = articleContent1.copy(content =
      s"""<$EmbedTagName $resourceAttr="$h5pType" $urlAttr="http://some.h5p.org"></$EmbedTagName>"""
    )
    readService.addUrlOnResource(articleContent3.content) should equal(articleContent3.content)
  }

  test("addIdAndUrlOnResource adds urls on all content translations in an article") {
    val article                = TestData.sampleArticleWithByNcSa.copy(content = Seq(articleContent1, articleContent2))
    val article1ExpectedResult = articleContent1.copy(content =
      s"""<$EmbedTagName $resourceIdAttr="123" $resourceAttr="$imageType" $urlAttr="$externalImageApiUrl/123"></$EmbedTagName><$EmbedTagName $resourceIdAttr="1234" $resourceAttr="$imageType" $urlAttr="$externalImageApiUrl/1234"></$EmbedTagName>"""
    )
    val article2ExpectedResult = articleContent1.copy(content =
      s"""<$EmbedTagName $resourceIdAttr="321" $resourceAttr="$imageType" $urlAttr="$externalImageApiUrl/321"></$EmbedTagName><$EmbedTagName $resourceIdAttr="4321" $resourceAttr="$imageType" $urlAttr="$externalImageApiUrl/4321"></$EmbedTagName>"""
    )

    val result = readService.addUrlsOnEmbedResources(article)
    result should equal(article.copy(content = Seq(article1ExpectedResult, article2ExpectedResult)))
  }

  test("addUrlOnResource adds url attribute on file embeds") {
    val filePath = "files/lel/fileste.pdf"
    val content  =
      s"""<div data-type="file"><$EmbedTagName $resourceAttr="${EmbedType.File}" ${TagAttribute.DataPath}="$filePath" ${TagAttribute.Title}="This fancy pdf"></$EmbedTagName><$EmbedTagName $resourceAttr="${EmbedType.File}" ${TagAttribute.DataPath}="$filePath" ${TagAttribute.Title}="This fancy pdf"></$EmbedTagName></div>"""
    val expectedResult =
      s"""<div data-type="file"><$EmbedTagName $resourceAttr="${EmbedType.File}" ${TagAttribute.DataPath}="$filePath" ${TagAttribute.Title}="This fancy pdf" $urlAttr="http://api-gateway.ndla-local/$filePath"></$EmbedTagName><$EmbedTagName $resourceAttr="${EmbedType.File}" ${TagAttribute.DataPath}="$filePath" ${TagAttribute.Title}="This fancy pdf" $urlAttr="http://api-gateway.ndla-local/$filePath"></$EmbedTagName></div>"""
    val result = readService.addUrlOnResource(content)
    result should equal(expectedResult)
  }

  test("addUrlOnResource adds url attribute on h5p embeds") {
    val h5pPath = "/resource/89734643-4006-4c65-a5de-34989ba7b2c8"
    val content =
      s"""<div><$EmbedTagName $resourceAttr="${EmbedType.H5P}" ${TagAttribute.DataPath}="$h5pPath" ${TagAttribute.Title}="This fancy h5p"></$EmbedTagName><$EmbedTagName $resourceAttr="${EmbedType.H5P}" ${TagAttribute.DataPath}="$h5pPath" ${TagAttribute.Title}="This fancy h5p"></$EmbedTagName></div>"""
    val expectedResult =
      s"""<div><$EmbedTagName $resourceAttr="${EmbedType.H5P}" ${TagAttribute.DataPath}="$h5pPath" ${TagAttribute.Title}="This fancy h5p" $urlAttr="https://h5p-test.ndla.no$h5pPath"></$EmbedTagName><$EmbedTagName $resourceAttr="${EmbedType.H5P}" ${TagAttribute.DataPath}="$h5pPath" ${TagAttribute.Title}="This fancy h5p" $urlAttr="https://h5p-test.ndla.no$h5pPath"></$EmbedTagName></div>"""
    val result = readService.addUrlOnResource(content)
    result should equal(expectedResult)
  }

  test("that getArticlesByIds works as expected") {
    val ids      = List(1L, 2L, 3L)
    val article1 = TestData.sampleDomainArticle.copy(id = Some(1))
    val article2 = TestData.sampleDomainArticle.copy(id = Some(2))
    val article3 = TestData.sampleDomainArticle.copy(id = Some(3))

    when(draftRepository.withIds(any, any, any)(using any)).thenReturn(Success(Seq(article1, article2, article3)))
    when(draftRepository.getExternalIdsFromId(any)(using any)).thenReturn(
      Success(List("")),
      Success(List("")),
      Success(List("")),
    )

    val Success(result) = readService.getArticlesByIds(
      articleIds = ids,
      language = "nb",
      fallback = true,
      page = 1,
      pageSize = 10,
    ): @unchecked
    result.length should be(3)

    verify(draftRepository, times(1)).withIds(any, any, any)(using any)
  }

  test("that getArticlesByIds fails if no ids were given") {
    reset(draftRepository)
    val Failure(result: ValidationException) = readService.getArticlesByIds(
      articleIds = List.empty,
      language = "nb",
      fallback = true,
      page = 1,
      pageSize = 10,
    ): @unchecked
    result.errors.head.message should be("Query parameter 'ids' is missing")

    verify(draftRepository, times(0)).withIds(any, any, any)(using any)
  }

  test("that getArticleRevisionHistory checks for possibility of deleting current revision") {
    val previousDraft  = TestData.sampleDomainArticle.copy(status = TestData.statusWithInProcess)
    val currentDraft   = TestData.sampleDomainArticle.copy(revision = Some(42))
    val publishedDraft = currentDraft.copy(status = TestData.statusWithPublished)
    val articleId      = previousDraft.id.get
    when(draftRepository.getExternalIdsFromId(eqTo(articleId))(using any)).thenReturn(Success(List("123")))

    when(draftRepository.articlesWithId(eqTo(articleId))(using any[DBSession])).thenReturn(
      Success(List(previousDraft, publishedDraft))
    )
    val revisionHistory = readService.getArticleRevisionHistory(articleId, "nb", fallback = true).failIfFailure
    revisionHistory.revisions.map(_.revision) should contain allOf (
      previousDraft.revision.get,
      publishedDraft.revision.get,
    )
    revisionHistory.canDeleteCurrentRevision should be(false)

    when(draftRepository.articlesWithId(eqTo(articleId))(using any[DBSession])).thenReturn(Success(List(previousDraft)))
    readService
      .getArticleRevisionHistory(articleId, "nb", fallback = true)
      .failIfFailure
      .canDeleteCurrentRevision should be(false)

    val partialPublishDraft =
      publishedDraft.copy(revision = Some(84), metaDescription = Seq(Description("new meta", "nb")))
    when(draftRepository.articlesWithId(eqTo(articleId))(using any[DBSession])).thenReturn(
      Success(List(publishedDraft, partialPublishDraft))
    )
    readService
      .getArticleRevisionHistory(articleId, "nb", fallback = true)
      .failIfFailure
      .canDeleteCurrentRevision should be(false)

    when(draftRepository.articlesWithId(eqTo(articleId))(using any[DBSession])).thenReturn(
      Success(List(previousDraft, currentDraft))
    )
    readService
      .getArticleRevisionHistory(articleId, "nb", fallback = true)
      .failIfFailure
      .canDeleteCurrentRevision should be(true)
  }
}
