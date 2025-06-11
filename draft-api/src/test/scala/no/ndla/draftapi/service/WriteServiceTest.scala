/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.service

import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.model
import no.ndla.common.model.api.{RelatedContentLinkDTO, UpdateWith}
import no.ndla.common.model.domain.*
import no.ndla.common.model.domain.article.{ArticleMetaDescriptionDTO, ArticleTagDTO, PartialPublishArticleDTO}
import no.ndla.common.model.domain.draft.DraftStatus.{IN_PROGRESS, PLANNED, PUBLISHED}
import no.ndla.common.model.domain.draft.*
import no.ndla.common.model.{NDLADate, RelatedContentLink, domain, api as commonApi}
import no.ndla.draftapi.integration.Node
import no.ndla.draftapi.model.api
import no.ndla.draftapi.model.api.PartialArticleFieldsDTO
import no.ndla.draftapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.network.tapir.auth.Permission.DRAFT_API_WRITE
import no.ndla.network.tapir.auth.TokenUser
import no.ndla.validation.HtmlTagRules
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{doAnswer, never, reset, times, verify, when}
import org.mockito.invocation.InvocationOnMock
import org.mockito.{ArgumentCaptor, Mockito}
import scalikejdbc.DBSession
import software.amazon.awssdk.services.s3.model.HeadObjectResponse

import java.util.UUID
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class WriteServiceTest extends UnitSuite with TestEnvironment {
  override val converterService = new ConverterService

  val today: NDLADate     = NDLADate.now()
  val yesterday: NDLADate = NDLADate.now().minusDays(1)
  val service             = new WriteService()

  val articleId   = 13L
  val agreementId = 14L

  val article: Draft =
    TestData.sampleArticleWithPublicDomain.copy(
      id = Some(articleId),
      created = yesterday,
      updated = yesterday,
      responsible = Some(Responsible("hei", clock.now())),
      started = true
    )

  val topicArticle: Draft =
    TestData.sampleTopicArticle.copy(id = Some(articleId), created = yesterday, updated = yesterday)

  override def beforeEach(): Unit = {
    reset(articleIndexService)
    reset(draftRepository)
    reset(tagIndexService)
    reset(grepCodesIndexService)
    reset(contentValidator)

    doAnswer((i: InvocationOnMock) => {
      val x = i.getArgument[DBSession => Try[?]](0)
      x(mock[DBSession])
    }).when(DBUtil).rollbackOnFailure(any)

    when(draftRepository.withId(eqTo(articleId))(any)).thenReturn(Option(article))
    when(articleIndexService.indexDocument(any[Draft])).thenAnswer((invocation: InvocationOnMock) =>
      Try(invocation.getArgument[Draft](0))
    )
    when(tagIndexService.indexDocument(any[Draft])).thenAnswer((invocation: InvocationOnMock) =>
      Try(invocation.getArgument[Draft](0))
    )
    when(grepCodesIndexService.indexDocument(any)).thenAnswer((invocation: InvocationOnMock) =>
      Try(invocation.getArgument[Draft](0))
    )
    when(readService.addUrlsOnEmbedResources(any[Draft])).thenAnswer((invocation: InvocationOnMock) =>
      invocation.getArgument[Draft](0)
    )
    when(contentValidator.validateArticle(any[Draft])).thenReturn(Success(article))
    when(contentValidator.validateArticle(any, any[Draft])).thenReturn(Success(article))
    when(contentValidator.validateArticleOnLanguage(any[Draft], any)).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument[Draft](0))
    )
    when(contentValidator.validateArticleOnLanguage(any, any[Draft], any)).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument[Draft](1))
    )
    when(draftRepository.getExternalIdsFromId(any[Long])(any[DBSession])).thenReturn(List("1234"))
    when(clock.now()).thenReturn(today)
    when(draftRepository.updateArticle(any[Draft])(any[DBSession]))
      .thenAnswer((invocation: InvocationOnMock) => {
        val arg = invocation.getArgument[Draft](0)
        Try(arg.copy(revision = Some(arg.revision.getOrElse(0) + 1)))
      })
    when(draftRepository.insert(any)(any))
      .thenAnswer((invocation: InvocationOnMock) => {
        val arg = invocation.getArgument[Draft](0)
        arg.copy(revision = Some(arg.revision.getOrElse(0) + 1))
      })
    when(draftRepository.storeArticleAsNewVersion(any[Draft], any[Option[TokenUser]], any[Boolean])(any[DBSession]))
      .thenAnswer((invocation: InvocationOnMock) => {
        val arg = invocation.getArgument[Draft](0)
        Try(arg.copy(revision = Some(arg.revision.getOrElse(0) + 1)))
      })

    when(taxonomyApiClient.updateTaxonomyIfExists(any[Long], any[Draft], any)).thenAnswer((i: InvocationOnMock) => {
      Success(i.getArgument[Long](0))
    })
  }

  override def afterEach(): Unit = {
    scala.util.Properties.clearProp("DEBUG_FLAKE")
  }

  test("newArticle should insert a given article") {
    when(draftRepository.getExternalIdsFromId(any[Long])(any[DBSession])).thenReturn(List.empty)
    when(contentValidator.validateArticle(any[Draft])).thenReturn(Success(article))
    when(draftRepository.newEmptyArticleId()(any[DBSession]))
      .thenReturn(Success(1: Long))

    val result = service
      .newArticle(TestData.newArticle, TestData.userWithWriteAccess)

    result.failIfFailure

    verify(draftRepository, times(1)).newEmptyArticleId()(any)
    verify(draftRepository, times(1)).insert(any[Draft])(any)
    verify(draftRepository, times(0)).updateArticle(any[Draft])(any)
    verify(articleIndexService, times(1)).indexAsync(any, any)(any)
    verify(tagIndexService, times(1)).indexAsync(any, any)(any)
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
        updated = today,
        started = true
      )

    when(writeService.partialPublish(any, any, any, any)).thenReturn((expectedArticle.id.get, Success(expectedArticle)))
    when(articleApiClient.partialPublishArticle(any, any, any)).thenReturn(Success(expectedArticle.id.get))

    service.updateArticle(articleId, updatedApiArticle, TestData.userWithWriteAccess) should equal(
      converterService.toApiArticle(expectedArticle, "en")
    )
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
        title = Seq(Title(newTitle, "en")),
        updated = today
      )

    service.updateArticle(articleId, updatedApiArticle, TestData.userWithWriteAccess) should equal(
      converterService.toApiArticle(expectedArticle, "en")
    )
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
    val newImageMeta           = api.NewArticleMetaImageDTO(updatedMetaId, updatedMetaAlt)
    val updatedVisualElement   = s"<$EmbedTagName something />"
    val updatedCopyright = model.api.DraftCopyrightDTO(
      Some(commonApi.LicenseDTO("a", Some("b"), None)),
      Some("c"),
      Seq(commonApi.AuthorDTO(ContributorType.Originator, "Jonas")),
      List(),
      List(),
      None,
      None,
      false
    )
    val updatedRequiredLib = api.RequiredLibraryDTO("tjup", "tjap", "tjim")
    val updatedArticleType = "topic-article"

    val updatedApiArticle = TestData.blankUpdatedArticle.copy(
      revision = 1,
      language = Some("en"),
      title = Some(updatedTitle),
      status = Some("PLANNED"),
      published = Some(updatedPublishedDate),
      content = Some(updatedContent),
      tags = Some(updatedTags),
      introduction = Some(updatedIntro),
      metaDescription = Some(updatedMetaDescription),
      metaImage = UpdateWith(newImageMeta),
      visualElement = Some(updatedVisualElement),
      copyright = Some(updatedCopyright),
      requiredLibraries = Some(Seq(updatedRequiredLib)),
      articleType = Some(updatedArticleType)
    )

    val expectedArticle = article.copy(
      revision = Some(article.revision.get + 1),
      title = Seq(Title(updatedTitle, "en")),
      content = Seq(ArticleContent(updatedContent, "en")),
      copyright = Some(
        DraftCopyright(
          Some("a"),
          Some("c"),
          Seq(Author(ContributorType.Originator, "Jonas")),
          List(),
          List(),
          None,
          None,
          false
        )
      ),
      tags = Seq(Tag(Seq("en", "to", "tre"), "en")),
      requiredLibraries = Seq(RequiredLibrary("tjup", "tjap", "tjim")),
      visualElement = Seq(VisualElement(updatedVisualElement, "en")),
      introduction = Seq(Introduction(updatedIntro, "en")),
      metaDescription = Seq(Description(updatedMetaDescription, "en")),
      metaImage = Seq(ArticleMetaImage(updatedMetaId, updatedMetaAlt, "en")),
      updated = today,
      published = yesterday,
      articleType = ArticleType.TopicArticle
    )

    service.updateArticle(
      articleId,
      updatedApiArticle,
      TestData.userWithWriteAccess
    ) should equal(converterService.toApiArticle(expectedArticle, "en"))
  }

  test("updateArticle should use user-defined status if defined") {
    val existing = TestData.sampleDomainArticle.copy(
      status = TestData.statusWithPlanned,
      responsible = Some(Responsible("hei", TestData.today))
    )
    val updatedArticle = TestData.sampleApiUpdateArticle.copy(status = Some("IN_PROGRESS"))
    when(draftRepository.withId(eqTo(existing.id.get))(any)).thenReturn(Some(existing))
    val Success(result) = service.updateArticle(
      existing.id.get,
      updatedArticle,
      TestData.userWithWriteAccess
    )
    result.status should equal(api.StatusDTO("IN_PROGRESS", Seq.empty))
  }

  test(
    "updateArticle should set status to PROPOSAL if user-defined status is undefined and current status is PUBLISHED"
  ) {
    val updatedArticle = TestData.sampleApiUpdateArticle.copy(status = None)

    val existing = TestData.sampleDomainArticle.copy(
      status = TestData.statusWithPublished,
      responsible = Some(Responsible("hei", TestData.today))
    )
    when(draftRepository.withId(eqTo(existing.id.get))(any)).thenReturn(Some(existing))
    val Success(result) = service.updateArticle(
      existing.id.get,
      updatedArticle,
      TestData.userWithWriteAccess
    )
    result.status should equal(api.StatusDTO("IN_PROGRESS", Seq("PUBLISHED")))
  }

  test("updateArticle should use current status if user-defined status is not set") {
    val updatedArticle = TestData.sampleApiUpdateArticle.copy(status = None)

    {
      val existing = TestData.sampleDomainArticle.copy(
        status = TestData.statusWithInProcess,
        responsible = Some(Responsible("hei", TestData.today))
      )
      when(draftRepository.withId(eqTo(existing.id.get))(any)).thenReturn(Some(existing))
      val Success(result) = service.updateArticle(
        existing.id.get,
        updatedArticle,
        TestData.userWithWriteAccess
      )
      result.status should equal(api.StatusDTO("IN_PROGRESS", Seq.empty))
    }

    {
      val existing = TestData.sampleDomainArticle.copy(
        status = TestData.statusWithExternalReview,
        responsible = Some(Responsible("hei", TestData.today))
      )
      when(draftRepository.withId(eqTo(existing.id.get))(any)).thenReturn(Some(existing))
      val Success(result) = service.updateArticle(
        existing.id.get,
        updatedArticle,
        TestData.userWithWriteAccess
      )
      result.status should equal(api.StatusDTO("EXTERNAL_REVIEW", Seq.empty))
    }

    {
      val existing = TestData.sampleDomainArticle.copy(
        status = TestData.statusWithInternalReview,
        responsible = Some(Responsible("hei", TestData.today))
      )
      when(draftRepository.withId(eqTo(existing.id.get))(any)).thenReturn(Some(existing))
      val Success(result) = service.updateArticle(
        existing.id.get,
        updatedArticle,
        TestData.userWithWriteAccess
      )
      result.status should equal(api.StatusDTO("INTERNAL_REVIEW", Seq.empty))
    }

    {
      val existing = TestData.sampleDomainArticle.copy(
        status = TestData.statusWithEndControl,
        responsible = Some(Responsible("hei", TestData.today))
      )
      when(draftRepository.withId(eqTo(existing.id.get))(any)).thenReturn(Some(existing))
      when(contentValidator.validateArticle(any[Draft])).thenReturn(Success(existing))
      when(articleApiClient.validateArticle(any[domain.article.Article], any[Boolean], any)).thenAnswer(
        (i: InvocationOnMock) => {
          Success(i.getArgument[domain.article.Article](0))
        }
      )
      val Success(result) = service.updateArticle(
        existing.id.get,
        updatedArticle,
        TestData.userWithWriteAccess
      )
      result.status should equal(api.StatusDTO("END_CONTROL", Seq.empty))
    }
  }

  test("That delete article should fail when only one language") {
    val Failure(result) = service.deleteLanguage(article.id.get, "nb", TokenUser("asdf", Set(), None))
    result.getMessage should equal("Only one language left")
  }

  test("That delete article removes language from all languagefields") {
    val article =
      TestData.sampleDomainArticle.copy(
        id = Some(3),
        title = Seq(Title("title", "nb"), Title("title", "nn"))
      )
    val articleCaptor: ArgumentCaptor[Draft] = ArgumentCaptor.forClass(classOf[Draft])

    when(draftRepository.withId(any)(any)).thenReturn(Some(article))
    service.deleteLanguage(article.id.get, "nn", TokenUser("asdf", Set(), None))
    verify(draftRepository).updateArticle(articleCaptor.capture())(any)

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
    val fileToUpload           = mock[domain.UploadedFile]
    val fileBytes: Array[Byte] = "these are not the bytes you're looking for".getBytes
    when(fileToUpload.fileSize).thenReturn(fileBytes.length.toLong)
    when(fileToUpload.contentType).thenReturn(Some("application/pdf"))
    when(fileToUpload.fileName).thenReturn(Some("myfile.pdf"))
    when(fileStorage.resourceExists(any)).thenReturn(false)
    when(fileStorage.uploadResourceFromStream(any[UploadedFile], any))
      .thenAnswer((_: InvocationOnMock) => Success(mock[HeadObjectResponse]))

    val uploaded = service.uploadFile(fileToUpload)

    val storageKeyCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
    uploaded.isSuccess should be(true)
    verify(fileStorage, times(1)).resourceExists(any)
    verify(fileStorage, times(1)).uploadResourceFromStream(any, storageKeyCaptor.capture())
    storageKeyCaptor.getValue.endsWith(".pdf") should be(true)
  }

  test("That updateStatus indexes the updated article") {
    reset(articleIndexService)
    reset(searchApiClient)

    val articleToUpdate = TestData.sampleDomainArticle.copy(
      id = Some(10),
      updated = yesterday,
      responsible = Some(Responsible("hei", TestData.today))
    )
    val user = TokenUser("Pelle", Set(DRAFT_API_WRITE), None)
    val updatedArticle = converterService
      .updateStatus(DraftStatus.IN_PROGRESS, articleToUpdate, user)
      .get
    val updatedAndInserted = updatedArticle
      .copy(
        revision = updatedArticle.revision.map(_ + 1),
        updated = today,
        notes = updatedArticle.notes.map(_.copy(timestamp = today))
      )

    when(draftRepository.withId(eqTo(10L))(any)).thenReturn(Some(articleToUpdate))
    when(draftRepository.updateArticle(any[Draft])(any)).thenReturn(Success(updatedAndInserted))

    when(articleIndexService.indexAsync(any, any[Draft])(any))
      .thenReturn(Future.successful(Success(updatedAndInserted)))
    when(searchApiClient.indexDocument(any[String], any[Draft], any)(any, any, any)).thenReturn(updatedAndInserted)

    service.updateArticleStatus(DraftStatus.IN_PROGRESS, 10, user)

    val argCap1: ArgumentCaptor[Draft] = ArgumentCaptor.forClass(classOf[Draft])
    val argCap2: ArgumentCaptor[Draft] = ArgumentCaptor.forClass(classOf[Draft])

    verify(articleIndexService, times(1)).indexAsync(any, argCap1.capture())(any)
    verify(searchApiClient, times(1)).indexDocument(any[String], argCap2.capture(), any)(any, any, any)

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
          Seq(ArticleContent("<section> Valid Content </section>", "nb"), ArticleContent("<div> content <div", "nn")),
        responsible = Some(Responsible("hei", TestData.today))
      )

    when(draftRepository.withId(any)(any)).thenReturn(Some(article))
    service.updateArticle(1, updatedArticle, TestData.userWithPublishAccess)

    verify(contentValidator, times(1)).validateArticleOnLanguage(any, any, eqTo(Some("nb")))
  }

  test("That articles are cloned with reasonable values") {
    val yesterday = NDLADate.now().minusDays(1)
    val today     = NDLADate.now()

    when(clock.now()).thenReturn(today)

    val article =
      TestData.sampleDomainArticle.copy(
        id = Some(5),
        title = Seq(Title("Tittel", "nb"), Title("Title", "en")),
        status = Status(DraftStatus.PUBLISHED, Set.empty),
        updated = yesterday,
        created = yesterday.minusDays(1),
        published = yesterday
      )

    val userinfo = TokenUser("somecoolid", Set.empty, None)

    val newId = 1231.toLong
    when(draftRepository.newEmptyArticleId()(any[DBSession]))
      .thenReturn(Success(newId))

    val expectedInsertedArticle = article.copy(
      id = Some(newId),
      title = Seq(Title("Tittel (Kopi)", "nb"), Title("Title (Kopi)", "en")),
      revision = Some(1),
      updated = today,
      created = today,
      published = today,
      updatedBy = userinfo.id,
      responsible = Some(Responsible(userinfo.id, today)),
      status = Status(DraftStatus.PLANNED, Set.empty),
      notes = article.notes ++
        converterService
          .newNotes(
            Seq("Opprettet artikkel, som kopi av artikkel med id: '5'."),
            userinfo,
            Status(DraftStatus.PLANNED, Set.empty)
          )
          .get
    )
    when(draftRepository.withId(any)(any)).thenReturn(Some(article))

    service.copyArticleFromId(5, userinfo, "*", true, true)

    val cap: ArgumentCaptor[Draft] = ArgumentCaptor.forClass(classOf[Draft])
    verify(draftRepository, times(1)).insert(cap.capture())(any[DBSession])
    val insertedArticle = cap.getValue
    insertedArticle should be(expectedInsertedArticle)
  }

  test("That articles are cloned without title postfix if flag is false") {
    val yesterday = NDLADate.now().minusDays(1)
    val today     = NDLADate.now().withNano(0)

    when(clock.now()).thenReturn(today)

    val article = TestData.sampleDomainArticle.copy(
      id = Some(5),
      title = Seq(Title("Tittel", "nb"), Title("Title", "en")),
      status = Status(DraftStatus.PUBLISHED, Set.empty),
      updated = yesterday,
      created = yesterday.minusDays(1),
      published = yesterday
    )

    val userinfo = TokenUser("somecoolid", Set.empty, None)

    val newId = 1231.toLong
    when(draftRepository.newEmptyArticleId()(any[DBSession]))
      .thenReturn(Success(newId))

    val expectedInsertedArticle = article.copy(
      id = Some(newId),
      revision = Some(1),
      updated = today,
      created = today,
      published = today,
      updatedBy = userinfo.id,
      responsible = Some(Responsible(userinfo.id, today)),
      status = Status(DraftStatus.PLANNED, Set.empty),
      notes = article.notes ++
        converterService
          .newNotes(
            Seq("Opprettet artikkel, som kopi av artikkel med id: '5'."),
            userinfo,
            Status(DraftStatus.PLANNED, Set.empty)
          )
          .get
    )
    when(draftRepository.withId(any)(any)).thenReturn(Some(article))
    service.copyArticleFromId(5, userinfo, "*", true, false)

    val cap: ArgumentCaptor[Draft] = ArgumentCaptor.forClass(classOf[Draft])
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
    when(draftRepository.withId(eqTo(existing.id.get))(any)).thenReturn(Some(existing))
    val Success(result1) = service.updateArticle(existing.id.get, updatedArticle, TestData.userWithWriteAccess)
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
      title = Seq(Title(existingTitle, "nb")),
      status = TestData.statusWithPublished
    )
    when(draftRepository.withId(eqTo(existing.id.get))(any)).thenReturn(Some(existing))
    val Success(result1) = service.updateArticle(existing.id.get, updatedArticle, TestData.userWithWriteAccess)
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
        model.api.DraftCopyrightDTO(
          license = Some(commonApi.LicenseDTO("COPYRIGHTED", None, None)),
          origin = None,
          creators = Seq.empty,
          processors = Seq.empty,
          rightsholders = Seq.empty,
          validFrom = None,
          validTo = None,
          processed = false
        )
      ),
      metaDescription = Some("newMeta"),
      relatedContent = Some(Seq(Left(commonApi.RelatedContentLinkDTO("title1", "url2")), Right(12L))),
      tags = Some(Seq("new", "tag"))
    )

    val existing = TestData.sampleDomainArticle.copy(
      title = Seq(Title(existingTitle, "nb")),
      status = TestData.statusWithPublished,
      availability = Availability.everyone,
      grepCodes = Seq.empty,
      copyright = Some(TestData.publicDomainCopyright.copy(license = Some("oldLicense"), origin = None)),
      metaDescription = Seq.empty,
      relatedContent = Seq.empty,
      tags = Seq.empty
    )

    when(draftRepository.withId(eqTo(existing.id.get))(any)).thenReturn(Some(existing))
    when(writeService.partialPublish(any, any, any, any)).thenReturn((existing.id.get, Success(existing)))
    when(articleApiClient.partialPublishArticle(any, any, any)).thenReturn(Success(existing.id.get))

    val Success(result1) = service.updateArticle(existing.id.get, updatedArticle, TestData.userWithWriteAccess)

    result1.status.current should be(existing.status.current.toString)
    result1.status.other.sorted should be(existing.status.other.map(_.toString).toSeq.sorted)

    result1.availability should be(Availability.teacher.toString)
    result1.grepCodes should be(Seq("a", "b", "c"))
    result1.copyright.get.license.get.license should be("COPYRIGHTED")
    result1.metaDescription.get.metaDescription should be("newMeta")
    result1.relatedContent.head.leftSide should be(Left(commonApi.RelatedContentLinkDTO("title1", "url2")))
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
        model.api.DraftCopyrightDTO(
          license = Some(commonApi.LicenseDTO("COPYRIGHTED", None, None)),
          origin = Some("shouldCauseStatusChange"),
          creators = Seq.empty,
          processors = Seq.empty,
          rightsholders = Seq.empty,
          validFrom = None,
          validTo = None,
          processed = false
        )
      )
    )

    val existing = TestData.sampleDomainArticle.copy(
      title = Seq(Title(existingTitle, "nb")),
      status = TestData.statusWithPublished,
      availability = Availability.everyone,
      grepCodes = Seq.empty,
      copyright = Some(TestData.publicDomainCopyright.copy(license = Some("oldLicense"), origin = None)),
      metaDescription = Seq.empty,
      relatedContent = Seq.empty,
      tags = Seq.empty,
      responsible = Some(Responsible("hei", TestData.today))
    )

    when(draftRepository.withId(eqTo(existing.id.get))(any)).thenReturn(Some(existing))
    when(writeService.partialPublish(any, any, any, any)).thenReturn((existing.id.get, Success(existing)))
    when(articleApiClient.partialPublishArticle(any, any, any)).thenReturn(Success(existing.id.get))

    val Success(result1) = service.updateArticle(existing.id.get, updatedArticle, TestData.userWithWriteAccess)

    result1.status.current should not be existing.status.current.toString
    result1.status.current should be(DraftStatus.IN_PROGRESS.toString)
    result1.status.other.sorted should not be existing.status.other.map(_.toString).toSeq.sorted
    result1.notes.head.note should not be "Artikkelen har blitt delpublisert"
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
        model.api.DraftCopyrightDTO(
          license = Some(commonApi.LicenseDTO("COPYRIGHTED", None, None)),
          origin = None,
          creators = Seq.empty,
          processors = Seq.empty,
          rightsholders = Seq.empty,
          validFrom = None,
          validTo = None,
          processed = false
        )
      ),
      metaDescription = Some("newMeta"),
      relatedContent = Some(Seq(Left(commonApi.RelatedContentLinkDTO("title1", "url2")), Right(12L))),
      tags = Some(Seq("new", "tag")),
      conceptIds = Some(Seq(1, 2, 3))
    )

    val existing = TestData.sampleDomainArticle.copy(
      title = Seq(Title(existingTitle, "nb")),
      status = TestData.statusWithPublished,
      availability = Availability.everyone,
      grepCodes = Seq.empty,
      copyright = Some(TestData.publicDomainCopyright.copy(license = Some("oldLicense"), origin = None)),
      metaDescription = Seq.empty,
      relatedContent = Seq.empty,
      tags = Seq.empty,
      conceptIds = Seq.empty,
      responsible = Some(Responsible("hei", TestData.today))
    )

    when(draftRepository.withId(eqTo(existing.id.get))(any)).thenReturn(Some(existing))
    when(writeService.partialPublish(any, any, any, any)).thenReturn((existing.id.get, Success(existing)))
    when(articleApiClient.partialPublishArticle(any, any, any)).thenReturn(Success(existing.id.get))

    val Success(result1) = service.updateArticle(existing.id.get, updatedArticle, TestData.userWithWriteAccess)

    result1.status.current should not be existing.status.current.toString
    result1.status.current should be(DraftStatus.IN_PROGRESS.toString)
    result1.status.other.sorted should not be existing.status.other.map(_.toString).toSeq.sorted

    result1.availability should be(Availability.teacher.toString)
    result1.grepCodes should be(Seq("a", "b", "c"))
    result1.copyright.get.license.get.license should be("COPYRIGHTED")
    result1.metaDescription.get.metaDescription should be("newMeta")
    result1.relatedContent.head.leftSide should be(Left(commonApi.RelatedContentLinkDTO("title1", "url2")))
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

    val updated = service.updateArticle(articleId, updatedArticle, TestData.userWithAdminAccess)

    updated.get.notes.length should be(1)

    verify(draftRepository, never).updateArticle(any[Draft])(any[DBSession])
    verify(draftRepository, times(1)).storeArticleAsNewVersion(any[Draft], any[Option[TokenUser]], any[Boolean])(
      any[DBSession]
    )
  }

  test("contentWithClonedFiles clones files as expected") {
    when(fileStorage.copyResource(any, any))
      .thenReturn(
        Success("resources/new123.pdf"),
        Success("resources/new456.pdf"),
        Success("resources/new789.pdf"),
        Success("resources/new101112.pdf")
      )
    val embed1 =
      s"""<$EmbedTagName data-alt="Kul alt1" data-path="/files/resources/abc123.pdf" data-resource="file" data-title="Kul tittel1" data-type="pdf" />"""
    val embed2 =
      s"""<$EmbedTagName data-alt="Kul alt2" data-path="/files/resources/abc456.pdf" data-resource="file" data-title="Kul tittel2" data-type="pdf" />"""
    val embed3 =
      s"""<$EmbedTagName data-alt="Kul alt3" data-path="/files/resources/abc789.pdf" data-resource="file" data-title="Kul tittel3" data-type="pdf" />"""
    val contentNb = ArticleContent(s"<section><h1>Hei</h1>$embed1$embed2</section>", "nb")
    val contentEn = ArticleContent(s"<section><h1>Hello</h1>$embed1$embed3</section>", "en")

    val expectedEmbed1 =
      s"""<$EmbedTagName data-alt="Kul alt1" data-path="/files/resources/new123.pdf" data-resource="file" data-title="Kul tittel1" data-type="pdf" />"""
    val expectedEmbed2 =
      s"""<$EmbedTagName data-alt="Kul alt2" data-path="/files/resources/new456.pdf" data-resource="file" data-title="Kul tittel2" data-type="pdf" />"""
    val expectedEmbed3 =
      s"""<$EmbedTagName data-alt="Kul alt1" data-path="/files/resources/new789.pdf" data-resource="file" data-title="Kul tittel1" data-type="pdf" />"""
    val expectedEmbed4 =
      s"""<$EmbedTagName data-alt="Kul alt3" data-path="/files/resources/new101112.pdf" data-resource="file" data-title="Kul tittel3" data-type="pdf" />"""

    val expectedNb = ArticleContent(s"<section><h1>Hei</h1>$expectedEmbed1$expectedEmbed2</section>", "nb")
    val expectedEn = ArticleContent(s"<section><h1>Hello</h1>$expectedEmbed3$expectedEmbed4</section>", "en")

    val result = service.contentWithClonedFiles(List(contentNb, contentEn))

    result should be(Success(List(expectedNb, expectedEn)))

  }

  test("cloneEmbedAndUpdateElement updates file embeds") {
    import scala.jdk.CollectionConverters.*
    val embed1 =
      s"""<$EmbedTagName data-alt="Kul alt1" data-path="/files/resources/abc123.pdf" data-resource="file" data-title="Kul tittel1" data-type="pdf" />"""
    val embed2 =
      s"""<$EmbedTagName data-alt="Kul alt2" data-path="/files/resources/abc456.pdf" data-resource="file" data-title="Kul tittel2" data-type="pdf" />"""

    val doc    = HtmlTagRules.stringToJsoupDocument(s"<section>$embed1</section><section>$embed2</section>")
    val embeds = doc.select(s"$EmbedTagName[data-resource='file']").asScala
    when(fileStorage.copyResource(any, any)).thenReturn(
      Success("resources/new123.pdf"),
      Success("resources/new456.pdf")
    )

    val results = embeds.map(service.cloneEmbedAndUpdateElement)
    results.map(_.isSuccess should be(true))

    val expectedEmbed1 =
      s"""<$EmbedTagName data-alt="Kul alt1" data-path="/files/resources/new123.pdf" data-resource="file" data-title="Kul tittel1" data-type="pdf" />"""
    val expectedEmbed2 =
      s"""<$EmbedTagName data-alt="Kul alt2" data-path="/files/resources/new456.pdf" data-resource="file" data-title="Kul tittel2" data-type="pdf" />"""

    HtmlTagRules.jsoupDocumentToString(doc) should be(
      s"<section>$expectedEmbed1</section><section>$expectedEmbed2</section>"
    )
  }

  test("That partialArticleFieldsUpdate updates fields correctly based on language") {
    val today         = NDLADate.now()
    val yesterday     = today.minusDays(1)
    val tomorrow      = today.plusDays(1)
    val afterTomorrow = today.plusDays(2)

    val existingArticle = TestData.sampleDomainArticle.copy(
      availability = Availability.everyone,
      grepCodes = Seq("A", "B"),
      copyright = Some(DraftCopyright(Some("CC-BY-4.0"), Some("origin"), Seq(), Seq(), Seq(), None, None, false)),
      metaDescription = Seq(
        Description("oldDesc", "nb"),
        Description("oldDescc", "es"),
        Description("oldDesccc", "ru"),
        Description("oldDescccc", "nn")
      ),
      relatedContent = Seq(Left(RelatedContentLink("title1", "url2")), Right(12L)),
      tags = Seq(
        Tag(Seq("old", "tag"), "nb"),
        Tag(Seq("guten", "tag"), "de"),
        Tag(Seq("oldd", "tagg"), "es")
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
      ),
      published = tomorrow
    )

    val articleFieldsToUpdate = Seq(
      api.PartialArticleFieldsDTO.availability,
      api.PartialArticleFieldsDTO.grepCodes,
      api.PartialArticleFieldsDTO.license,
      api.PartialArticleFieldsDTO.metaDescription,
      api.PartialArticleFieldsDTO.relatedContent,
      api.PartialArticleFieldsDTO.tags,
      api.PartialArticleFieldsDTO.revisionDate,
      api.PartialArticleFieldsDTO.published
    )

    val expectedPartialPublishFields = PartialPublishArticleDTO(
      availability = Some(Availability.everyone),
      grepCodes = Some(Seq("A", "B")),
      license = Some("CC-BY-4.0"),
      metaDescription = Some(Seq(ArticleMetaDescriptionDTO("oldDesc", "nb"))),
      relatedContent = Some(Seq(Left(RelatedContentLinkDTO("title1", "url2")), Right(12L))),
      tags = Some(Seq(ArticleTagDTO(Seq("old", "tag"), "nb"))),
      revisionDate = UpdateWith(tomorrow),
      published = Some(tomorrow)
    )
    val expectedPartialPublishFieldsLangEN = PartialPublishArticleDTO(
      availability = Some(Availability.everyone),
      grepCodes = Some(Seq("A", "B")),
      license = Some("CC-BY-4.0"),
      metaDescription = Some(Seq.empty),
      relatedContent = Some(Seq(Left(RelatedContentLinkDTO("title1", "url2")), Right(12L))),
      tags = Some(Seq.empty),
      revisionDate = UpdateWith(tomorrow),
      published = Some(tomorrow)
    )
    val expectedPartialPublishFieldsLangALL = PartialPublishArticleDTO(
      availability = Some(Availability.everyone),
      grepCodes = Some(Seq("A", "B")),
      license = Some("CC-BY-4.0"),
      metaDescription = Some(
        Seq(
          ArticleMetaDescriptionDTO("oldDesc", "nb"),
          ArticleMetaDescriptionDTO("oldDescc", "es"),
          ArticleMetaDescriptionDTO("oldDesccc", "ru"),
          ArticleMetaDescriptionDTO("oldDescccc", "nn")
        )
      ),
      relatedContent = Some(Seq(Left(RelatedContentLinkDTO("title1", "url2")), Right(12L))),
      tags = Some(
        Seq(
          ArticleTagDTO(Seq("old", "tag"), "nb"),
          ArticleTagDTO(Seq("guten", "tag"), "de"),
          ArticleTagDTO(Seq("oldd", "tagg"), "es")
        )
      ),
      revisionDate = UpdateWith(tomorrow),
      published = Some(tomorrow)
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
    val apiRelatedContent1    = commonApi.RelatedContentLinkDTO("url1", "title1")
    val domainRelatedContent1 = RelatedContentLink("url1", "title1")
    val relatedContent2       = 2L

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

    service.updateArticle(articleId, updatedApiArticle, TestData.userWithWriteAccess) should equal(
      converterService.toApiArticle(expectedArticle, "en")
    )
  }

  test("That updateArticle should get editor notes if RevisionMeta is added or updated") {
    when(uuidUtil.randomUUID()).thenCallRealMethod()
    val revision = api.RevisionMetaDTO(None, NDLADate.now(), "Ny revision", RevisionStatus.NeedsRevision.entryName)
    val updatedApiArticle = TestData.blankUpdatedArticle.copy(
      revision = 1,
      revisionMeta = Some(Seq(revision))
    )

    val saved = service.updateArticle(articleId, updatedApiArticle, TestData.userWithWriteAccess)
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
    val domainRev = RevisionMeta(
      id = UUID.fromString(savedRevision.id.get),
      revisionDate = savedRevision.revisionDate,
      note = savedRevision.note,
      status = RevisionStatus.fromString(savedRevision.status, RevisionStatus.NeedsRevision)
    )
    val another: Draft = article.copy(revisionMeta = Seq(domainRev))
    when(draftRepository.withId(eqTo(articleId))(any)).thenReturn(Option(another))

    val updated2 = service.updateArticle(articleId, revisedApiArticle, TestData.userWithWriteAccess)
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
        model.api.DraftCopyrightDTO(
          license = Some(commonApi.LicenseDTO("COPYRIGHTED", None, None)),
          origin = None,
          creators = Seq.empty,
          processors = Seq.empty,
          rightsholders = Seq.empty,
          validFrom = None,
          validTo = None,
          processed = false
        )
      ),
      metaDescription = Some("newMeta"),
      relatedContent = Some(Seq(Left(commonApi.RelatedContentLinkDTO("title1", "url2")), Right(12L))),
      tags = Some(Seq("new", "tag"))
    )

    val existing = TestData.sampleDomainArticle.copy(
      title = Seq(Title(existingTitle, "nb")),
      status = TestData.statusWithPublished,
      availability = Availability.everyone,
      grepCodes = Seq.empty,
      copyright = Some(TestData.publicDomainCopyright.copy(license = Some("oldLicense"), origin = None)),
      metaDescription = Seq.empty,
      relatedContent = Seq.empty,
      tags = Seq.empty
    )

    when(draftRepository.withId(eqTo(existing.id.get))(any)).thenReturn(Some(existing))
    when(writeService.partialPublish(any, any, any, any)).thenReturn((existing.id.get, Success(existing)))
    when(articleApiClient.partialPublishArticle(any, any, any)).thenReturn(Success(existing.id.get))

    val Success(result1) = service.updateArticle(existing.id.get, updatedArticle, TestData.userWithWriteAccess)

    result1.status.current should be(existing.status.current.toString)
    result1.status.other.sorted should be(existing.status.other.map(_.toString).toSeq.sorted)

    result1.availability should be(Availability.teacher.toString)
    result1.grepCodes should be(Seq("a", "b", "c"))
    result1.copyright.get.license.get.license should be("COPYRIGHTED")
    result1.metaDescription.get.metaDescription should be("newMeta")
    result1.relatedContent.head.leftSide should be(Left(commonApi.RelatedContentLinkDTO("title1", "url2")))
    result1.relatedContent.reverse.head should be(Right(12L))
    result1.tags.get.tags should be(Seq("new", "tag"))
    result1.notes.head.note should be("Artikkelen har blitt delpublisert")

    val captor: ArgumentCaptor[Draft] = ArgumentCaptor.forClass(classOf[Draft])
    Mockito.verify(draftRepository).updateArticle(captor.capture())(any)
    val articlePassedToUpdate = captor.getValue
    articlePassedToUpdate.notes.head.note should be("Artikkelen har blitt delpublisert")
  }

  test("New articles are not made with empty-strings for empty fields") {
    val newArt = TestData.newArticle.copy(
      language = "nb",
      title = "Jonas",
      content = Some(""),
      introduction = Some(""),
      tags = None,
      metaDescription = Some(""),
      visualElement = Some("")
    )

    when(draftRepository.newEmptyArticleId()(any[DBSession])).thenReturn(Success(10L))

    val Success(_) = service.newArticle(newArt, TestData.userWithWriteAccess)

    val captor: ArgumentCaptor[Draft] = ArgumentCaptor.forClass(classOf[Draft])
    Mockito.verify(draftRepository).insert(captor.capture())(any)
    val articlePassedToUpdate = captor.getValue

    articlePassedToUpdate.content should be(Seq.empty)
    articlePassedToUpdate.introduction should be(Seq.empty)
    articlePassedToUpdate.tags should be(Seq.empty)
    articlePassedToUpdate.metaDescription should be(Seq.empty)
    articlePassedToUpdate.visualElement should be(Seq.empty)

  }

  test("shouldUpdateStatus returns false if articles are equal") {
    val nnTitle = Title("Title", "nn")
    val nbTitle = Title("Title", "nb")

    val article1 = TestData.sampleDomainArticle.copy(title = Seq(nnTitle, nbTitle))
    val article2 = TestData.sampleDomainArticle.copy(title = Seq(nnTitle, nbTitle))
    service.shouldUpdateStatus(article1, article2) should be(false)

    val article3 = TestData.sampleDomainArticle.copy(title = Seq(nnTitle, nbTitle))
    val article4 = TestData.sampleDomainArticle.copy(title = Seq(nbTitle, nnTitle))
    service.shouldUpdateStatus(article3, article4) should be(false)
  }

  test("shouldUpdateStatus should returns false when comparing comments") {
    val comment1 =
      Comment(
        id = UUID.randomUUID(),
        created = clock.now(),
        updated = clock.now(),
        content = "hei",
        isOpen = true,
        solved = false
      )
    val comment2 =
      Comment(
        id = UUID.randomUUID(),
        created = clock.now(),
        updated = clock.now(),
        content = "hi hi",
        isOpen = false,
        solved = false
      )
    val comment3 = Comment(
      id = UUID.randomUUID(),
      created = clock.now().minusDays(1),
      updated = clock.now().minusDays(1),
      content = "hello",
      isOpen = true,
      solved = false
    )

    val article1 = TestData.sampleDomainArticle.copy(comments = Seq(comment1, comment2))
    val article2 = TestData.sampleDomainArticle.copy(comments = Seq(comment2, comment3))
    service.shouldUpdateStatus(article1, article2) should be(false)

    val article3 = TestData.sampleDomainArticle.copy(comments = Seq(comment1, comment2, comment3))
    val article4 = TestData.sampleDomainArticle.copy(comments = Seq.empty)
    service.shouldUpdateStatus(article3, article4) should be(false)
  }

  test("shouldPartialPublish return empty-set if articles are equal") {
    val nnMeta = Description("Meta nn", "nn")
    val nbMeta = Description("Meta nb", "nb")

    val article1 = TestData.sampleDomainArticle.copy(
      status = Status(PLANNED, Set(PUBLISHED)),
      metaDescription = Seq(nnMeta, nbMeta)
    )
    val article2 = TestData.sampleDomainArticle.copy(
      status = Status(PLANNED, Set(PUBLISHED)),
      metaDescription = Seq(nnMeta, nbMeta)
    )
    service.shouldPartialPublish(Some(article1), article2) should be(Set.empty)

    val article3 = TestData.sampleDomainArticle.copy(
      status = Status(PLANNED, Set(PUBLISHED)),
      metaDescription = Seq(nnMeta, nbMeta)
    )
    val article4 = TestData.sampleDomainArticle.copy(
      status = Status(PLANNED, Set(PUBLISHED)),
      metaDescription = Seq(nbMeta, nnMeta)
    )
    service.shouldPartialPublish(Some(article3), article4) should be(Set.empty)

  }

  test("shouldPartialPublish returns set of changed fields") {

    val article1 = TestData.sampleDomainArticle.copy(
      status = Status(PLANNED, Set(PUBLISHED)),
      metaDescription = Seq(
        Description("Meta nn", "nn"),
        Description("Meta nb", "nb")
      ),
      grepCodes = Seq(
        "KE123"
      )
    )

    val article2 = TestData.sampleDomainArticle.copy(
      status = Status(PLANNED, Set(PUBLISHED)),
      metaDescription = Seq(
        Description("Ny Meta nn", "nn"),
        Description("Meta nb", "nb")
      ),
      grepCodes = Seq("KE123", "KE456")
    )

    service.shouldPartialPublish(Some(article1), article2) should be(
      Set(
        PartialArticleFieldsDTO.metaDescription,
        PartialArticleFieldsDTO.grepCodes
      )
    )
  }

  test("copyRevisionDates updates articles") {
    val nodeId   = "urn:topic:1"
    val node     = Node(nodeId, "Topic", Some("urn:article:1"), List.empty)
    val child    = Node("urn:topic:2", "Topic", Some("urn:article:2"), List.empty)
    val resource = Node("urn:resource:1", "Resource", Some("urn:article:3"), List.empty)

    val revisionMeta =
      RevisionMeta(UUID.randomUUID(), NDLADate.now(), "Test revision", RevisionStatus.NeedsRevision)
    val article1 = TestData.sampleDomainArticle.copy(id = Some(1L), revisionMeta = Seq(revisionMeta))
    val article2 = TestData.sampleDomainArticle.copy(id = Some(2L))
    val article3 = TestData.sampleDomainArticle.copy(id = Some(3L))

    when(taxonomyApiClient.getNode(nodeId)).thenReturn(Success(node))
    when(taxonomyApiClient.getChildNodes(nodeId)).thenReturn(Success(List(child)))
    when(taxonomyApiClient.getChildResources(any))
      .thenReturn(Success(List(resource)), Success(List(resource)), Success(List.empty))
    when(draftRepository.withId(eqTo(1L))(any)).thenReturn(Some(article1))
    when(draftRepository.withId(eqTo(2L))(any)).thenReturn(Some(article2))
    when(draftRepository.withId(eqTo(3L))(any)).thenReturn(Some(article3))
    service.copyRevisionDates(nodeId) should be(Success(()))
    verify(draftRepository, times(3)).updateArticle(any[Draft])(any[DBSession])
  }

  test("copyRevisionDates does nothing if revisionMeta is empty") {
    val nodeId   = "urn:topic:1"
    val node     = Node(nodeId, "Topic", Some("urn:article:1"), List.empty)
    val child    = Node("urn:topic:2", "Topic", Some("urn:article:2"), List.empty)
    val resource = Node("urn:resource:1", "Resource", Some("urn:article:3"), List.empty)

    val article1 = TestData.sampleDomainArticle.copy(id = Some(1))
    val article2 = TestData.sampleDomainArticle.copy(id = Some(2))
    val article3 = TestData.sampleDomainArticle.copy(id = Some(3))

    when(taxonomyApiClient.getNode(nodeId)).thenReturn(Success(node))
    when(taxonomyApiClient.getChildNodes(nodeId)).thenReturn(Success(List(child)))
    when(taxonomyApiClient.getChildResources(any)).thenReturn(Success(List(resource)))
    when(draftRepository.withId(eqTo(1L))(any)).thenReturn(Some(article1))
    when(draftRepository.withId(eqTo(2L))(any)).thenReturn(Some(article2))
    when(draftRepository.withId(eqTo(3L))(any)).thenReturn(Some(article3))
    service.copyRevisionDates(nodeId) should be(Success(()))
    verify(draftRepository, times(0)).updateArticle(any[Draft])(any[DBSession])
  }

  test("copyRevisionDates skips empty contentUris") {
    val nodeId   = "urn:topic:1"
    val node     = Node(nodeId, "Topic", Some("urn:article:1"), List.empty)
    val child    = Node("urn:topic:2", "Topic", None, List.empty)
    val resource = Node("urn:resource:1", "Resource", Some("urn:article:2"), List.empty)

    val revisionMeta =
      RevisionMeta(UUID.randomUUID(), NDLADate.now(), "Test revision", RevisionStatus.NeedsRevision)
    val article1 = TestData.sampleDomainArticle.copy(
      id = Some(1),
      revisionMeta = Seq(revisionMeta)
    )
    val article2 = TestData.sampleDomainArticle.copy(id = Some(2))

    when(taxonomyApiClient.getNode(nodeId)).thenReturn(Success(node))
    when(taxonomyApiClient.getChildNodes(nodeId)).thenReturn(Success(List(child)))
    when(taxonomyApiClient.getChildResources(any))
      .thenReturn(Success(List(resource)), Success(List(resource)), Success(List.empty))
    when(draftRepository.withId(eqTo(1L))(any)).thenReturn(Some(article1))
    when(draftRepository.withId(eqTo(2L))(any)).thenReturn(Some(article2))
    service.copyRevisionDates(nodeId) should be(Success(()))
    verify(draftRepository, times(2)).updateArticle(any[Draft])(any[DBSession])
  }

  test("That started is updated when article is changed") {
    val existing = TestData.sampleDomainArticle.copy(
      started = false,
      status = TestData.statusWithPlanned,
      responsible = Some(Responsible("123", NDLADate.now()))
    )

    val updatedArticle = TestData.blankUpdatedArticle.copy(
      revision = 1,
      title = Some("updated title"),
      language = Some("nb")
    )

    when(draftRepository.withId(eqTo(existing.id.get))(any)).thenReturn(Some(existing))
    val result = service
      .updateArticle(
        existing.id.get,
        updatedArticle,
        TestData.userWithWriteAccess
      )
      .get

    result.started should be(true)
  }

  test("That started is reset when status is changed") {
    val existing = TestData.sampleDomainArticle.copy(
      started = true,
      status = TestData.statusWithPlanned,
      responsible = Some(Responsible("responsible", NDLADate.now()))
    )
    val updatedArticle = TestData.blankUpdatedArticle.copy(
      revision = 1,
      title = Some("updated title"),
      language = Some("nb"),
      status = Some("IN_PROGRESS")
    )

    when(draftRepository.withId(eqTo(existing.id.get))(any)).thenReturn(Some(existing))
    val result = service
      .updateArticle(
        existing.id.get,
        updatedArticle,
        TestData.userWithWriteAccess
      )
      .get

    result.started should be(false)
  }

  test("That started is reset when responsible is changed") {
    val existing = TestData.sampleDomainArticle.copy(
      started = true,
      status = TestData.statusWithPlanned,
      responsible = Some(Responsible("responsible", NDLADate.now()))
    )
    val updatedArticle = TestData.blankUpdatedArticle.copy(
      revision = 1,
      title = Some("updated title"),
      language = Some("nb"),
      responsibleId = UpdateWith("heiho")
    )
    when(draftRepository.withId(eqTo(existing.id.get))(any)).thenReturn(Some(existing))
    val result = service
      .updateArticle(
        existing.id.get,
        updatedArticle,
        TestData.userWithWriteAccess
      )
      .get

    result.started should be(false)
  }

  test("That started is true'd when articles is changed when published") {
    val existing = TestData.sampleDomainArticle.copy(
      started = false,
      status = TestData.statusWithPublished,
      responsible = Some(Responsible("responsible", NDLADate.now()))
    )
    val updatedArticle = TestData.blankUpdatedArticle.copy(
      revision = 1,
      title = Some("updated title"),
      language = Some("nb")
    )
    when(draftRepository.withId(eqTo(existing.id.get))(any)).thenReturn(Some(existing))
    val result = service
      .updateArticle(
        existing.id.get,
        updatedArticle,
        TestData.userWithWriteAccess
      )
      .get

    result.status.current should be(IN_PROGRESS.toString)
    result.started should be(true)
  }

  test("That partial published fields does not set started") {
    val existing = TestData.sampleDomainArticle.copy(
      started = false,
      status = TestData.statusWithPublished,
      responsible = Some(Responsible("responsible", NDLADate.now()))
    )
    val updatedArticle = TestData.blankUpdatedArticle.copy(
      revision = 1,
      metaDescription = Some("updated title"),
      language = Some("nb")
    )
    when(draftRepository.withId(eqTo(existing.id.get))(any)).thenReturn(Some(existing))
    val result = service
      .updateArticle(
        existing.id.get,
        updatedArticle,
        TestData.userWithWriteAccess
      )
      .get

    result.status.current should be(PUBLISHED.toString)
    result.started should be(false)
  }

  test("That started is reset when published via PUBLISHED field") {
    scala.util.Properties.setProp("DEBUG_FLAKE", "true")
    doAnswer((i: InvocationOnMock) => { Success(i.getArgument[Draft](1)) })
      .when(articleApiClient)
      .updateArticle(any, any, any, any, any)

    val existing = TestData.sampleDomainArticle.copy(
      started = true,
      status = TestData.statusWithInProcess,
      responsible = None
    )
    val updatedArticle = TestData.blankUpdatedArticle.copy(
      revision = 1,
      status = Some("PUBLISHED"),
      language = Some("nb")
    )
    when(draftRepository.withId(eqTo(existing.id.get))(any)).thenReturn(Some(existing))
    val result = service
      .updateArticle(
        existing.id.get,
        updatedArticle,
        TestData.userWithAdminAccess
      )
      .get

    result.status.current should be(PUBLISHED.toString)
    result.started should be(false)
  }

  test("that deleting current revision fails if status is PUBLISHED") {
    val existing = TestData.sampleDomainArticle.copy(status = TestData.statusWithPublished)
    when(draftRepository.withId(eqTo(existing.id.get))(any)).thenReturn(Some(existing))
    val result = service.deleteCurrentRevision(existing.id.get)
    result.isFailure should be(true)
  }

  test("that deleting current revision fails if number of revisions is 1 or less") {
    val existing = TestData.sampleDomainArticle.copy(status = TestData.statusWithInProcess)
    when(draftRepository.withId(eqTo(existing.id.get))(any)).thenReturn(Some(existing))
    when(draftRepository.revisionCountForArticleId(eqTo(existing.id.get))(any)).thenReturn(Success(1))
    val result = service.deleteCurrentRevision(existing.id.get)
    result.isFailure should be(true)
  }
}
