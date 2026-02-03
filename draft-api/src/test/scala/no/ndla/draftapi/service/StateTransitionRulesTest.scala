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
import no.ndla.draftapi.model.api.IllegalStatusStateTransition
import no.ndla.common.model.domain.{Priority, Responsible, Status}
import no.ndla.common.model.domain.draft.{Draft, DraftStatus}
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
  override implicit lazy val stateTransitionRules: StateTransitionRules = new StateTransitionRules
  val PlannedStatus: Status                                             = common.Status(PLANNED, Set(END_CONTROL))
  val PlannedWithPublishedStatus: Status                                = common.Status(PLANNED, Set(PUBLISHED))
  val PublishedStatus: Status                                           = common.Status(PUBLISHED, Set.empty)
  val ExternalReviewStatus: Status                                      = common.Status(EXTERNAL_REVIEW, Set(IN_PROGRESS))
  val UnpublishedStatus: Status                                         = common.Status(UNPUBLISHED, Set.empty)
  val InProcessStatus: Status                                           = common.Status(IN_PROGRESS, Set.empty)
  val ArchivedStatus: Status                                            = common.Status(ARCHIVED, Set(PUBLISHED))
  val responsible: Responsible                                          = common.Responsible("someid", TestData.today)
  val InProcessArticle: Draft                                           = TestData
    .sampleArticleWithByNcSa
    .copy(status = InProcessStatus, responsible = Some(responsible))
  val PublishedArticle: Draft = TestData
    .sampleArticleWithByNcSa
    .copy(status = PublishedStatus, responsible = Some(responsible))
  val UnpublishedArticle: Draft = TestData
    .sampleArticleWithByNcSa
    .copy(status = UnpublishedStatus, responsible = Some(responsible))

  test("doTransition should succeed when performing a legal transition") {
    val expected          = common.Status(PUBLISHED, Set.empty)
    val (Success(res), _) = stateTransitionRules.doTransitionWithoutSideEffect(
      InProcessArticle,
      PUBLISHED,
      TestData.userWithAdminAccess,
    ): @unchecked
    res.status should equal(expected)
  }

  test("doTransition should keep some states when performing a legal transition") {
    val expected          = common.Status(EXTERNAL_REVIEW, Set(IN_PROGRESS))
    val (Success(res), _) = stateTransitionRules.doTransitionWithoutSideEffect(
      InProcessArticle.copy(status = ExternalReviewStatus),
      EXTERNAL_REVIEW,
      TestData.userWithPublishAccess,
    ): @unchecked
    res.status should equal(expected)

    val expected2          = common.Status(IN_PROGRESS, Set(PUBLISHED))
    val (Success(res2), _) = stateTransitionRules.doTransitionWithoutSideEffect(
      InProcessArticle.copy(status = PublishedStatus),
      IN_PROGRESS,
      TestData.userWithPublishAccess,
    ): @unchecked
    res2.status should equal(expected2)

  }

  test("doTransition every state change to Archived should succeed") {
    val expected1          = common.Status(ARCHIVED, Set.empty)
    val (Success(res1), _) = stateTransitionRules.doTransitionWithoutSideEffect(
      InProcessArticle.copy(status = PublishedStatus),
      ARCHIVED,
      TestData.userWithPublishAccess,
    ): @unchecked
    res1.status should equal(expected1)

    val expected2          = common.Status(ARCHIVED, Set.empty)
    val (Success(res2), _) = stateTransitionRules.doTransitionWithoutSideEffect(
      InProcessArticle.copy(status = UnpublishedStatus),
      ARCHIVED,
      TestData.userWithPublishAccess,
    ): @unchecked
    res2.status should equal(expected2)

    val expected3          = common.Status(ARCHIVED, Set.empty)
    val (Success(res3), _) = stateTransitionRules.doTransitionWithoutSideEffect(
      InProcessArticle.copy(status = InProcessStatus),
      ARCHIVED,
      TestData.userWithPublishAccess,
    ): @unchecked
    res3.status should equal(expected3)

    val expected4          = common.Status(ARCHIVED, Set.empty)
    val (Success(res4), _) = stateTransitionRules.doTransitionWithoutSideEffect(
      InProcessArticle.copy(status = ExternalReviewStatus),
      ARCHIVED,
      TestData.userWithPublishAccess,
    ): @unchecked
    res4.status should equal(expected4)

    val expected5          = common.Status(ARCHIVED, Set.empty)
    val (Success(res5), _) = stateTransitionRules.doTransitionWithoutSideEffect(
      InProcessArticle.copy(status = PlannedStatus),
      ARCHIVED,
      TestData.userWithPublishAccess,
    ): @unchecked
    res5.status should equal(expected5)

    val expected6          = common.Status(ARCHIVED, Set.empty)
    val (Success(res6), _) = stateTransitionRules.doTransitionWithoutSideEffect(
      InProcessArticle.copy(status = PublishedStatus),
      ARCHIVED,
      TestData.userWithPublishAccess,
    ): @unchecked
    res6.status should equal(expected6)

  }

  test("doTransition should fail when performing an illegal transition") {
    val (res, _) =
      stateTransitionRules.doTransitionWithoutSideEffect(InProcessArticle, END_CONTROL, TestData.userWithPublishAccess)
    res.isFailure should be(true)
  }

  test("doTransition should publish the article when transitioning to PUBLISHED") {
    val expectedStatus  = common.Status(PUBLISHED, Set.empty)
    val editorNotes     = Seq(common.EditorNote("Status endret", "unit_test", expectedStatus, NDLADate.now()))
    val expectedArticle = InProcessArticle.copy(status = expectedStatus, notes = editorNotes)
    when(draftRepository.getExternalIdsFromId(any[Long])(using any[DBSession])).thenReturn(Success(List("1234")))
    when(converterService.getEmbeddedConceptIds(any[Draft])).thenReturn(Seq.empty)
    when(converterService.getEmbeddedH5PPaths(any[Draft])).thenReturn(Seq.empty)
    when(h5pApiClient.publishH5Ps(eqTo(Seq.empty), any)).thenReturn(Success(()))
    when(taxonomyApiClient.updateTaxonomyIfExists(eqTo(InProcessArticle.id.get), any, any)).thenReturn(
      Success(InProcessArticle.id.get)
    )
    when(articleApiClient.updateArticle(eqTo(InProcessArticle.id.get), any[Draft], eqTo(List("1234")), eqTo(true), any))
      .thenReturn(Success(expectedArticle))

    val (Success(res), sideEffect) = stateTransitionRules.doTransitionWithoutSideEffect(
      InProcessArticle,
      PUBLISHED,
      TestData.userWithAdminAccess,
    ): @unchecked
    sideEffect.map(sf => sf.run(res, TestData.userWithAdminAccess).get.status should equal(expectedStatus))

    val captor = ArgumentCaptor.forClass(classOf[Draft])
    verify(articleApiClient, times(1)).updateArticle(
      eqTo(InProcessArticle.id.get),
      captor.capture(),
      eqTo(List("1234")),
      eqTo(true),
      any,
    )
    verify(taxonomyApiClient, times(1)).updateTaxonomyIfExists(eqTo(InProcessArticle.id.get), any, any)

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

    val (Success(res), sideEffect) = stateTransitionRules.doTransitionWithoutSideEffect(
      PublishedArticle,
      UNPUBLISHED,
      TestData.userWithAdminAccess,
    ): @unchecked
    sideEffect.map(sf => sf.run(res, TestData.userWithAdminAccess).get.status should equal(expectedStatus))

    val captor = ArgumentCaptor.forClass(classOf[Draft])

    verify(articleApiClient, times(1)).unpublishArticle(captor.capture(), any)

    val argumentArticle: Draft   = captor.getValue
    val argumentArticleWithNotes = argumentArticle.copy(notes = editorNotes)
    argumentArticleWithNotes should equal(expectedArticle)
  }

  test("doTransition should not remove article from search when transitioning to ARCHIVED") {
    val expectedStatus = common.Status(ARCHIVED, Set.empty)

    when(articleIndexService.deleteDocument(UnpublishedArticle.id.get)).thenReturn(Success(UnpublishedArticle.id.get))

    val (Success(res), sideEffect) = stateTransitionRules.doTransitionWithoutSideEffect(
      UnpublishedArticle,
      ARCHIVED,
      TestData.userWithPublishAccess,
    ): @unchecked
    sideEffect.map(sf => sf.run(res, TestData.userWithAdminAccess).get.status should equal(expectedStatus))

    verify(articleIndexService, times(0)).deleteDocument(UnpublishedArticle.id.get)
  }

  test("user without required roles should not be permitted to perform the status transition") {
    val proposalArticle                                = TestData.sampleArticleWithByNcSa.copy(status = InProcessStatus)
    val (Failure(ex: IllegalStatusStateTransition), _) = stateTransitionRules.doTransitionWithoutSideEffect(
      proposalArticle,
      PUBLISHED,
      TestData.userWithWriteAccess,
    ): @unchecked
    ex.getMessage should equal("Cannot go to PUBLISHED when article is IN_PROGRESS")
  }

  test("PUBLISHED should be removed when transitioning to UNPUBLISHED") {
    val expected          = common.Status(UNPUBLISHED, Set())
    val publishedArticle  = InProcessArticle.copy(status = common.Status(current = PUBLISHED, other = Set()))
    val (Success(res), _) = stateTransitionRules.doTransitionWithoutSideEffect(
      publishedArticle,
      UNPUBLISHED,
      TestData.userWithAdminAccess,
    ): @unchecked

    res.status should equal(expected)
  }

  test("unpublishArticle should fail if article is used in a learningstep") {
    val articleId: Long = 7L
    val article         = TestData.sampleDomainArticle.copy(id = Some(articleId))
    val learningPath    = TestData.sampleLearningPath
    when(learningpathApiClient.getLearningpathsWithId(any[Long], any)).thenReturn(Success(Seq(learningPath)))

    val res = stateTransitionRules.unpublishArticle.run(article, TestData.userWithAdminAccess)
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
      stateTransitionRules.checkIfArticleIsInUse.run(article, TestData.userWithAdminAccess): @unchecked
    res.errors should equal(
      Seq(ValidationMessage("status.current", "Article is in use in these published article(s) 1 (Title)"))
    )
  }

  test("unpublishArticle should fail if article is used in a learningstep with a taxonomy-url") {
    val articleId: Long = 7L
    val article         = TestData.sampleDomainArticle.copy(id = Some(articleId))
    val learningPath    = TestData.sampleLearningPath
    when(learningpathApiClient.getLearningpathsWithId(eqTo(articleId), any)).thenReturn(Success(Seq(learningPath)))
    when(draftRepository.getIdFromExternalId(any[String])(using any[DBSession])).thenReturn(Success(Some(articleId)))

    val res = stateTransitionRules.unpublishArticle.run(article, TestData.userWithAdminAccess)
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

    val res = stateTransitionRules.unpublishArticle.run(article, TestData.userWithAdminAccess)
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
      stateTransitionRules.checkIfArticleIsInUse.run(article, TestData.userWithAdminAccess): @unchecked
    res.errors.head.message should equal("Learningpath(s) 1 (Title) contains a learning step that uses this article")
  }

  test("checkIfArticleIsUsedInLearningStep should fail if article is used in a learningstep with a taxonomy-url") {
    val articleId: Long = 7L
    val article         = TestData.sampleDomainArticle.copy(id = Some(articleId))
    val learningPath    = TestData.sampleLearningPath
    when(learningpathApiClient.getLearningpathsWithId(eqTo(articleId), any)).thenReturn(Success(Seq(learningPath)))
    when(draftRepository.getIdFromExternalId(any[String])(using any[DBSession])).thenReturn(Success(Some(articleId)))

    val Failure(res: ValidationException) =
      stateTransitionRules.checkIfArticleIsInUse.run(article, TestData.userWithAdminAccess): @unchecked
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

    val res = stateTransitionRules.checkIfArticleIsInUse.run(article, TestData.userWithAdminAccess)
    res.isSuccess should be(true)
  }

  test("validateArticle should be called when transitioning to END_CONTROL") {
    val articleId = 7L
    val draft     = Draft(
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
      disclaimer = OptLanguageFields.empty,
      traits = List.empty,
      publishedCount = None,
    )
    val article = common
      .article
      .Article(
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
        disclaimer = OptLanguageFields.empty,
        traits = List.empty,
      )
    val status = common.Status(END_CONTROL, Set.empty)

    when(converterService.toArticleApiArticle(any[Draft])).thenReturn(Success(article))

    val transitionsToTest = stateTransitionRules.StateTransitions.filter(_.to == END_CONTROL)
    transitionsToTest.foreach(t =>
      stateTransitionRules.doTransition(
        draft.copy(status = status.copy(current = t.from)),
        END_CONTROL,
        TestData.userWithPublishAccess,
      )
    )
    verify(articleApiClient, times(transitionsToTest.size)).validateArticle(
      any[common.article.Article],
      any[Boolean],
      any,
    )
  }

  test("publishArticle should call h5p api") {
    reset(h5pApiClient)
    reset(articleApiClient)
    val h5pId           = "123-kulid-123"
    val h5pPaths        = Seq(s"/resource/$h5pId")
    val expectedStatus  = common.Status(PUBLISHED, Set.empty)
    val editorNotes     = Seq(common.EditorNote("Status endret", "unit_test", expectedStatus, NDLADate.now()))
    val expectedArticle = InProcessArticle.copy(status = expectedStatus, notes = editorNotes)
    when(draftRepository.getExternalIdsFromId(any[Long])(using any[DBSession])).thenReturn(Success(List("1234")))
    when(converterService.getEmbeddedConceptIds(any[Draft])).thenReturn(Seq.empty)
    when(converterService.getEmbeddedH5PPaths(any[Draft])).thenReturn(h5pPaths)
    when(h5pApiClient.publishH5Ps(eqTo(h5pPaths), any)).thenReturn(Success(()))
    when(taxonomyApiClient.updateTaxonomyIfExists(eqTo(InProcessArticle.id.get), any, any)).thenReturn(
      Success(InProcessArticle.id.get)
    )
    when(articleApiClient.updateArticle(eqTo(InProcessArticle.id.get), any[Draft], eqTo(List("1234")), eqTo(true), any))
      .thenReturn(Success(expectedArticle))

    val (Success(res), sideEffect) = stateTransitionRules.doTransitionWithoutSideEffect(
      InProcessArticle,
      PUBLISHED,
      TestData.userWithAdminAccess,
    ): @unchecked
    sideEffect.map(sf => sf.run(res, TestData.userWithAdminAccess).get.status should equal(expectedStatus))

    val captor = ArgumentCaptor.forClass(classOf[Draft])
    verify(articleApiClient, times(1)).updateArticle(
      eqTo(InProcessArticle.id.get),
      captor.capture(),
      eqTo(List("1234")),
      eqTo(true),
      any,
    )

    verify(h5pApiClient, times(1)).publishH5Ps(eqTo(h5pPaths), any)
    verify(taxonomyApiClient, times(1)).updateTaxonomyIfExists(eqTo(InProcessArticle.id.get), any, any)

    val argumentArticle: Draft   = captor.getValue
    val argumentArticleWithNotes = argumentArticle.copy(notes = editorNotes)
    argumentArticleWithNotes should equal(expectedArticle)
  }

  test("That publishing article results in responsibleId being reset") {
    val articleId         = 100L
    val beforeResponsible = Responsible("heisann", clock.now())
    val draft             = Draft(
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
      disclaimer = OptLanguageFields.empty,
      traits = List.empty,
      publishedCount = None,
    )
    val status            = common.Status(PLANNED, Set.empty)
    val transitionsToTest = stateTransitionRules.StateTransitions.filter(_.to == PUBLISHED)
    when(draftRepository.getExternalIdsFromId(any[Long])(using any[DBSession])).thenReturn(Success(List.empty))
    when(taxonomyApiClient.updateTaxonomyIfExists(any, any, any)).thenAnswer(i => Success(i.getArgument(0)))
    when(articleApiClient.updateArticle(any, any, any, any, any)).thenAnswer((i: InvocationOnMock) => {
      val x = i.getArgument[Draft](1)
      Success(x)
    })

    for (t <- transitionsToTest) {
      val fromDraft = draft.copy(status = status.copy(current = t.from), responsible = Some(beforeResponsible))
      val result    = stateTransitionRules.doTransition(fromDraft, PUBLISHED, TestData.userWithAdminAccess)

      if (result.get.responsible.isDefined) {
        fail(s"${t.from} -> ${t.to} did not reset responsible >:( Look at the sideeffects in `StateTransitionRules`")
      }
    }
  }

  test("That archiving article results in responsibleId being reset") {
    val articleId         = 100L
    val beforeResponsible = Responsible("heisann", clock.now())
    val draft             = Draft(
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
      disclaimer = OptLanguageFields.empty,
      traits = List.empty,
      publishedCount = None,
    )
    val status            = common.Status(PLANNED, Set.empty)
    val transitionsToTest = stateTransitionRules.StateTransitions.filter(_.to == ARCHIVED)
    when(draftRepository.getExternalIdsFromId(any[Long])(using any[DBSession])).thenReturn(Success(List.empty))
    when(articleApiClient.updateArticle(any, any, any, any, any)).thenAnswer((i: InvocationOnMock) => {
      val x = i.getArgument[Draft](1)
      Success(x)
    })
    when(taxonomyApiClient.queryNodes(100L)).thenReturn(Success(List()))
    when(articleApiClient.unpublishArticle(any, any)).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument[Draft](0))
    )
    for (t <- transitionsToTest) {
      val fromDraft = draft.copy(status = status.copy(current = t.from), responsible = Some(beforeResponsible))
      val result    = stateTransitionRules.doTransition(fromDraft, ARCHIVED, TestData.userWithAdminAccess)

      if (result.get.responsible.isDefined) {
        fail(s"${t.from} -> ${t.to} did not reset responsible >:( Look at the sideeffects in `StateTransitionRules`")
      }
    }
  }

  test("That unpublishing article results in responsibleId being reset") {
    val articleId         = 100L
    val beforeResponsible = Responsible("heisann", clock.now())
    val draft             = Draft(
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
      disclaimer = OptLanguageFields.empty,
      traits = List.empty,
      publishedCount = None,
    )
    val status            = common.Status(PLANNED, Set.empty)
    val transitionsToTest = stateTransitionRules.StateTransitions.filter(_.to == UNPUBLISHED)
    when(draftRepository.getExternalIdsFromId(any[Long])(using any[DBSession])).thenReturn(Success(List.empty))
    when(articleApiClient.updateArticle(any, any, any, any, any)).thenAnswer((i: InvocationOnMock) => {
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
      val result    = stateTransitionRules.doTransition(fromDraft, UNPUBLISHED, TestData.userWithAdminAccess)

      if (result.get.responsible.isDefined) {
        fail(s"${t.from} -> ${t.to} did not reset responsible >:( Look at the sideeffects in `StateTransitionRules`")
      }
    }
  }

  test("That responsibleId is updated at status change from published to in progress") {
    val articleId = 100L
    val draft     = Draft(
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
      disclaimer = OptLanguageFields.empty,
      traits = List.empty,
      publishedCount = None,
    )
    val status                            = common.Status(PUBLISHED, Set.empty)
    val transitionToTest: StateTransition = PUBLISHED -> IN_PROGRESS
    val expected                          = TestData.userWithAdminAccess.id
    when(draftRepository.getExternalIdsFromId(any[Long])(using any[DBSession])).thenReturn(Success(List.empty))
    when(articleApiClient.updateArticle(any, any, any, any, any)).thenAnswer((i: InvocationOnMock) => {
      val x = i.getArgument[Draft](1)
      Success(x)
    })
    when(taxonomyApiClient.queryNodes(100L)).thenReturn(Success(List()))
    when(articleApiClient.unpublishArticle(any, any)).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument[Draft](0))
    )
    when(searchApiClient.publishedWhereUsed(eqTo(100L), any)).thenReturn(Seq())

    val fromDraft = draft.copy(status = status.copy(current = transitionToTest.from))
    val result    = stateTransitionRules.doTransition(fromDraft, IN_PROGRESS, TestData.userWithAdminAccess)
    result.get.responsible.get.responsibleId should be(expected)
  }

  test("stateTransitionsToApi should return only disabled entries if user has no roles") {
    val Success(res) = stateTransitionRules.stateTransitionsToApi(TestData.userWithNoRoles, None): @unchecked
    res.forall { case (_, to) =>
      to.isEmpty
    } should be(true)
  }

  test("stateTransitionsToApi should allow all users to archive articles that have not been published") {
    val articleId: Long = 1
    val article: Draft  = TestData
      .sampleArticleWithPublicDomain
      .copy(id = Some(articleId), status = Status(DraftStatus.PLANNED, Set()))
    when(draftRepository.withId(eqTo(articleId))(using any)).thenReturn(Success(Some(article)))
    val Success(noTrans) =
      stateTransitionRules.stateTransitionsToApi(TestData.userWithWriteAccess, Some(articleId)): @unchecked
    noTrans(PLANNED.toString) should contain(DraftStatus.ARCHIVED.toString)
    noTrans(IN_PROGRESS.toString) should contain(DraftStatus.ARCHIVED.toString)
    noTrans(EXTERNAL_REVIEW.toString) should contain(DraftStatus.ARCHIVED.toString)
    noTrans(INTERNAL_REVIEW.toString) should contain(DraftStatus.ARCHIVED.toString)
    noTrans(END_CONTROL.toString) should contain(DraftStatus.ARCHIVED.toString)
    noTrans(LANGUAGE.toString) should contain(DraftStatus.ARCHIVED.toString)
    noTrans(FOR_APPROVAL.toString) should contain(DraftStatus.ARCHIVED.toString)
    noTrans(PUBLISHED.toString) should not contain (DraftStatus.ARCHIVED.toString)
    noTrans(UNPUBLISHED.toString) should contain(DraftStatus.ARCHIVED.toString)
  }

  test("stateTransitionsToApi should not allow all users to archive articles that are currently published") {

    val articleId: Long = 1
    val article: Draft  = TestData
      .sampleArticleWithPublicDomain
      .copy(id = Some(articleId), status = Status(DraftStatus.PUBLISHED, Set()))
    when(draftRepository.withId(eqTo(articleId))(using any)).thenReturn(Success(Some(article)))
    val Success(noTrans) =
      stateTransitionRules.stateTransitionsToApi(TestData.userWithWriteAccess, Some(articleId)): @unchecked

    noTrans(PLANNED.toString) should not contain (DraftStatus.ARCHIVED.toString)
    noTrans(IN_PROGRESS.toString) should not contain (DraftStatus.ARCHIVED.toString)
    noTrans(EXTERNAL_REVIEW.toString) should not contain (DraftStatus.ARCHIVED.toString)
    noTrans(INTERNAL_REVIEW.toString) should not contain (DraftStatus.ARCHIVED.toString)
    noTrans(END_CONTROL.toString) should not contain (DraftStatus.ARCHIVED.toString)
    noTrans(LANGUAGE.toString) should not contain (DraftStatus.ARCHIVED.toString)
    noTrans(FOR_APPROVAL.toString) should not contain (DraftStatus.ARCHIVED.toString)
    noTrans(PUBLISHED.toString) should not contain (DraftStatus.ARCHIVED.toString)
    noTrans(UNPUBLISHED.toString) should not contain (DraftStatus.ARCHIVED.toString)
  }

  test("stateTransitionsToApi should filter some transitions based on publishing status") {
    val articleId: Long    = 1
    val unpublished: Draft = TestData
      .sampleArticleWithPublicDomain
      .copy(id = Some(articleId), status = Status(DraftStatus.IN_PROGRESS, Set()))
    when(draftRepository.withId(eqTo(articleId))(using any)).thenReturn(Success(Some(unpublished)))
    val Success(transOne) =
      stateTransitionRules.stateTransitionsToApi(TestData.userWithWriteAccess, Some(articleId)): @unchecked
    transOne(IN_PROGRESS.toString) should not contain (DraftStatus.LANGUAGE.toString)

    val published: Draft = TestData
      .sampleArticleWithPublicDomain
      .copy(id = Some(articleId), status = Status(DraftStatus.IN_PROGRESS, Set(DraftStatus.PUBLISHED)))
    when(draftRepository.withId(eqTo(articleId))(using any)).thenReturn(Success(Some(published)))
    val Success(transTwo) =
      stateTransitionRules.stateTransitionsToApi(TestData.userWithWriteAccess, Some(articleId)): @unchecked
    transTwo(IN_PROGRESS.toString) should contain(DraftStatus.LANGUAGE.toString)
  }

  test("stateTransitionsToApi should not allow all users to archive articles that have previously been published") {

    val articleId      = 1L
    val article: Draft = TestData
      .sampleArticleWithPublicDomain
      .copy(id = Some(articleId), status = Status(DraftStatus.PLANNED, Set(DraftStatus.PUBLISHED)))
    when(draftRepository.withId(eqTo(articleId))(using any)).thenReturn(Success(Some(article)))
    val Success(noTrans) = stateTransitionRules.stateTransitionsToApi(TestData.userWithWriteAccess, None): @unchecked

    noTrans(PLANNED.toString) should not contain (DraftStatus.ARCHIVED)
    noTrans(IN_PROGRESS.toString) should not contain (DraftStatus.ARCHIVED)
    noTrans(EXTERNAL_REVIEW.toString) should not contain (DraftStatus.ARCHIVED)
    noTrans(INTERNAL_REVIEW.toString) should not contain (DraftStatus.ARCHIVED)
    noTrans(END_CONTROL.toString) should not contain (DraftStatus.ARCHIVED)
    noTrans(LANGUAGE.toString) should not contain (DraftStatus.ARCHIVED)
    noTrans(FOR_APPROVAL.toString) should not contain (DraftStatus.ARCHIVED)
    noTrans(PUBLISHED.toString) should not contain (DraftStatus.ARCHIVED)
    noTrans(UNPUBLISHED.toString) should not contain (DraftStatus.ARCHIVED)
  }

  test("stateTransitionsToApi should return different number of transitions based on access") {
    val Success(adminTrans) = stateTransitionRules.stateTransitionsToApi(TestData.userWithAdminAccess, None): @unchecked
    val Success(writeTrans) = stateTransitionRules.stateTransitionsToApi(TestData.userWithWriteAccess, None): @unchecked

    // format: off
    writeTrans(PLANNED.toString).length should be(adminTrans(PLANNED.toString).length)
    writeTrans(IN_PROGRESS.toString).length should be < adminTrans(IN_PROGRESS.toString).length
    writeTrans(EXTERNAL_REVIEW.toString).length should be < adminTrans(EXTERNAL_REVIEW.toString).length
    writeTrans(INTERNAL_REVIEW.toString).length should be < adminTrans(INTERNAL_REVIEW.toString).length
    writeTrans(END_CONTROL.toString).length should be < adminTrans(END_CONTROL.toString).length
    writeTrans(LANGUAGE.toString).length should be < adminTrans(LANGUAGE.toString).length
    writeTrans(FOR_APPROVAL.toString).length should be < adminTrans(FOR_APPROVAL.toString).length
    writeTrans(PUBLISHED.toString).length should be < adminTrans(PUBLISHED.toString).length
    writeTrans(UNPUBLISHED.toString).length should be < adminTrans(UNPUBLISHED.toString).length
    // format: on
  }

  test("stateTransitionsToApi should have transitions from all statuses if admin") {
    val Success(adminTrans) = stateTransitionRules.stateTransitionsToApi(TestData.userWithAdminAccess, None): @unchecked
    adminTrans.size should be(DraftStatus.values.size - 1)
  }

  test("stateTransitionsToApi should have transitions in inserted order") {
    val Success(adminTrans) = stateTransitionRules.stateTransitionsToApi(TestData.userWithAdminAccess, None): @unchecked
    adminTrans(LANGUAGE.toString) should be(
      Seq(
        IN_PROGRESS.toString,
        QUALITY_ASSURANCE.toString,
        LANGUAGE.toString,
        FOR_APPROVAL.toString,
        PUBLISHED.toString,
        ARCHIVED.toString,
      )
    )
    adminTrans(FOR_APPROVAL.toString) should be(
      Seq(
        IN_PROGRESS.toString,
        LANGUAGE.toString,
        FOR_APPROVAL.toString,
        END_CONTROL.toString,
        PUBLISHED.toString,
        ARCHIVED.toString,
      )
    )
  }

  test("updateStatus should return an IO[Failure] if the status change is illegal") {
    val Failure(res: IllegalStatusStateTransition) = stateTransitionRules.doTransition(
      TestData.sampleArticleWithByNcSa,
      PUBLISHED,
      TestData.userWithWriteAccess,
    ): @unchecked
    res.getMessage should equal(
      s"Cannot go to PUBLISHED when article is ${TestData.sampleArticleWithByNcSa.status.current}"
    )
  }

  test("Should not be able to go to ARCHIVED if published") {
    val status  = Status(DraftStatus.PLANNED, other = Set(DraftStatus.PUBLISHED))
    val article = TestData
      .sampleDomainArticle
      .copy(status = status, responsible = Some(Responsible("hei", clock.now())))
    val Failure(res: IllegalStatusStateTransition) =
      stateTransitionRules.doTransition(article, ARCHIVED, TestData.userWithPublishAccess): @unchecked

    res.getMessage should equal(s"Cannot go to ARCHIVED when article contains ${status.other}")
  }

}
