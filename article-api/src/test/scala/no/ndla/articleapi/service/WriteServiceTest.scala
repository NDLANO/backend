/*
 * Part of NDLA article-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import no.ndla.articleapi.{TestEnvironment, UnitSuite}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.article.Article
import no.ndla.network.tapir.auth.TokenUser
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.mockito.invocation.InvocationOnMock
import org.mockito.{ArgumentCaptor, Mockito}
import scalikejdbc.DBSession

import scala.util.{Success, Try}

class WriteServiceTest extends UnitSuite with TestEnvironment {
  override implicit lazy val converterService: ConverterService = new ConverterService

  val today: NDLADate     = NDLADate.now()
  val yesterday: NDLADate = NDLADate.now().minusDays(1)
  val service             = new WriteService()

  val articleId = 13L

  val article: Article = TestData
    .sampleArticleWithPublicDomain
    .copy(id = Some(articleId), created = yesterday, updated = yesterday)

  override def beforeEach(): Unit = {
    Mockito.reset(articleIndexService, articleRepository)

    when(articleRepository.withId(eqTo(articleId))(any)).thenReturn(Option(toArticleRow(article)))
    when(articleIndexService.indexDocument(any[Article])).thenAnswer((invocation: InvocationOnMock) =>
      Try(invocation.getArgument[Article](0))
    )
    when(readService.addUrlsOnEmbedResources(any[Article])).thenAnswer((invocation: InvocationOnMock) =>
      invocation.getArgument[Article](0)
    )
    when(articleRepository.getExternalIdsFromId(any[Long])(using any[DBSession])).thenReturn(List("1234"))
    when(clock.now()).thenReturn(today)
    when(contentValidator.validateArticle(any[Article], any[Boolean])).thenAnswer((invocation: InvocationOnMock) =>
      Success(invocation.getArgument[Article](0))
    )
  }

  test("That updateArticle indexes the updated article") {
    reset(articleIndexService, searchApiClient)

    val articleToUpdate    = TestData.sampleDomainArticle.copy(id = Some(10), updated = yesterday)
    val updatedAndInserted = articleToUpdate.copy(revision = articleToUpdate.revision.map(_ + 1), updated = today)

    when(articleRepository.withId(eqTo(10L))(any)).thenReturn(Some(toArticleRow(articleToUpdate)))
    when(articleRepository.updateArticleFromDraftApi(any[Article], any)(using any[DBSession])).thenReturn(
      Success(updatedAndInserted)
    )

    when(articleIndexService.indexDocument(any[Article])).thenReturn(Success(updatedAndInserted))
    when(searchApiClient.indexDocument(any[String], any[Article], any[Option[TokenUser]])(using any, any, any))
      .thenReturn(updatedAndInserted)

    service.updateArticle(
      articleToUpdate,
      List.empty,
      useImportValidation = false,
      useSoftValidation = false,
      skipValidation = false,
    )()

    val argCap1: ArgumentCaptor[Article] = ArgumentCaptor.forClass(classOf[Article])
    val argCap2: ArgumentCaptor[Article] = ArgumentCaptor.forClass(classOf[Article])

    verify(articleIndexService, times(1)).indexDocument(argCap1.capture())
    verify(searchApiClient, times(1)).indexDocument(any[String], argCap2.capture(), any[Option[TokenUser]])(using
      any,
      any,
      any,
    )

    val captured1 = argCap1.getValue
    captured1.copy(updated = today) should be(updatedAndInserted)

    val captured2 = argCap2.getValue
    captured2.copy(updated = today) should be(updatedAndInserted)
  }

  test("That unpublisArticle removes article from indexes") {
    reset(articleIndexService, searchApiClient)
    val articleIdToUnpublish = 11L

    when(articleRepository.unpublishMaxRevision(any[Long])(using any[DBSession])).thenReturn(
      Success(articleIdToUnpublish)
    )
    when(articleIndexService.deleteDocument(any[Long])).thenReturn(Success(articleIdToUnpublish))
    when(searchApiClient.deleteDocument(any[Long], any[String])).thenReturn(articleIdToUnpublish)

    service.unpublishArticle(articleIdToUnpublish, None)

    verify(articleIndexService, times(1)).deleteDocument(any[Long])
    verify(searchApiClient, times(1)).deleteDocument(any[Long], any[String])
  }

  test("That deleteArticle removes article from indexes") {
    reset(articleIndexService, searchApiClient)
    val articleIdToUnpublish = 11L

    when(articleRepository.deleteMaxRevision(any[Long])(using any[DBSession])).thenReturn(Success(articleIdToUnpublish))
    when(articleIndexService.deleteDocument(any[Long])).thenReturn(Success(articleIdToUnpublish))
    when(searchApiClient.deleteDocument(any[Long], any[String])).thenReturn(articleIdToUnpublish)

    service.deleteArticle(articleIdToUnpublish, None)

    verify(articleIndexService, times(1)).deleteDocument(any[Long])
    verify(searchApiClient, times(1)).deleteDocument(any[Long], any[String])
  }
}
