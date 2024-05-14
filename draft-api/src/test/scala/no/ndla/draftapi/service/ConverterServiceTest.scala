/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service

import no.ndla.common
import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.errors.ValidationException
import no.ndla.common.model.api.{Delete, Missing, UpdateWith}
import no.ndla.common.model.domain.*
import no.ndla.common.model.domain.draft.DraftStatus.*
import no.ndla.common.model.domain.draft.{Comment, Draft, DraftCopyright, DraftStatus}
import no.ndla.common.model.{NDLADate, api as commonApi}
import no.ndla.draftapi.model.api
import no.ndla.draftapi.model.api.{NewComment, UpdatedComment}
import no.ndla.draftapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.mapping.License.CC_BY
import no.ndla.network.tapir.auth.TokenUser
import no.ndla.validation.{ResourceType, TagAttribute}
import org.jsoup.nodes.Element
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock

import java.util.UUID
import scala.util.{Failure, Success}

class ConverterServiceTest extends UnitSuite with TestEnvironment {

  val service = new ConverterService

  test("toApiLicense defaults to unknown if the license was not found") {
    service.toApiLicense("invalid") should equal(commonApi.License("unknown", None, None))
  }

  test("toApiLicense converts a short license string to a license object with description and url") {
    service.toApiLicense(CC_BY.toString) should equal(
      commonApi.License(
        CC_BY.toString,
        Some("Creative Commons Attribution 4.0 International"),
        Some("https://creativecommons.org/licenses/by/4.0/")
      )
    )
  }

  test("toApiArticleTitle returns both title and plainTitle") {
    val title = Title("Title with <span data-language=\"uk\">ukrainian</span> word", "en")
    service.toApiArticleTitle(title) should equal(
      api.ArticleTitle("Title with ukrainian word", "Title with <span data-language=\"uk\">ukrainian</span> word", "en")
    )
  }

  test("toApiArticleIntroduction returns both introduction and plainIntroduction") {
    val introduction = Introduction("<p>Introduction with <em>emphasis</em></p>", "en")
    service.toApiArticleIntroduction(introduction) should equal(
      api.ArticleIntroduction("Introduction with emphasis", "<p>Introduction with <em>emphasis</em></p>", "en")
    )
  }

  test("toApiArticle converts a domain.Article to an api.ArticleV2") {
    when(draftRepository.getExternalIdsFromId(eqTo(TestData.articleId))(any)).thenReturn(List(TestData.externalId))
    service.toApiArticle(TestData.sampleDomainArticle, "nb") should equal(Success(TestData.apiArticleV2))
  }

  test("that toApiArticle returns sorted supportedLanguages") {
    when(draftRepository.getExternalIdsFromId(eqTo(TestData.articleId))(any)).thenReturn(List(TestData.externalId))
    val result = service.toApiArticle(
      TestData.sampleDomainArticle.copy(title = TestData.sampleDomainArticle.title :+ Title("hehe", "und")),
      "nb"
    )
    result.get.supportedLanguages should be(Seq("nb", "und"))
  }

  test("that toApiArticleV2 returns none if article does not exist on language, and fallback is not specified") {
    when(draftRepository.getExternalIdsFromId(eqTo(TestData.articleId))(any)).thenReturn(List(TestData.externalId))
    val result = service.toApiArticle(TestData.sampleDomainArticle, "en")
    result.isFailure should be(true)
  }

  test(
    "That toApiArticleV2 returns article on existing language if fallback is specified even if selected language does not exist"
  ) {
    when(draftRepository.getExternalIdsFromId(eqTo(TestData.articleId))(any)).thenReturn(List(TestData.externalId))
    val result = service.toApiArticle(TestData.sampleDomainArticle, "en", fallback = true)
    result.get.title.get.language should be("nb")
    result.get.title.get.title should be(TestData.sampleDomainArticle.title.head.title)
    result.isFailure should be(false)
  }

  test("toDomainArticleShould should remove unneeded attributes on embed-tags") {
    val content =
      s"""<h1>hello</h1><$EmbedTagName ${TagAttribute.DataResource}="${ResourceType.Image}" ${TagAttribute.DataUrl}="http://some-url" data-random="hehe" />"""
    val expectedContent = s"""<h1>hello</h1><$EmbedTagName ${TagAttribute.DataResource}="${ResourceType.Image}" />"""
    val visualElement =
      s"""<$EmbedTagName ${TagAttribute.DataResource}="${ResourceType.Image}" ${TagAttribute.DataUrl}="http://some-url" data-random="hehe" />"""
    val expectedVisualElement = s"""<$EmbedTagName ${TagAttribute.DataResource}="${ResourceType.Image}" />"""
    val apiArticle            = TestData.newArticle.copy(content = Some(content), visualElement = Some(visualElement))
    val expectedTime          = TestData.today

    when(clock.now()).thenReturn(expectedTime)

    val Success(result) = service.toDomainArticle(1, apiArticle, List.empty, TestData.userWithWriteAccess, None, None)
    result.content.head.content should equal(expectedContent)
    result.visualElement.head.resource should equal(expectedVisualElement)
    result.created should equal(expectedTime)
    result.updated should equal(expectedTime)
  }

  test("toDomainArticleShould should use created and updated dates from parameter list if defined") {
    val apiArticle = TestData.newArticle
    val created    = NDLADate.fromString("2016-12-06T16:20:05.000Z").get
    val updated    = NDLADate.fromString("2017-03-07T21:18:19.000Z").get

    val Success(result) =
      service.toDomainArticle(1, apiArticle, List.empty, TestData.userWithWriteAccess, Some(created), Some(updated))
    result.created should equal(created)
    result.updated should equal(updated)
  }

  test("toDomainArticle should fail if trying to update language fields without language being set") {
    val updatedArticle = TestData.sampleApiUpdateArticle.copy(language = None, title = Some("kakemonster"))
    val res =
      service.toDomainArticle(
        TestData.sampleDomainArticle.copy(status = Status(PLANNED, Set())),
        updatedArticle,
        isImported = false,
        TestData.userWithWriteAccess,
        None,
        None
      )
    res.isFailure should be(true)

    val errors = res.failed.get.asInstanceOf[ValidationException].errors
    errors.length should be(1)
    errors.head.message should equal("This field must be specified when updating language fields")
  }

  test("toDomainArticle should succeed if trying to update language fields with language being set") {
    val updatedArticle = TestData.sampleApiUpdateArticle.copy(language = Some("nb"), title = Some("kakemonster"))
    val Success(res) =
      service.toDomainArticle(
        TestData.sampleDomainArticle.copy(status = Status(PLANNED, Set())),
        updatedArticle,
        isImported = false,
        TestData.userWithWriteAccess,
        None,
        None
      )
    res.title.find(_.language == "nb").get.title should equal("kakemonster")
  }

  test("updateStatus should return an IO[Failure] if the status change is illegal") {
    val Failure(res: IllegalStatusStateTransition) =
      service.updateStatus(PUBLISHED, TestData.sampleArticleWithByNcSa, TestData.userWithWriteAccess, false)
    res.getMessage should equal(
      s"Cannot go to PUBLISHED when article is ${TestData.sampleArticleWithByNcSa.status.current}"
    )
  }

  test("stateTransitionsToApi should return only disabled entries if user has no roles") {
    val Success(res) = service.stateTransitionsToApi(TestData.userWithNoRoles, None)
    res.forall { case (_, to) => to.isEmpty } should be(true)
  }

  test("stateTransitionsToApi should allow all users to archive articles that have not been published") {
    val articleId: Long = 1
    val article: Draft =
      TestData.sampleArticleWithPublicDomain.copy(id = Some(articleId), status = Status(DraftStatus.PLANNED, Set()))
    when(draftRepository.withId(eqTo(articleId))(any)).thenReturn(Some(article))
    val Success(noTrans) = service.stateTransitionsToApi(TestData.userWithWriteAccess, Some(articleId))
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
    val article: Draft =
      TestData.sampleArticleWithPublicDomain.copy(id = Some(articleId), status = Status(DraftStatus.PUBLISHED, Set()))
    when(draftRepository.withId(eqTo(articleId))(any)).thenReturn(Some(article))
    val Success(noTrans) = service.stateTransitionsToApi(TestData.userWithWriteAccess, Some(articleId))

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
    val articleId: Long = 1
    val unpublished: Draft =
      TestData.sampleArticleWithPublicDomain.copy(id = Some(articleId), status = Status(DraftStatus.IN_PROGRESS, Set()))
    when(draftRepository.withId(eqTo(articleId))(any)).thenReturn(Some(unpublished))
    val Success(transOne) = service.stateTransitionsToApi(TestData.userWithWriteAccess, Some(articleId))
    transOne(IN_PROGRESS.toString) should not contain (DraftStatus.LANGUAGE.toString)

    val published: Draft =
      TestData.sampleArticleWithPublicDomain.copy(
        id = Some(articleId),
        status = Status(DraftStatus.IN_PROGRESS, Set(DraftStatus.PUBLISHED))
      )
    when(draftRepository.withId(eqTo(articleId))(any)).thenReturn(Some(published))
    val Success(transTwo) = service.stateTransitionsToApi(TestData.userWithWriteAccess, Some(articleId))
    transTwo(IN_PROGRESS.toString) should contain(DraftStatus.LANGUAGE.toString)
  }

  test("stateTransitionsToApi should not allow all users to archive articles that have previously been published") {

    val articleId = 1L
    val article: Draft =
      TestData.sampleArticleWithPublicDomain.copy(
        id = Some(articleId),
        status = Status(DraftStatus.PLANNED, Set(DraftStatus.PUBLISHED))
      )
    when(draftRepository.withId(eqTo(articleId))(any)).thenReturn(Some(article))
    val Success(noTrans) = service.stateTransitionsToApi(TestData.userWithWriteAccess, None)

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
    val Success(adminTrans) = service.stateTransitionsToApi(TestData.userWithAdminAccess, None)
    val Success(writeTrans) = service.stateTransitionsToApi(TestData.userWithWriteAccess, None)

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
    val Success(adminTrans) = service.stateTransitionsToApi(TestData.userWithAdminAccess, None)
    adminTrans.size should be(DraftStatus.values.size - 1)
  }

  test("stateTransitionsToApi should have transitions in inserted order") {
    val Success(adminTrans) = service.stateTransitionsToApi(TestData.userWithAdminAccess, None)
    adminTrans(LANGUAGE.toString) should be(
      Seq(
        IN_PROGRESS.toString,
        QUALITY_ASSURANCE.toString,
        LANGUAGE.toString,
        FOR_APPROVAL.toString,
        PUBLISHED.toString,
        ARCHIVED.toString
      )
    )
    adminTrans(FOR_APPROVAL.toString) should be(
      Seq(
        IN_PROGRESS.toString,
        LANGUAGE.toString,
        FOR_APPROVAL.toString,
        END_CONTROL.toString,
        PUBLISHED.toString,
        ARCHIVED.toString
      )
    )
  }

  test("newNotes should fail if empty strings are recieved") {
    service
      .newNotes(Seq("", "jonas"), TokenUser.apply("Kari", Set.empty, None), Status(DraftStatus.IN_PROGRESS, Set.empty))
      .isFailure should be(true)
  }

  test("Merging language fields of article should not delete not updated fields") {
    when(clock.now()).thenReturn(TestData.today)
    val status = Status(DraftStatus.PUBLISHED, other = Set.empty)
    val art = Draft(
      id = Some(3),
      revision = Some(4),
      status = status,
      title = Seq(Title("Title test", "nb")),
      content = Seq(ArticleContent("Content test", "nb")),
      copyright = TestData.sampleArticleWithByNcSa.copyright,
      tags = Seq(Tag(Seq("a", "b", "c"), "nb")),
      requiredLibraries = Seq(RequiredLibrary("", "", "")),
      visualElement = Seq(VisualElement("someembed", "nb")),
      introduction = Seq(Introduction("introduction", "nb")),
      metaDescription = Seq(Description("metadesc", "nb")),
      metaImage = Seq(ArticleMetaImage("123", "metaimgalt", "nb")),
      created = TestData.today,
      updated = TestData.today,
      updatedBy = "theuserthatchangeditid",
      published = TestData.today,
      articleType = ArticleType.Standard,
      notes = Seq(EditorNote("Note here", "sheeps", status, TestData.today)),
      previousVersionsNotes = Seq.empty,
      editorLabels = Seq.empty,
      grepCodes = Seq.empty,
      conceptIds = Seq.empty,
      availability = Availability.everyone,
      relatedContent = Seq.empty,
      revisionMeta = Seq.empty,
      responsible = None,
      slug = None,
      comments = Seq.empty,
      priority = Priority.Unspecified,
      started = false,
      qualityEvaluation = None
    )

    val updatedNothing = TestData.blankUpdatedArticle.copy(
      revision = 4,
      language = Some("nb")
    )

    val user = TokenUser("theuserthatchangeditid", Set.empty, None)

    service.toDomainArticle(art, updatedNothing, false, user, None, None).get should be(art)
  }

  test("mergeArticleLanguageFields should replace every field correctly") {
    when(clock.now()).thenReturn(TestData.today)
    val status = Status(DraftStatus.PUBLISHED, other = Set.empty)
    val art = Draft(
      id = Some(3),
      revision = Some(4),
      status = status,
      title = Seq(Title("Title test", "nb")),
      content = Seq(ArticleContent("Content test", "nb")),
      copyright = TestData.sampleArticleWithByNcSa.copyright,
      tags = Seq(Tag(Seq("a", "b", "c"), "nb")),
      requiredLibraries = Seq(RequiredLibrary("", "", "")),
      visualElement = Seq(VisualElement("someembed", "nb")),
      introduction = Seq(Introduction("introduction", "nb")),
      metaDescription = Seq(Description("metadesc", "nb")),
      metaImage = Seq(ArticleMetaImage("123", "metaimgalt", "nb")),
      created = TestData.today,
      updated = TestData.today,
      updatedBy = "theuserthatchangeditid",
      published = TestData.today,
      articleType = ArticleType.Standard,
      notes = Seq(EditorNote("Note here", "sheeps", status, TestData.today)),
      previousVersionsNotes = Seq.empty,
      editorLabels = Seq.empty,
      grepCodes = Seq.empty,
      conceptIds = Seq.empty,
      availability = Availability.everyone,
      relatedContent = Seq.empty,
      revisionMeta = Seq.empty,
      responsible = None,
      slug = None,
      comments = Seq.empty,
      priority = Priority.Unspecified,
      started = false,
      qualityEvaluation = None
    )

    val expectedArticle = Draft(
      id = Some(3),
      revision = Some(4),
      status = status,
      title = Seq(Title("NyTittel", "nb")),
      content = Seq(ArticleContent("NyContent", "nb")),
      copyright = TestData.sampleArticleWithByNcSa.copyright,
      tags = Seq(Tag(Seq("1", "2", "3"), "nb")),
      requiredLibraries = Seq(RequiredLibrary("", "", "")),
      visualElement = Seq(VisualElement("NyVisualElement", "nb")),
      introduction = Seq(Introduction("NyIntro", "nb")),
      metaDescription = Seq(Description("NyMeta", "nb")),
      metaImage = Seq(ArticleMetaImage("321", "NyAlt", "nb")),
      created = TestData.today,
      updated = TestData.today,
      updatedBy = "theuserthatchangeditid",
      published = TestData.today,
      articleType = ArticleType.Standard,
      notes = Seq(EditorNote("Note here", "sheeps", status, TestData.today)),
      previousVersionsNotes = Seq.empty,
      editorLabels = Seq.empty,
      grepCodes = Seq.empty,
      conceptIds = Seq.empty,
      availability = Availability.everyone,
      relatedContent = Seq.empty,
      revisionMeta = Seq.empty,
      responsible = None,
      slug = None,
      comments = Seq.empty,
      priority = Priority.Unspecified,
      started = false,
      qualityEvaluation = None
    )

    val updatedEverything = TestData.blankUpdatedArticle.copy(
      revision = 4,
      language = Some("nb"),
      title = Some("NyTittel"),
      status = None,
      published = None,
      content = Some("NyContent"),
      tags = Some(Seq("1", "2", "3")),
      introduction = Some("NyIntro"),
      metaDescription = Some("NyMeta"),
      metaImage = UpdateWith(api.NewArticleMetaImage("321", "NyAlt")),
      visualElement = Some("NyVisualElement"),
      copyright = None,
      requiredLibraries = None,
      articleType = None,
      notes = None,
      editorLabels = None,
      grepCodes = None,
      conceptIds = None,
      createNewVersion = None
    )

    val user = TokenUser("theuserthatchangeditid", Set.empty, None)
    service.toDomainArticle(art, updatedEverything, false, user, None, None).get should be(expectedArticle)

  }

  test("mergeArticleLanguageFields should merge every field correctly") {
    when(clock.now()).thenReturn(TestData.today)
    val status = Status(DraftStatus.PUBLISHED, other = Set.empty)
    val art = Draft(
      id = Some(3),
      revision = Some(4),
      status = status,
      title = Seq(Title("Title test", "nb")),
      content = Seq(ArticleContent("Content test", "nb")),
      copyright = TestData.sampleArticleWithByNcSa.copyright,
      tags = Seq(Tag(Seq("a", "b", "c"), "nb")),
      requiredLibraries = Seq(RequiredLibrary("", "", "")),
      visualElement = Seq(VisualElement("someembed", "nb")),
      introduction = Seq(Introduction("introduction", "nb")),
      metaDescription = Seq(Description("metadesc", "nb")),
      metaImage = Seq(ArticleMetaImage("123", "metaimgalt", "nb")),
      created = TestData.today,
      updated = TestData.today,
      updatedBy = "theuserthatchangeditid",
      published = TestData.today,
      articleType = ArticleType.Standard,
      notes = Seq(EditorNote("Note here", "sheeps", status, TestData.today)),
      previousVersionsNotes = Seq.empty,
      editorLabels = Seq.empty,
      grepCodes = Seq.empty,
      conceptIds = Seq.empty,
      availability = Availability.everyone,
      relatedContent = Seq.empty,
      revisionMeta = Seq.empty,
      responsible = None,
      slug = None,
      comments = Seq.empty,
      priority = Priority.Unspecified,
      started = false,
      qualityEvaluation = None
    )

    val expectedArticle = Draft(
      id = Some(3),
      revision = Some(4),
      status = status,
      title = Seq(Title("Title test", "nb"), Title("NyTittel", "en")),
      content = Seq(ArticleContent("Content test", "nb"), ArticleContent("NyContent", "en")),
      copyright = TestData.sampleArticleWithByNcSa.copyright,
      tags = Seq(Tag(Seq("a", "b", "c"), "nb"), Tag(Seq("1", "2", "3"), "en")),
      requiredLibraries = Seq(RequiredLibrary("", "", "")),
      visualElement = Seq(VisualElement("someembed", "nb"), VisualElement("NyVisualElement", "en")),
      introduction = Seq(Introduction("introduction", "nb"), Introduction("NyIntro", "en")),
      metaDescription = Seq(Description("metadesc", "nb"), Description("NyMeta", "en")),
      metaImage = Seq(ArticleMetaImage("123", "metaimgalt", "nb"), ArticleMetaImage("321", "NyAlt", "en")),
      created = TestData.today,
      updated = TestData.today,
      updatedBy = "theuserthatchangeditid",
      published = TestData.today,
      articleType = ArticleType.Standard,
      notes = Seq(
        EditorNote("Note here", "sheeps", status, TestData.today),
        EditorNote(
          "Ny språkvariant 'en' ble lagt til.",
          "theuserthatchangeditid",
          Status(PUBLISHED, Set()),
          TestData.today
        )
      ),
      previousVersionsNotes = Seq.empty,
      editorLabels = Seq.empty,
      grepCodes = Seq.empty,
      conceptIds = Seq.empty,
      availability = Availability.everyone,
      relatedContent = Seq.empty,
      revisionMeta = Seq.empty,
      responsible = None,
      slug = None,
      comments = Seq.empty,
      priority = Priority.Unspecified,
      started = false,
      qualityEvaluation = None
    )

    val updatedEverything = TestData.blankUpdatedArticle.copy(
      revision = 4,
      language = Some("en"),
      title = Some("NyTittel"),
      status = None,
      published = None,
      content = Some("NyContent"),
      tags = Some(Seq("1", "2", "3")),
      introduction = Some("NyIntro"),
      metaDescription = Some("NyMeta"),
      metaImage = UpdateWith(api.NewArticleMetaImage("321", "NyAlt")),
      visualElement = Some("NyVisualElement"),
      copyright = None,
      requiredLibraries = None,
      articleType = None,
      notes = None,
      editorLabels = None,
      grepCodes = None,
      conceptIds = None,
      createNewVersion = None
    )

    val user = TokenUser("theuserthatchangeditid", Set.empty, None)
    service.toDomainArticle(art, updatedEverything, false, user, None, None).get should be(expectedArticle)

  }

  test("toDomainArticle should merge notes correctly") {
    val updatedArticleWithoutNotes =
      TestData.sampleApiUpdateArticle.copy(language = Some("nb"), title = Some("kakemonster"))
    val updatedArticleWithNotes = TestData.sampleApiUpdateArticle.copy(
      language = Some("nb"),
      title = Some("kakemonster"),
      notes = Some(Seq("fleibede"))
    )
    val existingNotes = Seq(EditorNote("swoop", "", Status(PLANNED, Set()), TestData.today))
    val Success(res1) =
      service.toDomainArticle(
        TestData.sampleDomainArticle.copy(status = Status(PLANNED, Set()), notes = existingNotes),
        updatedArticleWithoutNotes,
        isImported = false,
        TestData.userWithWriteAccess,
        None,
        None
      )
    val Success(res2) =
      service.toDomainArticle(
        TestData.sampleDomainArticle.copy(status = Status(PLANNED, Set()), notes = Seq.empty),
        updatedArticleWithoutNotes,
        isImported = false,
        TestData.userWithWriteAccess,
        None,
        None
      )
    val Success(res3) =
      service.toDomainArticle(
        TestData.sampleDomainArticle.copy(status = Status(PLANNED, Set()), notes = existingNotes),
        updatedArticleWithNotes,
        isImported = false,
        TestData.userWithWriteAccess,
        None,
        None
      )
    val Success(res4) =
      service.toDomainArticle(
        TestData.sampleDomainArticle.copy(status = Status(PLANNED, Set()), notes = Seq.empty),
        updatedArticleWithNotes,
        isImported = false,
        TestData.userWithWriteAccess,
        None,
        None
      )

    res1.notes should be(existingNotes)
    res2.notes should be(Seq.empty)

    res3.notes.map(_.note) should be(Seq("swoop", "fleibede"))
    res4.notes.map(_.note) should be(Seq("fleibede"))
  }

  test("Should not be able to go to ARCHIVED if published") {
    val status = Status(DraftStatus.PLANNED, other = Set(DraftStatus.PUBLISHED))
    val article =
      TestData.sampleDomainArticle.copy(status = status, responsible = Some(Responsible("hei", clock.now())))
    val Failure(res: IllegalStatusStateTransition) =
      service.updateStatus(ARCHIVED, article, TestData.userWithPublishAccess, isImported = false)

    res.getMessage should equal(s"Cannot go to ARCHIVED when article contains ${status.other}")
  }

  test("Adding new language to article will add note") {
    val updatedArticleWithoutNotes =
      TestData.sampleApiUpdateArticle.copy(title = Some("kakemonster"))
    val updatedArticleWithNotes =
      TestData.sampleApiUpdateArticle.copy(title = Some("kakemonster"), notes = Some(Seq("fleibede")))
    val existingNotes = Seq(EditorNote("swoop", "", Status(PLANNED, Set()), TestData.today))
    val Success(res1) =
      service.toDomainArticle(
        TestData.sampleDomainArticle.copy(status = Status(PLANNED, Set()), notes = existingNotes),
        updatedArticleWithNotes.copy(language = Some("sna")),
        isImported = false,
        TestData.userWithWriteAccess,
        None,
        None
      )
    val Success(res2) =
      service.toDomainArticle(
        TestData.sampleDomainArticle.copy(status = Status(PLANNED, Set()), notes = existingNotes),
        updatedArticleWithNotes.copy(language = Some("nb")),
        isImported = false,
        TestData.userWithWriteAccess,
        None,
        None
      )
    val Success(res3) =
      service.toDomainArticle(
        TestData.sampleDomainArticle.copy(status = Status(PLANNED, Set()), notes = existingNotes),
        updatedArticleWithoutNotes.copy(language = Some("sna")),
        isImported = false,
        TestData.userWithWriteAccess,
        None,
        None
      )

    res1.notes.map(_.note) should be(Seq("swoop", "fleibede", s"Ny språkvariant 'sna' ble lagt til."))
    res2.notes.map(_.note) should be(Seq("swoop", "fleibede"))
    res3.notes.map(_.note) should be(Seq("swoop", s"Ny språkvariant 'sna' ble lagt til."))

  }

  test("toDomainArticle(NewArticle) should convert grepCodes correctly") {
    val Success(res1) = service.toDomainArticle(
      1,
      TestData.newArticle.copy(grepCodes = Some(Seq("a", "b"))),
      List(TestData.externalId),
      TestData.userWithWriteAccess,
      None,
      None
    )

    val Success(res2) = service.toDomainArticle(
      1,
      TestData.newArticle.copy(grepCodes = None),
      List(TestData.externalId),
      TestData.userWithWriteAccess,
      None,
      None
    )

    res1.grepCodes should be(Seq("a", "b"))
    res2.grepCodes should be(Seq.empty)
  }

  test("toDomainArticle(UpdateArticle) should convert grepCodes correctly") {
    val Success(res1) = service.toDomainArticle(
      TestData.sampleDomainArticle.copy(grepCodes = Seq("a", "b", "c")),
      TestData.sampleApiUpdateArticle.copy(grepCodes = Some(Seq("x", "y"))),
      isImported = false,
      TestData.userWithWriteAccess,
      None,
      None
    )

    val Success(res2) = service.toDomainArticle(
      TestData.sampleDomainArticle.copy(grepCodes = Seq("a", "b", "c")),
      TestData.sampleApiUpdateArticle.copy(grepCodes = Some(Seq.empty)),
      isImported = false,
      TestData.userWithWriteAccess,
      None,
      None
    )

    val Success(res3) = service.toDomainArticle(
      TestData.sampleDomainArticle.copy(grepCodes = Seq("a", "b", "c")),
      TestData.sampleApiUpdateArticle.copy(grepCodes = None),
      isImported = false,
      TestData.userWithWriteAccess,
      None,
      None
    )

    res1.grepCodes should be(Seq("x", "y"))
    res2.grepCodes should be(Seq.empty)
    res3.grepCodes should be(Seq("a", "b", "c"))
  }

  test("toDomainArticle(updateNullDocumentArticle) should convert grepCodes correctly") {

    val Success(res1) = service.toDomainArticle(
      1,
      TestData.sampleApiUpdateArticle.copy(grepCodes = Some(Seq("a", "b"))),
      isImported = false,
      TestData.userWithWriteAccess,
      None,
      None
    )

    val Success(res2) = service.toDomainArticle(
      2,
      TestData.sampleApiUpdateArticle.copy(grepCodes = Some(Seq.empty)),
      isImported = false,
      TestData.userWithWriteAccess,
      None,
      None
    )

    val Success(res3) = service.toDomainArticle(
      3,
      TestData.sampleApiUpdateArticle.copy(grepCodes = None),
      isImported = false,
      TestData.userWithWriteAccess,
      None,
      None
    )

    res1.grepCodes should be(Seq("a", "b"))
    res2.grepCodes should be(Seq.empty)
    res3.grepCodes should be(Seq.empty)
  }

  test("toDomainArticle(UpdateArticle) should update metaImage correctly") {

    val beforeUpdate = TestData.sampleDomainArticle.copy(
      metaImage = Seq(ArticleMetaImage("1", "Hei", "nb"), ArticleMetaImage("2", "Hej", "nn"))
    )

    val Success(res1) = service.toDomainArticle(
      beforeUpdate,
      TestData.sampleApiUpdateArticle.copy(language = Some("nb"), metaImage = Delete),
      isImported = false,
      TestData.userWithWriteAccess,
      None,
      None
    )

    val Success(res2) = service.toDomainArticle(
      beforeUpdate,
      TestData.sampleApiUpdateArticle
        .copy(language = Some("nb"), metaImage = UpdateWith(api.NewArticleMetaImage("1", "Hola"))),
      isImported = false,
      TestData.userWithWriteAccess,
      None,
      None
    )

    val Success(res3) = service.toDomainArticle(
      beforeUpdate,
      TestData.sampleApiUpdateArticle.copy(language = Some("nb"), metaImage = Missing),
      isImported = false,
      TestData.userWithWriteAccess,
      None,
      None
    )

    res1.metaImage should be(Seq(ArticleMetaImage("2", "Hej", "nn")))
    res2.metaImage should be(Seq(ArticleMetaImage("2", "Hej", "nn"), ArticleMetaImage("1", "Hola", "nb")))
    res3.metaImage should be(beforeUpdate.metaImage)
  }

  test("toDomainArticle(updateNullDocumentArticle) should update metaImage correctly") {

    val Success(res1) = service.toDomainArticle(
      1,
      TestData.sampleApiUpdateArticle.copy(metaImage = Delete),
      isImported = false,
      TestData.userWithWriteAccess,
      None,
      None
    )

    val Success(res2) = service.toDomainArticle(
      2,
      TestData.sampleApiUpdateArticle.copy(metaImage = UpdateWith(api.NewArticleMetaImage("1", "Hola"))),
      isImported = false,
      TestData.userWithWriteAccess,
      None,
      None
    )

    val Success(res3) = service.toDomainArticle(
      3,
      TestData.sampleApiUpdateArticle.copy(metaImage = Missing),
      isImported = false,
      TestData.userWithWriteAccess,
      None,
      None
    )

    res1.metaImage should be(Seq.empty)
    res2.metaImage should be(Seq(ArticleMetaImage("1", "Hola", "nb")))
    res3.metaImage should be(Seq.empty)
  }

  test("toDomainArticle should clone files if existing files appear in new language") {
    val embed1 =
      s"""<$EmbedTagName data-alt="Kul alt1" data-path="/files/resources/abc123.pdf" data-resource="file" data-title="Kul tittel1" data-type="pdf">"""
    val existingArticle = TestData.sampleDomainArticle.copy(
      content = Seq(ArticleContent(s"<section><h1>Hei</h1>$embed1</section>", "nb"))
    )

    val newContent = s"<section><h1>Hello</h1>$embed1</section>"
    val apiArticle = TestData.blankUpdatedArticle.copy(
      language = Some("en"),
      title = Some("Eng title"),
      content = Some(newContent)
    )

    when(writeService.cloneEmbedAndUpdateElement(any[Element])).thenAnswer((i: InvocationOnMock) => {
      val element = i.getArgument[Element](0)
      Success(element.attr("data-path", "/files/resources/new.pdf"))
    })

    val Success(_) = service.toDomainArticle(
      existingArticle,
      apiArticle,
      false,
      TestData.userWithWriteAccess,
      None,
      None
    )

  }

  test("Extracting h5p paths works as expected") {
    val enPath1 = s"/resources/${UUID.randomUUID().toString}"
    val enPath2 = s"/resources/${UUID.randomUUID().toString}"
    val nbPath1 = s"/resources/${UUID.randomUUID().toString}"
    val nbPath2 = s"/resources/${UUID.randomUUID().toString}"
    val vePath1 = s"/resources/${UUID.randomUUID().toString}"
    val vePath2 = s"/resources/${UUID.randomUUID().toString}"

    val expectedPaths = Seq(enPath1, enPath2, nbPath1, nbPath2, vePath1, vePath2).sorted

    val articleContentNb = ArticleContent(
      s"""<section><h1>Heisann</h1><$EmbedTagName data-path="$nbPath1" data-resource="h5p" /></section><section><p>Joda<$EmbedTagName data-path="$nbPath2" data-resource="h5p" /></p><$EmbedTagName data-resource="concept" data-path="thisisinvalidbutletsdoit"/></section>""",
      "nb"
    )
    val articleContentEn = ArticleContent(
      s"""<section><h1>Hello</h1><$EmbedTagName data-path="$enPath1" data-resource="h5p" /></section><section><p>Joda<$EmbedTagName data-path="$enPath2" data-resource="h5p" /></p><$EmbedTagName data-resource="concept" data-path="thisisinvalidbutletsdoit"/></section>""",
      "en"
    )

    val visualElementNb = VisualElement(s"""<$EmbedTagName data-path="$vePath1" data-resource="h5p" />""", "nb")
    val visualElementEn = VisualElement(s"""<$EmbedTagName data-path="$vePath2" data-resource="h5p" />""", "en")

    val article = TestData.sampleDomainArticle.copy(
      id = Some(1),
      content = Seq(articleContentNb, articleContentEn),
      visualElement = Seq(visualElementNb, visualElementEn)
    )
    service.getEmbeddedH5PPaths(article).sorted should be(expectedPaths)
  }

  test("toDomainArticle(NewArticle) should convert availability correctly") {
    val Success(res1) =
      service.toDomainArticle(
        1,
        TestData.newArticle.copy(availability = Some(Availability.teacher.toString)),
        List(TestData.externalId),
        TestData.userWithWriteAccess,
        None,
        None
      )

    val Success(res2) = service.toDomainArticle(
      1,
      TestData.newArticle.copy(availability = None),
      List(TestData.externalId),
      TestData.userWithWriteAccess,
      None,
      None
    )

    val Success(res3) = service.toDomainArticle(
      1,
      TestData.newArticle.copy(availability = Some("Krutte go")),
      List(TestData.externalId),
      TestData.userWithWriteAccess,
      None,
      None
    )

    res1.availability should be(Availability.teacher)
    res1.availability should not be (Availability.everyone)
    // Should default til everyone
    res2.availability should be(Availability.everyone)
    res3.availability should be(Availability.everyone)
  }

  test("toDomainArticle(UpdateArticle) should convert availability correctly") {
    val Success(res1) = service.toDomainArticle(
      TestData.sampleDomainArticle.copy(availability = Availability.everyone),
      TestData.sampleApiUpdateArticle.copy(availability = Some(Availability.teacher.toString)),
      isImported = false,
      TestData.userWithWriteAccess,
      None,
      None
    )

    val Success(res2) = service.toDomainArticle(
      TestData.sampleDomainArticle.copy(availability = Availability.everyone),
      TestData.sampleApiUpdateArticle.copy(availability = Some("Krutte go")),
      isImported = false,
      TestData.userWithWriteAccess,
      None,
      None
    )

    val Success(res3) = service.toDomainArticle(
      TestData.sampleDomainArticle.copy(availability = Availability.teacher),
      TestData.sampleApiUpdateArticle.copy(availability = None),
      isImported = false,
      TestData.userWithWriteAccess,
      None,
      None
    )

    res1.availability should be(Availability.teacher)
    res2.availability should be(Availability.everyone)
    res3.availability should be(Availability.teacher)
  }

  test("toDomainArticle(updateNullDocumentArticle) should convert availability correctly") {

    val Success(res1) =
      service.toDomainArticle(
        1,
        TestData.sampleApiUpdateArticle.copy(availability = Some(Availability.teacher.toString)),
        isImported = false,
        TestData.userWithWriteAccess,
        None,
        None
      )

    val Success(res2) = service.toDomainArticle(
      2,
      TestData.sampleApiUpdateArticle.copy(availability = Some("Krutte go")),
      isImported = false,
      TestData.userWithWriteAccess,
      None,
      None
    )

    val Success(res3) = service.toDomainArticle(
      3,
      TestData.sampleApiUpdateArticle.copy(availability = None),
      isImported = false,
      TestData.userWithWriteAccess,
      None,
      None
    )

    res1.availability should be(Availability.teacher)
    res2.availability should be(Availability.everyone)
    res3.availability should be(Availability.everyone)
  }

  test("toDomainArticle should convert relatedContent correctly") {

    val Success(res1) =
      service.toDomainArticle(
        1,
        TestData.sampleApiUpdateArticle.copy(relatedContent = Some(List(Right(1)))),
        isImported = false,
        TestData.userWithWriteAccess,
        None,
        None
      )

    res1.relatedContent should be(List(Right(1L)))
  }

  test("Changing responsible for article will add note") {

    val updatedArticleWithNotes =
      TestData.sampleApiUpdateArticle.copy(title = Some("kakemonster"), notes = Some(Seq("fleibede")))

    val existingNotes = Seq(EditorNote("swoop", "", Status(PLANNED, Set()), TestData.today))

    val existingRepsonsible = Responsible("oldId", TestData.today.minusDays(1))

    val Success(res1) =
      service.toDomainArticle(
        TestData.sampleDomainArticle
          .copy(status = Status(PLANNED, Set()), notes = existingNotes, responsible = Some(existingRepsonsible)),
        updatedArticleWithNotes.copy(language = Some("nb"), responsibleId = UpdateWith("nyid")),
        isImported = false,
        TestData.userWithWriteAccess,
        None,
        None
      )

    val Success(res2) =
      service.toDomainArticle(
        TestData.sampleDomainArticle
          .copy(status = Status(PLANNED, Set()), notes = existingNotes, responsible = None),
        updatedArticleWithNotes.copy(language = Some("nb"), responsibleId = UpdateWith("nyid")),
        isImported = false,
        TestData.userWithWriteAccess,
        None,
        None
      )
    val Success(res3) =
      service.toDomainArticle(
        TestData.sampleDomainArticle
          .copy(status = Status(PLANNED, Set()), notes = existingNotes, responsible = Some(existingRepsonsible)),
        updatedArticleWithNotes.copy(language = Some("nb")),
        isImported = false,
        TestData.userWithWriteAccess,
        None,
        None
      )

    res1.notes.map(_.note) should be(Seq("swoop", "fleibede", "Ansvarlig endret."))
    res2.notes.map(_.note) should be(Seq("swoop", "fleibede", "Ansvarlig endret."))
    res3.notes.map(_.note) should be(Seq("swoop", "fleibede"))

  }

  test("Changing responsible for article will update timestamp") {

    val updatedArticle      = TestData.sampleApiUpdateArticle.copy(title = Some("kakemonster"))
    val yesterday           = TestData.today.minusDays(1)
    val existingRepsonsible = Responsible("oldId", yesterday)

    val Success(res1) =
      service.toDomainArticle(
        TestData.sampleDomainArticle
          .copy(status = Status(PLANNED, Set()), responsible = Some(existingRepsonsible)),
        updatedArticle.copy(language = Some("nb"), responsibleId = UpdateWith("nyid")),
        isImported = false,
        TestData.userWithWriteAccess,
        None,
        None
      )
    val Success(res2) =
      service.toDomainArticle(
        TestData.sampleDomainArticle
          .copy(status = Status(PLANNED, Set()), responsible = None),
        updatedArticle.copy(language = Some("nb"), responsibleId = UpdateWith("nyid")),
        isImported = false,
        TestData.userWithWriteAccess,
        None,
        None
      )
    val Success(res3) =
      service.toDomainArticle(
        TestData.sampleDomainArticle
          .copy(status = Status(PLANNED, Set()), responsible = Some(existingRepsonsible)),
        updatedArticle.copy(language = Some("nb"), responsibleId = UpdateWith("oldId")),
        isImported = false,
        TestData.userWithWriteAccess,
        None,
        None
      )

    res1.responsible.get.responsibleId should be("nyid")
    res1.responsible.get.lastUpdated should not be (yesterday)
    res2.responsible.get.responsibleId should be("nyid")
    res2.responsible.get.lastUpdated should not be (yesterday)
    res3.responsible.get.responsibleId should be("oldId")
    res3.responsible.get.lastUpdated should be(yesterday)
  }

  test("that toArticleApiArticle transforms Draft to Article correctly") {
    when(clock.now()).thenReturn(TestData.today)
    val articleId = 42L
    val draft = Draft(
      id = Some(articleId),
      revision = Some(3),
      status = Status(PLANNED, Set.empty),
      title = Seq(Title("articleTitle", "nb")),
      content = Seq(ArticleContent("content", "nb")),
      copyright = Some(DraftCopyright(Some(CC_BY.toString), None, Seq.empty, Seq.empty, Seq.empty, None, None, false)),
      tags = Seq(Tag(Seq("a", "b", "zz"), "nb")),
      requiredLibraries = Seq(RequiredLibrary("asd", "library", "www/libra.ry")),
      visualElement = Seq(VisualElement("e", "nb")),
      introduction = Seq(Introduction("intro", "nb")),
      metaDescription = Seq(Description("desc", "nb")),
      metaImage = Seq(ArticleMetaImage("id", "alt", "nb")),
      created = clock.now(),
      updated = clock.now(),
      updatedBy = "meg",
      published = clock.now(),
      articleType = ArticleType.FrontpageArticle,
      notes = Seq(EditorNote("note", "meg", Status(PLANNED, Set.empty), clock.now())),
      previousVersionsNotes = Seq.empty,
      editorLabels = Seq("asd", "kek"),
      grepCodes = Seq("grep", "codes"),
      conceptIds = Seq(1, 2),
      availability = Availability.everyone,
      relatedContent = Seq.empty,
      revisionMeta = Seq.empty,
      responsible = None,
      slug = Some("kjempe-slug"),
      comments = Seq.empty,
      priority = Priority.Unspecified,
      started = false,
      qualityEvaluation = None
    )
    val article = common.model.domain.article.Article(
      id = Some(articleId),
      revision = Some(3),
      title = Seq(Title("articleTitle", "nb")),
      content = Seq(ArticleContent("content", "nb")),
      copyright =
        common.model.domain.article.Copyright(CC_BY.toString, None, Seq.empty, Seq.empty, Seq.empty, None, None, false),
      tags = Seq(Tag(Seq("a", "b", "zz"), "nb")),
      requiredLibraries = Seq(RequiredLibrary("asd", "library", "www/libra.ry")),
      visualElement = Seq(VisualElement("e", "nb")),
      introduction = Seq(Introduction("intro", "nb")),
      metaDescription = Seq(Description("desc", "nb")),
      metaImage = Seq(ArticleMetaImage("id", "alt", "nb")),
      created = clock.now(),
      updated = clock.now(),
      updatedBy = "meg",
      published = clock.now(),
      articleType = ArticleType.FrontpageArticle,
      grepCodes = Seq("grep", "codes"),
      conceptIds = Seq(1, 2),
      availability = Availability.everyone,
      relatedContent = Seq.empty,
      revisionDate = None,
      slug = Some("kjempe-slug")
    )

    val result = service.toArticleApiArticle(draft)
    result should be(Success(article))
  }

  test("that toArticleApiArticle fails if copyright is not present") {
    val draft                                 = TestData.sampleDomainArticle.copy(copyright = None)
    val Failure(result1: ValidationException) = service.toArticleApiArticle(draft)
    result1.errors.head.message should be("Copyright must be present when publishing an article")
  }

  test("that updatedCommentToDomain creates and updates comments correctly") {
    val uuid = UUID.randomUUID()
    val now  = NDLADate.now()
    when(clock.now()).thenReturn(now)
    when(uuidUtil.randomUUID()).thenReturn(uuid)

    val updatedComments =
      List(
        UpdatedComment(id = None, content = "hei", isOpen = Some(true), solved = Some(false)),
        UpdatedComment(id = Some(uuid.toString), content = "yoo", isOpen = Some(false), solved = Some(false))
      )
    val existingComments =
      Seq(Comment(id = uuid, created = now, updated = now, content = "nja", isOpen = true, solved = false))
    val expectedComments = Seq(
      Comment(id = uuid, created = now, updated = now, content = "hei", isOpen = true, solved = false),
      Comment(id = uuid, created = now, updated = now, content = "yoo", isOpen = false, solved = false)
    )
    service.updatedCommentToDomain(updatedComments, existingComments) should be(expectedComments)
  }

  test("that updatedCommentToDomain only keeps updatedComments and deletes rest") {
    val uuid  = UUID.randomUUID()
    val uuid2 = UUID.randomUUID()
    val uuid3 = UUID.randomUUID()
    val now   = NDLADate.now()
    when(clock.now()).thenReturn(now)

    val updatedComments = List(
      UpdatedComment(id = Some(uuid.toString), content = "updated keep", isOpen = Some(true), solved = Some(false))
    )
    val existingComments = Seq(
      Comment(id = uuid, created = now, updated = now, content = "keep", isOpen = true, solved = false),
      Comment(id = uuid2, created = now, updated = now, content = "delete", isOpen = true, solved = false),
      Comment(id = uuid3, created = now, updated = now, content = "delete", isOpen = true, solved = false)
    )
    val expectedComments =
      Seq(Comment(id = uuid, created = now, updated = now, content = "updated keep", isOpen = true, solved = false))
    val result = service.updatedCommentToDomain(updatedComments, existingComments)
    result should be(expectedComments)
  }

  test("that newCommentToDomain creates comments correctly") {
    val uuid = UUID.randomUUID()
    val now  = NDLADate.now()
    when(clock.now()).thenReturn(now)

    val newComments = List(NewComment(content = "hei", isOpen = None))
    val expectedComment =
      Comment(id = uuid, created = now, updated = now, content = "hei", isOpen = true, solved = false)
    service.newCommentToDomain(newComments).head.copy(id = uuid) should be(expectedComment)
  }

  test("that updatedCommentToDomainNullDocument creates and updates comments correctly") {
    val uuid = UUID.randomUUID()
    val now  = NDLADate.now()
    when(clock.now()).thenReturn(now)
    when(uuidUtil.randomUUID()).thenReturn(uuid)

    val updatedComments =
      List(
        UpdatedComment(id = None, content = "hei", isOpen = Some(true), solved = Some(false)),
        UpdatedComment(id = Some(uuid.toString), content = "yoo", isOpen = None, solved = Some(false))
      )
    val expectedComments = Success(
      Seq(
        Comment(id = uuid, created = now, updated = now, content = "hei", isOpen = true, solved = false),
        Comment(id = uuid, created = now, updated = now, content = "yoo", isOpen = true, solved = false)
      )
    )
    service.updatedCommentToDomainNullDocument(updatedComments) should be(expectedComments)
  }

  test("that updatedCommentToDomainNullDocument fails if UUID is malformed") {
    val updatedComments =
      List(UpdatedComment(id = Some("malformed-UUID"), content = "yoo", isOpen = Some(true), solved = Some(false)))
    service.updatedCommentToDomainNullDocument(updatedComments).isFailure should be(true)
  }

  test("filterComments should remove comments") {
    val content =
      Seq(
        ArticleContent(
          s"""<h1>hello</h1><$EmbedTagName ${TagAttribute.DataResource}="${ResourceType.Comment}" ${TagAttribute.DataText}="Dette er min kommentar" ${TagAttribute.DataType}="inline"><p>Litt tekst</p></$EmbedTagName>""",
          "nb"
        )
      )
    val expectedContent = Seq(ArticleContent(s"""<h1>hello</h1><p>Litt tekst</p>""", "nb"))

    val blockContent =
      Seq(
        ArticleContent(
          s"""<h1>hello</h1><$EmbedTagName ${TagAttribute.DataResource}="${ResourceType.Comment}" ${TagAttribute.DataText}="Dette er min kommentar" ${TagAttribute.DataType}="block"/>""",
          "nb"
        ),
        ArticleContent(
          s"""<h1>hello</h1><$EmbedTagName ${TagAttribute.DataResource}="${ResourceType.Comment}" ${TagAttribute.DataText}="Dette er min kommentar" ${TagAttribute.DataType}="block"/>""",
          "nn"
        )
      )

    val expectedBlockContent =
      Seq(
        ArticleContent(
          s"""<h1>hello</h1>""",
          "nb"
        ),
        ArticleContent(
          s"""<h1>hello</h1>""",
          "nn"
        )
      )

    val expectedTime = TestData.today

    when(clock.now()).thenReturn(expectedTime)

    val result      = service.filterComments(content)
    val blockResult = service.filterComments(blockContent)
    result should be(expectedContent)
    blockResult should be(expectedBlockContent)
  }

}
