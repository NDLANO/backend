/*
 * Part of NDLA concept-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.service

import no.ndla.common.model.domain.concept.{Concept, ConceptContent, ConceptStatus, ConceptType, Status}
import no.ndla.common.model.domain.draft.DraftCopyright
import no.ndla.common.model.domain.{Author, ContributorType, Responsible, Tag, Title}
import no.ndla.conceptapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.conceptapi.model.domain.StateTransition
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock

import scala.util.Success

class StateTransitionRulesTest extends UnitSuite with TestEnvironment {
  test("That publishing concept results in responsibleId being reset") {
    val beforeResponsible = Responsible("heisann", clock.now())
    val concept = Concept(
      id = Some(1L),
      revision = Some(1),
      title = Seq(Title("tittel", "nb")),
      content = Seq(ConceptContent("content", "nb")),
      copyright = Some(
        DraftCopyright(
          license = Some("CC-BY-4.0"),
          origin = None,
          creators = Seq(Author(ContributorType.Writer, "Ape Katt")),
          processors = Seq.empty,
          rightsholders = Seq.empty,
          validFrom = None,
          validTo = None,
          false
        )
      ),
      created = TestData.today,
      updated = TestData.today,
      updatedBy = Seq("user"),
      tags = Seq(Tag(Seq("Hei", "hå"), "nb")),
      status = Status(ConceptStatus.IN_PROGRESS, Set.empty),
      visualElement = Seq.empty,
      responsible = Some(Responsible("hei", TestData.today)),
      conceptType = ConceptType.CONCEPT,
      glossData = None,
      editorNotes = Seq.empty
    )
    val status            = Status(ConceptStatus.IN_PROGRESS, Set.empty)
    val transitionsToTest = StateTransitionRules.StateTransitions.filter(_.to == ConceptStatus.PUBLISHED)
    when(writeService.publishConcept(any)).thenAnswer((i: InvocationOnMock) => Success(i.getArgument[Concept](0)))
    when(writeService.unpublishConcept(any)).thenAnswer((i: InvocationOnMock) => Success(i.getArgument[Concept](0)))
    for (t <- transitionsToTest) {
      val fromDraft = concept.copy(status = status.copy(current = t.from), responsible = Some(beforeResponsible))
      val result =
        StateTransitionRules.doTransition(fromDraft, ConceptStatus.PUBLISHED, TestData.userWithWriteAndPublishAccess)

      if (result.get.responsible.isDefined) {
        fail(s"${t.from} -> ${t.to} did not reset responsible >:( Look at the sideeffects in `StateTransitionRules`")
      }
    }
  }

  test("That archiving concept results in responsibleId being reset") {
    val beforeResponsible = Responsible("heisann", clock.now())
    val concept = Concept(
      id = Some(1L),
      revision = Some(1),
      title = Seq(Title("tittel", "nb")),
      content = Seq(ConceptContent("content", "nb")),
      copyright = Some(
        DraftCopyright(
          license = Some("CC-BY-4.0"),
          origin = None,
          creators = Seq(Author(ContributorType.Writer, "Ape Katt")),
          processors = Seq.empty,
          rightsholders = Seq.empty,
          validFrom = None,
          validTo = None,
          false
        )
      ),
      created = TestData.today,
      updated = TestData.today,
      updatedBy = Seq("user"),
      tags = Seq(Tag(Seq("Hei", "hå"), "nb")),
      status = Status(ConceptStatus.IN_PROGRESS, Set.empty),
      visualElement = Seq.empty,
      responsible = Some(Responsible("hei", TestData.today)),
      conceptType = ConceptType.CONCEPT,
      glossData = None,
      editorNotes = Seq.empty
    )
    val status            = Status(ConceptStatus.IN_PROGRESS, Set.empty)
    val transitionsToTest = StateTransitionRules.StateTransitions.filter(_.to == ConceptStatus.ARCHIVED)
    when(writeService.publishConcept(any)).thenAnswer((i: InvocationOnMock) => Success(i.getArgument[Concept](0)))
    when(writeService.unpublishConcept(any)).thenAnswer((i: InvocationOnMock) => Success(i.getArgument[Concept](0)))
    for (t <- transitionsToTest) {
      val fromDraft = concept.copy(status = status.copy(current = t.from), responsible = Some(beforeResponsible))
      val result = StateTransitionRules
        .doTransition(fromDraft, ConceptStatus.ARCHIVED, TestData.userWithWriteAndPublishAccess)

      if (result.get.responsible.isDefined) {
        fail(s"${t.from} -> ${t.to} did not reset responsible >:( Look at the sideeffects in `StateTransitionRules`")
      }
    }
  }

  test("That unpublishing concept results in responsibleId being reset") {
    val beforeResponsible = Responsible("heisann", clock.now())
    val concept = Concept(
      id = Some(1L),
      revision = Some(1),
      title = Seq(Title("tittel", "nb")),
      content = Seq(ConceptContent("content", "nb")),
      copyright = Some(
        DraftCopyright(
          license = Some("CC-BY-4.0"),
          origin = None,
          creators = Seq(Author(ContributorType.Writer, "Ape Katt")),
          processors = Seq.empty,
          rightsholders = Seq.empty,
          validFrom = None,
          validTo = None,
          false
        )
      ),
      created = TestData.today,
      updated = TestData.today,
      updatedBy = Seq("user"),
      tags = Seq(Tag(Seq("Hei", "hå"), "nb")),
      status = Status(ConceptStatus.IN_PROGRESS, Set.empty),
      visualElement = Seq.empty,
      responsible = Some(Responsible("hei", TestData.today)),
      conceptType = ConceptType.CONCEPT,
      glossData = None,
      editorNotes = Seq.empty
    )
    val status            = Status(ConceptStatus.IN_PROGRESS, Set.empty)
    val transitionsToTest = StateTransitionRules.StateTransitions.filter(_.to == ConceptStatus.UNPUBLISHED)
    when(writeService.publishConcept(any)).thenAnswer((i: InvocationOnMock) => Success(i.getArgument[Concept](0)))
    when(writeService.unpublishConcept(any)).thenAnswer((i: InvocationOnMock) => Success(i.getArgument[Concept](0)))
    for (t <- transitionsToTest) {
      val fromDraft = concept.copy(status = status.copy(current = t.from), responsible = Some(beforeResponsible))
      val result = StateTransitionRules
        .doTransition(fromDraft, ConceptStatus.UNPUBLISHED, TestData.userWithWriteAndPublishAccess)

      if (result.get.responsible.isDefined) {
        fail(s"${t.from} -> ${t.to} did not reset responsible >:( Look at the sideeffects in `StateTransitionRules`")
      }
    }
  }

  test("That responsibleId is updated at status change from published to in progress") {
    val concept = Concept(
      id = Some(1L),
      revision = Some(1),
      title = Seq(Title("tittel", "nb")),
      content = Seq(ConceptContent("content", "nb")),
      copyright = Some(
        DraftCopyright(
          license = Some("CC-BY-4.0"),
          origin = None,
          creators = Seq(Author(ContributorType.Writer, "Katronk")),
          processors = Seq.empty,
          rightsholders = Seq.empty,
          validFrom = None,
          validTo = None,
          false
        )
      ),
      created = TestData.today,
      updated = TestData.today,
      updatedBy = Seq("user"),
      tags = Seq(Tag(Seq("Hei", "hå"), "nb")),
      status = Status(ConceptStatus.PUBLISHED, Set.empty),
      visualElement = Seq.empty,
      responsible = None,
      conceptType = ConceptType.CONCEPT,
      glossData = None,
      editorNotes = Seq.empty
    )
    val status                            = Status(ConceptStatus.PUBLISHED, Set.empty)
    val transitionToTest: StateTransition = ConceptStatus.PUBLISHED -> ConceptStatus.IN_PROGRESS
    val expected                          = TestData.userWithWriteAndPublishAccess.id
    when(writeService.publishConcept(any)).thenAnswer((i: InvocationOnMock) => Success(i.getArgument[Concept](0)))
    when(writeService.unpublishConcept(any)).thenAnswer((i: InvocationOnMock) => Success(i.getArgument[Concept](0)))

    val fromConcept = concept.copy(status = status.copy(current = transitionToTest.from))
    val result = StateTransitionRules
      .doTransition(fromConcept, ConceptStatus.IN_PROGRESS, TestData.userWithWriteAndPublishAccess)

    result.get.responsible.get.responsibleId should be(expected)
  }
}
