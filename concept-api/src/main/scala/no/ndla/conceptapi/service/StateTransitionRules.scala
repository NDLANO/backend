/*
 * Part of NDLA concept-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.service

import no.ndla.common.model.domain.concept.{ConceptEditorNote, ConceptStatus, Status, Concept as DomainConcept}
import no.ndla.common.model.domain.{Responsible, concept}
import no.ndla.conceptapi.model.api.ErrorHandling
import no.ndla.common.model.domain.concept.ConceptStatus.*
import no.ndla.conceptapi.model.domain.SideEffect.SideEffect
import no.ndla.conceptapi.model.domain.StateTransition
import no.ndla.conceptapi.repository.{DraftConceptRepository, PublishedConceptRepository}
import no.ndla.conceptapi.service.search.DraftConceptIndexService
import no.ndla.conceptapi.validation.ContentValidator
import no.ndla.common.Clock
import no.ndla.network.tapir.auth.Permission.{CONCEPT_API_ADMIN, CONCEPT_API_WRITE}
import no.ndla.network.tapir.auth.{Permission, TokenUser}

import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

trait StateTransitionRules {
  this: WriteService & DraftConceptRepository & PublishedConceptRepository & WriteService & ConverterService &
    ContentValidator & DraftConceptIndexService & PublishedConceptRepository & ErrorHandling & Clock =>

  object StateTransitionRules {

    private[service] val unpublishConcept: SideEffect =
      (concept: DomainConcept, _: TokenUser) => writeService.unpublishConcept(concept)

    private[service] val publishConcept: SideEffect =
      (concept: DomainConcept, _: TokenUser) => writeService.publishConcept(concept)

    private val resetResponsible: SideEffect = (concept: DomainConcept, _: TokenUser) =>
      Success(concept.copy(responsible = None))
    private val addResponsible: SideEffect = (concept: DomainConcept, user: TokenUser) => {
      val responsible = concept.responsible.getOrElse(Responsible(user.id, clock.now()))
      Success(concept.copy(responsible = Some(responsible)))
    }

    import StateTransition.*

    private val WritePermission: Set[Permission]   = Set(CONCEPT_API_WRITE)
    private val PublishPermission: Set[Permission] = Set(CONCEPT_API_ADMIN)

    // format: off
    val StateTransitions: Set[StateTransition] = Set(
       IN_PROGRESS        -> IN_PROGRESS,
      (IN_PROGRESS        -> ARCHIVED)            require WritePermission withIllegalStatuses  Set(PUBLISHED) withSideEffect resetResponsible,
      (IN_PROGRESS        -> EXTERNAL_REVIEW)     keepStates Set(PUBLISHED),
      (IN_PROGRESS        -> INTERNAL_REVIEW)     keepStates Set(PUBLISHED),
      (IN_PROGRESS        -> LANGUAGE)            keepStates Set(PUBLISHED),
      (IN_PROGRESS        -> PUBLISHED)           keepStates Set() require PublishPermission withSideEffect publishConcept withSideEffect resetResponsible,
      (EXTERNAL_REVIEW    -> IN_PROGRESS)         keepStates Set(PUBLISHED),
       EXTERNAL_REVIEW    -> EXTERNAL_REVIEW,
      (EXTERNAL_REVIEW    -> INTERNAL_REVIEW)     keepStates Set(PUBLISHED),
       INTERNAL_REVIEW    -> INTERNAL_REVIEW,
      (INTERNAL_REVIEW    -> IN_PROGRESS)         keepStates Set(PUBLISHED),
      (INTERNAL_REVIEW    -> EXTERNAL_REVIEW)     keepStates Set(PUBLISHED),
      (INTERNAL_REVIEW    -> QUALITY_ASSURANCE)   keepStates Set(PUBLISHED),
       ARCHIVED           -> ARCHIVED             withSideEffect resetResponsible,
       ARCHIVED           -> IN_PROGRESS,
      (ARCHIVED           -> PUBLISHED)           keepStates Set() require PublishPermission withSideEffect publishConcept withSideEffect resetResponsible,
       QUALITY_ASSURANCE  -> QUALITY_ASSURANCE,
      (QUALITY_ASSURANCE  -> LANGUAGE)            keepStates Set(PUBLISHED) require PublishPermission,
      (QUALITY_ASSURANCE  -> PUBLISHED)           keepStates Set() require PublishPermission withSideEffect publishConcept withSideEffect resetResponsible,
      (QUALITY_ASSURANCE  -> IN_PROGRESS)         keepStates Set(PUBLISHED),
      (QUALITY_ASSURANCE  -> INTERNAL_REVIEW)     keepStates Set(PUBLISHED),
      (PUBLISHED          -> IN_PROGRESS)         withSideEffect addResponsible keepCurrentOnTransition,
      (PUBLISHED          -> UNPUBLISHED)         keepStates Set() require PublishPermission withSideEffect unpublishConcept withSideEffect resetResponsible,
       UNPUBLISHED        -> UNPUBLISHED          withSideEffect resetResponsible,
      (UNPUBLISHED        -> PUBLISHED)           keepStates Set() require PublishPermission withSideEffect publishConcept withSideEffect resetResponsible,
       UNPUBLISHED        -> IN_PROGRESS,
      (UNPUBLISHED        -> ARCHIVED)            require WritePermission withIllegalStatuses  Set(PUBLISHED) withSideEffect resetResponsible,
       LANGUAGE           -> LANGUAGE,
      (LANGUAGE           -> FOR_APPROVAL)        keepStates Set(PUBLISHED) require PublishPermission,
      (LANGUAGE           -> IN_PROGRESS)         keepStates Set(PUBLISHED),
      (LANGUAGE           -> INTERNAL_REVIEW)     keepStates Set(PUBLISHED),
      (LANGUAGE           -> PUBLISHED)           keepStates Set() require PublishPermission withSideEffect publishConcept withSideEffect resetResponsible,
       FOR_APPROVAL       -> FOR_APPROVAL,
      (FOR_APPROVAL       -> END_CONTROL)         keepStates Set(PUBLISHED),
       FOR_APPROVAL       -> IN_PROGRESS          keepStates Set(PUBLISHED),
       FOR_APPROVAL       -> INTERNAL_REVIEW      keepStates Set(PUBLISHED),
      (FOR_APPROVAL       -> PUBLISHED)           keepStates Set() require PublishPermission withSideEffect publishConcept withSideEffect resetResponsible,
       END_CONTROL        -> END_CONTROL,
      (END_CONTROL        -> FOR_APPROVAL)        keepStates Set(PUBLISHED),
      (END_CONTROL        -> IN_PROGRESS)         keepStates Set(PUBLISHED),
      (END_CONTROL        -> INTERNAL_REVIEW)     keepStates Set(PUBLISHED),
      (END_CONTROL        -> PUBLISHED)           keepStates Set() require PublishPermission withSideEffect publishConcept withSideEffect resetResponsible,
    )
    // format: on

    private def getTransition(
        from: ConceptStatus,
        to: ConceptStatus,
        user: TokenUser
    ): Option[StateTransition] =
      StateTransitions
        .find(transition => transition.from == from && transition.to == to)
        .filter(t => user.hasPermissions(t.requiredPermissions))

    private def validateTransition(current: DomainConcept, transition: StateTransition): Try[Unit] = {
      val statusRequiresResponsible       = ConceptStatus.thatRequiresResponsible.contains(transition.to)
      val statusFromPublishedToInProgress =
        current.status.current == PUBLISHED && transition.to == IN_PROGRESS
      if (statusRequiresResponsible && current.responsible.isEmpty && !statusFromPublishedToInProgress) {
        return Failure(
          IllegalStatusStateTransition(
            s"The action triggered a state transition to ${transition.to}, this is invalid without setting new responsible."
          )
        )
      }

      val containsIllegalStatuses = current.status.other.intersect(transition.illegalStatuses)
      if (containsIllegalStatuses.nonEmpty) {
        val illegalStateTransition = IllegalStatusStateTransition(
          s"Cannot go to ${transition.to} when concept contains $containsIllegalStatuses"
        )
        return Failure(illegalStateTransition)
      }

      Success(())
    }
    private def newEditorNotesForTransition(
        current: DomainConcept,
        to: ConceptStatus,
        newStatus: Status,
        user: TokenUser
    ) = {
      if (current.status.current != to)
        current.editorNotes :+ ConceptEditorNote(
          "Status changed",
          user.id,
          newStatus,
          clock.now()
        )
      else current.editorNotes
    }
    private[service] def doTransitionWithoutSideEffect(
        current: DomainConcept,
        to: ConceptStatus,
        user: TokenUser
    ): (Try[DomainConcept], Seq[SideEffect]) = {
      getTransition(current.status.current, to, user) match {
        case Some(t) =>
          validateTransition(current, t) match {
            case Failure(ex) => (Failure(ex), Seq.empty)
            case Success(_)  =>
              val currentToOther =
                if (t.addCurrentStateToOthersOnTransition) Set(current.status.current)
                else Set.empty
              val other            = current.status.other.intersect(t.otherStatesToKeepOnTransition) ++ currentToOther
              val newStatus        = concept.Status(to, other)
              val newEditorNotes   = newEditorNotesForTransition(current, to, newStatus, user)
              val convertedArticle = current.copy(status = newStatus, editorNotes = newEditorNotes)
              (Success(convertedArticle), t.sideEffects)
          }
        case None =>
          val illegalStateTransition = IllegalStatusStateTransition(
            s"Cannot go to $to when concept is ${current.status.current}"
          )
          (Failure(illegalStateTransition), Seq.empty)
      }
    }

    def doTransition(
        current: DomainConcept,
        to: ConceptStatus,
        user: TokenUser
    ): Try[DomainConcept] = {
      val (convertedArticle, sideEffects) = doTransitionWithoutSideEffect(current, to, user)
      convertedArticle.flatMap(conceptBeforeSideEffect => {
        sideEffects.foldLeft(Try(conceptBeforeSideEffect))((accumulatedConcept, sideEffect) => {
          accumulatedConcept.flatMap(c => sideEffect(c, user))
        })
      })
    }
  }
}
