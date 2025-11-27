/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.service

import no.ndla.common.model.api.{Delete, Missing, UpdateWith}
import no.ndla.common.model.domain.concept.*
import no.ndla.common.model.domain.{ContributorType, Responsible, concept}
import no.ndla.common.model.{NDLADate, api as commonApi, domain as common}
import no.ndla.conceptapi.model.api
import no.ndla.conceptapi.model.api.{NewConceptDTO, NotFoundException, UpdatedConceptDTO}
import no.ndla.conceptapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.network.tapir.auth.Permission.{CONCEPT_API_ADMIN, CONCEPT_API_WRITE}
import no.ndla.network.tapir.auth.TokenUser
import org.mockito.Mockito.when

import scala.util.{Failure, Success}

class ConverterServiceTest extends UnitSuite with TestEnvironment {

  override lazy val converterService = new ConverterService
  val userInfo: TokenUser            = TokenUser("", Set(CONCEPT_API_WRITE, CONCEPT_API_ADMIN), None)

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
          TestData.domainConcept.supportedLanguages.toSeq,
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

    val updateWith = UpdatedConceptDTO("nb", Some("heisann"), None, None, None, None, None, Missing, None, None)
    converterService.toDomainConcept(TestData.domainConcept, updateWith, userInfo).get should be(
      TestData
        .domainConcept
        .copy(title = Seq(common.Title("Tittelur", "nn"), common.Title("heisann", "nb")), updated = updated)
    )
  }

  test("toDomainConcept updates content in concept correctly") {
    val updated = NDLADate.now()
    when(clock.now()).thenReturn(updated)

    val updateWith = UpdatedConceptDTO("nn", None, Some("Nytt innhald"), None, None, None, None, Missing, None, None)
    converterService.toDomainConcept(TestData.domainConcept, updateWith, userInfo).get should be(
      TestData
        .domainConcept
        .copy(
          content = Seq(ConceptContent("Innhold", "nb"), concept.ConceptContent("Nytt innhald", "nn")),
          updated = updated,
        )
    )
  }

  test("toDomainConcept adds new language in concept correctly") {
    val updated = NDLADate.now()
    when(clock.now()).thenReturn(updated)

    val updateWith =
      UpdatedConceptDTO("en", Some("Title"), Some("My content"), None, None, None, None, Missing, None, None)
    converterService.toDomainConcept(TestData.domainConcept, updateWith, userInfo).get should be(
      TestData
        .domainConcept
        .copy(
          title = Seq(common.Title("Tittel", "nb"), common.Title("Tittelur", "nn"), common.Title("Title", "en")),
          content = Seq(
            concept.ConceptContent("Innhold", "nb"),
            concept.ConceptContent("Innhald", "nn"),
            concept.ConceptContent("My content", "en"),
          ),
          updated = updated,
        )
    )
  }

  test("toDomainConcept updates copyright correctly") {
    val updated = NDLADate.now()
    when(clock.now()).thenReturn(updated)

    val updateWith = UpdatedConceptDTO(
      "nn",
      None,
      Some("Nytt innhald"),
      Option(
        commonApi.DraftCopyrightDTO(
          None,
          None,
          Seq(commonApi.AuthorDTO(ContributorType.Photographer, "Photographer")),
          Seq(commonApi.AuthorDTO(ContributorType.Photographer, "Photographer")),
          Seq(commonApi.AuthorDTO(ContributorType.Photographer, "Photographer")),
          None,
          None,
          false,
        )
      ),
      None,
      None,
      None,
      Missing,
      None,
      None,
    )
    converterService.toDomainConcept(TestData.domainConcept, updateWith, userInfo).get should be(
      TestData
        .domainConcept
        .copy(
          content = Seq(concept.ConceptContent("Innhold", "nb"), concept.ConceptContent("Nytt innhald", "nn")),
          copyright = Option(
            common
              .draft
              .DraftCopyright(
                None,
                None,
                Seq(common.Author(ContributorType.Photographer, "Photographer")),
                Seq(common.Author(ContributorType.Photographer, "Photographer")),
                Seq(common.Author(ContributorType.Photographer, "Photographer")),
                None,
                None,
                false,
              )
          ),
          updated = updated,
        )
    )
  }

  test("toDomainConcept deletes removes all articleIds when getting empty list as parameter") {
    val updated = NDLADate.now()
    when(clock.now()).thenReturn(updated)

    val beforeUpdate = TestData.domainConcept.copy(updated = updated)
    val afterUpdate  = TestData.domainConcept.copy(updated = updated)
    val updateWith   = TestData.emptyApiUpdatedConcept.copy()

    converterService.toDomainConcept(beforeUpdate, updateWith, userInfo).get should be(afterUpdate)
  }

  test("toDomainConcept updates articleIds when getting list as a parameter") {
    val updated = NDLADate.now()
    when(clock.now()).thenReturn(updated)

    val beforeUpdate = TestData.domainConcept.copy(updated = updated)
    val afterUpdate  = TestData.domainConcept.copy(updated = updated)
    val updateWith   = TestData.emptyApiUpdatedConcept.copy()

    converterService.toDomainConcept(beforeUpdate, updateWith, userInfo).get should be(afterUpdate)
  }

  test("toDomainConcept does nothing to articleId when getting None as a parameter") {
    val updated = NDLADate.now()
    when(clock.now()).thenReturn(updated)

    val beforeUpdate = TestData.domainConcept.copy(updated = updated)
    val afterUpdate  = TestData.domainConcept.copy(updated = updated)
    val updateWith   = TestData.emptyApiUpdatedConcept.copy()

    converterService.toDomainConcept(beforeUpdate, updateWith, userInfo).get should be(afterUpdate)
  }

  test("toDomainConcept update concept with ID updates articleId when getting new articleId as a parameter") {
    val today = NDLADate.now()
    when(clock.now()).thenReturn(today)

    val afterUpdate = TestData
      .domainConcept_toDomainUpdateWithId
      .copy(
        id = Some(12),
        created = today,
        updated = today,
        editorNotes = Seq(ConceptEditorNote("Created concept", "", Status(ConceptStatus.IN_PROGRESS, Set.empty), today)),
      )
    val updateWith = TestData.emptyApiUpdatedConcept.copy()

    converterService.toDomainConcept(12, updateWith, userInfo) should be(afterUpdate)
  }

  test("toDomainConcept update concept with ID sets articleIds to empty list when articleId is not specified") {
    val today = NDLADate.now()
    when(clock.now()).thenReturn(today)

    val afterUpdate = TestData
      .domainConcept_toDomainUpdateWithId
      .copy(
        id = Some(12),
        created = today,
        updated = today,
        editorNotes = Seq(
          ConceptEditorNote("Created concept", "", concept.Status(concept.ConceptStatus.IN_PROGRESS, Set.empty), today)
        ),
      )
    val updateWith = TestData.emptyApiUpdatedConcept.copy()

    converterService.toDomainConcept(12, updateWith, userInfo) should be(afterUpdate)
  }

  test("toDomainConcept updates updatedBy with new entry from userToken") {
    val updated = NDLADate.now()
    when(clock.now()).thenReturn(updated)

    val beforeUpdate = TestData.domainConcept.copy(updated = updated, updatedBy = Seq.empty)
    val afterUpdate  = TestData.domainConcept.copy(updated = updated, updatedBy = Seq("test"))
    val updateWith   = TokenUser.SystemUser.copy(id = "test")
    val dummy        = TestData.emptyApiUpdatedConcept

    converterService.toDomainConcept(beforeUpdate, dummy, updateWith).get should be(afterUpdate)
  }

  test("toDomainConcept does not produce duplicates in updatedBy") {
    val updated = NDLADate.now()
    when(clock.now()).thenReturn(updated)

    val beforeUpdate = TestData.domainConcept.copy(updated = updated, updatedBy = Seq("test1", "test2"))
    val afterUpdate  = TestData.domainConcept.copy(updated = updated, updatedBy = Seq("test1", "test2"))
    val updateWith   = TokenUser.SystemUser.copy(id = "test1")
    val dummy        = TestData.emptyApiUpdatedConcept

    converterService.toDomainConcept(beforeUpdate, dummy, updateWith).get should be(afterUpdate)
  }

  test("toDomainConcept update concept with ID updates updatedBy with new entry from userToken") {
    val today = NDLADate.now()
    when(clock.now()).thenReturn(today)

    val afterUpdate = TestData
      .domainConcept_toDomainUpdateWithId
      .copy(
        id = Some(12),
        created = today,
        updated = today,
        updatedBy = Seq("test"),
        editorNotes = Seq(
          ConceptEditorNote(
            "Created concept",
            "test",
            concept.Status(concept.ConceptStatus.IN_PROGRESS, Set.empty),
            today,
          )
        ),
      )
    val updateWith = TokenUser.SystemUser.copy(id = "test")
    val dummy      = TestData.emptyApiUpdatedConcept

    converterService.toDomainConcept(12, dummy, updateWith) should be(afterUpdate)
  }

  test("toDomainConcept updates updatedBy with new entry from userToken on create") {
    val today = NDLADate.now()
    when(clock.now()).thenReturn(today)

    val afterUpdate = TestData
      .domainConcept_toDomainUpdateWithId
      .copy(
        title = Seq(common.Title("", "")),
        created = today,
        updated = today,
        updatedBy = Seq("test"),
        editorNotes = Seq(
          ConceptEditorNote(
            "Created concept",
            "test",
            concept.Status(concept.ConceptStatus.IN_PROGRESS, Set.empty),
            today,
          )
        ),
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

    val withOldResponsible = TestData.domainConcept.copy(updated = updated, responsible = Some(responsible))
    val withNewResponsible = TestData.domainConcept.copy(updated = updated, responsible = Some(newResponsible))
    val withoutResponsible = TestData.domainConcept.copy(updated = updated)

    val updateWith = TestData.emptyApiUpdatedConcept.copy(language = "nb", responsibleId = UpdateWith("newId"))
    converterService.toDomainConcept(withOldResponsible, updateWith, userInfo).get should be(withNewResponsible)

    val updateWith2 = TestData.emptyApiUpdatedConcept.copy(language = "nb", responsibleId = UpdateWith("oldId"))
    converterService.toDomainConcept(withOldResponsible, updateWith2, userInfo).get should be(withOldResponsible)

    val updateWith3 = TestData.emptyApiUpdatedConcept.copy(language = "nb", responsibleId = Delete)
    converterService.toDomainConcept(withOldResponsible, updateWith3, userInfo).get should be(withoutResponsible)
  }

  test("that toDomainConcept (new concept) creates glossData correctly") {
    val newGlossExamples1 = List(
      api.GlossExampleDTO(example = "nei men saa", language = "nb", transcriptions = Map("a" -> "b")),
      api.GlossExampleDTO(example = "jog har inta", "nn", transcriptions = Map("b" -> "c")),
    )
    val newGlossExamples2 =
      List(api.GlossExampleDTO(example = "nei men da saa", language = "nb", transcriptions = Map("a" -> "b")))
    val newGlossData = api.GlossDataDTO(
      gloss = "juan",
      wordClass = List("noun"),
      originalLanguage = "nb",
      examples = List(newGlossExamples1, newGlossExamples2),
      transcriptions = Map("zh" -> "a", "pinyin" -> "b"),
    )
    val newConcept = TestData.emptyApiNewConcept.copy(conceptType = "gloss", glossData = Some(newGlossData))

    val expectedGlossExample1 = List(
      GlossExample(example = "nei men saa", language = "nb", transcriptions = Map("a" -> "b")),
      concept.GlossExample(example = "jog har inta", "nn", transcriptions = Map("b" -> "c")),
    )
    val expectedGlossExample2 =
      List(concept.GlossExample(example = "nei men da saa", language = "nb", transcriptions = Map("a" -> "b")))
    val expectedGlossData = Some(
      GlossData(
        gloss = "juan",
        wordClass = List(WordClass.NOUN),
        originalLanguage = "nb",
        examples = List(expectedGlossExample1, expectedGlossExample2),
        transcriptions = Map("zh" -> "a", "pinyin" -> "b"),
      )
    )
    val expectedConceptType = ConceptType.GLOSS

    val result = converterService.toDomainConcept(newConcept, TestData.userWithWriteAccess).get
    result.conceptType should be(expectedConceptType)
    result.glossData should be(expectedGlossData)
  }

  test("that toDomainConcept (new concept) fails if either conceptType or wordClass is outside of supported values") {
    val newGlossExamples1 = List(
      api.GlossExampleDTO(example = "nei men saa", language = "nb", transcriptions = Map("a" -> "b")),
      api.GlossExampleDTO(example = "jog har inta", "nn", transcriptions = Map("a" -> "b")),
    )
    val newGlossExamples2 =
      List(api.GlossExampleDTO(example = "nei men da saa", language = "nb", transcriptions = Map("a" -> "b")))
    val newGlossData = api.GlossDataDTO(
      gloss = "huehue",
      wordClass = List("ikke"),
      originalLanguage = "nb",
      examples = List(newGlossExamples1, newGlossExamples2),
      transcriptions = Map("zh" -> "a", "pinyin" -> "b"),
    )
    val newConcept = TestData.emptyApiNewConcept.copy(conceptType = "gloss", glossData = Some(newGlossData))

    val Failure(result1) = converterService.toDomainConcept(newConcept, TestData.userWithWriteAccess): @unchecked
    result1.getMessage should include("'ikke' is not a valid gloss type")

//    val newConcept2 =
//      newConcept.copy(conceptType = "ikke eksisterende", glossData = Some(newGlossData.copy(wordClass = List("noun"))))
//    val Failure(result2) = converterService.toDomainConcept(newConcept2, TestData.userWithWriteAccess)
//    result2.getMessage should include("'ikke eksisterende' is not a valid concept type")
  }

  test("that toDomainConcept (update concept) updates glossData correctly") {
    val updatedGlossExamples1 = List(
      api.GlossExampleDTO(example = "nei men saa", language = "nb", transcriptions = Map("a" -> "b")),
      api.GlossExampleDTO(example = "jog har inta", "nn", transcriptions = Map("a" -> "b")),
    )
    val updatedGlossExamples2 =
      List(api.GlossExampleDTO(example = "nei men da saa", language = "nb", transcriptions = Map("a" -> "b")))
    val updatedGlossData = api.GlossDataDTO(
      gloss = "huehue",
      wordClass = List("noun"),
      originalLanguage = "nb",
      examples = List(updatedGlossExamples1, updatedGlossExamples2),
      transcriptions = Map("zh" -> "a", "pinyin" -> "b"),
    )
    val updatedConcept = TestData
      .emptyApiUpdatedConcept
      .copy(conceptType = Some("gloss"), glossData = Some(updatedGlossData))

    val expectedGlossExample1 = List(
      concept.GlossExample(example = "nei men saa", language = "nb", transcriptions = Map("a" -> "b")),
      concept.GlossExample(example = "jog har inta", "nn", transcriptions = Map("a" -> "b")),
    )
    val expectedGlossExample2 =
      List(concept.GlossExample(example = "nei men da saa", language = "nb", transcriptions = Map("a" -> "b")))
    val expectedGlossData = Some(
      concept.GlossData(
        gloss = "huehue",
        wordClass = List(concept.WordClass.NOUN),
        originalLanguage = "nb",
        examples = List(expectedGlossExample1, expectedGlossExample2),
        transcriptions = Map("zh" -> "a", "pinyin" -> "b"),
      )
    )
    val expectedConceptType = concept.ConceptType.GLOSS
    val existingConcept     = TestData.domainConcept.copy(conceptType = concept.ConceptType.CONCEPT, glossData = None)

    val result = converterService.toDomainConcept(existingConcept, updatedConcept, TestData.userWithWriteAccess).get
    result.conceptType should be(expectedConceptType)
    result.glossData should be(expectedGlossData)
  }

  test("that toDomainConcept (update concept) fails if gloss type is not a valid value") {
    val updatedGlossExamples1 = List(
      api.GlossExampleDTO(example = "nei men saa", language = "nb", transcriptions = Map("a" -> "b")),
      api.GlossExampleDTO(example = "jog har inta", "nn", transcriptions = Map("a" -> "b")),
    )
    val updatedGlossExamples2 =
      List(api.GlossExampleDTO(example = "nei men da saa", language = "nb", transcriptions = Map("a" -> "b")))
    val updatedGlossData = api.GlossDataDTO(
      gloss = "yesp",
      wordClass = List("ikke eksisterende"),
      originalLanguage = "nb",
      examples = List(updatedGlossExamples1, updatedGlossExamples2),
      transcriptions = Map("zh" -> "a", "pinyin" -> "b"),
    )
    val updatedConcept = TestData
      .emptyApiUpdatedConcept
      .copy(conceptType = Some("gloss"), glossData = Some(updatedGlossData))

    val existingConcept = TestData.domainConcept.copy(conceptType = concept.ConceptType.CONCEPT, glossData = None)

    val Failure(result) =
      converterService.toDomainConcept(existingConcept, updatedConcept, TestData.userWithWriteAccess): @unchecked
    result.getMessage should include("'ikke eksisterende' is not a valid gloss type")
  }

  test("that toApiConcept converts gloss data correctly") {
    val domainGlossExample1 = List(
      concept.GlossExample(example = "nei men saa", language = "nb", transcriptions = Map("a" -> "b")),
      concept.GlossExample(example = "jog har inta", "nn", transcriptions = Map("b" -> "c")),
    )
    val domainGlossExample2 =
      List(concept.GlossExample(example = "nei men da saa", language = "nb", transcriptions = Map("a" -> "b")))
    val domainGlossData = Some(
      concept.GlossData(
        gloss = "gestalt",
        wordClass = List(concept.WordClass.NOUN),
        originalLanguage = "nb",
        examples = List(domainGlossExample1, domainGlossExample2),
        transcriptions = Map("zh" -> "a", "pinyin" -> "b"),
      )
    )
    val existingConcept = TestData
      .domainConcept
      .copy(
        title = Seq(common.Title("title", "nb")),
        conceptType = concept.ConceptType.GLOSS,
        glossData = domainGlossData,
      )

    val expectedGlossExamples1 = List(
      api.GlossExampleDTO(example = "nei men saa", language = "nb", transcriptions = Map("a" -> "b")),
      api.GlossExampleDTO(example = "jog har inta", "nn", transcriptions = Map("b" -> "c")),
    )
    val expectedGlossExamples2 =
      List(api.GlossExampleDTO(example = "nei men da saa", language = "nb", transcriptions = Map("a" -> "b")))
    val expectedGlossData = api.GlossDataDTO(
      gloss = "gestalt",
      wordClass = List("noun"),
      originalLanguage = "nb",
      examples = List(expectedGlossExamples1, expectedGlossExamples2),
      transcriptions = Map("zh" -> "a", "pinyin" -> "b"),
    )
    val result = converterService.toApiConcept(existingConcept, "nb", false, Some(userInfo)).get
    result.conceptType should be("gloss")
    result.glossData should be(Some(expectedGlossData))
  }

  test("that toDomainGlossData converts correctly when apiGlossData is Some") {
    val apiGlossExample =
      api.GlossExampleDTO(example = "some example", language = "nb", transcriptions = Map("a" -> "b"))
    val apiGlossData = Some(
      api.GlossDataDTO(
        gloss = "yoink",
        wordClass = List("verb"),
        originalLanguage = "nb",
        examples = List(List(apiGlossExample)),
        transcriptions = Map("zh" -> "a", "pinyin" -> "b"),
      )
    )

    val expectedGlossExample =
      concept.GlossExample(example = "some example", language = "nb", transcriptions = Map("a" -> "b"))
    val expectedGlossData = concept.GlossData(
      gloss = "yoink",
      wordClass = List(WordClass.VERB),
      originalLanguage = "nb",
      examples = List(List(expectedGlossExample)),
      transcriptions = Map("zh" -> "a", "pinyin" -> "b"),
    )

    converterService.toDomainGlossData(apiGlossData) should be(Success(Some(expectedGlossData)))
  }

  test("that toDomainGlossData converts correctly when apiGlossData is None") {
    converterService.toDomainGlossData(None) should be(Success(None))
  }

  test("that toDomainGlossData fails if apiGlossData has malformed data") {
    val apiGlossExample =
      api.GlossExampleDTO(example = "some example", language = "nb", transcriptions = Map("a" -> "b"))
    val apiGlossData = Some(
      api.GlossDataDTO(
        gloss = "neie",
        wordClass = List("nonexistent"),
        originalLanguage = "nb",
        examples = List(List(apiGlossExample)),
        transcriptions = Map("zh" -> "a", "pinyin" -> "b"),
      )
    )

    val Failure(result) = converterService.toDomainGlossData(apiGlossData): @unchecked
    result.getMessage should include("'nonexistent' is not a valid gloss type")
  }

  test("unknown embed attributes should be stripped from new concepts") {
    val newConcept = NewConceptDTO(
      language = "nb",
      title = "tittel",
      content = Some("Nokko innhald"),
      copyright = None,
      tags = None,
      visualElement = Some(
        "<ndlaembed data-resource=\"audio\" data-resource_id=\"2755\" data-type=\"standard\" data-url=\"https://api.test.ndla.no/audio-api/v1/audio/2755\"></ndlaembed>"
      ),
      responsibleId = None,
      conceptType = ConceptType.CONCEPT.toString,
      glossData = None,
    )

    val result = converterService.toDomainConcept(newConcept, TokenUser.SystemUser).get

    result.visualElement should be(
      Seq(
        VisualElement(
          "<ndlaembed data-resource=\"audio\" data-resource_id=\"2755\" data-type=\"standard\"></ndlaembed>",
          "nb",
        )
      )
    )
  }

  test("unknown embed attributes should be stripped from updated concepts (if null document)") {
    val updatedConcept = UpdatedConceptDTO(
      language = "nb",
      title = Some("tittel"),
      content = Some("Nokko innhald"),
      copyright = None,
      tags = None,
      status = None,
      visualElement = Some(
        "<ndlaembed data-resource=\"audio\" data-resource_id=\"2755\" data-type=\"standard\" data-url=\"https://api.test.ndla.no/audio-api/v1/audio/2755\"></ndlaembed>"
      ),
      responsibleId = Missing,
      conceptType = None,
      glossData = None,
    )

    val result = converterService.toDomainConcept(1, updatedConcept, TokenUser.SystemUser)
    result.visualElement should be(
      Seq(
        VisualElement(
          "<ndlaembed data-resource=\"audio\" data-resource_id=\"2755\" data-type=\"standard\"></ndlaembed>",
          "nb",
        )
      )
    )
  }

  test("unknown embed attributes should be stripped from updated concepts") {
    val updatedConcept = UpdatedConceptDTO(
      language = "nb",
      title = Some("tittel"),
      content = Some("Nokko innhald"),
      copyright = None,
      tags = None,
      status = None,
      visualElement = Some(
        "<ndlaembed data-resource=\"audio\" data-resource_id=\"2755\" data-type=\"standard\" data-url=\"https://api.test.ndla.no/audio-api/v1/audio/2755\"></ndlaembed>"
      ),
      responsibleId = Missing,
      conceptType = None,
      glossData = None,
    )

    val result = converterService.toDomainConcept(TestData.domainConcept, updatedConcept, TokenUser.SystemUser).get
    result.visualElement should be(
      Seq(
        VisualElement(
          "<ndlaembed data-resource=\"audio\" data-resource_id=\"2755\" data-type=\"standard\"></ndlaembed>",
          "nb",
        )
      )
    )
  }
}
