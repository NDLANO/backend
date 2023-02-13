/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.service

import no.ndla.common.model.{domain => common}
import no.ndla.conceptapi.auth.UserInfo
import no.ndla.conceptapi.model.api.{Copyright, NotFoundException, UpdatedConcept}
import no.ndla.conceptapi.model.{api, domain}
import no.ndla.conceptapi.{TestData, TestEnvironment, UnitSuite}

import java.time.LocalDateTime
import scala.util.{Failure, Success}

class ConverterServiceTest extends UnitSuite with TestEnvironment {

  override val converterService = new ConverterService
  val userInfo: UserInfo        = UserInfo.SystemUser.copy(id = "")

  test("toApiConcept converts a domain.Concept to an api.Concept with defined language") {
    converterService.toApiConcept(TestData.domainConcept, "nn", fallback = false) should be(
      Success(TestData.sampleNnApiConcept)
    )
    converterService.toApiConcept(TestData.domainConcept, "nb", fallback = false) should be(
      Success(TestData.sampleNbApiConcept)
    )
  }

  test("toApiConcept failure if concept not found in specified language without fallback") {
    converterService.toApiConcept(TestData.domainConcept, "hei", fallback = false) should be(
      Failure(
        NotFoundException(
          s"The concept with id ${TestData.domainConcept.id.get} and language 'hei' was not found.",
          TestData.domainConcept.supportedLanguages.toSeq
        )
      )
    )
  }

  test("toApiConcept success if concept not found in specified language, but with fallback") {
    converterService.toApiConcept(TestData.domainConcept, "hei", fallback = true) should be(
      Success(TestData.sampleNbApiConcept)
    )
  }

  test("toDomainConcept updates title in concept correctly") {
    val updated = LocalDateTime.now()
    when(clock.now()).thenReturn(updated)

    val updateWith =
      UpdatedConcept(
        "nb",
        Some("heisann"),
        None,
        Right(None),
        None,
        None,
        None,
        None,
        Some(Seq(42L)),
        None,
        None,
        Right(None)
      )
    converterService.toDomainConcept(TestData.domainConcept, updateWith, userInfo) should be(
      TestData.domainConcept.copy(
        title = Seq(
          common.Title("Tittelur", "nn"),
          common.Title("heisann", "nb")
        ),
        updated = updated
      )
    )
  }

  test("toDomainConcept updates content in concept correctly") {
    val updated = LocalDateTime.now()
    when(clock.now()).thenReturn(updated)

    val updateWith =
      UpdatedConcept(
        "nn",
        None,
        Some("Nytt innhald"),
        Right(None),
        None,
        None,
        None,
        None,
        Some(Seq(42L)),
        None,
        None,
        Right(None)
      )
    converterService.toDomainConcept(TestData.domainConcept, updateWith, userInfo) should be(
      TestData.domainConcept.copy(
        content = Seq(
          domain.ConceptContent("Innhold", "nb"),
          domain.ConceptContent("Nytt innhald", "nn")
        ),
        updated = updated
      )
    )
  }

  test("toDomainConcept adds new language in concept correctly") {
    val updated = LocalDateTime.now()
    when(clock.now()).thenReturn(updated)

    val updateWith =
      UpdatedConcept(
        "en",
        Some("Title"),
        Some("My content"),
        Right(None),
        None,
        None,
        None,
        None,
        Some(Seq(42L)),
        None,
        None,
        Right(None)
      )
    converterService.toDomainConcept(TestData.domainConcept, updateWith, userInfo) should be(
      TestData.domainConcept.copy(
        title = Seq(
          common.Title("Tittel", "nb"),
          common.Title("Tittelur", "nn"),
          common.Title("Title", "en")
        ),
        content = Seq(
          domain.ConceptContent("Innhold", "nb"),
          domain.ConceptContent("Innhald", "nn"),
          domain.ConceptContent("My content", "en")
        ),
        updated = updated
      )
    )
  }

  test("toDomainConcept updates copyright correctly") {
    val updated = LocalDateTime.now()
    when(clock.now()).thenReturn(updated)

    val updateWith = UpdatedConcept(
      "nn",
      None,
      Some("Nytt innhald"),
      Right(None),
      Option(
        Copyright(
          None,
          None,
          Seq(api.Author("Photographer", "Photographer")),
          Seq(api.Author("Photographer", "Photographer")),
          Seq(api.Author("Photographer", "Photographer")),
          None,
          None,
          None
        )
      ),
      None,
      None,
      None,
      Some(Seq(42L)),
      None,
      None,
      Right(None)
    )
    converterService.toDomainConcept(TestData.domainConcept, updateWith, userInfo) should be(
      TestData.domainConcept.copy(
        content = Seq(
          domain.ConceptContent("Innhold", "nb"),
          domain.ConceptContent("Nytt innhald", "nn")
        ),
        copyright = Option(
          common.draft.Copyright(
            None,
            None,
            Seq(common.Author("Photographer", "Photographer")),
            Seq(common.Author("Photographer", "Photographer")),
            Seq(common.Author("Photographer", "Photographer")),
            None,
            None,
            None
          )
        ),
        updated = updated
      )
    )
  }

  test("toDomainConcept deletes removes all articleIds when getting empty list as parameter") {
    val updated = LocalDateTime.now()
    when(clock.now()).thenReturn(updated)

    val beforeUpdate = TestData.domainConcept.copy(
      articleIds = Seq(12),
      updated = updated
    )
    val afterUpdate = TestData.domainConcept.copy(
      articleIds = Seq.empty,
      updated = updated
    )
    val updateWith = TestData.emptyApiUpdatedConcept.copy(articleIds = Some(Seq.empty))

    converterService.toDomainConcept(beforeUpdate, updateWith, userInfo) should be(afterUpdate)
  }

  test("toDomainConcept updates articleIds when getting list as a parameter") {
    val updated = LocalDateTime.now()
    when(clock.now()).thenReturn(updated)

    val beforeUpdate = TestData.domainConcept.copy(
      articleIds = Seq.empty,
      updated = updated
    )
    val afterUpdate = TestData.domainConcept.copy(
      articleIds = Seq(12),
      updated = updated
    )
    val updateWith = TestData.emptyApiUpdatedConcept.copy(articleIds = Some(Seq(12)))

    converterService.toDomainConcept(beforeUpdate, updateWith, userInfo) should be(afterUpdate)
  }

  test("toDomainConcept does nothing to articleId when getting None as a parameter") {
    val updated = LocalDateTime.now()
    when(clock.now()).thenReturn(updated)

    val beforeUpdate = TestData.domainConcept.copy(
      articleIds = Seq(12),
      updated = updated
    )
    val afterUpdate = TestData.domainConcept.copy(
      articleIds = Seq(12),
      updated = updated
    )
    val updateWith = TestData.emptyApiUpdatedConcept.copy(articleIds = None)

    converterService.toDomainConcept(beforeUpdate, updateWith, userInfo) should be(afterUpdate)
  }

  test("toDomainConcept update concept with ID updates articleId when getting new articleId as a parameter") {
    val today = LocalDateTime.now()
    when(clock.now()).thenReturn(today)

    val afterUpdate = TestData.domainConcept_toDomainUpdateWithId.copy(
      id = Some(12),
      articleIds = Seq(15),
      created = today,
      updated = today
    )
    val updateWith = TestData.emptyApiUpdatedConcept.copy(articleIds = Some(Seq(15)))

    converterService.toDomainConcept(12, updateWith, userInfo) should be(
      afterUpdate
    )
  }

  test("toDomainConcept update concept with ID sets articleIds to empty list when articleId is not specified") {
    val today = LocalDateTime.now()
    when(clock.now()).thenReturn(today)

    val afterUpdate = TestData.domainConcept_toDomainUpdateWithId.copy(
      id = Some(12),
      articleIds = Seq.empty,
      created = today,
      updated = today
    )
    val updateWith = TestData.emptyApiUpdatedConcept.copy(articleIds = None)

    converterService.toDomainConcept(12, updateWith, userInfo) should be(
      afterUpdate
    )
  }

  test("toDomainConcept deletes metaImage when getting null as a parameter") {
    val updated = LocalDateTime.now()
    when(clock.now()).thenReturn(updated)

    val beforeUpdate = TestData.domainConcept.copy(
      metaImage = Seq(domain.ConceptMetaImage("1", "Hei", "nb"), domain.ConceptMetaImage("2", "Hej", "nn")),
      updated = updated
    )
    val afterUpdate = TestData.domainConcept.copy(
      metaImage = Seq(domain.ConceptMetaImage("2", "Hej", "nn")),
      updated = updated
    )
    val updateWith = TestData.emptyApiUpdatedConcept.copy(language = "nb", metaImage = Left(null))

    converterService.toDomainConcept(beforeUpdate, updateWith, userInfo) should be(afterUpdate)
  }

  test("toDomainConcept updates metaImage when getting new metaImage as a parameter") {
    val updated = LocalDateTime.now()
    when(clock.now()).thenReturn(updated)

    val beforeUpdate = TestData.domainConcept.copy(
      metaImage = Seq(domain.ConceptMetaImage("1", "Hei", "nb"), domain.ConceptMetaImage("2", "Hej", "nn")),
      updated = updated
    )
    val afterUpdate = TestData.domainConcept.copy(
      metaImage = Seq(domain.ConceptMetaImage("2", "Hej", "nn"), domain.ConceptMetaImage("1", "Hola", "nb")),
      updated = updated
    )
    val updateWith = TestData.emptyApiUpdatedConcept.copy(
      language = "nb",
      metaImage = Right(Some(api.NewConceptMetaImage("1", "Hola")))
    )

    converterService.toDomainConcept(beforeUpdate, updateWith, userInfo) should be(afterUpdate)
  }

  test("toDomainConcept does nothing to metaImage when getting None as a parameter") {
    val updated = LocalDateTime.now()
    when(clock.now()).thenReturn(updated)

    val beforeUpdate = TestData.domainConcept.copy(
      metaImage = Seq(domain.ConceptMetaImage("1", "Hei", "nb"), domain.ConceptMetaImage("2", "Hej", "nn")),
      updated = updated
    )
    val afterUpdate = TestData.domainConcept.copy(
      metaImage = Seq(domain.ConceptMetaImage("1", "Hei", "nb"), domain.ConceptMetaImage("2", "Hej", "nn")),
      updated = updated
    )
    val updateWith = TestData.emptyApiUpdatedConcept.copy(language = "nb", metaImage = Right(None))

    converterService.toDomainConcept(beforeUpdate, updateWith, userInfo) should be(afterUpdate)
  }

  test("toDomainConcept update concept with ID updates metaImage when getting new metaImage as a parameter") {
    val today = LocalDateTime.now()
    when(clock.now()).thenReturn(today)

    val afterUpdate = TestData.domainConcept_toDomainUpdateWithId.copy(
      id = Some(12),
      metaImage = Seq(domain.ConceptMetaImage("1", "Hola", "nb")),
      created = today,
      updated = today
    )
    val updateWith = TestData.emptyApiUpdatedConcept.copy(
      language = "nb",
      metaImage = Right(Some(api.NewConceptMetaImage("1", "Hola")))
    )

    converterService.toDomainConcept(12, updateWith, userInfo) should be(
      afterUpdate
    )
  }

  test("toDomainConcept update concept with ID sets metaImage to Seq.empty when metaImage is not specified") {
    val today = LocalDateTime.now()
    when(clock.now()).thenReturn(today)

    val afterUpdate = TestData.domainConcept_toDomainUpdateWithId.copy(
      id = Some(12),
      metaImage = Seq.empty,
      created = today,
      updated = today
    )
    val updateWith = TestData.emptyApiUpdatedConcept.copy(language = "nb", metaImage = Left(null))

    converterService.toDomainConcept(12, updateWith, userInfo) should be(
      afterUpdate
    )
  }

  test("toDomainConcept updates updatedBy with new entry from userToken") {
    val updated = LocalDateTime.now()
    when(clock.now()).thenReturn(updated)

    val beforeUpdate = TestData.domainConcept.copy(
      updatedBy = Seq.empty,
      updated = updated
    )
    val afterUpdate = TestData.domainConcept.copy(
      updatedBy = Seq("test"),
      updated = updated
    )
    val updateWith = UserInfo.SystemUser.copy(id = "test")
    val dummy      = TestData.emptyApiUpdatedConcept

    converterService.toDomainConcept(beforeUpdate, dummy, updateWith) should be(afterUpdate)
  }

  test("toDomainConcept does not produce duplicates in updatedBy") {
    val updated = LocalDateTime.now()
    when(clock.now()).thenReturn(updated)

    val beforeUpdate = TestData.domainConcept.copy(
      updatedBy = Seq("test1", "test2"),
      updated = updated
    )
    val afterUpdate = TestData.domainConcept.copy(
      updatedBy = Seq("test1", "test2"),
      updated = updated
    )
    val updateWith = UserInfo.SystemUser.copy(id = "test1")
    val dummy      = TestData.emptyApiUpdatedConcept

    converterService.toDomainConcept(beforeUpdate, dummy, updateWith) should be(afterUpdate)
  }

  test("toDomainConcept update concept with ID updates updatedBy with new entry from userToken") {
    val today = LocalDateTime.now()
    when(clock.now()).thenReturn(today)

    val afterUpdate = TestData.domainConcept_toDomainUpdateWithId.copy(
      id = Some(12),
      updatedBy = Seq("test"),
      created = today,
      updated = today
    )
    val updateWith = UserInfo.SystemUser.copy(id = "test")
    val dummy      = TestData.emptyApiUpdatedConcept

    converterService.toDomainConcept(12, dummy, updateWith) should be(afterUpdate)
  }

  test("toDomainConcept updates updatedBy with new entry from userToken on create") {
    val today = LocalDateTime.now()
    when(clock.now()).thenReturn(today)

    val afterUpdate = TestData.domainConcept_toDomainUpdateWithId.copy(
      title = Seq(common.Title("", "")),
      updatedBy = Seq("test"),
      created = today,
      updated = today
    )
    val updateWith = UserInfo.SystemUser.copy(id = "test")
    val dummy      = TestData.emptyApiNewConcept

    converterService.toDomainConcept(dummy, updateWith) should be(Success(afterUpdate))
  }
}
