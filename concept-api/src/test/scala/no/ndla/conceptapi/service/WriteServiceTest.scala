/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.service

import java.util.Date

import no.ndla.conceptapi.auth.UserInfo
import no.ndla.conceptapi.model.api
import no.ndla.conceptapi.model.domain
import no.ndla.conceptapi.model.domain._
import no.ndla.conceptapi.{TestData, TestEnvironment, UnitSuite}
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers._

import scala.util.{Failure, Success, Try}
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, Mockito}
import org.mockito.invocation.InvocationOnMock
import scalikejdbc.DBSession

class WriteServiceTest extends UnitSuite with TestEnvironment {
  override val converterService = new ConverterService

  val today: Date = DateTime.now().toDate
  val yesterday: Date = DateTime.now().minusDays(1).toDate
  val service = new WriteService()
  val conceptId = 13
  val userInfo = UserInfo.SystemUser

  val concept: api.Concept =
    TestData.sampleNbApiConcept.copy(id = conceptId.toLong, updated = today, supportedLanguages = Set("nb"))

  val domainConcept: domain.Concept = TestData.sampleNbDomainConcept.copy(id = Some(conceptId.toLong))

  def mockWithConcept(concept: domain.Concept) = {
    when(draftConceptRepository.withId(conceptId)).thenReturn(Option(concept))
    when(draftConceptRepository.update(any[Concept])(any[DBSession]))
      .thenAnswer((invocation: InvocationOnMock) => Try(invocation.getArgument[Concept](0)))

    when(contentValidator.validateConcept(any[Concept])).thenAnswer((invocation: InvocationOnMock) =>
      Try(invocation.getArgument[Concept](0)))

    when(draftConceptIndexService.indexDocument(any[Concept])).thenAnswer((invocation: InvocationOnMock) =>
      Try(invocation.getArgument[Concept](0)))
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
      api.UpdatedConcept("en",
                         None,
                         content = Some(newContent),
                         Right(None),
                         None,
                         None,
                         None,
                         None,
                         Some(Seq.empty),
                         None,
                         None)
    val expectedConcept = concept.copy(content = Option(api.ConceptContent(newContent, "en")),
                                       updated = today,
                                       supportedLanguages = Set("nb", "en"),
                                       articleIds = Seq.empty)
    val result = service.updateConcept(conceptId, updatedApiConcept, userInfo.copy(id = "")).get
    result should equal(expectedConcept)
  }

  test("That update function updates only title properly") {
    val newTitle = "NewTitleTest"
    val updatedApiConcept =
      api.UpdatedConcept("nn",
                         title = Some(newTitle),
                         None,
                         Right(None),
                         None,
                         None,
                         None,
                         None,
                         Some(Seq.empty),
                         None,
                         None)
    val expectedConcept = concept.copy(title = Option(api.ConceptTitle(newTitle, "nn")),
                                       updated = today,
                                       supportedLanguages = Set("nb", "nn"),
                                       articleIds = Seq.empty)
    service.updateConcept(conceptId, updatedApiConcept, userInfo.copy(id = "")).get should equal(expectedConcept)
  }

  test("That updateConcept updates multiple fields properly") {
    val updatedTitle = "NyTittelTestJee"
    val updatedContent = "NyContentTestYepp"
    val updatedCopyright =
      api.Copyright(None, Some("c"), Seq(api.Author("Opphavsmann", "Katrine")), List(), List(), None, None, None)
    val updatedMetaImage = api.NewConceptMetaImage("2", "AltTxt")
    val updatedSource = "https://www.ndla.no"

    val updatedApiConcept = api.UpdatedConcept(
      "en",
      Some(updatedTitle),
      Some(updatedContent),
      Right(Some(updatedMetaImage)),
      Some(updatedCopyright),
      Some(updatedSource),
      Some(Seq("Nye", "Tags")),
      Some(Seq("urn:subject:900")),
      Some(Seq(69L)),
      None,
      None
    )

    val expectedConcept = concept.copy(
      title = Option(api.ConceptTitle(updatedTitle, "en")),
      content = Option(api.ConceptContent(updatedContent, "en")),
      metaImage = Some(api.ConceptMetaImage("http://api-gateway.ndla-local/image-api/raw/id/2", "AltTxt", "en")),
      copyright = Some(
        api.Copyright(None, Some("c"), Seq(api.Author("Opphavsmann", "Katrine")), List(), List(), None, None, None)),
      source = Some("https://www.ndla.no"),
      supportedLanguages = Set("nb", "en"),
      tags = Some(api.ConceptTags(Seq("Nye", "Tags"), "en")),
      subjectIds = Some(Set("urn:subject:900")),
      articleIds = Seq(69L)
    )

    service.updateConcept(conceptId, updatedApiConcept, userInfo.copy(id = "")) should equal(Success(expectedConcept))

  }

  test("That delete concept should fail when only one language") {
    val Failure(result) = service.deleteLanguage(concept.id, "nb", userInfo)

    result.getMessage should equal("Only one language left")
  }

  test("That delete concept removes language from all languagefields") {
    val concept =
      TestData.sampleNbDomainConcept.copy(id = Some(3.toLong),
                                          title = Seq(ConceptTitle("title", "nb"), ConceptTitle("title", "nn")))
    val conceptCaptor: ArgumentCaptor[Concept] = ArgumentCaptor.forClass(classOf[Concept])

    when(draftConceptRepository.withId(anyLong)).thenReturn(Some(concept))

    service.deleteLanguage(concept.id.get, "nn", userInfo)
    verify(draftConceptRepository).update(conceptCaptor.capture())

    conceptCaptor.getValue.title.length should be(1)
  }

  test("That updating concepts updates revision") {
    reset(draftConceptRepository)

    val conceptToUpdate = domainConcept.copy(
      revision = Some(951),
      title = Seq(domain.ConceptTitle("Yolo", "en")),
      updated = new Date(0),
      created = new Date(0)
    )

    mockWithConcept(conceptToUpdate)

    val updatedTitle = "NyTittelTestJee"
    val updatedApiConcept = TestData.emptyApiUpdatedConcept.copy(language = "en", title = Some(updatedTitle))

    val conceptCaptor: ArgumentCaptor[Concept] = ArgumentCaptor.forClass(classOf[Concept])

    service.updateConcept(conceptId, updatedApiConcept, userInfo)

    verify(draftConceptRepository).update(conceptCaptor.capture())(any[DBSession])

    conceptCaptor.getValue.revision should be(Some(951))
    conceptCaptor.getValue.title should be(Seq(domain.ConceptTitle(updatedTitle, "en")))
  }

}
