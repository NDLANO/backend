/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.service

import no.ndla.common.model.domain.Responsible
import no.ndla.common.model.{api as commonApi, domain as common}
import no.ndla.conceptapi.model.api.{NewConcept, NotFoundException, UpdatedConcept}
import no.ndla.conceptapi.model.domain.{ConceptType, VisualElement, WordClass}
import no.ndla.conceptapi.model.{api, domain}
import no.ndla.conceptapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.network.tapir.auth.Permission.{CONCEPT_API_ADMIN, CONCEPT_API_WRITE}
import no.ndla.network.tapir.auth.TokenUser

import scala.util.{Failure, Success}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.{Delete, Missing, UpdateWith}
import org.mockito.Mockito.when

class ConverterServiceTest extends UnitSuite with TestEnvironment {

  override val converterService = new ConverterService
  val userInfo: TokenUser       = TokenUser("", Set(CONCEPT_API_WRITE, CONCEPT_API_ADMIN), None)

  test("toApiConcept converts a domain.Concept to an api.Concept with defined language") {
    converterService.toApiConcept(TestData.domainConcept, "nn", fallback = false, Some(userInfo)) should be(
      Success(TestData.sampleNnApiConcept)
    )
    converterService.toApiConcept(TestData.domainConcept, "nb", fallback = false, Some(userInfo)) should be(
      Success(TestData.sampleNbApiConcept)
    )
  }

  test("toApiConcept failure if concept not found in specified language without fallback") {
    converterService.toApiConcept(TestData.domainConcept, "hei", fallback = false, Some(userInfo)) should be(
      Failure(
        NotFoundException(
          s"The concept with id ${TestData.domainConcept.id.get} and language 'hei' was not found.",
          TestData.domainConcept.supportedLanguages.toSeq
        )
      )
    )
  }

  test("toApiConcept success if concept not found in specified language, but with fallback") {
    converterService.toApiConcept(TestData.domainConcept, "hei", fallback = true, Some(userInfo)) should be(
      Success(TestData.sampleNbApiConcept)
    )
  }

  test("toDomainConcept updates title in concept correctly") {
    val updated = NDLADate.now()
    when(clock.now()).thenReturn(updated)

    val updateWith =
      UpdatedConcept(
        "nb",
        Some("heisann"),
        None,
        Missing,
        None,
        None,
        None,
        Some(Seq(42L)),
        None,
        None,
        Missing,
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
    val updated = NDLADate.now()
    when(clock.now()).thenReturn(updated)

    val updateWith =
      UpdatedConcept(
        "nn",
        None,
        Some("Nytt innhald"),
        Missing,
        None,
        None,
        None,
        Some(Seq(42L)),
        None,
        None,
        Missing,
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
    val updated = NDLADate.now()
    when(clock.now()).thenReturn(updated)

    val updateWith =
      UpdatedConcept(
        "en",
        Some("Title"),
        Some("My content"),
        Missing,
        None,
        None,
        None,
        Some(Seq(42L)),
        None,
        None,
        Missing,
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
    val updated = NDLADate.now()
    when(clock.now()).thenReturn(updated)

    val updateWith = UpdatedConcept(
      "nn",
      None,
      Some("Nytt innhald"),
      Missing,
      Option(
        commonApi.DraftCopyright(
          None,
          None,
          Seq(commonApi.Author("Photographer", "Photographer")),
          Seq(commonApi.Author("Photographer", "Photographer")),
          Seq(commonApi.Author("Photographer", "Photographer")),
          None,
          None,
          false
        )
      ),
      None,
      None,
      Some(Seq(42L)),
      None,
      None,
      Missing,
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
          common.draft.DraftCopyright(
            None,
            None,
            Seq(common.Author("Photographer", "Photographer")),
            Seq(common.Author("Photographer", "Photographer")),
            Seq(common.Author("Photographer", "Photographer")),
            None,
            None,
            false
          )
        ),
        updated = updated
      )
    )
  }

  test("toDomainConcept deletes removes all articleIds when getting empty list as parameter") {
    val updated = NDLADate.now()
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
    val updated = NDLADate.now()
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
    val updated = NDLADate.now()
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
    val today = NDLADate.now()
    when(clock.now()).thenReturn(today)

    val afterUpdate = TestData.domainConcept_toDomainUpdateWithId.copy(
      id = Some(12),
      articleIds = Seq(15),
      created = today,
      updated = today,
      editorNotes = Seq(
        domain.EditorNote(
          "Created concept",
          "",
          domain.Status(domain.ConceptStatus.IN_PROGRESS, Set.empty),
          today
        )
      )
    )
    val updateWith = TestData.emptyApiUpdatedConcept.copy(articleIds = Some(Seq(15)))

    converterService.toDomainConcept(12, updateWith, userInfo) should be(afterUpdate)
  }

  test("toDomainConcept update concept with ID sets articleIds to empty list when articleId is not specified") {
    val today = NDLADate.now()
    when(clock.now()).thenReturn(today)

    val afterUpdate = TestData.domainConcept_toDomainUpdateWithId.copy(
      id = Some(12),
      articleIds = Seq.empty,
      created = today,
      updated = today,
      editorNotes = Seq(
        domain.EditorNote(
          "Created concept",
          "",
          domain.Status(domain.ConceptStatus.IN_PROGRESS, Set.empty),
          today
        )
      )
    )
    val updateWith = TestData.emptyApiUpdatedConcept.copy(articleIds = None)

    converterService.toDomainConcept(12, updateWith, userInfo) should be(afterUpdate)
  }

  test("toDomainConcept deletes metaImage when getting null as a parameter") {
    val updated = NDLADate.now()
    when(clock.now()).thenReturn(updated)

    val beforeUpdate = TestData.domainConcept.copy(
      metaImage = Seq(domain.ConceptMetaImage("1", "Hei", "nb"), domain.ConceptMetaImage("2", "Hej", "nn")),
      updated = updated
    )
    val afterUpdate = TestData.domainConcept.copy(
      metaImage = Seq(domain.ConceptMetaImage("2", "Hej", "nn")),
      updated = updated
    )
    val updateWith = TestData.emptyApiUpdatedConcept.copy(language = "nb", metaImage = Delete)

    converterService.toDomainConcept(beforeUpdate, updateWith, userInfo).get should be(afterUpdate)
  }

  test("toDomainConcept updates metaImage when getting new metaImage as a parameter") {
    val updated = NDLADate.now()
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
      metaImage = UpdateWith(api.NewConceptMetaImage("1", "Hola"))
    )

    converterService.toDomainConcept(beforeUpdate, updateWith, userInfo).get should be(afterUpdate)
  }

  test("toDomainConcept does nothing to metaImage when getting None as a parameter") {
    val updated = NDLADate.now()
    when(clock.now()).thenReturn(updated)

    val beforeUpdate = TestData.domainConcept.copy(
      metaImage = Seq(domain.ConceptMetaImage("1", "Hei", "nb"), domain.ConceptMetaImage("2", "Hej", "nn")),
      updated = updated
    )
    val afterUpdate = TestData.domainConcept.copy(
      metaImage = Seq(domain.ConceptMetaImage("1", "Hei", "nb"), domain.ConceptMetaImage("2", "Hej", "nn")),
      updated = updated
    )
    val updateWith = TestData.emptyApiUpdatedConcept.copy(language = "nb", metaImage = Missing)

    converterService.toDomainConcept(beforeUpdate, updateWith, userInfo).get should be(afterUpdate)
  }

  test("toDomainConcept update concept with ID updates metaImage when getting new metaImage as a parameter") {
    val today = NDLADate.now()
    when(clock.now()).thenReturn(today)

    val afterUpdate = TestData.domainConcept_toDomainUpdateWithId.copy(
      id = Some(12),
      metaImage = Seq(domain.ConceptMetaImage("1", "Hola", "nb")),
      created = today,
      updated = today,
      editorNotes = Seq(
        domain.EditorNote(
          "Created concept",
          "",
          domain.Status(domain.ConceptStatus.IN_PROGRESS, Set.empty),
          today
        )
      )
    )
    val updateWith = TestData.emptyApiUpdatedConcept.copy(
      language = "nb",
      metaImage = UpdateWith(api.NewConceptMetaImage("1", "Hola"))
    )

    converterService.toDomainConcept(12, updateWith, userInfo) should be(afterUpdate)
  }

  test("toDomainConcept update concept with ID sets metaImage to Seq.empty when metaImage is not specified") {
    val today = NDLADate.now()
    when(clock.now()).thenReturn(today)

    val afterUpdate = TestData.domainConcept_toDomainUpdateWithId.copy(
      id = Some(12),
      metaImage = Seq.empty,
      created = today,
      updated = today,
      editorNotes = Seq(
        domain.EditorNote(
          "Created concept",
          "",
          domain.Status(domain.ConceptStatus.IN_PROGRESS, Set.empty),
          today
        )
      )
    )
    val updateWith = TestData.emptyApiUpdatedConcept.copy(language = "nb", metaImage = Delete)

    converterService.toDomainConcept(12, updateWith, userInfo) should be(afterUpdate)
  }

  test("toDomainConcept updates updatedBy with new entry from userToken") {
    val updated = NDLADate.now()
    when(clock.now()).thenReturn(updated)

    val beforeUpdate = TestData.domainConcept.copy(
      updatedBy = Seq.empty,
      updated = updated
    )
    val afterUpdate = TestData.domainConcept.copy(
      updatedBy = Seq("test"),
      updated = updated
    )
    val updateWith = TokenUser.SystemUser.copy(id = "test")
    val dummy      = TestData.emptyApiUpdatedConcept

    converterService.toDomainConcept(beforeUpdate, dummy, updateWith).get should be(afterUpdate)
  }

  test("toDomainConcept does not produce duplicates in updatedBy") {
    val updated = NDLADate.now()
    when(clock.now()).thenReturn(updated)

    val beforeUpdate = TestData.domainConcept.copy(
      updatedBy = Seq("test1", "test2"),
      updated = updated
    )
    val afterUpdate = TestData.domainConcept.copy(
      updatedBy = Seq("test1", "test2"),
      updated = updated
    )
    val updateWith = TokenUser.SystemUser.copy(id = "test1")
    val dummy      = TestData.emptyApiUpdatedConcept

    converterService.toDomainConcept(beforeUpdate, dummy, updateWith).get should be(afterUpdate)
  }

  test("toDomainConcept update concept with ID updates updatedBy with new entry from userToken") {
    val today = NDLADate.now()
    when(clock.now()).thenReturn(today)

    val afterUpdate = TestData.domainConcept_toDomainUpdateWithId.copy(
      id = Some(12),
      updatedBy = Seq("test"),
      created = today,
      updated = today,
      editorNotes = Seq(
        domain.EditorNote(
          "Created concept",
          "test",
          domain.Status(domain.ConceptStatus.IN_PROGRESS, Set.empty),
          today
        )
      )
    )
    val updateWith = TokenUser.SystemUser.copy(id = "test")
    val dummy      = TestData.emptyApiUpdatedConcept

    converterService.toDomainConcept(12, dummy, updateWith) should be(afterUpdate)
  }

  test("toDomainConcept updates updatedBy with new entry from userToken on create") {
    val today = NDLADate.now()
    when(clock.now()).thenReturn(today)

    val afterUpdate = TestData.domainConcept_toDomainUpdateWithId.copy(
      title = Seq(common.Title("", "")),
      updatedBy = Seq("test"),
      created = today,
      updated = today,
      editorNotes = Seq(
        domain.EditorNote(
          "Created concept",
          "test",
          domain.Status(domain.ConceptStatus.IN_PROGRESS, Set.empty),
          today
        )
      )
    )
    val updateWith = TokenUser.SystemUser.copy(id = "test")
    val dummy      = TestData.emptyApiNewConcept

    converterService.toDomainConcept(dummy, updateWith) should be(Success(afterUpdate))
  }

  test("toDomainConcept updates timestamp on responsible when id is changed") {
    val updated = NDLADate.now()
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

    val updateWith = TestData.emptyApiUpdatedConcept.copy(language = "nb", responsibleId = UpdateWith("newId"))
    converterService.toDomainConcept(withOldResponsible, updateWith, userInfo).get should be(withNewResponsible)

    val updateWith2 = TestData.emptyApiUpdatedConcept.copy(language = "nb", responsibleId = UpdateWith("oldId"))
    converterService.toDomainConcept(withOldResponsible, updateWith2, userInfo).get should be(withOldResponsible)

    val updateWith3 = TestData.emptyApiUpdatedConcept.copy(language = "nb", responsibleId = Delete)
    converterService.toDomainConcept(withOldResponsible, updateWith3, userInfo).get should be(withoutResponsible)
  }

  test("that toDomainConcept (new concept) creates glossData correctly") {
    val newGlossExamples1 =
      List(
        api.GlossExample(example = "nei men saa", language = "nb", transcriptions = Map("a" -> "b")),
        api.GlossExample(example = "jog har inta", "nn", transcriptions = Map("b" -> "c"))
      )
    val newGlossExamples2 =
      List(api.GlossExample(example = "nei men da saa", language = "nb", transcriptions = Map("a" -> "b")))
    val newGlossData =
      api.GlossData(
        gloss = "juan",
        wordClass = "noun",
        originalLanguage = "nb",
        examples = List(newGlossExamples1, newGlossExamples2),
        transcriptions = Map("zh" -> "a", "pinyin" -> "b")
      )
    val newConcept = TestData.emptyApiNewConcept.copy(conceptType = "gloss", glossData = Some(newGlossData))

    val expectedGlossExample1 = List(
      domain.GlossExample(example = "nei men saa", language = "nb", transcriptions = Map("a" -> "b")),
      domain.GlossExample(example = "jog har inta", "nn", transcriptions = Map("b" -> "c"))
    )
    val expectedGlossExample2 =
      List(domain.GlossExample(example = "nei men da saa", language = "nb", transcriptions = Map("a" -> "b")))
    val expectedGlossData = Some(
      domain.GlossData(
        gloss = "juan",
        wordClass = domain.WordClass.NOUN,
        originalLanguage = "nb",
        examples = List(expectedGlossExample1, expectedGlossExample2),
        transcriptions = Map("zh" -> "a", "pinyin" -> "b")
      )
    )
    val expectedConceptType = domain.ConceptType.GLOSS

    val result = converterService.toDomainConcept(newConcept, TestData.userWithWriteAccess).get
    result.conceptType should be(expectedConceptType)
    result.glossData should be(expectedGlossData)
  }

  test("that toDomainConcept (new concept) fails if either conceptType or wordClass is outside of supported values") {
    val newGlossExamples1 =
      List(
        api.GlossExample(example = "nei men saa", language = "nb", transcriptions = Map("a" -> "b")),
        api.GlossExample(example = "jog har inta", "nn", transcriptions = Map("a" -> "b"))
      )
    val newGlossExamples2 =
      List(api.GlossExample(example = "nei men da saa", language = "nb", transcriptions = Map("a" -> "b")))
    val newGlossData =
      api.GlossData(
        gloss = "huehue",
        wordClass = "ikke",
        originalLanguage = "nb",
        examples = List(newGlossExamples1, newGlossExamples2),
        transcriptions = Map("zh" -> "a", "pinyin" -> "b")
      )
    val newConcept = TestData.emptyApiNewConcept.copy(conceptType = "gloss", glossData = Some(newGlossData))

    val Failure(result1) = converterService.toDomainConcept(newConcept, TestData.userWithWriteAccess)
    result1.getMessage should include("'ikke' is not a valid gloss type")

//    val newConcept2 =
//      newConcept.copy(conceptType = "ikke eksisterende", glossData = Some(newGlossData.copy(wordClass = "noun")))
//    val Failure(result2) = converterService.toDomainConcept(newConcept2, TestData.userWithWriteAccess)
//    result2.getMessage should include("'ikke eksisterende' is not a valid concept type")
  }

  test("that toDomainConcept (update concept) updates glossData correctly") {
    val updatedGlossExamples1 =
      List(
        api.GlossExample(example = "nei men saa", language = "nb", transcriptions = Map("a" -> "b")),
        api.GlossExample(example = "jog har inta", "nn", transcriptions = Map("a" -> "b"))
      )
    val updatedGlossExamples2 =
      List(api.GlossExample(example = "nei men da saa", language = "nb", transcriptions = Map("a" -> "b")))
    val updatedGlossData =
      api.GlossData(
        gloss = "huehue",
        wordClass = "noun",
        originalLanguage = "nb",
        examples = List(updatedGlossExamples1, updatedGlossExamples2),
        transcriptions = Map("zh" -> "a", "pinyin" -> "b")
      )
    val updatedConcept =
      TestData.emptyApiUpdatedConcept.copy(conceptType = Some("gloss"), glossData = Some(updatedGlossData))

    val expectedGlossExample1 = List(
      domain.GlossExample(
        example = "nei men saa",
        language = "nb",
        transcriptions = Map("a" -> "b")
      ),
      domain.GlossExample(example = "jog har inta", "nn", transcriptions = Map("a" -> "b"))
    )
    val expectedGlossExample2 =
      List(domain.GlossExample(example = "nei men da saa", language = "nb", transcriptions = Map("a" -> "b")))
    val expectedGlossData = Some(
      domain.GlossData(
        gloss = "huehue",
        wordClass = domain.WordClass.NOUN,
        originalLanguage = "nb",
        examples = List(expectedGlossExample1, expectedGlossExample2),
        transcriptions = Map("zh" -> "a", "pinyin" -> "b")
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
      List(
        api.GlossExample(example = "nei men saa", language = "nb", transcriptions = Map("a" -> "b")),
        api.GlossExample(example = "jog har inta", "nn", transcriptions = Map("a" -> "b"))
      )
    val updatedGlossExamples2 =
      List(api.GlossExample(example = "nei men da saa", language = "nb", transcriptions = Map("a" -> "b")))
    val updatedGlossData =
      api.GlossData(
        gloss = "yesp",
        wordClass = "ikke eksisterende",
        originalLanguage = "nb",
        examples = List(updatedGlossExamples1, updatedGlossExamples2),
        transcriptions = Map("zh" -> "a", "pinyin" -> "b")
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
      domain.GlossExample(example = "nei men saa", language = "nb", transcriptions = Map("a" -> "b")),
      domain.GlossExample(example = "jog har inta", "nn", transcriptions = Map("b" -> "c"))
    )
    val domainGlossExample2 =
      List(domain.GlossExample(example = "nei men da saa", language = "nb", transcriptions = Map("a" -> "b")))
    val domainGlossData = Some(
      domain.GlossData(
        gloss = "gestalt",
        wordClass = domain.WordClass.NOUN,
        originalLanguage = "nb",
        examples = List(domainGlossExample1, domainGlossExample2),
        transcriptions = Map("zh" -> "a", "pinyin" -> "b")
      )
    )
    val existingConcept =
      TestData.domainConcept.copy(
        conceptType = domain.ConceptType.GLOSS,
        glossData = domainGlossData,
        title = Seq(common.Title("title", "nb"))
      )

    val expectedGlossExamples1 =
      List(
        api.GlossExample(example = "nei men saa", language = "nb", transcriptions = Map("a" -> "b")),
        api.GlossExample(example = "jog har inta", "nn", transcriptions = Map("b" -> "c"))
      )
    val expectedGlossExamples2 =
      List(api.GlossExample(example = "nei men da saa", language = "nb", transcriptions = Map("a" -> "b")))
    val expectedGlossData = api.GlossData(
      gloss = "gestalt",
      wordClass = "noun",
      originalLanguage = "nb",
      examples = List(expectedGlossExamples1, expectedGlossExamples2),
      transcriptions = Map("zh" -> "a", "pinyin" -> "b")
    )
    val result = converterService.toApiConcept(existingConcept, "nb", false, Some(userInfo)).get
    result.conceptType should be("gloss")
    result.glossData should be(Some(expectedGlossData))
  }

  test("that toDomainGlossData converts correctly when apiGlossData is Some") {
    val apiGlossExample = api.GlossExample(example = "some example", language = "nb", transcriptions = Map("a" -> "b"))
    val apiGlossData =
      Some(
        api.GlossData(
          gloss = "yoink",
          wordClass = "verb",
          originalLanguage = "nb",
          examples = List(List(apiGlossExample)),
          transcriptions = Map("zh" -> "a", "pinyin" -> "b")
        )
      )

    val expectedGlossExample =
      domain.GlossExample(example = "some example", language = "nb", transcriptions = Map("a" -> "b"))
    val expectedGlossData =
      domain.GlossData(
        gloss = "yoink",
        wordClass = WordClass.VERB,
        originalLanguage = "nb",
        examples = List(List(expectedGlossExample)),
        transcriptions = Map("zh" -> "a", "pinyin" -> "b")
      )

    converterService.toDomainGlossData(apiGlossData) should be(Success(Some(expectedGlossData)))
  }

  test("that toDomainGlossData converts correctly when apiGlossData is None") {
    converterService.toDomainGlossData(None) should be(Success(None))
  }

  test("that toDomainGlossData fails if apiGlossData has malformed data") {
    val apiGlossExample = api.GlossExample(example = "some example", language = "nb", transcriptions = Map("a" -> "b"))
    val apiGlossData =
      Some(
        api.GlossData(
          gloss = "neie",
          wordClass = "nonexistent",
          originalLanguage = "nb",
          examples = List(List(apiGlossExample)),
          transcriptions = Map("zh" -> "a", "pinyin" -> "b")
        )
      )

    val Failure(result) = converterService.toDomainGlossData(apiGlossData)
    result.getMessage should include("'nonexistent' is not a valid gloss type")
  }

  test("unknown embed attributes should be stripped from new concepts") {
    val newConcept = NewConcept(
      language = "nb",
      title = "tittel",
      content = Some("Nokko innhald"),
      copyright = None,
      metaImage = None,
      tags = None,
      subjectIds = None,
      articleIds = None,
      visualElement = Some(
        "<ndlaembed data-resource=\"audio\" data-resource_id=\"2755\" data-type=\"standard\" data-url=\"https://api.test.ndla.no/audio-api/v1/audio/2755\"></ndlaembed>"
      ),
      responsibleId = None,
      conceptType = ConceptType.CONCEPT.toString,
      glossData = None
    )

    val result = converterService.toDomainConcept(newConcept, TokenUser.SystemUser).get

    result.visualElement should be(
      Seq(
        VisualElement(
          "<ndlaembed data-resource=\"audio\" data-resource_id=\"2755\" data-type=\"standard\"></ndlaembed>",
          "nb"
        )
      )
    )
  }

  test("unknown embed attributes should be stripped from updated concepts (if null document)") {
    val updatedConcept = UpdatedConcept(
      language = "nb",
      title = Some("tittel"),
      content = Some("Nokko innhald"),
      copyright = None,
      metaImage = Missing,
      tags = None,
      subjectIds = None,
      articleIds = None,
      visualElement = Some(
        "<ndlaembed data-resource=\"audio\" data-resource_id=\"2755\" data-type=\"standard\" data-url=\"https://api.test.ndla.no/audio-api/v1/audio/2755\"></ndlaembed>"
      ),
      responsibleId = Missing,
      conceptType = None,
      glossData = None,
      status = None
    )

    val result = converterService.toDomainConcept(1, updatedConcept, TokenUser.SystemUser)
    result.visualElement should be(
      Seq(
        VisualElement(
          "<ndlaembed data-resource=\"audio\" data-resource_id=\"2755\" data-type=\"standard\"></ndlaembed>",
          "nb"
        )
      )
    )
  }

  test("unknown embed attributes should be stripped from updated concepts") {
    val updatedConcept = UpdatedConcept(
      language = "nb",
      title = Some("tittel"),
      content = Some("Nokko innhald"),
      copyright = None,
      metaImage = Missing,
      tags = None,
      subjectIds = None,
      articleIds = None,
      visualElement = Some(
        "<ndlaembed data-resource=\"audio\" data-resource_id=\"2755\" data-type=\"standard\" data-url=\"https://api.test.ndla.no/audio-api/v1/audio/2755\"></ndlaembed>"
      ),
      responsibleId = Missing,
      conceptType = None,
      glossData = None,
      status = None
    )

    val result = converterService.toDomainConcept(TestData.domainConcept, updatedConcept, TokenUser.SystemUser).get
    result.visualElement should be(
      Seq(
        VisualElement(
          "<ndlaembed data-resource=\"audio\" data-resource_id=\"2755\" data-type=\"standard\"></ndlaembed>",
          "nb"
        )
      )
    )
  }
}
