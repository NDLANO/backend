/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.service

import no.ndla.common.model.domain.Responsible
import no.ndla.common.model.{domain => common}
import no.ndla.conceptapi.auth.UserInfo
import no.ndla.conceptapi.model.api.{Copyright, NotFoundException, UpdatedConcept}
import no.ndla.conceptapi.model.domain.GlossType
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
        Right(None),
        None,
        None
      )
    converterService.toDomainConcept(TestData.domainConcept, updateWith, userInfo).get should be(
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
        Right(None),
        None,
        None
      )
    converterService.toDomainConcept(TestData.domainConcept, updateWith, userInfo).get should be(
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
        Right(None),
        None,
        None
      )
    converterService.toDomainConcept(TestData.domainConcept, updateWith, userInfo).get should be(
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
      Right(None),
      None,
      None
    )
    converterService.toDomainConcept(TestData.domainConcept, updateWith, userInfo).get should be(
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

    converterService.toDomainConcept(beforeUpdate, updateWith, userInfo).get should be(afterUpdate)
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

    converterService.toDomainConcept(beforeUpdate, updateWith, userInfo).get should be(afterUpdate)
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

    converterService.toDomainConcept(beforeUpdate, updateWith, userInfo).get should be(afterUpdate)
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

    converterService.toDomainConcept(12, updateWith, userInfo) should be(afterUpdate)
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

    converterService.toDomainConcept(12, updateWith, userInfo) should be(afterUpdate)
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

    converterService.toDomainConcept(beforeUpdate, updateWith, userInfo).get should be(afterUpdate)
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

    converterService.toDomainConcept(beforeUpdate, updateWith, userInfo).get should be(afterUpdate)
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

    converterService.toDomainConcept(beforeUpdate, updateWith, userInfo).get should be(afterUpdate)
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

    converterService.toDomainConcept(12, updateWith, userInfo) should be(afterUpdate)
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

    converterService.toDomainConcept(12, updateWith, userInfo) should be(afterUpdate)
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

    converterService.toDomainConcept(beforeUpdate, dummy, updateWith).get should be(afterUpdate)
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

    converterService.toDomainConcept(beforeUpdate, dummy, updateWith).get should be(afterUpdate)
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

  test("toDomainConcept updates timestamp on responsible when id is changed") {
    val updated = LocalDateTime.now()
    when(clock.now()).thenReturn(updated)

    val responsible    = Responsible("oldId", updated.minusDays(1))
    val newResponsible = Responsible("newId", updated)

    val withOldResponsible = TestData.domainConcept.copy(
      responsible = Some(responsible),
      updated = updated
    )
    val withNewResponsible = TestData.domainConcept.copy(
      responsible = Some(newResponsible),
      updated = updated
    )
    val withoutResponsible = TestData.domainConcept.copy(
      updated = updated
    )

    val updateWith = TestData.emptyApiUpdatedConcept.copy(language = "nb", responsibleId = Right(Some("newId")))
    converterService.toDomainConcept(withOldResponsible, updateWith, userInfo).get should be(withNewResponsible)

    val updateWith2 = TestData.emptyApiUpdatedConcept.copy(language = "nb", responsibleId = Right(Some("oldId")))
    converterService.toDomainConcept(withOldResponsible, updateWith2, userInfo).get should be(withOldResponsible)

    val updateWith3 = TestData.emptyApiUpdatedConcept.copy(language = "nb", responsibleId = Left(null))
    converterService.toDomainConcept(withOldResponsible, updateWith3, userInfo).get should be(withoutResponsible)
  }

  test("that toDomainConcept (new concept) creates glossData correctly") {
    val newGlossExamples1 =
      List(api.GlossExample(example = "nei men saa", language = "nb"), api.GlossExample(example = "jog har inta", "nn"))
    val newGlossExamples2 = List(api.GlossExample(example = "nei men da saa", language = "nb"))
    val newGlossData =
      api.GlossData(glossType = "noun", originalLanguage = "nb", examples = List(newGlossExamples1, newGlossExamples2))
    val newConcept = TestData.emptyApiNewConcept.copy(conceptType = "gloss", glossData = Some(newGlossData))

    val expectedGlossExample1 = List(
      domain.GlossExample(example = "nei men saa", language = "nb"),
      domain.GlossExample(example = "jog har inta", "nn")
    )
    val expectedGlossExample2 = List(domain.GlossExample(example = "nei men da saa", language = "nb"))
    val expectedGlossData = Some(
      domain.GlossData(
        glossType = domain.GlossType.NOUN,
        originalLanguage = "nb",
        examples = List(expectedGlossExample1, expectedGlossExample2)
      )
    )
    val expectedConceptType = domain.ConceptType.GLOSS

    val result = converterService.toDomainConcept(newConcept, TestData.userWithWriteAccess).get
    result.conceptType should be(expectedConceptType)
    result.glossData should be(expectedGlossData)
  }

  test("that toDomainConcept (new concept) fails if either conceptType or glossType is outside of supported values") {
    val newGlossExamples1 =
      List(api.GlossExample(example = "nei men saa", language = "nb"), api.GlossExample(example = "jog har inta", "nn"))
    val newGlossExamples2 = List(api.GlossExample(example = "nei men da saa", language = "nb"))
    val newGlossData =
      api.GlossData(glossType = "ikke", originalLanguage = "nb", examples = List(newGlossExamples1, newGlossExamples2))
    val newConcept = TestData.emptyApiNewConcept.copy(conceptType = "gloss", glossData = Some(newGlossData))

    val Failure(result1) = converterService.toDomainConcept(newConcept, TestData.userWithWriteAccess)
    result1.getMessage should include("'ikke' is not a valid gloss type")

    val newConcept2 =
      newConcept.copy(conceptType = "ikke eksisterende", glossData = Some(newGlossData.copy(glossType = "noun")))
    val Failure(result2) = converterService.toDomainConcept(newConcept2, TestData.userWithWriteAccess)
    result2.getMessage should include("'ikke eksisterende' is not a valid concept type")
  }

  test("that toDomainConcept (update concept) updates glossData correctly") {
    val updatedGlossExamples1 =
      List(api.GlossExample(example = "nei men saa", language = "nb"), api.GlossExample(example = "jog har inta", "nn"))
    val updatedGlossExamples2 = List(api.GlossExample(example = "nei men da saa", language = "nb"))
    val updatedGlossData =
      api.GlossData(
        glossType = "noun",
        originalLanguage = "nb",
        examples = List(updatedGlossExamples1, updatedGlossExamples2)
      )
    val updatedConcept =
      TestData.emptyApiUpdatedConcept.copy(conceptType = Some("gloss"), glossData = Some(updatedGlossData))

    val expectedGlossExample1 = List(
      domain.GlossExample(example = "nei men saa", language = "nb"),
      domain.GlossExample(example = "jog har inta", "nn")
    )
    val expectedGlossExample2 = List(domain.GlossExample(example = "nei men da saa", language = "nb"))
    val expectedGlossData = Some(
      domain.GlossData(
        glossType = domain.GlossType.NOUN,
        originalLanguage = "nb",
        examples = List(expectedGlossExample1, expectedGlossExample2)
      )
    )
    val expectedConceptType = domain.ConceptType.GLOSS
    val existingConcept     = TestData.domainConcept.copy(conceptType = domain.ConceptType.CONCEPT, glossData = None)

    val result = converterService.toDomainConcept(existingConcept, updatedConcept, TestData.userWithWriteAccess).get
    result.conceptType should be(expectedConceptType)
    result.glossData should be(expectedGlossData)
  }

  test("that toDomainConcept (update concept) fails if gloss type is not a valid value") {
    val updatedGlossExamples1 =
      List(api.GlossExample(example = "nei men saa", language = "nb"), api.GlossExample(example = "jog har inta", "nn"))
    val updatedGlossExamples2 = List(api.GlossExample(example = "nei men da saa", language = "nb"))
    val updatedGlossData =
      api.GlossData(
        glossType = "ikke eksisterende",
        originalLanguage = "nb",
        examples = List(updatedGlossExamples1, updatedGlossExamples2)
      )
    val updatedConcept =
      TestData.emptyApiUpdatedConcept.copy(conceptType = Some("gloss"), glossData = Some(updatedGlossData))

    val existingConcept = TestData.domainConcept.copy(conceptType = domain.ConceptType.CONCEPT, glossData = None)

    val Failure(result) =
      converterService.toDomainConcept(existingConcept, updatedConcept, TestData.userWithWriteAccess)
    result.getMessage should include("'ikke eksisterende' is not a valid gloss type")
  }

  test("that toApiConcept converts gloss data correctly") {
    val domainGlossExample1 = List(
      domain.GlossExample(example = "nei men saa", language = "nb"),
      domain.GlossExample(example = "jog har inta", "nn")
    )
    val domainGlossExample2 = List(domain.GlossExample(example = "nei men da saa", language = "nb"))
    val domainGlossData = Some(
      domain.GlossData(
        glossType = domain.GlossType.NOUN,
        originalLanguage = "nb",
        examples = List(domainGlossExample1, domainGlossExample2)
      )
    )
    val existingConcept =
      TestData.domainConcept.copy(conceptType = domain.ConceptType.GLOSS, glossData = domainGlossData)

    val expectedGlossExamples1 =
      List(api.GlossExample(example = "nei men saa", language = "nb"), api.GlossExample(example = "jog har inta", "nn"))
    val expectedGlossExamples2 = List(api.GlossExample(example = "nei men da saa", language = "nb"))
    val expectedGlossData = api.GlossData(
      glossType = "noun",
      originalLanguage = "nb",
      examples = List(expectedGlossExamples1, expectedGlossExamples2)
    )
    val result = converterService.toApiConcept(existingConcept, "nb", false).get
    result.conceptType should be("gloss")
    result.glossData should be(Some(expectedGlossData))
  }

  test("that toDomainGlossData converts correctly when apiGlossData is Some") {
    val apiGlossExample = api.GlossExample(example = "some example", language = "nb")
    val apiGlossData =
      Some(api.GlossData(glossType = "verb", originalLanguage = "nb", examples = List(List(apiGlossExample))))

    val expectedGlossExample = domain.GlossExample(example = "some example", language = "nb")
    val expectedGlossData =
      domain.GlossData(glossType = GlossType.VERB, originalLanguage = "nb", examples = List(List(expectedGlossExample)))

    converterService.toDomainGlossData(apiGlossData) should be(Success(Some(expectedGlossData)))
  }

  test("that toDomainGlossData converts correctly when apiGlossData is None") {
    converterService.toDomainGlossData(None) should be(Success(None))
  }

  test("that toDomainGlossData fails if apiGlossData has malformed data") {
    val apiGlossExample = api.GlossExample(example = "some example", language = "nb")
    val apiGlossData =
      Some(api.GlossData(glossType = "nonexistent", originalLanguage = "nb", examples = List(List(apiGlossExample))))

    val Failure(result) = converterService.toDomainGlossData(apiGlossData)
    result.getMessage should include("'nonexistent' is not a valid gloss type")
  }
}
