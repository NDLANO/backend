/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service

import no.ndla.common.errors.{ValidationException, ValidationMessage}
import no.ndla.common.model.{domain => common}
import no.ndla.common.model.domain.draft.ArticleStatus._
import no.ndla.draftapi.integration.{ConceptStatus, DraftConcept, SearchHit, Title}
import no.ndla.draftapi.model.api
import no.ndla.draftapi.{TestData, TestEnvironment, UnitSuite}
import org.mockito.ArgumentCaptor
import org.mockito.invocation.InvocationOnMock
import scalikejdbc.DBSession

import java.time.LocalDateTime
import scala.collection.immutable.Seq
import scala.util.{Failure, Success, Try}

class StateTransitionRulesTest extends UnitSuite with TestEnvironment {
  import StateTransitionRules.doTransitionWithoutSideEffect

  val DraftStatus                      = common.Status(DRAFT, Set(QUALITY_ASSURED))
  val DraftWithPublishedStatus         = common.Status(DRAFT, Set(IMPORTED, PUBLISHED))
  val PublishedStatus                  = common.Status(PUBLISHED, Set(IMPORTED))
  val UserTestStatus                   = common.Status(USER_TEST, Set(PROPOSAL, IMPORTED))
  val AwaitingUnpublishStatus          = common.Status(AWAITING_UNPUBLISHING, Set.empty)
  val UnpublishedStatus                = common.Status(UNPUBLISHED, Set.empty)
  val ProposalStatus                   = common.Status(PROPOSAL, Set.empty)
  val ArchivedStatus                   = common.Status(ARCHIVED, Set(PUBLISHED))
  val DraftArticle: common.draft.Draft = TestData.sampleArticleWithByNcSa.copy(status = DraftStatus)
  val AwaitingUnpublishArticle: common.draft.Draft =
    TestData.sampleArticleWithByNcSa.copy(status = AwaitingUnpublishStatus)
  val UnpublishedArticle: common.draft.Draft = TestData.sampleArticleWithByNcSa.copy(status = UnpublishedStatus)

  test("doTransition should succeed when performing a legal transition") {
    val expected = common.Status(PUBLISHED, Set.empty)
    val (Success(res), _) =
      doTransitionWithoutSideEffect(DraftArticle, PUBLISHED, TestData.userWithAdminAccess, false)

    res.status should equal(expected)
  }

  test("doTransition should keep some states when performing a legal transition") {
    val expected = common.Status(USER_TEST, Set(PROPOSAL, IMPORTED))
    val (Success(res), _) =
      doTransitionWithoutSideEffect(
        DraftArticle.copy(status = UserTestStatus),
        USER_TEST,
        TestData.userWithPublishAccess,
        false
      )
    res.status should equal(expected)

    val expected2 = common.Status(DRAFT, Set(IMPORTED, PUBLISHED))
    val (Success(res2), _) =
      doTransitionWithoutSideEffect(
        DraftArticle.copy(status = PublishedStatus),
        DRAFT,
        TestData.userWithPublishAccess,
        false
      )
    res2.status should equal(expected2)

  }

  test("doTransition every state change to Archived should succeed") {
    val expected1 = common.Status(ARCHIVED, Set(IMPORTED))
    val (Success(res1), _) =
      doTransitionWithoutSideEffect(
        DraftArticle.copy(status = PublishedStatus),
        ARCHIVED,
        TestData.userWithPublishAccess,
        false
      )
    res1.status should equal(expected1)

    val expected2 = common.Status(ARCHIVED, Set.empty)
    val (Success(res2), _) =
      doTransitionWithoutSideEffect(
        DraftArticle.copy(status = UnpublishedStatus),
        ARCHIVED,
        TestData.userWithPublishAccess,
        false
      )
    res2.status should equal(expected2)

    val expected3 = common.Status(ARCHIVED, Set.empty)
    val (Success(res3), _) =
      doTransitionWithoutSideEffect(
        DraftArticle.copy(status = ProposalStatus),
        ARCHIVED,
        TestData.userWithPublishAccess,
        false
      )
    res3.status should equal(expected3)

    val expected4 = common.Status(ARCHIVED, Set(IMPORTED))
    val (Success(res4), _) =
      doTransitionWithoutSideEffect(
        DraftArticle.copy(status = UserTestStatus),
        ARCHIVED,
        TestData.userWithPublishAccess,
        false
      )
    res4.status should equal(expected4)

    val expected5 = common.Status(ARCHIVED, Set.empty)
    val (Success(res5), _) =
      doTransitionWithoutSideEffect(
        DraftArticle.copy(status = DraftStatus),
        ARCHIVED,
        TestData.userWithPublishAccess,
        false
      )
    res5.status should equal(expected5)

    val expected6 = common.Status(ARCHIVED, Set.empty)
    val (Success(res6), _) =
      doTransitionWithoutSideEffect(
        DraftArticle.copy(status = AwaitingUnpublishStatus),
        ARCHIVED,
        TestData.userWithPublishAccess,
        false
      )
    res6.status should equal(expected6)

  }

  test("doTransition should fail when performing an illegal transition") {
    val (res, _) = doTransitionWithoutSideEffect(DraftArticle, QUALITY_ASSURED, TestData.userWithPublishAccess, false)
    res.isFailure should be(true)
  }

  test("doTransition should publish the article when transitioning to PUBLISHED") {
    val expectedStatus  = common.Status(PUBLISHED, Set.empty)
    val editorNotes     = Seq(common.EditorNote("Status endret", "unit_test", expectedStatus, LocalDateTime.now()))
    val expectedArticle = AwaitingUnpublishArticle.copy(status = expectedStatus, notes = editorNotes)
    when(draftRepository.getExternalIdsFromId(any[Long])(any[DBSession])).thenReturn(List("1234"))
    when(converterService.getEmbeddedConceptIds(any[common.draft.Draft])).thenReturn(Seq.empty)
    when(converterService.getEmbeddedH5PPaths(any[common.draft.Draft])).thenReturn(Seq.empty)
    when(conceptApiClient.publishConceptsIfToPublishing(any[Seq[Long]]))
      .thenAnswer((i: InvocationOnMock) => {
        val ids = i.getArgument[Seq[Long]](0)
        ids.map(id => Try(DraftConcept(id, ConceptStatus("DRAFT"))))
      })
    when(h5pApiClient.publishH5Ps(Seq.empty)).thenReturn(Success(()))

    when(
      articleApiClient
        .updateArticle(
          eqTo(DraftArticle.id.get),
          any[common.draft.Draft],
          eqTo(List("1234")),
          eqTo(false),
          eqTo(true)
        )
    )
      .thenReturn(Success(expectedArticle))

    val (Success(res), sideEffect) =
      doTransitionWithoutSideEffect(DraftArticle, PUBLISHED, TestData.userWithAdminAccess, false)
    sideEffect.map(sf => sf(res, false).get.status should equal(expectedStatus))

    val captor = ArgumentCaptor.forClass(classOf[common.draft.Draft])
    verify(articleApiClient, times(1))
      .updateArticle(eqTo(DraftArticle.id.get), captor.capture(), eqTo(List("1234")), eqTo(false), eqTo(true))

    val argumentArticle: common.draft.Draft = captor.getValue
    val argumentArticleWithNotes            = argumentArticle.copy(notes = editorNotes)
    argumentArticleWithNotes should equal(expectedArticle)
  }

  test("doTransition should unpublish the article when transitioning to UNPUBLISHED") {
    val expectedStatus  = common.Status(UNPUBLISHED, Set.empty)
    val editorNotes     = Seq(common.EditorNote("Status endret", "unit_test", expectedStatus, LocalDateTime.now()))
    val expectedArticle = AwaitingUnpublishArticle.copy(status = expectedStatus, notes = editorNotes)

    when(learningpathApiClient.getLearningpathsWithPaths(Seq.empty)).thenReturn(Success(Seq.empty))
    when(searchApiClient.draftsWhereUsed(any[Long])).thenReturn(Seq.empty)
    when(searchApiClient.publishedWhereUsed(any[Long])).thenReturn(Seq.empty)
    when(taxonomyApiClient.queryResource(any[Long])).thenReturn(Success(List.empty))
    when(taxonomyApiClient.queryTopic(any[Long])).thenReturn(Success(List.empty))
    when(articleApiClient.unpublishArticle(any[common.draft.Draft])).thenReturn(Success(expectedArticle))

    val (Success(res), sideEffect) =
      doTransitionWithoutSideEffect(AwaitingUnpublishArticle, UNPUBLISHED, TestData.userWithAdminAccess, false)
    sideEffect.map(sf => sf(res, false).get.status should equal(expectedStatus))

    val captor = ArgumentCaptor.forClass(classOf[common.draft.Draft])

    verify(articleApiClient, times(1))
      .unpublishArticle(captor.capture())

    val argumentArticle: common.draft.Draft = captor.getValue
    val argumentArticleWithNotes            = argumentArticle.copy(notes = editorNotes)
    argumentArticleWithNotes should equal(expectedArticle)
  }

  test("doTransition should not remove article from search when transitioning to ARCHIVED") {
    val expectedStatus = common.Status(ARCHIVED, Set.empty)

    when(articleIndexService.deleteDocument(UnpublishedArticle.id.get)).thenReturn(Success(UnpublishedArticle.id.get))

    val (Success(res), sideEffect) =
      doTransitionWithoutSideEffect(UnpublishedArticle, ARCHIVED, TestData.userWithPublishAccess, false)
    sideEffect.map(sf => sf(res, false).get.status should equal(expectedStatus))

    verify(articleIndexService, times(0))
      .deleteDocument(UnpublishedArticle.id.get)
  }

  test("user without required roles should not be permitted to perform the status transition") {
    val proposalArticle = TestData.sampleArticleWithByNcSa.copy(status = ProposalStatus)
    val (Failure(ex: IllegalStatusStateTransition), _) =
      doTransitionWithoutSideEffect(DraftArticle, PUBLISHED, TestData.userWithWriteAccess, false)
    ex.getMessage should equal("Cannot go to PUBLISHED when article is DRAFT")

    val (Failure(ex2: IllegalStatusStateTransition), _) =
      doTransitionWithoutSideEffect(proposalArticle, QUEUED_FOR_PUBLISHING, TestData.userWithWriteAccess, false)
    ex2.getMessage should equal("Cannot go to QUEUED_FOR_PUBLISHING when article is PROPOSAL")
  }

  test("PUBLISHED should be removed when transitioning to UNPUBLISHED") {
    val expected         = common.Status(UNPUBLISHED, Set())
    val publishedArticle = DraftArticle.copy(status = common.Status(current = PUBLISHED, other = Set()))
    val (Success(res), _) =
      doTransitionWithoutSideEffect(publishedArticle, UNPUBLISHED, TestData.userWithAdminAccess, false)

    res.status should equal(expected)
  }

  test("unpublishArticle should fail if article is used in a learningstep") {
    val articleId: Long = 7
    val article         = TestData.sampleDomainArticle.copy(id = Some(articleId))
    val learningPath    = TestData.sampleLearningPath
    when(learningpathApiClient.getLearningpathsWithPaths(any[Seq[String]]))
      .thenReturn(Success(Seq(learningPath)))

    val res = StateTransitionRules.unpublishArticle(article, false)
    res.isFailure should be(true)
  }

  test("unpublishArticle should fail if article is used in another article") {
    val articleId: Long = 7
    val article         = TestData.sampleDomainArticle.copy(id = Some(articleId))
    when(taxonomyApiClient.queryResource(articleId)).thenReturn(Success(List.empty))
    when(taxonomyApiClient.queryTopic(articleId)).thenReturn(Success(List.empty))
    when(learningpathApiClient.getLearningpathsWithPaths(any[Seq[String]]))
      .thenReturn(Success(Seq.empty))
    when(searchApiClient.draftsWhereUsed(any[Long])).thenReturn(Seq(SearchHit(1, Title("Title", "nb"))))
    when(searchApiClient.publishedWhereUsed(any[Long])).thenReturn(Seq(SearchHit(1, Title("Title", "nb"))))

    val Failure(res: ValidationException) = StateTransitionRules.checkIfArticleIsInUse(article, false)
    res.errors should equal(
      Seq(
        ValidationMessage("status.current", "Article is in use in these draft(s) 1 (Title)"),
        ValidationMessage("status.current", "Article is in use in these published article(s) 1 (Title)")
      )
    )
  }

  test("unpublishArticle should fail if article is used in a learningstep with a taxonomy-url") {
    val articleId: Long = 7
    val externalId      = "169243"
    val article         = TestData.sampleDomainArticle.copy(id = Some(articleId))
    val learningPath    = TestData.sampleLearningPath
    when(
      learningpathApiClient
        .getLearningpathsWithPaths(
          Seq(s"/unknown/subjects/subject:15/topic:1:166611/topic:1:182229/resource:1:$externalId")
        )
    )
      .thenReturn(Success(Seq(learningPath)))
    when(draftRepository.getIdFromExternalId(any[String])(any[DBSession])).thenReturn(Some(articleId.toLong))

    val res = StateTransitionRules.unpublishArticle(article, false)
    res.isFailure should be(true)
  }

  test("unpublishArticle should succeed if article is not used in a learningstep") {
    reset(articleApiClient, taxonomyApiClient, learningpathApiClient)
    val articleId = 7
    val article   = TestData.sampleDomainArticle.copy(id = Some(articleId))
    val paths = Seq(
      s"/article-iframe/*/$articleId",
      s"/article-iframe/*/$articleId/",
      s"/article-iframe/*/$articleId/\\?*",
      s"/article-iframe/*/$articleId\\?*",
      s"/article/$articleId"
    )
    when(learningpathApiClient.getLearningpathsWithPaths(paths))
      .thenReturn(Success(Seq.empty))
    when(articleApiClient.unpublishArticle(article)).thenReturn(Success(article))
    when(taxonomyApiClient.queryResource(articleId)).thenReturn(Success(List.empty))
    when(taxonomyApiClient.queryTopic(articleId)).thenReturn(Success(List.empty))
    when(searchApiClient.draftsWhereUsed(any[Long])).thenReturn(Seq.empty)
    when(searchApiClient.publishedWhereUsed(any[Long])).thenReturn(Seq.empty)

    val res = StateTransitionRules.unpublishArticle(article, false)
    res.isSuccess should be(true)
    verify(articleApiClient, times(1)).unpublishArticle(article)
  }

  test("checkIfArticleIsUsedInLearningStep should fail if article is used in a learningstep") {
    val articleId: Long = 7
    val article         = TestData.sampleDomainArticle.copy(id = Some(articleId))
    val learningPath    = TestData.sampleLearningPath
    val paths = Seq(
      s"/article-iframe/*/$articleId",
      s"/article-iframe/*/$articleId/",
      s"/article-iframe/*/$articleId/\\?*",
      s"/article-iframe/*/$articleId\\?*",
      s"/article/$articleId"
    )
    when(learningpathApiClient.getLearningpathsWithPaths(paths))
      .thenReturn(Success(Seq(learningPath)))
    when(taxonomyApiClient.queryResource(articleId)).thenReturn(Success(List.empty))
    when(taxonomyApiClient.queryTopic(articleId)).thenReturn(Success(List.empty))

    val Failure(res: ValidationException) = StateTransitionRules.checkIfArticleIsInUse(article, false)
    res.errors.head.message should equal("Learningpath(s) 1 (Title) contains a learning step that uses this article")
  }

  test("checkIfArticleIsUsedInLearningStep should fail if article is used in a learningstep with a taxonomy-url") {
    val articleId: Long = 7
    val externalId      = "169243"
    val article         = TestData.sampleDomainArticle.copy(id = Some(articleId))
    val learningPath    = TestData.sampleLearningPath
    when(
      learningpathApiClient.getLearningpathsWithPaths(
        Seq(s"/unknown/subjects/subject:15/topic:1:166611/topic:1:182229/resource:1:$externalId")
      )
    )
      .thenReturn(Success(Seq(learningPath)))
    when(draftRepository.getIdFromExternalId(any[String])(any[DBSession])).thenReturn(Some(articleId.toLong))

    val Failure(res: ValidationException) = StateTransitionRules.checkIfArticleIsInUse(article, false)
    res.errors.head.message should equal("Learningpath(s) 1 (Title) contains a learning step that uses this article")
  }

  test("checkIfArticleIsUsedInLearningStep should succeed if article is not used in a learningstep") {
    reset(articleApiClient, taxonomyApiClient, learningpathApiClient)
    val articleId = 7
    val article   = TestData.sampleDomainArticle.copy(id = Some(articleId))
    val paths = Seq(
      s"/article-iframe/*/$articleId",
      s"/article-iframe/*/$articleId/",
      s"/article-iframe/*/$articleId/\\?*",
      s"/article-iframe/*/$articleId\\?*",
      s"/article/$articleId"
    )
    when(learningpathApiClient.getLearningpathsWithPaths(paths)).thenReturn(Success(Seq.empty))
    when(articleApiClient.unpublishArticle(article)).thenReturn(Success(article))
    when(taxonomyApiClient.queryResource(articleId)).thenReturn(Success(List.empty))
    when(taxonomyApiClient.queryTopic(articleId)).thenReturn(Success(List.empty))

    val res = StateTransitionRules.checkIfArticleIsInUse(article, false)
    res.isSuccess should be(true)
  }

  test("validateArticle should be called when transitioning to QUEUED_FOR_PUBLISHING") {
    val articleId = 7
    val article   = TestData.sampleDomainArticle.copy(id = Some(articleId))
    val status    = common.Status(QUALITY_ASSURED, Set.empty)

    val transitionsToTest = StateTransitionRules.StateTransitions.filter(_.to == QUEUED_FOR_PUBLISHING)
    transitionsToTest.foreach(t =>
      StateTransitionRules
        .doTransition(
          article.copy(status = status.copy(current = t.from)),
          QUEUED_FOR_PUBLISHING,
          TestData.userWithPublishAccess,
          false
        )
        .unsafeRunSync()
    )
    verify(articleApiClient, times(transitionsToTest.size)).validateArticle(any[api.ArticleApiArticle], any[Boolean])
  }

  test("publishArticle should call h5p api") {
    reset(conceptApiClient)
    reset(h5pApiClient)
    reset(articleApiClient)
    val h5pId           = "123-kulid-123"
    val h5pPaths        = Seq(s"/resource/$h5pId")
    val expectedStatus  = common.Status(PUBLISHED, Set.empty)
    val editorNotes     = Seq(common.EditorNote("Status endret", "unit_test", expectedStatus, LocalDateTime.now()))
    val expectedArticle = AwaitingUnpublishArticle.copy(status = expectedStatus, notes = editorNotes)
    when(draftRepository.getExternalIdsFromId(any[Long])(any[DBSession])).thenReturn(List("1234"))
    when(converterService.getEmbeddedConceptIds(any[common.draft.Draft])).thenReturn(Seq.empty)
    when(converterService.getEmbeddedH5PPaths(any[common.draft.Draft])).thenReturn(h5pPaths)
    when(conceptApiClient.publishConceptsIfToPublishing(any[Seq[Long]]))
      .thenAnswer((i: InvocationOnMock) => {
        val ids = i.getArgument[Seq[Long]](0)
        ids.map(id => Try(DraftConcept(id, ConceptStatus("DRAFT"))))
      })
    when(h5pApiClient.publishH5Ps(h5pPaths)).thenReturn(Success(()))

    when(
      articleApiClient
        .updateArticle(
          eqTo(DraftArticle.id.get),
          any[common.draft.Draft],
          eqTo(List("1234")),
          eqTo(false),
          eqTo(true)
        )
    )
      .thenReturn(Success(expectedArticle))

    val (Success(res), sideEffect) =
      doTransitionWithoutSideEffect(DraftArticle, PUBLISHED, TestData.userWithAdminAccess, false)
    sideEffect.map(sf => sf(res, false).get.status should equal(expectedStatus))

    val captor = ArgumentCaptor.forClass(classOf[common.draft.Draft])
    verify(articleApiClient, times(1))
      .updateArticle(eqTo(DraftArticle.id.get), captor.capture(), eqTo(List("1234")), eqTo(false), eqTo(true))

    verify(h5pApiClient, times(1)).publishH5Ps(h5pPaths)

    val argumentArticle: common.draft.Draft = captor.getValue
    val argumentArticleWithNotes            = argumentArticle.copy(notes = editorNotes)
    argumentArticleWithNotes should equal(expectedArticle)
  }

}
