/*
 * Part of NDLA draft-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.service

import no.ndla.common.errors.{ValidationException, ValidationMessage}
import no.ndla.common.model.api.search.{MultiSearchSummaryDTO, TitleWithHtmlDTO}
import no.ndla.common.model.domain.{Priority, Responsible, Status}
import no.ndla.common.model.domain.draft.Draft
import no.ndla.common.model.domain.draft.DraftStatus.*
import no.ndla.common.model.domain.language.OptLanguageFields
import no.ndla.common.model.{NDLADate, domain as common}
import no.ndla.draftapi.model.domain.StateTransition
import no.ndla.draftapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.mapping.License.CC_BY
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.mockito.invocation.InvocationOnMock
import scalikejdbc.DBSession

import scala.util.{Failure, Success}

class StateTransitionRulesTest extends UnitSuite with TestEnvironment {
  import StateTransitionRules.doTransitionWithoutSideEffect

  val PlannedStatus: Status              = common.Status(PLANNED, Set(END_CONTROL))
  val PlannedWithPublishedStatus: Status = common.Status(PLANNED, Set(PUBLISHED))
  val PublishedStatus: Status            = common.Status(PUBLISHED, Set.empty)
  val ExternalReviewStatus: Status       = common.Status(EXTERNAL_REVIEW, Set(IN_PROGRESS))
  val UnpublishedStatus: Status          = common.Status(UNPUBLISHED, Set.empty)
  val InProcessStatus: Status            = common.Status(IN_PROGRESS, Set.empty)
  val ArchivedStatus: Status             = common.Status(ARCHIVED, Set(PUBLISHED))
  val responsible: Responsible           = common.Responsible("someid", TestData.today)
  val InProcessArticle: Draft =
    TestData.sampleArticleWithByNcSa.copy(status = InProcessStatus, responsible = Some(responsible))
  val PublishedArticle: Draft =
    TestData.sampleArticleWithByNcSa.copy(status = PublishedStatus, responsible = Some(responsible))
  val UnpublishedArticle: Draft =
    TestData.sampleArticleWithByNcSa.copy(status = UnpublishedStatus, responsible = Some(responsible))

  test("doTransition should succeed when performing a legal transition") {
    val expected = common.Status(PUBLISHED, Set.empty)
    val (Success(res), _) =
      doTransitionWithoutSideEffect(InProcessArticle, PUBLISHED, TestData.userWithAdminAccess, false)

    res.status should equal(expected)
  }

  test("doTransition should keep some states when performing a legal transition") {
    val expected = common.Status(EXTERNAL_REVIEW, Set(IN_PROGRESS))
    val (Success(res), _) =
      doTransitionWithoutSideEffect(
        InProcessArticle.copy(status = ExternalReviewStatus),
        EXTERNAL_REVIEW,
        TestData.userWithPublishAccess,
        false
      )
    res.status should equal(expected)

    val expected2 = common.Status(IN_PROGRESS, Set(PUBLISHED))
    val (Success(res2), _) =
      doTransitionWithoutSideEffect(
        InProcessArticle.copy(status = PublishedStatus),
        IN_PROGRESS,
        TestData.userWithPublishAccess,
        false
      )
    res2.status should equal(expected2)

  }

  test("doTransition every state change to Archived should succeed") {
    val expected1 = common.Status(ARCHIVED, Set.empty)
    val (Success(res1), _) =
      doTransitionWithoutSideEffect(
        InProcessArticle.copy(status = PublishedStatus),
        ARCHIVED,
        TestData.userWithPublishAccess,
        false
      )
    res1.status should equal(expected1)

    val expected2 = common.Status(ARCHIVED, Set.empty)
    val (Success(res2), _) =
      doTransitionWithoutSideEffect(
        InProcessArticle.copy(status = UnpublishedStatus),
        ARCHIVED,
        TestData.userWithPublishAccess,
        false
      )
    res2.status should equal(expected2)

    val expected3 = common.Status(ARCHIVED, Set.empty)
    val (Success(res3), _) =
      doTransitionWithoutSideEffect(
        InProcessArticle.copy(status = InProcessStatus),
        ARCHIVED,
        TestData.userWithPublishAccess,
        false
      )
    res3.status should equal(expected3)

    val expected4 = common.Status(ARCHIVED, Set.empty)
    val (Success(res4), _) =
      doTransitionWithoutSideEffect(
        InProcessArticle.copy(status = ExternalReviewStatus),
        ARCHIVED,
        TestData.userWithPublishAccess,
        false
      )
    res4.status should equal(expected4)

    val expected5 = common.Status(ARCHIVED, Set.empty)
    val (Success(res5), _) =
      doTransitionWithoutSideEffect(
        InProcessArticle.copy(status = PlannedStatus),
        ARCHIVED,
        TestData.userWithPublishAccess,
        false
      )
    res5.status should equal(expected5)

    val expected6 = common.Status(ARCHIVED, Set.empty)
    val (Success(res6), _) =
      doTransitionWithoutSideEffect(
        InProcessArticle.copy(status = PublishedStatus),
        ARCHIVED,
        TestData.userWithPublishAccess,
        false
      )
    res6.status should equal(expected6)

  }

  test("doTransition should fail when performing an illegal transition") {
    val (res, _) = doTransitionWithoutSideEffect(InProcessArticle, END_CONTROL, TestData.userWithPublishAccess, false)
    res.isFailure should be(true)
  }

  test("doTransition should publish the article when transitioning to PUBLISHED") {
    val expectedStatus  = common.Status(PUBLISHED, Set.empty)
    val editorNotes     = Seq(common.EditorNote("Status endret", "unit_test", expectedStatus, NDLADate.now()))
    val expectedArticle = InProcessArticle.copy(status = expectedStatus, notes = editorNotes)
    when(draftRepository.getExternalIdsFromId(any[Long])(any[DBSession])).thenReturn(List("1234"))
    when(converterService.getEmbeddedConceptIds(any[Draft])).thenReturn(Seq.empty)
    when(converterService.getEmbeddedH5PPaths(any[Draft])).thenReturn(Seq.empty)
    when(h5pApiClient.publishH5Ps(eqTo(Seq.empty), any)).thenReturn(Success(()))

    when(
      articleApiClient
        .updateArticle(
          eqTo(InProcessArticle.id.get),
          any[Draft],
          eqTo(List("1234")),
          eqTo(false),
          eqTo(true),
          any
        )
    )
      .thenReturn(Success(expectedArticle))

    val (Success(res), sideEffect) =
      doTransitionWithoutSideEffect(InProcessArticle, PUBLISHED, TestData.userWithAdminAccess, false)
    sideEffect.map(sf => sf.run(res, false, TestData.userWithAdminAccess).get.status should equal(expectedStatus))

    val captor = ArgumentCaptor.forClass(classOf[Draft])
    verify(articleApiClient, times(1))
      .updateArticle(eqTo(InProcessArticle.id.get), captor.capture(), eqTo(List("1234")), eqTo(false), eqTo(true), any)

    val argumentArticle: Draft   = captor.getValue
    val argumentArticleWithNotes = argumentArticle.copy(notes = editorNotes)
    argumentArticleWithNotes should equal(expectedArticle)
  }

  test("doTransition should unpublish the article when transitioning to UNPUBLISHED") {
    val expectedStatus  = common.Status(UNPUBLISHED, Set.empty)
    val editorNotes     = Seq(common.EditorNote("Status endret", "unit_test", expectedStatus, NDLADate.now()))
    val expectedArticle = InProcessArticle.copy(status = expectedStatus, notes = editorNotes)

    when(learningpathApiClient.getLearningpathsWithId(any[Long], any)).thenReturn(Success(Seq.empty))
    when(searchApiClient.publishedWhereUsed(any[Long], any)).thenReturn(Seq.empty)
    when(taxonomyApiClient.queryNodes(any[Long])).thenReturn(Success(List.empty))
    when(articleApiClient.unpublishArticle(any[Draft], any)).thenReturn(Success(expectedArticle))

    val (Success(res), sideEffect) =
      doTransitionWithoutSideEffect(PublishedArticle, UNPUBLISHED, TestData.userWithAdminAccess, false)
    sideEffect.map(sf => sf.run(res, false, TestData.userWithAdminAccess).get.status should equal(expectedStatus))

    val captor = ArgumentCaptor.forClass(classOf[Draft])

    verify(articleApiClient, times(1))
      .unpublishArticle(captor.capture(), any)

    val argumentArticle: Draft   = captor.getValue
    val argumentArticleWithNotes = argumentArticle.copy(notes = editorNotes)
    argumentArticleWithNotes should equal(expectedArticle)
  }

  test("doTransition should not remove article from search when transitioning to ARCHIVED") {
    val expectedStatus = common.Status(ARCHIVED, Set.empty)

    when(articleIndexService.deleteDocument(UnpublishedArticle.id.get)).thenReturn(Success(UnpublishedArticle.id.get))

    val (Success(res), sideEffect) =
      doTransitionWithoutSideEffect(UnpublishedArticle, ARCHIVED, TestData.userWithPublishAccess, false)
    sideEffect.map(sf => sf.run(res, false, TestData.userWithAdminAccess).get.status should equal(expectedStatus))

    verify(articleIndexService, times(0))
      .deleteDocument(UnpublishedArticle.id.get)
  }

  test("user without required roles should not be permitted to perform the status transition") {
    val proposalArticle = TestData.sampleArticleWithByNcSa.copy(status = InProcessStatus)
    val (Failure(ex: IllegalStatusStateTransition), _) =
      doTransitionWithoutSideEffect(proposalArticle, PUBLISHED, TestData.userWithWriteAccess, false)
    ex.getMessage should equal("Cannot go to PUBLISHED when article is IN_PROGRESS")
  }

  test("PUBLISHED should be removed when transitioning to UNPUBLISHED") {
    val expected         = common.Status(UNPUBLISHED, Set())
    val publishedArticle = InProcessArticle.copy(status = common.Status(current = PUBLISHED, other = Set()))
    val (Success(res), _) =
      doTransitionWithoutSideEffect(publishedArticle, UNPUBLISHED, TestData.userWithAdminAccess, false)

    res.status should equal(expected)
  }

  test("unpublishArticle should fail if article is used in a learningstep") {
    val articleId: Long = 7L
    val article         = TestData.sampleDomainArticle.copy(id = Some(articleId))
    val learningPath    = TestData.sampleLearningPath
    when(learningpathApiClient.getLearningpathsWithId(any[Long], any)).thenReturn(Success(Seq(learningPath)))

    val res = StateTransitionRules.unpublishArticle.run(article, false, TestData.userWithAdminAccess)
    res.isFailure should be(true)
  }

  test("unpublishArticle should fail if article is used in another article") {
    val articleId: Long = 7L
    val article         = TestData.sampleDomainArticle.copy(id = Some(articleId))
    when(taxonomyApiClient.queryNodes(articleId)).thenReturn(Success(List.empty))
    when(learningpathApiClient.getLearningpathsWithId(any[Long], any)).thenReturn(Success(Seq.empty))
    val result = mock[MultiSearchSummaryDTO]
    when(result.title).thenReturn(TitleWithHtmlDTO("Title", "Title", "nb"))
    when(result.id).thenReturn(1L)
    when(searchApiClient.publishedWhereUsed(any[Long], any)).thenReturn(Seq(result))

    val Failure(res: ValidationException) =
      StateTransitionRules.checkIfArticleIsInUse.run(article, false, TestData.userWithAdminAccess)
    res.errors should equal(
      Seq(
        ValidationMessage("status.current", "Article is in use in these published article(s) 1 (Title)")
      )
    )
  }

  test("unpublishArticle should fail if article is used in a learningstep with a taxonomy-url") {
    val articleId: Long = 7L
    val article         = TestData.sampleDomainArticle.copy(id = Some(articleId))
    val learningPath    = TestData.sampleLearningPath
    when(learningpathApiClient.getLearningpathsWithId(eqTo(articleId), any)).thenReturn(Success(Seq(learningPath)))
    when(draftRepository.getIdFromExternalId(any[String])(any[DBSession])).thenReturn(Some(articleId))

    val res = StateTransitionRules.unpublishArticle.run(article, false, TestData.userWithAdminAccess)
    res.isFailure should be(true)
  }

  test("unpublishArticle should succeed if article is not used in a learningstep") {
    reset(articleApiClient)
    reset(taxonomyApiClient)
    reset(learningpathApiClient)
    val articleId = 7L
    val article   = TestData.sampleDomainArticle.copy(id = Some(articleId))
    when(learningpathApiClient.getLearningpathsWithId(eqTo(articleId), any)).thenReturn(Success(Seq.empty))
    when(articleApiClient.unpublishArticle(eqTo(article), any)).thenReturn(Success(article))
    when(taxonomyApiClient.queryNodes(eqTo(articleId))).thenReturn(Success(List.empty))
    when(searchApiClient.publishedWhereUsed(any[Long], any)).thenReturn(Seq.empty)

    val res = StateTransitionRules.unpublishArticle.run(article, false, TestData.userWithAdminAccess)
    res.isSuccess should be(true)
    verify(articleApiClient, times(1)).unpublishArticle(eqTo(article), any)
  }

  test("checkIfArticleIsUsedInLearningStep should fail if article is used in a learningstep") {
    val articleId: Long = 7L
    val article         = TestData.sampleDomainArticle.copy(id = Some(articleId))
    val learningPath    = TestData.sampleLearningPath
    when(learningpathApiClient.getLearningpathsWithId(eqTo(articleId), any)).thenReturn(Success(Seq(learningPath)))
    when(taxonomyApiClient.queryNodes(articleId)).thenReturn(Success(List.empty))

    val Failure(res: ValidationException) =
      StateTransitionRules.checkIfArticleIsInUse.run(article, false, TestData.userWithAdminAccess)
    res.errors.head.message should equal("Learningpath(s) 1 (Title) contains a learning step that uses this article")
  }

  test("checkIfArticleIsUsedInLearningStep should fail if article is used in a learningstep with a taxonomy-url") {
    val articleId: Long = 7L
    val article         = TestData.sampleDomainArticle.copy(id = Some(articleId))
    val learningPath    = TestData.sampleLearningPath
    when(learningpathApiClient.getLearningpathsWithId(eqTo(articleId), any)).thenReturn(Success(Seq(learningPath)))
    when(draftRepository.getIdFromExternalId(any[String])(any[DBSession])).thenReturn(Some(articleId))

    val Failure(res: ValidationException) =
      StateTransitionRules.checkIfArticleIsInUse.run(article, false, TestData.userWithAdminAccess)
    res.errors.head.message should equal("Learningpath(s) 1 (Title) contains a learning step that uses this article")
  }

  test("checkIfArticleIsUsedInLearningStep should succeed if article is not used in a learningstep") {
    reset(articleApiClient)
    reset(taxonomyApiClient)
    reset(learningpathApiClient)
    val articleId = 7L
    val article   = TestData.sampleDomainArticle.copy(id = Some(articleId))
    when(learningpathApiClient.getLearningpathsWithId(eqTo(articleId), any)).thenReturn(Success(Seq.empty))
    when(articleApiClient.unpublishArticle(eqTo(article), any)).thenReturn(Success(article))
    when(taxonomyApiClient.queryNodes(articleId)).thenReturn(Success(List.empty))

    val res = StateTransitionRules.checkIfArticleIsInUse.run(article, false, TestData.userWithAdminAccess)
    res.isSuccess should be(true)
  }

  test("validateArticle should be called when transitioning to END_CONTROL") {
    val articleId = 7L
    val draft = Draft(
      id = Some(articleId),
      revision = None,
      status = common.Status(PLANNED, Set.empty),
      title = Seq.empty,
      content = Seq.empty,
      copyright = Some(
        common.draft.DraftCopyright(Some(CC_BY.toString), Some(""), Seq.empty, Seq.empty, Seq.empty, None, None, false)
      ),
      tags = Seq.empty,
      requiredLibraries = Seq.empty,
      visualElement = Seq.empty,
      introduction = Seq.empty,
      metaDescription = Seq.empty,
      metaImage = Seq.empty,
      created = clock.now(),
      updated = clock.now(),
      updatedBy = "",
      published = clock.now(),
      articleType = common.ArticleType.Standard,
      notes = Seq.empty,
      previousVersionsNotes = Seq.empty,
      editorLabels = Seq.empty,
      grepCodes = Seq.empty,
      conceptIds = Seq.empty,
      availability = common.Availability.everyone,
      relatedContent = Seq.empty,
      revisionMeta = Seq.empty,
      responsible = Some(Responsible("hei", clock.now())),
      slug = None,
      comments = Seq.empty,
      priority = Priority.Unspecified,
      started = false,
      qualityEvaluation = None,
      disclaimer = OptLanguageFields.empty
    )
    val article = common.article.Article(
      id = Some(articleId),
      revision = None,
      title = Seq.empty,
      content = Seq.empty,
      copyright = common.article.Copyright(CC_BY.toString, None, Seq.empty, Seq.empty, Seq.empty, None, None, false),
      tags = Seq.empty,
      requiredLibraries = Seq.empty,
      visualElement = Seq.empty,
      introduction = Seq.empty,
      metaDescription = Seq.empty,
      metaImage = Seq.empty,
      created = clock.now(),
      updated = clock.now(),
      updatedBy = "",
      published = clock.now(),
      articleType = common.ArticleType.Standard,
      grepCodes = Seq.empty,
      conceptIds = Seq.empty,
      availability = common.Availability.everyone,
      relatedContent = Seq.empty,
      revisionDate = None,
      slug = None,
      disclaimer = OptLanguageFields.empty
    )
    val status = common.Status(END_CONTROL, Set.empty)

    when(converterService.toArticleApiArticle(any[Draft])).thenReturn(Success(article))

    val transitionsToTest = StateTransitionRules.StateTransitions.filter(_.to == END_CONTROL)
    transitionsToTest.foreach(t =>
      StateTransitionRules
        .doTransition(
          draft.copy(status = status.copy(current = t.from)),
          END_CONTROL,
          TestData.userWithPublishAccess,
          false
        )
    )
    verify(articleApiClient, times(transitionsToTest.size))
      .validateArticle(any[common.article.Article], any[Boolean], any)
  }

  test("publishArticle should call h5p api") {
    reset(h5pApiClient)
    reset(articleApiClient)
    val h5pId           = "123-kulid-123"
    val h5pPaths        = Seq(s"/resource/$h5pId")
    val expectedStatus  = common.Status(PUBLISHED, Set.empty)
    val editorNotes     = Seq(common.EditorNote("Status endret", "unit_test", expectedStatus, NDLADate.now()))
    val expectedArticle = InProcessArticle.copy(status = expectedStatus, notes = editorNotes)
    when(draftRepository.getExternalIdsFromId(any[Long])(any[DBSession])).thenReturn(List("1234"))
    when(converterService.getEmbeddedConceptIds(any[Draft])).thenReturn(Seq.empty)
    when(converterService.getEmbeddedH5PPaths(any[Draft])).thenReturn(h5pPaths)
    when(h5pApiClient.publishH5Ps(eqTo(h5pPaths), any)).thenReturn(Success(()))

    when(
      articleApiClient
        .updateArticle(
          eqTo(InProcessArticle.id.get),
          any[Draft],
          eqTo(List("1234")),
          eqTo(false),
          eqTo(true),
          any
        )
    )
      .thenReturn(Success(expectedArticle))

    val (Success(res), sideEffect) =
      doTransitionWithoutSideEffect(InProcessArticle, PUBLISHED, TestData.userWithAdminAccess, false)
    sideEffect.map(sf => sf.run(res, false, TestData.userWithAdminAccess).get.status should equal(expectedStatus))

    val captor = ArgumentCaptor.forClass(classOf[Draft])
    verify(articleApiClient, times(1))
      .updateArticle(eqTo(InProcessArticle.id.get), captor.capture(), eqTo(List("1234")), eqTo(false), eqTo(true), any)

    verify(h5pApiClient, times(1)).publishH5Ps(eqTo(h5pPaths), any)

    val argumentArticle: Draft   = captor.getValue
    val argumentArticleWithNotes = argumentArticle.copy(notes = editorNotes)
    argumentArticleWithNotes should equal(expectedArticle)
  }

  test("That publishing article results in responsibleId being reset") {
    val articleId         = 100L
    val beforeResponsible = Responsible("heisann", clock.now())
    val draft = Draft(
      id = Some(articleId),
      revision = None,
      status = common.Status(PLANNED, Set.empty),
      title = Seq.empty,
      content = Seq.empty,
      copyright = Some(
        common.draft.DraftCopyright(Some(CC_BY.toString), Some(""), Seq.empty, Seq.empty, Seq.empty, None, None, false)
      ),
      tags = Seq.empty,
      requiredLibraries = Seq.empty,
      visualElement = Seq.empty,
      introduction = Seq.empty,
      metaDescription = Seq.empty,
      metaImage = Seq.empty,
      created = clock.now(),
      updated = clock.now(),
      updatedBy = "updated",
      published = clock.now(),
      articleType = common.ArticleType.Standard,
      notes = Seq.empty,
      previousVersionsNotes = Seq.empty,
      editorLabels = Seq.empty,
      grepCodes = Seq.empty,
      conceptIds = Seq.empty,
      availability = common.Availability.everyone,
      relatedContent = Seq.empty,
      revisionMeta = Seq.empty,
      responsible = Some(beforeResponsible),
      slug = None,
      comments = Seq.empty,
      priority = Priority.Unspecified,
      started = false,
      qualityEvaluation = None,
      disclaimer = OptLanguageFields.empty
    )
    val status            = common.Status(PLANNED, Set.empty)
    val transitionsToTest = StateTransitionRules.StateTransitions.filter(_.to == PUBLISHED)
    when(draftRepository.getExternalIdsFromId(any[Long])(any[DBSession])).thenReturn(List.empty)
    when(articleApiClient.updateArticle(any, any, any, any, any, any)).thenAnswer((i: InvocationOnMock) => {
      val x = i.getArgument[Draft](1)
      Success(x)
    })
    for (t <- transitionsToTest) {
      val fromDraft = draft.copy(status = status.copy(current = t.from), responsible = Some(beforeResponsible))
      val result = StateTransitionRules
        .doTransition(fromDraft, PUBLISHED, TestData.userWithAdminAccess, isImported = false)

      if (result.get.responsible.isDefined) {
        fail(s"${t.from} -> ${t.to} did not reset responsible >:( Look at the sideeffects in `StateTransitionRules`")
      }
    }
  }

  test("That archiving article results in responsibleId being reset") {
    val articleId         = 100L
    val beforeResponsible = Responsible("heisann", clock.now())
    val draft = Draft(
      id = Some(articleId),
      revision = None,
      status = common.Status(PLANNED, Set.empty),
      title = Seq.empty,
      content = Seq.empty,
      copyright = Some(
        common.draft.DraftCopyright(Some(CC_BY.toString), Some(""), Seq.empty, Seq.empty, Seq.empty, None, None, false)
      ),
      tags = Seq.empty,
      requiredLibraries = Seq.empty,
      visualElement = Seq.empty,
      introduction = Seq.empty,
      metaDescription = Seq.empty,
      metaImage = Seq.empty,
      created = clock.now(),
      updated = clock.now(),
      updatedBy = "updated",
      published = clock.now(),
      articleType = common.ArticleType.Standard,
      notes = Seq.empty,
      previousVersionsNotes = Seq.empty,
      editorLabels = Seq.empty,
      grepCodes = Seq.empty,
      conceptIds = Seq.empty,
      availability = common.Availability.everyone,
      relatedContent = Seq.empty,
      revisionMeta = Seq.empty,
      responsible = Some(beforeResponsible),
      slug = None,
      comments = Seq.empty,
      priority = Priority.Unspecified,
      started = false,
      qualityEvaluation = None,
      disclaimer = OptLanguageFields.empty
    )
    val status            = common.Status(PLANNED, Set.empty)
    val transitionsToTest = StateTransitionRules.StateTransitions.filter(_.to == ARCHIVED)
    when(draftRepository.getExternalIdsFromId(any[Long])(any[DBSession])).thenReturn(List.empty)
    when(articleApiClient.updateArticle(any, any, any, any, any, any)).thenAnswer((i: InvocationOnMock) => {
      val x = i.getArgument[Draft](1)
      Success(x)
    })
    when(taxonomyApiClient.queryNodes(100L)).thenReturn(Success(List()))
    when(articleApiClient.unpublishArticle(any, any)).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument[Draft](0))
    )
    for (t <- transitionsToTest) {
      val fromDraft = draft.copy(status = status.copy(current = t.from), responsible = Some(beforeResponsible))
      val result = StateTransitionRules
        .doTransition(fromDraft, ARCHIVED, TestData.userWithAdminAccess, isImported = false)

      if (result.get.responsible.isDefined) {
        fail(s"${t.from} -> ${t.to} did not reset responsible >:( Look at the sideeffects in `StateTransitionRules`")
      }
    }
  }

  test("That unpublishing article results in responsibleId being reset") {
    val articleId         = 100L
    val beforeResponsible = Responsible("heisann", clock.now())
    val draft = Draft(
      id = Some(articleId),
      revision = None,
      status = common.Status(PLANNED, Set.empty),
      title = Seq.empty,
      content = Seq.empty,
      copyright = Some(
        common.draft.DraftCopyright(Some(CC_BY.toString), Some(""), Seq.empty, Seq.empty, Seq.empty, None, None, false)
      ),
      tags = Seq.empty,
      requiredLibraries = Seq.empty,
      visualElement = Seq.empty,
      introduction = Seq.empty,
      metaDescription = Seq.empty,
      metaImage = Seq.empty,
      created = clock.now(),
      updated = clock.now(),
      updatedBy = "updated",
      published = clock.now(),
      articleType = common.ArticleType.Standard,
      notes = Seq.empty,
      previousVersionsNotes = Seq.empty,
      editorLabels = Seq.empty,
      grepCodes = Seq.empty,
      conceptIds = Seq.empty,
      availability = common.Availability.everyone,
      relatedContent = Seq.empty,
      revisionMeta = Seq.empty,
      responsible = Some(beforeResponsible),
      slug = None,
      comments = Seq.empty,
      priority = Priority.Unspecified,
      started = false,
      qualityEvaluation = None,
      disclaimer = OptLanguageFields.empty
    )
    val status            = common.Status(PLANNED, Set.empty)
    val transitionsToTest = StateTransitionRules.StateTransitions.filter(_.to == UNPUBLISHED)
    when(draftRepository.getExternalIdsFromId(any[Long])(any[DBSession])).thenReturn(List.empty)
    when(articleApiClient.updateArticle(any, any, any, any, any, any)).thenAnswer((i: InvocationOnMock) => {
      val x = i.getArgument[Draft](1)
      Success(x)
    })
    when(taxonomyApiClient.queryNodes(100L)).thenReturn(Success(List()))
    when(articleApiClient.unpublishArticle(any, any)).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument[Draft](0))
    )
    when(searchApiClient.publishedWhereUsed(eqTo(100L), any)).thenReturn(Seq())

    for (t <- transitionsToTest) {
      val fromDraft = draft.copy(status = status.copy(current = t.from), responsible = Some(beforeResponsible))
      val result = StateTransitionRules
        .doTransition(fromDraft, UNPUBLISHED, TestData.userWithAdminAccess, isImported = false)

      if (result.get.responsible.isDefined) {
        fail(s"${t.from} -> ${t.to} did not reset responsible >:( Look at the sideeffects in `StateTransitionRules`")
      }
    }
  }

  test("That responsibleId is updated at status change from published to in progress") {
    val articleId = 100L
    val draft = Draft(
      id = Some(articleId),
      revision = None,
      status = common.Status(PUBLISHED, Set.empty),
      title = Seq.empty,
      content = Seq.empty,
      copyright = Some(
        common.draft.DraftCopyright(Some(CC_BY.toString), Some(""), Seq.empty, Seq.empty, Seq.empty, None, None, false)
      ),
      tags = Seq.empty,
      requiredLibraries = Seq.empty,
      visualElement = Seq.empty,
      introduction = Seq.empty,
      metaDescription = Seq.empty,
      metaImage = Seq.empty,
      created = clock.now(),
      updated = clock.now(),
      updatedBy = "updated",
      published = clock.now(),
      articleType = common.ArticleType.Standard,
      notes = Seq.empty,
      previousVersionsNotes = Seq.empty,
      editorLabels = Seq.empty,
      grepCodes = Seq.empty,
      conceptIds = Seq.empty,
      availability = common.Availability.everyone,
      relatedContent = Seq.empty,
      revisionMeta = Seq.empty,
      responsible = None,
      slug = None,
      comments = Seq.empty,
      priority = Priority.Unspecified,
      started = false,
      qualityEvaluation = None,
      disclaimer = OptLanguageFields.empty
    )
    val status                            = common.Status(PUBLISHED, Set.empty)
    val transitionToTest: StateTransition = PUBLISHED -> IN_PROGRESS
    val expected                          = TestData.userWithAdminAccess.id
    when(draftRepository.getExternalIdsFromId(any[Long])(any[DBSession])).thenReturn(List.empty)
    when(articleApiClient.updateArticle(any, any, any, any, any, any)).thenAnswer((i: InvocationOnMock) => {
      val x = i.getArgument[Draft](1)
      Success(x)
    })
    when(taxonomyApiClient.queryNodes(100L)).thenReturn(Success(List()))
    when(articleApiClient.unpublishArticle(any, any)).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument[Draft](0))
    )
    when(searchApiClient.publishedWhereUsed(eqTo(100L), any)).thenReturn(Seq())

    val fromDraft = draft.copy(status = status.copy(current = transitionToTest.from))
    val result = StateTransitionRules
      .doTransition(fromDraft, IN_PROGRESS, TestData.userWithAdminAccess, isImported = false)

    result.get.responsible.get.responsibleId should be(expected)
  }

}
