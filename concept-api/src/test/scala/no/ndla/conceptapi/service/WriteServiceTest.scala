/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.service

import no.ndla.common.model.domain.{ContributorType, Responsible, Tag, Title}
import no.ndla.common.model.api as commonApi
import no.ndla.conceptapi.model.api.ConceptResponsibleDTO
import no.ndla.conceptapi.model.api
import no.ndla.conceptapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.network.tapir.auth.TokenUser
import org.mockito.invocation.InvocationOnMock
import org.mockito.{ArgumentCaptor, Mockito}
import scalikejdbc.DBSession

import scala.util.{Failure, Success, Try}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.{Missing, UpdateWith}
import no.ndla.common.model.domain.concept.{Concept, ConceptContent, ConceptStatus, Status, VisualElement}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.mockito.stubbing.OngoingStubbing

class WriteServiceTest extends UnitSuite with TestEnvironment {
  override val converterService = new ConverterService

  val today: NDLADate     = NDLADate.now()
  val yesterday: NDLADate = NDLADate.now().minusDays(1)
  val service             = new WriteService()
  val conceptId           = 13L
  val userInfo: TokenUser = TokenUser.SystemUser

  val concept: api.ConceptDTO =
    TestData.sampleNbApiConcept.copy(
      id = conceptId,
      updated = today,
      supportedLanguages = Set("nb"),
      responsible = Some(ConceptResponsibleDTO("hei", TestData.today))
    )

  val domainConcept: Concept =
    TestData.sampleNbDomainConcept.copy(id = Some(conceptId), responsible = Some(Responsible("hei", TestData.today)))

  def mockWithConcept(concept: Concept): OngoingStubbing[NDLADate] = {
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
    verify(searchApiClient, times(1)).indexDocument(any[String], any[Concept], any[Option[TokenUser]])(any, any, any)
  }

  test("That update function updates only content properly") {
    val newContent = "NewContentTest"
    val updatedApiConcept =
      api.UpdatedConceptDTO(
        "en",
        None,
        content = Some(newContent),
        None,
        None,
        None,
        None,
        Missing,
        None,
        None
      )
    val expectedConcept = concept.copy(
      content = Option(api.ConceptContent(newContent, newContent, "en")),
      updated = today,
      supportedLanguages = Set("nb", "en"),
      editorNotes = Some(
        Seq(
          api.EditorNoteDTO("New language 'en' added", "", api.StatusDTO("IN_PROGRESS", Seq.empty), today)
        )
      )
    )
    val result = service.updateConcept(conceptId, updatedApiConcept, userInfo.copy(id = "")).get
    result should equal(expectedConcept)
  }

  test("That update function updates only title properly") {
    val newTitle = "NewTitleTest"
    val updatedApiConcept =
      api.UpdatedConceptDTO("nn", title = Some(newTitle), None, None, None, None, None, Missing, None, None)
    val expectedConcept = concept.copy(
      title = api.ConceptTitleDTO(newTitle, newTitle, "nn"),
      updated = today,
      supportedLanguages = Set("nb", "nn"),
      editorNotes = Some(
        Seq(
          api.EditorNoteDTO("New language 'nn' added", "", api.StatusDTO("IN_PROGRESS", Seq.empty), today)
        )
      )
    )
    service.updateConcept(conceptId, updatedApiConcept, userInfo.copy(id = "")).get should equal(expectedConcept)
  }

  test("That updateConcept updates multiple fields properly") {
    val updatedTitle   = "NyTittelTestJee"
    val updatedContent = "NyContentTestYepp"
    val updatedCopyright =
      commonApi.DraftCopyrightDTO(
        None,
        Some("https://ndla.no"),
        Seq(commonApi.AuthorDTO(ContributorType.Originator, "Katrine")),
        List(),
        List(),
        None,
        None,
        false
      )

    val updatedApiConcept = api.UpdatedConceptDTO(
      "en",
      Some(updatedTitle),
      Some(updatedContent),
      Some(updatedCopyright),
      Some(Seq("Nye", "Tags")),
      None,
      None,
      UpdateWith("123"),
      None,
      None
    )

    val expectedConcept = concept.copy(
      title = api.ConceptTitleDTO(updatedTitle, updatedTitle, "en"),
      content = Option(api.ConceptContent(updatedContent, updatedContent, "en")),
      copyright = Some(
        commonApi.DraftCopyrightDTO(
          None,
          Some("https://ndla.no"),
          Seq(commonApi.AuthorDTO(ContributorType.Originator, "Katrine")),
          List(),
          List(),
          None,
          None,
          false
        )
      ),
      source = Some("https://ndla.no"),
      tags = Some(api.ConceptTagsDTO(Seq("Nye", "Tags"), "en")),
      supportedLanguages = Set("nb", "en"),
      responsible = Some(api.ConceptResponsibleDTO("123", today)),
      editorNotes = Some(
        Seq(
          api.EditorNoteDTO("New language 'en' added", "", api.StatusDTO("IN_PROGRESS", Seq.empty), today),
          api.EditorNoteDTO("Responsible changed", "", api.StatusDTO("IN_PROGRESS", Seq.empty), today)
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
        status = Status(ConceptStatus.IN_PROGRESS, Set.empty),
        visualElement = Seq(VisualElement("VisueltElement", "nb"), VisualElement("VisueltElement", "nn")),
        responsible = Some(Responsible("hei", TestData.today))
      )
    val conceptCaptor: ArgumentCaptor[Concept] = ArgumentCaptor.forClass(classOf[Concept])

    when(draftConceptRepository.withId(any)).thenReturn(Some(concept))

    val updated = service.deleteLanguage(concept.id.get, "nn", userInfo).get
    verify(draftConceptRepository).update(conceptCaptor.capture())

    conceptCaptor.getValue.title.length should be(1)
    conceptCaptor.getValue.content.length should be(1)
    conceptCaptor.getValue.tags.length should be(1)
    conceptCaptor.getValue.visualElement.length should be(1)
    updated.supportedLanguages should not contain "nn"
  }

  test("That updating concepts updates revision") {
    reset(draftConceptRepository)

    val conceptToUpdate = domainConcept.copy(
      revision = Some(951),
      title = Seq(Title("Yolo", "en")),
      created = NDLADate.fromUnixTime(0),
      updated = NDLADate.fromUnixTime(0)
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
      api.UpdatedConceptDTO(
        language = "nb",
        title = None,
        content = None,
        copyright = None,
        tags = None,
        status = None,
        visualElement = None,
        responsibleId = UpdateWith(responsibleId),
        conceptType = None,
        glossData = None
      )
    val expectedConcept = concept.copy(
      updated = today,
      supportedLanguages = Set("nb"),
      responsible = Some(api.ConceptResponsibleDTO(responsibleId, today)),
      editorNotes = Some(
        Seq(
          api.EditorNoteDTO("Responsible changed", "", api.StatusDTO("IN_PROGRESS", Seq.empty), today)
        )
      )
    )
    service.updateConcept(conceptId, updatedApiConcept, userInfo.copy(id = "")).get should equal(expectedConcept)
  }

}
