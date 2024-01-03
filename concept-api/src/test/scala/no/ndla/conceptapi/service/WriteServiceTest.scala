/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.service

import no.ndla.common.model.domain.{Responsible, Tag, Title}
import no.ndla.common.model.{api => commonApi}
import no.ndla.conceptapi.model.api.ConceptResponsible
import no.ndla.conceptapi.model.domain._
import no.ndla.conceptapi.model.{api, domain}
import no.ndla.conceptapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.network.tapir.auth.TokenUser
import org.mockito.invocation.InvocationOnMock
import org.mockito.{ArgumentCaptor, Mockito}
import scalikejdbc.DBSession

import scala.util.{Failure, Success, Try}
import no.ndla.common.model.NDLADate

class WriteServiceTest extends UnitSuite with TestEnvironment {
  override val converterService = new ConverterService

  val today: NDLADate     = NDLADate.now()
  val yesterday: NDLADate = NDLADate.now().minusDays(1)
  val service             = new WriteService()
  val conceptId           = 13L
  val userInfo: TokenUser = TokenUser.SystemUser

  val concept: api.Concept =
    TestData.sampleNbApiConcept.copy(
      id = conceptId.toLong,
      updated = today,
      supportedLanguages = Set("nb"),
      responsible = Some(ConceptResponsible("hei", TestData.today))
    )

  val domainConcept: domain.Concept = TestData.sampleNbDomainConcept.copy(
    id = Some(conceptId.toLong),
    responsible = Some(Responsible("hei", TestData.today))
  )

  def mockWithConcept(concept: domain.Concept) = {
    when(draftConceptRepository.withId(conceptId)).thenReturn(Option(concept))
    when(draftConceptRepository.update(any[Concept])(any[DBSession]))
      .thenAnswer((invocation: InvocationOnMock) => Try(invocation.getArgument[Concept](0)))

    when(contentValidator.validateConcept(any[Concept])).thenAnswer((invocation: InvocationOnMock) =>
      Try(invocation.getArgument[Concept](0))
    )

    when(draftConceptIndexService.indexDocument(any[Concept])).thenAnswer((invocation: InvocationOnMock) =>
      Try(invocation.getArgument[Concept](0))
    )
    when(clock.now()).thenReturn(today)
  }

  override def beforeEach(): Unit = {
    Mockito.reset(draftConceptRepository)
    mockWithConcept(domainConcept)
  }

  test("newConcept should insert a given Concept") {
    when(draftConceptRepository.insert(any[Concept])(any[DBSession])).thenReturn(domainConcept)
    when(contentValidator.validateConcept(any[Concept])).thenReturn(Success(domainConcept))

    service.newConcept(TestData.sampleNewConcept, userInfo).get.id.toString should equal(domainConcept.id.get.toString)
    verify(draftConceptRepository, times(1)).insert(any[Concept])
    verify(draftConceptIndexService, times(1)).indexDocument(any[Concept])
  }

  test("That update function updates only content properly") {
    val newContent = "NewContentTest"
    val updatedApiConcept =
      api.UpdatedConcept(
        "en",
        None,
        content = Some(newContent),
        Right(None),
        None,
        None,
        None,
        Some(Seq.empty),
        None,
        None,
        Right(None),
        None,
        None
      )
    val expectedConcept = concept.copy(
      content = Option(api.ConceptContent(newContent, "en")),
      updated = today,
      supportedLanguages = Set("nb", "en"),
      articleIds = Seq.empty,
      editorNotes = Some(
        Seq(
          api.EditorNote("New language 'en' added", "", api.Status("IN_PROGRESS", Seq.empty), today)
        )
      )
    )
    val result = service.updateConcept(conceptId, updatedApiConcept, userInfo.copy(id = "")).get
    result should equal(expectedConcept)
  }

  test("That update function updates only title properly") {
    val newTitle = "NewTitleTest"
    val updatedApiConcept =
      api.UpdatedConcept(
        "nn",
        title = Some(newTitle),
        None,
        Right(None),
        None,
        None,
        None,
        Some(Seq.empty),
        None,
        None,
        Right(None),
        None,
        None
      )
    val expectedConcept = concept.copy(
      title = api.ConceptTitle(newTitle, "nn"),
      updated = today,
      supportedLanguages = Set("nb", "nn"),
      articleIds = Seq.empty,
      editorNotes = Some(
        Seq(
          api.EditorNote("New language 'nn' added", "", api.Status("IN_PROGRESS", Seq.empty), today)
        )
      )
    )
    service.updateConcept(conceptId, updatedApiConcept, userInfo.copy(id = "")).get should equal(expectedConcept)
  }

  test("That updateConcept updates multiple fields properly") {
    val updatedTitle   = "NyTittelTestJee"
    val updatedContent = "NyContentTestYepp"
    val updatedCopyright =
      commonApi.DraftCopyright(
        None,
        Some("https://ndla.no"),
        Seq(commonApi.Author("Opphavsmann", "Katrine")),
        List(),
        List(),
        None,
        None,
        false
      )
    val updatedMetaImage = api.NewConceptMetaImage("2", "AltTxt")

    val updatedApiConcept = api.UpdatedConcept(
      "en",
      Some(updatedTitle),
      Some(updatedContent),
      Right(Some(updatedMetaImage)),
      Some(updatedCopyright),
      Some(Seq("Nye", "Tags")),
      Some(Seq("urn:subject:900")),
      Some(Seq(69L)),
      None,
      None,
      Right(Some("123")),
      None,
      None
    )

    val expectedConcept = concept.copy(
      title = api.ConceptTitle(updatedTitle, "en"),
      content = Option(api.ConceptContent(updatedContent, "en")),
      metaImage = Some(api.ConceptMetaImage("http://api-gateway.ndla-local/image-api/raw/id/2", "AltTxt", "en")),
      copyright = Some(
        commonApi.DraftCopyright(
          None,
          Some("https://ndla.no"),
          Seq(commonApi.Author("Opphavsmann", "Katrine")),
          List(),
          List(),
          None,
          None,
          false
        )
      ),
      source = Some("https://ndla.no"),
      supportedLanguages = Set("nb", "en"),
      tags = Some(api.ConceptTags(Seq("Nye", "Tags"), "en")),
      subjectIds = Some(Set("urn:subject:900")),
      articleIds = Seq(69L),
      responsible = Some(api.ConceptResponsible("123", today)),
      editorNotes = Some(
        Seq(
          api.EditorNote("New language 'en' added", "", api.Status("IN_PROGRESS", Seq.empty), today),
          api.EditorNote("Responsible changed", "", api.Status("IN_PROGRESS", Seq.empty), today)
        )
      )
    )

    service.updateConcept(conceptId, updatedApiConcept, userInfo.copy(id = "")) should equal(Success(expectedConcept))

  }

  test("That delete concept should fail when only one language") {
    val Failure(result) = service.deleteLanguage(concept.id, "nb", userInfo)

    result.getMessage should equal("Only one language left")
  }

  test("That delete concept removes language from all languagefields") {
    val concept =
      TestData.sampleNbDomainConcept.copy(
        id = Some(3.toLong),
        title = Seq(Title("title", "nb"), Title("title", "nn")),
        content = Seq(ConceptContent("Innhold", "nb"), ConceptContent("Innhald", "nn")),
        tags = Seq(Tag(Seq("tag"), "nb"), Tag(Seq("tag"), "nn")),
        visualElement = Seq(VisualElement("VisueltElement", "nb"), VisualElement("VisueltElement", "nn")),
        metaImage = Seq(ConceptMetaImage("1", "Hei", "nb"), ConceptMetaImage("1", "Hei", "nn")),
        status = Status(ConceptStatus.IN_PROGRESS, Set.empty),
        responsible = Some(Responsible("hei", TestData.today))
      )
    val conceptCaptor: ArgumentCaptor[Concept] = ArgumentCaptor.forClass(classOf[Concept])

    when(draftConceptRepository.withId(anyLong)).thenReturn(Some(concept))

    val updated = service.deleteLanguage(concept.id.get, "nn", userInfo).get
    verify(draftConceptRepository).update(conceptCaptor.capture())

    conceptCaptor.getValue.title.length should be(1)
    conceptCaptor.getValue.content.length should be(1)
    conceptCaptor.getValue.tags.length should be(1)
    conceptCaptor.getValue.visualElement.length should be(1)
    conceptCaptor.getValue.metaImage.length should be(1)
    updated.supportedLanguages should not contain "nn"
  }

  test("That updating concepts updates revision") {
    reset(draftConceptRepository)

    val conceptToUpdate = domainConcept.copy(
      revision = Some(951),
      title = Seq(Title("Yolo", "en")),
      updated = NDLADate.fromUnixTime(0),
      created = NDLADate.fromUnixTime(0)
    )

    mockWithConcept(conceptToUpdate)

    val updatedTitle      = "NyTittelTestJee"
    val updatedApiConcept = TestData.emptyApiUpdatedConcept.copy(language = "en", title = Some(updatedTitle))

    val conceptCaptor: ArgumentCaptor[Concept] = ArgumentCaptor.forClass(classOf[Concept])

    service.updateConcept(conceptId, updatedApiConcept, userInfo)

    verify(draftConceptRepository).update(conceptCaptor.capture())(any[DBSession])

    conceptCaptor.getValue.revision should be(Some(951))
    conceptCaptor.getValue.title should be(Seq(Title(updatedTitle, "en")))
  }

  test("That update function updates only responsible properly") {
    val responsibleId = "ResponsibleId"
    val updatedApiConcept =
      api.UpdatedConcept(
        language = "nb",
        title = None,
        content = None,
        metaImage = Right(None),
        copyright = None,
        tags = None,
        subjectIds = None,
        articleIds = Some(Seq(42)),
        status = None,
        visualElement = None,
        responsibleId = Right(Some(responsibleId)),
        conceptType = None,
        glossData = None
      )
    val expectedConcept = concept.copy(
      updated = today,
      supportedLanguages = Set("nb"),
      articleIds = Seq(42),
      responsible = Some(api.ConceptResponsible(responsibleId, today)),
      editorNotes = Some(
        Seq(
          api.EditorNote("Responsible changed", "", api.Status("IN_PROGRESS", Seq.empty), today)
        )
      )
    )
    service.updateConcept(conceptId, updatedApiConcept, userInfo.copy(id = "")).get should equal(expectedConcept)
  }

}
