/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.service

import no.ndla.common.model.domain.Availability
import no.ndla.draftapi.auth.{Role, UserInfo}
import no.ndla.draftapi.integration.{Resource, Topic}
import no.ndla.draftapi.model.api.{ArticleApiArticle, PartialArticleFields}
import no.ndla.draftapi.model.domain.ArticleStatus.{DRAFT, PUBLISHED}
import no.ndla.draftapi.model.domain._
import no.ndla.draftapi.model.{api, domain}
import no.ndla.draftapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.validation.{HtmlTagRules, ValidationMessage}
import org.mockito.ArgumentMatchers._
import org.mockito.invocation.InvocationOnMock
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatra.servlet.FileItem
import scalikejdbc.DBSession

import java.io.ByteArrayInputStream
import java.time.LocalDateTime
import java.util.UUID
import scala.util.{Failure, Success, Try}

class WriteServiceTest extends UnitSuite with TestEnvironment {
  override val converterService = new ConverterService

  val today: LocalDateTime     = LocalDateTime.now()
  val yesterday: LocalDateTime = LocalDateTime.now().minusDays(1)
  val service                  = new WriteService()

  val articleId   = 13
  val agreementId = 14

  val article: Article =
    TestData.sampleArticleWithPublicDomain.copy(id = Some(articleId), created = yesterday, updated = yesterday)

  val topicArticle: Article =
    TestData.sampleTopicArticle.copy(id = Some(articleId), created = yesterday, updated = yesterday)
  val agreement: Agreement = TestData.sampleDomainAgreement.copy(id = Some(agreementId))

  override def beforeEach(): Unit = {
    reset(
      articleIndexService,
      draftRepository,
      agreementIndexService,
      tagIndexService,
      grepCodesIndexService,
      agreementRepository,
      contentValidator
    )

    when(draftRepository.withId(articleId)).thenReturn(Option(article))
    when(agreementRepository.withId(agreementId)).thenReturn(Option(agreement))
    when(articleIndexService.indexDocument(any[Article])).thenAnswer((invocation: InvocationOnMock) =>
      Try(invocation.getArgument[Article](0))
    )
    when(tagIndexService.indexDocument(any[Article])).thenAnswer((invocation: InvocationOnMock) =>
      Try(invocation.getArgument[Article](0))
    )
    when(grepCodesIndexService.indexDocument(any[Article])).thenAnswer((invocation: InvocationOnMock) =>
      Try(invocation.getArgument[Article](0))
    )
    when(agreementIndexService.indexDocument(any[Agreement])).thenAnswer((invocation: InvocationOnMock) =>
      Try(invocation.getArgument[Agreement](0))
    )
    when(readService.addUrlsOnEmbedResources(any[Article])).thenAnswer((invocation: InvocationOnMock) =>
      invocation.getArgument[Article](0)
    )
    when(contentValidator.validateArticle(any[Article])).thenReturn(Success(article))
    when(contentValidator.validateAgreement(any[Agreement], any[Seq[ValidationMessage]])).thenReturn(Success(agreement))
    when(draftRepository.getExternalIdsFromId(any[Long])(any[DBSession])).thenReturn(List("1234"))
    when(clock.now()).thenReturn(today)
    when(draftRepository.updateArticle(any[Article], any[Boolean])(any[DBSession]))
      .thenAnswer((invocation: InvocationOnMock) => {
        val arg = invocation.getArgument[Article](0)
        Try(arg.copy(revision = Some(arg.revision.getOrElse(0) + 1)))
      })
    when(draftRepository.storeArticleAsNewVersion(any[Article], any[Option[UserInfo]])(any[DBSession]))
      .thenAnswer((invocation: InvocationOnMock) => {
        val arg = invocation.getArgument[Article](0)
        Try(arg.copy(revision = Some(arg.revision.getOrElse(0) + 1)))
      })

    when(agreementRepository.update(any[Agreement])(any[DBSession])).thenAnswer((invocation: InvocationOnMock) => {
      val arg = invocation.getArgument[Agreement](0)
      Try(arg)
    })
    when(taxonomyApiClient.updateTaxonomyIfExists(any[Long], any[domain.Article])).thenAnswer((i: InvocationOnMock) => {
      Success(i.getArgument[Long](0))
    })
  }

  test("newArticle should insert a given article") {
    when(draftRepository.getExternalIdsFromId(any[Long])(any[DBSession])).thenReturn(List.empty)
    when(contentValidator.validateArticle(any[Article])).thenReturn(Success(article))
    when(draftRepository.newEmptyArticle(any[List[String]], any[List[String]])(any[DBSession]))
      .thenReturn(Success(1: Long))

    service
      .newArticle(TestData.newArticle, List.empty, Seq.empty, TestData.userWithWriteAccess, None, None, None)
      .isSuccess should be(true)

    verify(draftRepository, times(1)).newEmptyArticle()
    verify(draftRepository, times(0)).insert(any[Article])(any)
    verify(draftRepository, times(1)).updateArticle(any[Article], any[Boolean])(any)
    verify(articleIndexService, times(1)).indexDocument(any[Article])
  }

  test("newAgreement should insert a given Agreement") {
    when(agreementRepository.insert(any[Agreement])(any[DBSession])).thenReturn(agreement)
    when(contentValidator.validateAgreement(any[Agreement], any[Seq[ValidationMessage]])).thenReturn(Success(agreement))

    service.newAgreement(TestData.newAgreement, TestData.userWithWriteAccess).get.id.toString should equal(
      agreement.id.get.toString
    )
    verify(agreementRepository, times(1)).insert(any[Agreement])(any)
    verify(agreementIndexService, times(1)).indexDocument(any[Agreement])
  }

  test("That updateAgreement updates only content properly") {
    val newContent          = "NyContentTest"
    val updatedApiAgreement = api.UpdatedAgreement(None, Some(newContent), None)
    val expectedAgreement   = agreement.copy(content = newContent, updated = today)

    service.updateAgreement(agreementId, updatedApiAgreement, TestData.userWithWriteAccess).get should equal(
      converterService.toApiAgreement(expectedAgreement)
    )
  }

  test("That updateArticle updates only content properly") {
    val newContent = "NyContentTest"
    val updatedApiArticle = TestData.blankUpdatedArticle.copy(
      revision = 1,
      language = Some("en"),
      content = Some(newContent)
    )
    val expectedArticle =
      article.copy(
        revision = Some(article.revision.get + 1),
        content = Seq(ArticleContent(newContent, "en")),
        updated = today
      )

    when(writeService.partialPublish(any, any, any)).thenReturn((expectedArticle.id.get, Success(expectedArticle)))
    when(articleApiClient.partialPublishArticle(any, any)).thenReturn(Success(expectedArticle.id.get))

    service.updateArticle(
      articleId,
      updatedApiArticle,
      List.empty,
      Seq.empty,
      TestData.userWithWriteAccess,
      None,
      None,
      None
    ) should equal(converterService.toApiArticle(expectedArticle, "en"))
  }

  test("That updateArticle updates only title properly") {
    val newTitle = "NyTittelTest"
    val updatedApiArticle = TestData.blankUpdatedArticle.copy(
      revision = 1,
      language = Some("en"),
      title = Some(newTitle)
    )
    val expectedArticle =
      article.copy(
        revision = Some(article.revision.get + 1),
        title = Seq(ArticleTitle(newTitle, "en")),
        updated = today
      )

    service.updateArticle(
      articleId,
      updatedApiArticle,
      List.empty,
      Seq.empty,
      TestData.userWithWriteAccess,
      None,
      None,
      None
    ) should equal(converterService.toApiArticle(expectedArticle, "en"))
  }

  test("That updateArticle updates multiple fields properly") {
    val updatedTitle           = "NyTittelTest"
    val updatedPublishedDate   = yesterday
    val updatedContent         = "NyContentTest"
    val updatedTags            = Seq("en", "to", "tre")
    val updatedMetaDescription = "updatedMetaHere"
    val updatedIntro           = "introintro"
    val updatedMetaId          = "1234"
    val updatedMetaAlt         = "HeheAlt"
    val newImageMeta           = api.NewArticleMetaImage(updatedMetaId, updatedMetaAlt)
    val updatedVisualElement   = "<embed something>"
    val updatedCopyright = api.Copyright(
      Some(api.License("a", Some("b"), None)),
      Some("c"),
      Seq(api.Author("Opphavsmann", "Jonas")),
      List(),
      List(),
      None,
      None,
      None
    )
    val updatedRequiredLib = api.RequiredLibrary("tjup", "tjap", "tjim")
    val updatedArticleType = "topic-article"

    val updatedApiArticle = TestData.blankUpdatedArticle.copy(
      revision = 1,
      language = Some("en"),
      title = Some(updatedTitle),
      status = Some("DRAFT"),
      published = Some(updatedPublishedDate),
      content = Some(updatedContent),
      tags = Some(updatedTags),
      introduction = Some(updatedIntro),
      metaDescription = Some(updatedMetaDescription),
      metaImage = Right(Some(newImageMeta)),
      visualElement = Some(updatedVisualElement),
      copyright = Some(updatedCopyright),
      requiredLibraries = Some(Seq(updatedRequiredLib)),
      articleType = Some(updatedArticleType)
    )

    val expectedArticle = article.copy(
      revision = Some(article.revision.get + 1),
      title = Seq(ArticleTitle(updatedTitle, "en")),
      content = Seq(ArticleContent(updatedContent, "en")),
      copyright =
        Some(Copyright(Some("a"), Some("c"), Seq(Author("Opphavsmann", "Jonas")), List(), List(), None, None, None)),
      tags = Seq(ArticleTag(Seq("en", "to", "tre"), "en")),
      requiredLibraries = Seq(RequiredLibrary("tjup", "tjap", "tjim")),
      visualElement = Seq(VisualElement(updatedVisualElement, "en")),
      introduction = Seq(ArticleIntroduction(updatedIntro, "en")),
      metaDescription = Seq(ArticleMetaDescription(updatedMetaDescription, "en")),
      metaImage = Seq(ArticleMetaImage(updatedMetaId, updatedMetaAlt, "en")),
      updated = today,
      published = yesterday,
      articleType = ArticleType.TopicArticle
    )

    service.updateArticle(
      articleId,
      updatedApiArticle,
      List.empty,
      Seq.empty,
      TestData.userWithWriteAccess,
      None,
      None,
      None
    ) should equal(converterService.toApiArticle(expectedArticle, "en"))
  }

  test("updateArticle should use user-defined status if defined") {
    val existing       = TestData.sampleDomainArticle.copy(status = TestData.statusWithDraft)
    val updatedArticle = TestData.sampleApiUpdateArticle.copy(status = Some("PROPOSAL"))
    when(draftRepository.withId(existing.id.get)).thenReturn(Some(existing))
    val Success(result) = service.updateArticle(
      existing.id.get,
      updatedArticle,
      List.empty,
      Seq.empty,
      TestData.userWithWriteAccess,
      None,
      None,
      None
    )
    result.status should equal(api.Status("PROPOSAL", Seq.empty))
  }

  test(
    "updateArticle should set status to PROPOSAL if user-defined status is undefined and current status is PUBLISHED"
  ) {
    val updatedArticle = TestData.sampleApiUpdateArticle.copy(status = None)

    val existing = TestData.sampleDomainArticle.copy(status = TestData.statusWithPublished)
    when(draftRepository.withId(existing.id.get)).thenReturn(Some(existing))
    val Success(result) = service.updateArticle(
      existing.id.get,
      updatedArticle,
      List.empty,
      Seq.empty,
      TestData.userWithWriteAccess,
      None,
      None,
      None
    )
    result.status should equal(api.Status("PROPOSAL", Seq("PUBLISHED")))
  }

  test("updateArticle should use current status if user-defined status is not set") {
    val updatedArticle = TestData.sampleApiUpdateArticle.copy(status = None)

    {
      val existing = TestData.sampleDomainArticle.copy(status = TestData.statusWithProposal)
      when(draftRepository.withId(existing.id.get)).thenReturn(Some(existing))
      val Success(result) = service.updateArticle(
        existing.id.get,
        updatedArticle,
        List.empty,
        Seq.empty,
        TestData.userWithWriteAccess,
        None,
        None,
        None
      )
      result.status should equal(api.Status("PROPOSAL", Seq.empty))
    }

    {
      val existing = TestData.sampleDomainArticle.copy(status = TestData.statusWithUserTest)
      when(draftRepository.withId(existing.id.get)).thenReturn(Some(existing))
      val Success(result) = service.updateArticle(
        existing.id.get,
        updatedArticle,
        List.empty,
        Seq.empty,
        TestData.userWithWriteAccess,
        None,
        None,
        None
      )
      result.status should equal(api.Status("USER_TEST", Seq.empty))
    }

    {
      val existing = TestData.sampleDomainArticle.copy(status = TestData.statusWithAwaitingQA)
      when(draftRepository.withId(existing.id.get)).thenReturn(Some(existing))
      val Success(result) = service.updateArticle(
        existing.id.get,
        updatedArticle,
        List.empty,
        Seq.empty,
        TestData.userWithWriteAccess,
        None,
        None,
        None
      )
      result.status should equal(api.Status("AWAITING_QUALITY_ASSURANCE", Seq.empty))
    }

    {
      val existing = TestData.sampleDomainArticle.copy(status = TestData.statusWithQueuedForPublishing)
      when(draftRepository.withId(existing.id.get)).thenReturn(Some(existing))
      when(contentValidator.validateArticle(any[Article])).thenReturn(Success(existing))
      when(articleApiClient.validateArticle(any[ArticleApiArticle], any[Boolean])).thenAnswer((i: InvocationOnMock) => {
        Success(i.getArgument[ArticleApiArticle](0))
      })
      val Success(result) = service.updateArticle(
        existing.id.get,
        updatedArticle,
        List.empty,
        Seq.empty,
        TestData.userWithWriteAccess,
        None,
        None,
        None
      )
      result.status should equal(api.Status("QUEUED_FOR_PUBLISHING", Seq.empty))
    }
  }

  test("That delete article should fail when only one language") {
    val Failure(result) = service.deleteLanguage(article.id.get, "nb", UserInfo("asdf", Set()))
    result.getMessage should equal("Only one language left")
  }

  test("That delete article removes language from all languagefields") {
    val article =
      TestData.sampleDomainArticle.copy(
        id = Some(3),
        title = Seq(ArticleTitle("title", "nb"), ArticleTitle("title", "nn"))
      )
    val articleCaptor: ArgumentCaptor[Article] = ArgumentCaptor.forClass(classOf[Article])

    when(draftRepository.withId(anyLong)).thenReturn(Some(article))
    service.deleteLanguage(article.id.get, "nn", UserInfo("asdf", Set()))
    verify(draftRepository).updateArticle(articleCaptor.capture(), anyBoolean)(any)

    articleCaptor.getValue.title.length should be(1)
  }

  test("That get file extension will split extension and name as expected") {
    val a = "test.pdf"
    val b = "test"
    val c = "te.st.csv"
    val d = ".te....st.txt"
    val e = "kek.jpeg"

    service.getFileExtension(a) should be(Success(".pdf"))
    service.getFileExtension(b).isFailure should be(true)
    service.getFileExtension(c) should be(Success(".csv"))
    service.getFileExtension(d) should be(Success(".txt"))
    service.getFileExtension(e).isFailure should be(true)
  }

  test("uploading file calls fileStorageService as expected") {
    val fileToUpload           = mock[FileItem]
    val fileBytes: Array[Byte] = "these are not the bytes you're looking for".getBytes
    when(fileToUpload.get()).thenReturn(fileBytes)
    when(fileToUpload.size).thenReturn(fileBytes.length)
    when(fileToUpload.getContentType).thenReturn(Some("application/pdf"))
    when(fileToUpload.name).thenReturn("myfile.pdf")
    when(fileStorage.resourceExists(anyString())).thenReturn(false)
    when(
      fileStorage
        .uploadResourceFromStream(
          any[ByteArrayInputStream],
          anyString(),
          eqTo("application/pdf"),
          eqTo(fileBytes.length.toLong)
        )
    )
      .thenAnswer((i: InvocationOnMock) => {
        val fn = i.getArgument[String](1)
        Success(s"resource/$fn")
      })

    val uploaded = service.uploadFile(fileToUpload)

    val storageKeyCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
    uploaded.isSuccess should be(true)
    verify(fileStorage, times(1)).resourceExists(anyString)
    verify(fileStorage, times(1)).uploadResourceFromStream(
      any[ByteArrayInputStream],
      storageKeyCaptor.capture(),
      eqTo("application/pdf"),
      eqTo(fileBytes.length.toLong)
    )
    storageKeyCaptor.getValue.endsWith(".pdf") should be(true)
  }

  test("That updateStatus indexes the updated article") {
    reset(articleIndexService)
    reset(searchApiClient)

    val articleToUpdate = TestData.sampleDomainArticle.copy(id = Some(10), updated = yesterday)
    val user            = UserInfo("Pelle", Set(Role.WRITE))
    val updatedArticle = converterService
      .updateStatus(ArticleStatus.PROPOSAL, articleToUpdate, user, isImported = false)
      .attempt
      .unsafeRunSync()
      .toTry
      .get
      .get
    val updatedAndInserted = updatedArticle
      .copy(
        revision = updatedArticle.revision.map(_ + 1),
        updated = today,
        notes = updatedArticle.notes.map(_.copy(timestamp = today))
      )

    when(draftRepository.withId(10)).thenReturn(Some(articleToUpdate))
    when(draftRepository.updateArticle(any[Article], eqTo(false))(any)).thenReturn(Success(updatedAndInserted))

    when(articleIndexService.indexDocument(any[Article])).thenReturn(Success(updatedAndInserted))
    when(searchApiClient.indexDraft(any[Article])).thenReturn(updatedAndInserted)

    service.updateArticleStatus(ArticleStatus.PROPOSAL, 10, user, isImported = false)

    val argCap1: ArgumentCaptor[Article] = ArgumentCaptor.forClass(classOf[Article])
    val argCap2: ArgumentCaptor[Article] = ArgumentCaptor.forClass(classOf[Article])

    verify(articleIndexService, times(1)).indexDocument(argCap1.capture())
    verify(searchApiClient, times(1)).indexDraft(argCap2.capture())

    val captured1 = argCap1.getValue
    captured1.copy(updated = today, notes = captured1.notes.map(_.copy(timestamp = today))) should be(
      updatedAndInserted
    )

    val captured2 = argCap2.getValue
    captured2.copy(updated = today, notes = captured2.notes.map(_.copy(timestamp = today))) should be(
      updatedAndInserted
    )
  }
  test("That we only validate the given language") {
    val updatedArticle = TestData.sampleApiUpdateArticle.copy(language = Some("nb"))
    val article =
      TestData.sampleDomainArticle.copy(
        id = Some(5),
        content =
          Seq(ArticleContent("<section> Valid Content </section>", "nb"), ArticleContent("<div> content <div", "nn"))
      )
    val nbArticle =
      article.copy(
        content = Seq(ArticleContent("<section> Valid Content </section>", "nb"))
      )

    when(draftRepository.withId(anyLong)).thenReturn(Some(article))
    service.updateArticle(1, updatedArticle, List(), List(), TestData.userWithPublishAccess, None, None, None)

    val argCap: ArgumentCaptor[Article] = ArgumentCaptor.forClass(classOf[Article])
    verify(contentValidator, times(1)).validateArticle(argCap.capture())
    val captured = argCap.getValue
    captured.content should equal(nbArticle.content)
  }

  test("That articles are cloned with reasonable values") {
    val yesterday = LocalDateTime.now().minusDays(1)
    val today     = LocalDateTime.now()

    when(clock.now()).thenReturn(today)

    val article =
      TestData.sampleDomainArticle.copy(
        id = Some(5),
        title = Seq(domain.ArticleTitle("Tittel", "nb"), domain.ArticleTitle("Title", "en")),
        status = Status(ArticleStatus.PUBLISHED, Set(ArticleStatus.IMPORTED)),
        updated = yesterday,
        created = yesterday.minusDays(1),
        published = yesterday
      )

    val userinfo = UserInfo("somecoolid", Set.empty)

    val newId = 1231.toLong
    when(draftRepository.newEmptyArticle(any[List[String]], any[List[String]])(any[DBSession]))
      .thenReturn(Success(newId))

    val expectedInsertedArticle = article.copy(
      id = Some(newId),
      title = Seq(domain.ArticleTitle("Tittel (Kopi)", "nb"), domain.ArticleTitle("Title (Kopi)", "en")),
      revision = Some(1),
      updated = today,
      created = today,
      published = today,
      updatedBy = userinfo.id,
      status = Status(ArticleStatus.DRAFT, Set.empty),
      notes = article.notes ++
        converterService
          .newNotes(
            Seq("Opprettet artikkel, som kopi av artikkel med id: '5'."),
            userinfo,
            Status(ArticleStatus.DRAFT, Set.empty)
          )
          .get
    )
    when(draftRepository.withId(anyLong)).thenReturn(Some(article))
    when(draftRepository.insert(any[Article])(any[DBSession])).thenAnswer((i: InvocationOnMock) =>
      i.getArgument[Article](0)
    )

    service.copyArticleFromId(5, userinfo, "*", true, true)

    val cap: ArgumentCaptor[Article] = ArgumentCaptor.forClass(classOf[Article])
    verify(draftRepository, times(1)).insert(cap.capture())(any[DBSession])
    val insertedArticle = cap.getValue
    insertedArticle should be(expectedInsertedArticle)
  }

  test("That articles are cloned without title postfix if flag is false") {
    val yesterday = LocalDateTime.now().minusDays(1)
    val today     = LocalDateTime.now().withNano(0)

    when(clock.now()).thenReturn(today)

    val article = TestData.sampleDomainArticle.copy(
      id = Some(5),
      title = Seq(domain.ArticleTitle("Tittel", "nb"), domain.ArticleTitle("Title", "en")),
      status = Status(ArticleStatus.PUBLISHED, Set(ArticleStatus.IMPORTED)),
      updated = yesterday,
      created = yesterday.minusDays(1),
      published = yesterday
    )

    val userinfo = UserInfo("somecoolid", Set.empty)

    val newId = 1231.toLong
    when(draftRepository.newEmptyArticle(any[List[String]], any[List[String]])(any[DBSession]))
      .thenReturn(Success(newId))

    val expectedInsertedArticle = article.copy(
      id = Some(newId),
      revision = Some(1),
      updated = today,
      created = today,
      published = today,
      updatedBy = userinfo.id,
      status = Status(ArticleStatus.DRAFT, Set.empty),
      notes = article.notes ++
        converterService
          .newNotes(
            Seq("Opprettet artikkel, som kopi av artikkel med id: '5'."),
            userinfo,
            Status(ArticleStatus.DRAFT, Set.empty)
          )
          .get
    )
    when(draftRepository.withId(anyLong)).thenReturn(Some(article))
    when(draftRepository.insert(any[Article])(any[DBSession])).thenAnswer((i: InvocationOnMock) =>
      i.getArgument[Article](0)
    )

    service.copyArticleFromId(5, userinfo, "*", true, false)

    val cap: ArgumentCaptor[Article] = ArgumentCaptor.forClass(classOf[Article])
    verify(draftRepository, times(1)).insert(cap.capture())(any[DBSession])
    val insertedArticle = cap.getValue
    insertedArticle should be(expectedInsertedArticle)
  }

  test("article status should not be updated if only notes are changed") {
    val updatedArticle = TestData.blankUpdatedArticle.copy(
      revision = 1,
      notes = Some(Seq("note1", "note2")),
      editorLabels = Some(Seq("note3", "note4"))
    )

    val existing = TestData.sampleDomainArticle.copy(status = TestData.statusWithPublished)
    when(draftRepository.withId(existing.id.get)).thenReturn(Some(existing))
    val Success(result1) = service.updateArticle(
      existing.id.get,
      updatedArticle,
      List.empty,
      Seq.empty,
      TestData.userWithWriteAccess,
      None,
      None,
      None
    )
    result1.status.current should be(existing.status.current.toString)
    result1.status.other.sorted should be(existing.status.other.map(_.toString).toSeq.sorted)
  }

  test("article status should not be updated if changes only affect notes") {
    val existingTitle = "apekatter"
    val updatedArticle = TestData.blankUpdatedArticle.copy(
      revision = 1,
      language = Some("nb"),
      title = Some(existingTitle),
      notes = Some(Seq("note1", "note2")),
      editorLabels = Some(Seq("note3", "note4"))
    )

    val existing = TestData.sampleDomainArticle.copy(
      title = Seq(ArticleTitle(existingTitle, "nb")),
      status = TestData.statusWithPublished
    )
    when(draftRepository.withId(existing.id.get)).thenReturn(Some(existing))
    val Success(result1) = service.updateArticle(
      existing.id.get,
      updatedArticle,
      List.empty,
      Seq.empty,
      TestData.userWithWriteAccess,
      None,
      None,
      None
    )
    result1.status.current should be(existing.status.current.toString)
    result1.status.other.sorted should be(existing.status.other.map(_.toString).toSeq.sorted)
  }

  test("article status should not be updated if any of the PartialArticleFields changes") {
    val existingTitle = "tittel"
    val updatedArticle = TestData.blankUpdatedArticle.copy(
      revision = 1,
      language = Some("nb"),
      title = Some(existingTitle),
      availability = Some(Availability.teacher.toString),
      grepCodes = Some(Seq("a", "b", "c")),
      copyright = Some(
        api.Copyright(
          license = Some(api.License("COPYRIGHTED", None, None)),
          origin = None,
          creators = Seq.empty,
          processors = Seq.empty,
          rightsholders = Seq.empty,
          agreementId = None,
          validFrom = None,
          validTo = None
        )
      ),
      metaDescription = Some("newMeta"),
      relatedContent = Some(Seq(Left(api.RelatedContentLink("title1", "url2")), Right(12L))),
      tags = Some(Seq("new", "tag"))
    )

    val existing = TestData.sampleDomainArticle.copy(
      title = Seq(ArticleTitle(existingTitle, "nb")),
      status = TestData.statusWithPublished,
      availability = Availability.everyone,
      grepCodes = Seq.empty,
      copyright = Some(TestData.publicDomainCopyright.copy(license = Some("oldLicense"), origin = None)),
      metaDescription = Seq.empty,
      relatedContent = Seq.empty,
      tags = Seq.empty
    )

    when(draftRepository.withId(existing.id.get)).thenReturn(Some(existing))
    when(writeService.partialPublish(any, any, any)).thenReturn((existing.id.get, Success(existing)))
    when(articleApiClient.partialPublishArticle(any, any)).thenReturn(Success(existing.id.get))

    val Success(result1) = service.updateArticle(
      existing.id.get,
      updatedArticle,
      List.empty,
      Seq.empty,
      TestData.userWithWriteAccess,
      None,
      None,
      None
    )

    result1.status.current should be(existing.status.current.toString)
    result1.status.other.sorted should be(existing.status.other.map(_.toString).toSeq.sorted)

    result1.availability should be(Availability.teacher.toString)
    result1.grepCodes should be(Seq("a", "b", "c"))
    result1.copyright.get.license.get.license should be("COPYRIGHTED")
    result1.metaDescription.get.metaDescription should be("newMeta")
    result1.relatedContent.head.leftSide should be(Left(api.RelatedContentLink("title1", "url2")))
    result1.relatedContent.reverse.head should be(Right(12L))
    result1.tags.get.tags should be(Seq("new", "tag"))
    result1.notes.head.note should be("Artikkelen har blitt delpublisert")
  }

  test("article status should change if any of the other fields changes") {
    val existingTitle = "tittel"
    val updatedArticle = TestData.blankUpdatedArticle.copy(
      revision = 1,
      language = Some("nb"),
      title = Some(existingTitle),
      copyright = Some(
        api.Copyright(
          license = Some(api.License("COPYRIGHTED", None, None)),
          origin = Some("shouldCauseStatusChange"),
          creators = Seq.empty,
          processors = Seq.empty,
          rightsholders = Seq.empty,
          agreementId = None,
          validFrom = None,
          validTo = None
        )
      )
    )

    val existing = TestData.sampleDomainArticle.copy(
      title = Seq(ArticleTitle(existingTitle, "nb")),
      status = TestData.statusWithPublished,
      availability = Availability.everyone,
      grepCodes = Seq.empty,
      copyright = Some(TestData.publicDomainCopyright.copy(license = Some("oldLicense"), origin = None)),
      metaDescription = Seq.empty,
      relatedContent = Seq.empty,
      tags = Seq.empty
    )

    when(draftRepository.withId(existing.id.get)).thenReturn(Some(existing))
    when(writeService.partialPublish(any, any, any)).thenReturn((existing.id.get, Success(existing)))
    when(articleApiClient.partialPublishArticle(any, any)).thenReturn(Success(existing.id.get))

    val Success(result1) = service.updateArticle(
      existing.id.get,
      updatedArticle,
      List.empty,
      Seq.empty,
      TestData.userWithWriteAccess,
      None,
      None,
      None
    )

    result1.status.current should not be (existing.status.current.toString)
    result1.status.current should be(ArticleStatus.PROPOSAL.toString)
    result1.status.other.sorted should not be (existing.status.other.map(_.toString).toSeq.sorted)
    result1.notes.head.note should not be ("Artikkelen har blitt delpublisert")
  }

  test("article status should change if both the PartialArticleFields and other fields changes") {
    val existingTitle = "tittel"
    val updatedArticle = TestData.blankUpdatedArticle.copy(
      revision = 1,
      language = Some("nb"),
      title = Some(existingTitle),
      availability = Some(Availability.teacher.toString),
      grepCodes = Some(Seq("a", "b", "c")),
      copyright = Some(
        api.Copyright(
          license = Some(api.License("COPYRIGHTED", None, None)),
          origin = None,
          creators = Seq.empty,
          processors = Seq.empty,
          rightsholders = Seq.empty,
          agreementId = None,
          validFrom = None,
          validTo = None
        )
      ),
      metaDescription = Some("newMeta"),
      relatedContent = Some(Seq(Left(api.RelatedContentLink("title1", "url2")), Right(12L))),
      tags = Some(Seq("new", "tag")),
      conceptIds = Some(Seq(1, 2, 3))
    )

    val existing = TestData.sampleDomainArticle.copy(
      title = Seq(ArticleTitle(existingTitle, "nb")),
      status = TestData.statusWithPublished,
      availability = Availability.everyone,
      grepCodes = Seq.empty,
      copyright = Some(TestData.publicDomainCopyright.copy(license = Some("oldLicense"), origin = None)),
      metaDescription = Seq.empty,
      relatedContent = Seq.empty,
      tags = Seq.empty,
      conceptIds = Seq.empty
    )

    when(draftRepository.withId(existing.id.get)).thenReturn(Some(existing))
    when(writeService.partialPublish(any, any, any)).thenReturn((existing.id.get, Success(existing)))
    when(articleApiClient.partialPublishArticle(any, any)).thenReturn(Success(existing.id.get))

    val Success(result1) = service.updateArticle(
      existing.id.get,
      updatedArticle,
      List.empty,
      Seq.empty,
      TestData.userWithWriteAccess,
      None,
      None,
      None
    )

    result1.status.current should not be (existing.status.current.toString)
    result1.status.current should be(ArticleStatus.PROPOSAL.toString)
    result1.status.other.sorted should not be (existing.status.other.map(_.toString).toSeq.sorted)

    result1.availability should be(Availability.teacher.toString)
    result1.grepCodes should be(Seq("a", "b", "c"))
    result1.copyright.get.license.get.license should be("COPYRIGHTED")
    result1.metaDescription.get.metaDescription should be("newMeta")
    result1.relatedContent.head.leftSide should be(Left(api.RelatedContentLink("title1", "url2")))
    result1.relatedContent.reverse.head should be(Right(12L))
    result1.tags.get.tags should be(Seq("new", "tag"))
    result1.conceptIds should be(Seq(1, 2, 3))
    result1.notes.reverse.head.note should be("Artikkelen har blitt delpublisert")
  }

  test("Deleting storage should be called with correct path") {
    val imported      = "https://api.ndla.no/files/194277/Temahefte%20egg%20og%20meieriprodukterNN.pdf"
    val notImported   = "https://api.ndla.no/files/resources/01f6TKKF1wpAsc1Z.pdf"
    val onlyPath      = "resources/01f6TKKF1wpAsc1Z.pdf"
    val pathWithSlash = "/resources/01f6TKKF1wpAsc1Z.pdf"
    val pathWithFiles = "/files/resources/01f6TKKF1wpAsc1Z.pdf"
    service.getFilePathFromUrl(imported) should be("194277/Temahefte egg og meieriprodukterNN.pdf")
    service.getFilePathFromUrl(notImported) should be("resources/01f6TKKF1wpAsc1Z.pdf")
    service.getFilePathFromUrl(onlyPath) should be("resources/01f6TKKF1wpAsc1Z.pdf")
    service.getFilePathFromUrl(pathWithSlash) should be("resources/01f6TKKF1wpAsc1Z.pdf")
    service.getFilePathFromUrl(pathWithFiles) should be("resources/01f6TKKF1wpAsc1Z.pdf")
  }

  test("Article should not be saved, but only copied if createNewVersion is specified") {
    val updatedArticle = TestData.blankUpdatedArticle.copy(
      language = Some("nb"),
      title = Some("detteErEnNyTittel"),
      createNewVersion = Some(true)
    )

    val updated = service.updateArticle(
      articleId,
      updatedArticle,
      List.empty,
      Seq.empty,
      TestData.userWithAdminAccess,
      None,
      None,
      None
    )

    updated.get.notes.length should be(1)

    verify(draftRepository, never).updateArticle(any[Article], anyBoolean)(any[DBSession])
    verify(draftRepository, times(1)).storeArticleAsNewVersion(any[Article], any[Option[UserInfo]])(any[DBSession])
  }

  test("contentWithClonedFiles clones files as expected") {
    when(fileStorage.copyResource(anyString(), anyString()))
      .thenReturn(
        Success("resources/new123.pdf"),
        Success("resources/new456.pdf"),
        Success("resources/new789.pdf"),
        Success("resources/new101112.pdf")
      )
    val embed1 =
      """<embed data-alt="Kul alt1" data-path="/files/resources/abc123.pdf" data-resource="file" data-title="Kul tittel1" data-type="pdf">"""
    val embed2 =
      """<embed data-alt="Kul alt2" data-path="/files/resources/abc456.pdf" data-resource="file" data-title="Kul tittel2" data-type="pdf">"""
    val embed3 =
      """<embed data-alt="Kul alt3" data-path="/files/resources/abc789.pdf" data-resource="file" data-title="Kul tittel3" data-type="pdf">"""
    val contentNb = domain.ArticleContent(s"<section><h1>Hei</h1>$embed1$embed2</section>", "nb")
    val contentEn = domain.ArticleContent(s"<section><h1>Hello</h1>$embed1$embed3</section>", "en")

    val expectedEmbed1 =
      """<embed data-alt="Kul alt1" data-path="/files/resources/new123.pdf" data-resource="file" data-title="Kul tittel1" data-type="pdf">"""
    val expectedEmbed2 =
      """<embed data-alt="Kul alt2" data-path="/files/resources/new456.pdf" data-resource="file" data-title="Kul tittel2" data-type="pdf">"""
    val expectedEmbed3 =
      """<embed data-alt="Kul alt1" data-path="/files/resources/new789.pdf" data-resource="file" data-title="Kul tittel1" data-type="pdf">"""
    val expectedEmbed4 =
      """<embed data-alt="Kul alt3" data-path="/files/resources/new101112.pdf" data-resource="file" data-title="Kul tittel3" data-type="pdf">"""

    val expectedNb = domain.ArticleContent(s"<section><h1>Hei</h1>$expectedEmbed1$expectedEmbed2</section>", "nb")
    val expectedEn = domain.ArticleContent(s"<section><h1>Hello</h1>$expectedEmbed3$expectedEmbed4</section>", "en")

    val result = service.contentWithClonedFiles(List(contentNb, contentEn))

    result should be(Success(List(expectedNb, expectedEn)))

  }

  test("cloneEmbedAndUpdateElement updates file embeds") {
    import scala.jdk.CollectionConverters._
    val embed1 =
      """<embed data-alt="Kul alt1" data-path="/files/resources/abc123.pdf" data-resource="file" data-title="Kul tittel1" data-type="pdf">"""
    val embed2 =
      """<embed data-alt="Kul alt2" data-path="/files/resources/abc456.pdf" data-resource="file" data-title="Kul tittel2" data-type="pdf">"""

    val doc    = HtmlTagRules.stringToJsoupDocument(s"<section>$embed1</section><section>$embed2</section>")
    val embeds = doc.select(s"embed[data-resource='file']").asScala
    when(fileStorage.copyResource(anyString, anyString)).thenReturn(
      Success("resources/new123.pdf"),
      Success("resources/new456.pdf")
    )

    val results = embeds.map(service.cloneEmbedAndUpdateElement)
    results.map(_.isSuccess should be(true))

    val expectedEmbed1 =
      """<embed data-alt="Kul alt1" data-path="/files/resources/new123.pdf" data-resource="file" data-title="Kul tittel1" data-type="pdf">"""
    val expectedEmbed2 =
      """<embed data-alt="Kul alt2" data-path="/files/resources/new456.pdf" data-resource="file" data-title="Kul tittel2" data-type="pdf">"""

    HtmlTagRules.jsoupDocumentToString(doc) should be(
      s"<section>$expectedEmbed1</section><section>$expectedEmbed2</section>"
    )
  }

  test("That partialArticleFieldsUpdate updates fields correctly based on language") {
    val today         = LocalDateTime.now()
    val yesterday     = today.minusDays(1)
    val tomorrow      = today.plusDays(1)
    val afterTomorrow = today.plusDays(2)

    val existingArticle = TestData.sampleDomainArticle.copy(
      availability = Availability.everyone,
      grepCodes = Seq("A", "B"),
      copyright = Some(Copyright(Some("CC-BY-4.0"), Some("origin"), Seq(), Seq(), Seq(), None, None, None)),
      metaDescription = Seq(
        ArticleMetaDescription("oldDesc", "nb"),
        ArticleMetaDescription("oldDescc", "es"),
        ArticleMetaDescription("oldDesccc", "ru"),
        ArticleMetaDescription("oldDescccc", "nn")
      ),
      relatedContent = Seq(Left(RelatedContentLink("title1", "url2")), Right(12L)),
      tags = Seq(
        ArticleTag(Seq("old", "tag"), "nb"),
        ArticleTag(Seq("guten", "tag"), "de"),
        ArticleTag(Seq("oldd", "tagg"), "es")
      ),
      revisionMeta = Seq(
        RevisionMeta(
          UUID.randomUUID(),
          yesterday,
          "Test1",
          RevisionStatus.Revised
        ),
        RevisionMeta(
          UUID.randomUUID(),
          tomorrow,
          "Test2",
          RevisionStatus.Revised
        ),
        RevisionMeta(
          UUID.randomUUID(),
          tomorrow,
          "Test3",
          RevisionStatus.NeedsRevision
        ),
        RevisionMeta(
          UUID.randomUUID(),
          afterTomorrow,
          "Test4",
          RevisionStatus.NeedsRevision
        )
      )
    )

    val articleFieldsToUpdate = Seq(
      api.PartialArticleFields.availability,
      api.PartialArticleFields.grepCodes,
      api.PartialArticleFields.license,
      api.PartialArticleFields.metaDescription,
      api.PartialArticleFields.relatedContent,
      api.PartialArticleFields.tags,
      api.PartialArticleFields.revisionDate
    )

    val expectedPartialPublishFields = PartialPublishArticle(
      availability = Some(Availability.everyone),
      grepCodes = Some(Seq("A", "B")),
      license = Some("CC-BY-4.0"),
      metaDescription = Some(Seq(api.ArticleMetaDescription("oldDesc", "nb"))),
      relatedContent = Some(Seq(Left(api.RelatedContentLink("title1", "url2")), Right(12L))),
      tags = Some(Seq(api.ArticleTag(Seq("old", "tag"), "nb"))),
      revisionDate = Right(Some(tomorrow))
    )
    val expectedPartialPublishFieldsLangEN = PartialPublishArticle(
      availability = Some(Availability.everyone),
      grepCodes = Some(Seq("A", "B")),
      license = Some("CC-BY-4.0"),
      metaDescription = Some(Seq.empty),
      relatedContent = Some(Seq(Left(api.RelatedContentLink("title1", "url2")), Right(12L))),
      tags = Some(Seq.empty),
      revisionDate = Right(Some(tomorrow))
    )
    val expectedPartialPublishFieldsLangALL = PartialPublishArticle(
      availability = Some(Availability.everyone),
      grepCodes = Some(Seq("A", "B")),
      license = Some("CC-BY-4.0"),
      metaDescription = Some(
        Seq(
          api.ArticleMetaDescription("oldDesc", "nb"),
          api.ArticleMetaDescription("oldDescc", "es"),
          api.ArticleMetaDescription("oldDesccc", "ru"),
          api.ArticleMetaDescription("oldDescccc", "nn")
        )
      ),
      relatedContent = Some(Seq(Left(api.RelatedContentLink("title1", "url2")), Right(12L))),
      tags = Some(
        Seq(
          api.ArticleTag(Seq("old", "tag"), "nb"),
          api.ArticleTag(Seq("guten", "tag"), "de"),
          api.ArticleTag(Seq("oldd", "tagg"), "es")
        )
      ),
      revisionDate = Right(Some(tomorrow))
    )

    service.partialArticleFieldsUpdate(existingArticle, articleFieldsToUpdate, "nb") should be(
      expectedPartialPublishFields
    )
    service.partialArticleFieldsUpdate(existingArticle, articleFieldsToUpdate, "en") should be(
      expectedPartialPublishFieldsLangEN
    )
    service.partialArticleFieldsUpdate(existingArticle, articleFieldsToUpdate, "*") should be(
      expectedPartialPublishFieldsLangALL
    )
  }

  test("That updateArticle updates relatedContent") {
    val apiRelatedContent1    = api.RelatedContentLink("url1", "title1")
    val domainRelatedContent1 = domain.RelatedContentLink("url1", "title1")
    val relatedContent2       = 2

    val updatedApiArticle = TestData.blankUpdatedArticle.copy(
      revision = 1,
      relatedContent = Some(Seq(Left(apiRelatedContent1), Right(relatedContent2)))
    )
    val expectedArticle =
      article.copy(
        revision = Some(article.revision.get + 1),
        relatedContent = Seq(Left(domainRelatedContent1), Right(relatedContent2)),
        updated = today
      )

    service.updateArticle(
      articleId,
      updatedApiArticle,
      List.empty,
      Seq.empty,
      TestData.userWithWriteAccess,
      None,
      None,
      None
    ) should equal(converterService.toApiArticle(expectedArticle, "en"))
  }

  test("That updateArticle should get editor notes if RevisionMeta is added or updated") {
    val revision = api.RevisionMeta(None, LocalDateTime.now(), "Ny revision", RevisionStatus.NeedsRevision.entryName)
    val updatedApiArticle = TestData.blankUpdatedArticle.copy(
      revision = 1,
      revisionMeta = Some(Seq(revision))
    )

    val saved = service.updateArticle(
      articleId,
      updatedApiArticle,
      List.empty,
      Seq.empty,
      TestData.userWithWriteAccess,
      None,
      None,
      None
    )
    saved.get.notes.size should be(2)
    saved.get.notes.map(n => n.note) should be(
      Seq("Lagt til revisjon Ny revision.", "Slettet revisjon Automatisk revisjonsdato satt av systemet.")
    )
    val savedRevision = saved.get.revisions.head

    val revised = revision.copy(id = savedRevision.id, status = RevisionStatus.Revised.entryName)
    val revisedApiArticle = TestData.blankUpdatedArticle.copy(
      revision = 1,
      revisionMeta = Some(Seq(revised))
    )
    val domainRev = domain.RevisionMeta(
      id = UUID.fromString(savedRevision.id.get),
      revisionDate = savedRevision.revisionDate,
      note = savedRevision.note,
      status = RevisionStatus.fromString(savedRevision.status, RevisionStatus.NeedsRevision)
    )
    val another: Article = article.copy(revisionMeta = Seq(domainRev))
    when(draftRepository.withId(articleId)).thenReturn(Option(another))

    val updated2 = service.updateArticle(
      articleId,
      revisedApiArticle,
      List.empty,
      Seq.empty,
      TestData.userWithWriteAccess,
      None,
      None,
      None
    )
    updated2.get.notes.size should be(1)
    updated2.get.notes.head.note should be("Fullført revisjon Ny revision.")

  }

  test("partial publish notes should be updated before update function") {
    val existingTitle = "tittel"
    val updatedArticle = TestData.blankUpdatedArticle.copy(
      revision = 1,
      language = Some("nb"),
      title = Some(existingTitle),
      availability = Some(Availability.teacher.toString),
      grepCodes = Some(Seq("a", "b", "c")),
      copyright = Some(
        api.Copyright(
          license = Some(api.License("COPYRIGHTED", None, None)),
          origin = None,
          creators = Seq.empty,
          processors = Seq.empty,
          rightsholders = Seq.empty,
          agreementId = None,
          validFrom = None,
          validTo = None
        )
      ),
      metaDescription = Some("newMeta"),
      relatedContent = Some(Seq(Left(api.RelatedContentLink("title1", "url2")), Right(12L))),
      tags = Some(Seq("new", "tag"))
    )

    val existing = TestData.sampleDomainArticle.copy(
      title = Seq(ArticleTitle(existingTitle, "nb")),
      status = TestData.statusWithPublished,
      availability = Availability.everyone,
      grepCodes = Seq.empty,
      copyright = Some(TestData.publicDomainCopyright.copy(license = Some("oldLicense"), origin = None)),
      metaDescription = Seq.empty,
      relatedContent = Seq.empty,
      tags = Seq.empty
    )

    when(draftRepository.withId(existing.id.get)).thenReturn(Some(existing))
    when(writeService.partialPublish(any, any, any)).thenReturn((existing.id.get, Success(existing)))
    when(articleApiClient.partialPublishArticle(any, any)).thenReturn(Success(existing.id.get))

    val Success(result1) = service.updateArticle(
      existing.id.get,
      updatedArticle,
      List.empty,
      Seq.empty,
      TestData.userWithWriteAccess,
      None,
      None,
      None
    )

    result1.status.current should be(existing.status.current.toString)
    result1.status.other.sorted should be(existing.status.other.map(_.toString).toSeq.sorted)

    result1.availability should be(Availability.teacher.toString)
    result1.grepCodes should be(Seq("a", "b", "c"))
    result1.copyright.get.license.get.license should be("COPYRIGHTED")
    result1.metaDescription.get.metaDescription should be("newMeta")
    result1.relatedContent.head.leftSide should be(Left(api.RelatedContentLink("title1", "url2")))
    result1.relatedContent.reverse.head should be(Right(12L))
    result1.tags.get.tags should be(Seq("new", "tag"))
    result1.notes.head.note should be("Artikkelen har blitt delpublisert")

    val captor: ArgumentCaptor[domain.Article] = ArgumentCaptor.forClass(classOf[domain.Article])
    Mockito.verify(draftRepository).updateArticle(captor.capture(), anyBoolean)(any)
    val articlePassedToUpdate = captor.getValue
    articlePassedToUpdate.notes.head.note should be("Artikkelen har blitt delpublisert")
  }

  test("New articles are not made with empty-strings for empty fields") {
    val newArt = TestData.newArticle.copy(
      language = "nb",
      title = "Jonas",
      content = Some(""),
      introduction = Some(""),
      tags = Seq(),
      metaDescription = Some(""),
      visualElement = Some("")
    )

    when(draftRepository.newEmptyArticle(any[List[String]], any[Seq[String]])(any[DBSession])).thenReturn(Success(10L))

    val Success(created) = service.newArticle(
      newArt,
      List.empty,
      Seq.empty,
      TestData.userWithWriteAccess,
      None,
      None,
      None
    )

    val captor: ArgumentCaptor[domain.Article] = ArgumentCaptor.forClass(classOf[domain.Article])
    Mockito.verify(draftRepository).updateArticle(captor.capture(), anyBoolean)(any)
    val articlePassedToUpdate = captor.getValue

    articlePassedToUpdate.content should be(Seq.empty)
    articlePassedToUpdate.introduction should be(Seq.empty)
    articlePassedToUpdate.tags should be(Seq.empty)
    articlePassedToUpdate.metaDescription should be(Seq.empty)
    articlePassedToUpdate.visualElement should be(Seq.empty)

  }

  test("shouldUpdateStatus returns false if articles are equal") {
    val nnTitle = ArticleTitle("Title", "nn")
    val nbTitle = ArticleTitle("Title", "nb")

    val article1 = TestData.sampleDomainArticle.copy(title = Seq(nnTitle, nbTitle))
    val article2 = TestData.sampleDomainArticle.copy(title = Seq(nnTitle, nbTitle))
    service.shouldUpdateStatus(article1, article2) should be(false)

    val article3 = TestData.sampleDomainArticle.copy(title = Seq(nnTitle, nbTitle))
    val article4 = TestData.sampleDomainArticle.copy(title = Seq(nbTitle, nnTitle))
    service.shouldUpdateStatus(article3, article4) should be(false)
  }

  test("shouldPartialPublish return empty-set if articles are equal") {
    val nnMeta = ArticleMetaDescription("Meta nn", "nn")
    val nbMeta = ArticleMetaDescription("Meta nb", "nb")

    val article1 = TestData.sampleDomainArticle.copy(
      status = domain.Status(DRAFT, Set(PUBLISHED)),
      metaDescription = Seq(nnMeta, nbMeta)
    )
    val article2 = TestData.sampleDomainArticle.copy(
      status = domain.Status(DRAFT, Set(PUBLISHED)),
      metaDescription = Seq(nnMeta, nbMeta)
    )
    service.shouldPartialPublish(Some(article1), article2) should be(Set.empty)

    val article3 = TestData.sampleDomainArticle.copy(
      status = domain.Status(DRAFT, Set(PUBLISHED)),
      metaDescription = Seq(nnMeta, nbMeta)
    )
    val article4 = TestData.sampleDomainArticle.copy(
      status = domain.Status(DRAFT, Set(PUBLISHED)),
      metaDescription = Seq(nbMeta, nnMeta)
    )
    service.shouldPartialPublish(Some(article3), article4) should be(Set.empty)

  }

  test("shouldPartialPublish returns set of changed fields") {

    val article1 = TestData.sampleDomainArticle.copy(
      status = domain.Status(DRAFT, Set(PUBLISHED)),
      metaDescription = Seq(
        ArticleMetaDescription("Meta nn", "nn"),
        ArticleMetaDescription("Meta nb", "nb")
      ),
      grepCodes = Seq(
        "KE123"
      )
    )

    val article2 = TestData.sampleDomainArticle.copy(
      status = domain.Status(DRAFT, Set(PUBLISHED)),
      metaDescription = Seq(
        ArticleMetaDescription("Ny Meta nn", "nn"),
        ArticleMetaDescription("Meta nb", "nb")
      ),
      grepCodes = Seq("KE123", "KE456")
    )

    service.shouldPartialPublish(Some(article1), article2) should be(
      Set(
        PartialArticleFields.metaDescription,
        PartialArticleFields.grepCodes
      )
    )
  }

  test("copyRevisionDates updates articles") {
    val nodeId   = "urn:topic:1"
    val node     = Topic(nodeId, "Topic", Some("urn:article:1"), List.empty)
    val child    = Topic("urn:topic:2", "Topic", Some("urn:article:2"), List.empty)
    val resource = Resource("urn:resource:1", "Resource", Some("urn:article:3"), List.empty)

    val revisionMeta =
      RevisionMeta(UUID.randomUUID(), LocalDateTime.now(), "Test revision", RevisionStatus.NeedsRevision)
    val article1 = TestData.sampleDomainArticle.copy(
      id = Some(1),
      revisionMeta = Seq(revisionMeta)
    )
    val article2 = TestData.sampleDomainArticle.copy(id = Some(2))
    val article3 = TestData.sampleDomainArticle.copy(id = Some(3))

    when(taxonomyApiClient.getNode(nodeId)).thenReturn(Success(node))
    when(taxonomyApiClient.getChildNodes(nodeId)).thenReturn(Success(List(child)))
    when(taxonomyApiClient.getChildResources(anyString())).thenReturn(Success(List(resource)))
    when(draftRepository.withId(1)).thenReturn(Some(article1))
    when(draftRepository.withId(2)).thenReturn(Some(article2))
    when(draftRepository.withId(3)).thenReturn(Some(article3))
    service.copyRevisionDates(nodeId) should be(Success(()))
    verify(draftRepository, times(3)).updateArticle(any[Article], any[Boolean])(any[DBSession])
  }

  test("copyRevisionDates does nothing if revisionMeta is empty") {
    val nodeId   = "urn:topic:1"
    val node     = Topic(nodeId, "Topic", Some("urn:article:1"), List.empty)
    val child    = Topic("urn:topic:2", "Topic", Some("urn:article:2"), List.empty)
    val resource = Resource("urn:resource:1", "Resource", Some("urn:article:3"), List.empty)

    val article1 = TestData.sampleDomainArticle.copy(id = Some(1))
    val article2 = TestData.sampleDomainArticle.copy(id = Some(2))
    val article3 = TestData.sampleDomainArticle.copy(id = Some(3))

    when(taxonomyApiClient.getNode(nodeId)).thenReturn(Success(node))
    when(taxonomyApiClient.getChildNodes(nodeId)).thenReturn(Success(List(child)))
    when(taxonomyApiClient.getChildResources(anyString())).thenReturn(Success(List(resource)))
    when(draftRepository.withId(1)).thenReturn(Some(article1))
    when(draftRepository.withId(2)).thenReturn(Some(article2))
    when(draftRepository.withId(3)).thenReturn(Some(article3))
    service.copyRevisionDates(nodeId) should be(Success(()))
    verify(draftRepository, times(0)).updateArticle(any[Article], any[Boolean])(any[DBSession])
  }

  test("copyRevisionDates skips empty contentUris") {
    val nodeId   = "urn:topic:1"
    val node     = Topic(nodeId, "Topic", Some("urn:article:1"), List.empty)
    val child    = Topic("urn:topic:2", "Topic", None, List.empty)
    val resource = Resource("urn:resource:1", "Resource", Some("urn:article:2"), List.empty)

    val revisionMeta =
      RevisionMeta(UUID.randomUUID(), LocalDateTime.now(), "Test revision", RevisionStatus.NeedsRevision)
    val article1 = TestData.sampleDomainArticle.copy(
      id = Some(1),
      revisionMeta = Seq(revisionMeta)
    )
    val article2 = TestData.sampleDomainArticle.copy(id = Some(2))

    when(taxonomyApiClient.getNode(nodeId)).thenReturn(Success(node))
    when(taxonomyApiClient.getChildNodes(nodeId)).thenReturn(Success(List(child)))
    when(taxonomyApiClient.getChildResources(anyString())).thenReturn(Success(List(resource)))
    when(draftRepository.withId(1)).thenReturn(Some(article1))
    when(draftRepository.withId(2)).thenReturn(Some(article2))
    service.copyRevisionDates(nodeId) should be(Success(()))
    verify(draftRepository, times(2)).updateArticle(any[Article], any[Boolean])(any[DBSession])
  }
}
