/*
 * Part of NDLA concept-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.service

import cats.effect.IO
import no.ndla.conceptapi.auth.UserInfo
import no.ndla.conceptapi.model.api.ErrorHelpers
import no.ndla.conceptapi.model.domain.SideEffect.SideEffect
import no.ndla.conceptapi.model.domain
import no.ndla.conceptapi.model.domain.{ConceptStatus, SideEffect, StateTransition}
import no.ndla.conceptapi.repository.{DraftConceptRepository, PublishedConceptRepository}
import no.ndla.conceptapi.service.search.DraftConceptIndexService
import no.ndla.conceptapi.validation.ContentValidator
import no.ndla.conceptapi.model.domain.ConceptStatus._
import no.ndla.network.model.RequestInfo

import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

trait StateTransitionRules {
  this: WriteService
    with DraftConceptRepository
    with PublishedConceptRepository
    with WriteService
    with ConverterService
    with ContentValidator
    with DraftConceptIndexService
    with PublishedConceptRepository
    with ErrorHelpers =>

  object StateTransitionRules {

    private[service] val unpublishConcept: SideEffect =
      (concept: domain.Concept) => writeService.unpublishConcept(concept)

    private[service] val publishConcept: SideEffect =
      (concept: domain.Concept) => writeService.publishConcept(concept)

    import StateTransition._

    // format: off
    val StateTransitions: Set[StateTransition] = Set(
       IN_PROGRESS        -> IN_PROGRESS,
      (IN_PROGRESS        -> ARCHIVED)            require UserInfo.WriteRoles illegalStatuses Set(PUBLISHED),
      (IN_PROGRESS        -> EXTERNAL_REVIEW)     keepStates Set(PUBLISHED),
      (IN_PROGRESS        -> INTERNAL_REVIEW)     keepStates Set(PUBLISHED),
      (IN_PROGRESS        -> PUBLISHED)           keepStates Set() require UserInfo.PublishRoles withSideEffect publishConcept,
       INTERNAL_REVIEW    -> INTERNAL_REVIEW,
      (INTERNAL_REVIEW     -> IN_PROGRESS)        keepStates Set(PUBLISHED),
      (INTERNAL_REVIEW     -> EXTERNAL_REVIEW)    keepStates Set(PUBLISHED),
      (INTERNAL_REVIEW     -> QUALITY_ASSURANCE)  keepStates Set(PUBLISHED),
       ARCHIVED           -> ARCHIVED,
       ARCHIVED           -> IN_PROGRESS,
      (ARCHIVED           -> PUBLISHED)           keepStates Set() require UserInfo.PublishRoles withSideEffect publishConcept,
       QUALITY_ASSURANCE  -> QUALITY_ASSURANCE,
      (QUALITY_ASSURANCE  -> LANGUAGE)            keepStates Set(PUBLISHED) require UserInfo.PublishRoles,
      (QUALITY_ASSURANCE  -> PUBLISHED)           keepStates Set() require UserInfo.PublishRoles withSideEffect publishConcept,
       (QUALITY_ASSURANCE  -> IN_PROGRESS)        keepStates Set(PUBLISHED),
       (QUALITY_ASSURANCE  -> INTERNAL_REVIEW)    keepStates Set(PUBLISHED),
      (PUBLISHED          -> IN_PROGRESS)         keepCurrentOnTransition,
      (PUBLISHED          -> UNPUBLISHED)         keepStates Set() require UserInfo.PublishRoles withSideEffect unpublishConcept,
       UNPUBLISHED        -> UNPUBLISHED,
      (UNPUBLISHED        -> PUBLISHED)           keepStates Set() require UserInfo.PublishRoles withSideEffect publishConcept,
      (UNPUBLISHED        -> IN_PROGRESS),
      (UNPUBLISHED        -> ARCHIVED)            require UserInfo.WriteRoles illegalStatuses Set(PUBLISHED),
       LANGUAGE           -> LANGUAGE,
      (LANGUAGE           -> FOR_APPROVAL)        keepStates Set(PUBLISHED) require UserInfo.PublishRoles,
      (LANGUAGE           -> IN_PROGRESS)         keepStates Set(PUBLISHED),
      (LANGUAGE           -> INTERNAL_REVIEW)     keepStates Set(PUBLISHED),
      (LANGUAGE           -> PUBLISHED)           keepStates Set() require UserInfo.PublishRoles withSideEffect publishConcept,
       FOR_APPROVAL       -> FOR_APPROVAL,
      (FOR_APPROVAL       -> END_CONTROL)         keepStates Set(PUBLISHED),
       FOR_APPROVAL       -> IN_PROGRESS          keepStates Set(PUBLISHED),
       FOR_APPROVAL       -> INTERNAL_REVIEW      keepStates Set(PUBLISHED),
      (FOR_APPROVAL       -> PUBLISHED)           keepStates Set() require UserInfo.PublishRoles withSideEffect publishConcept,
       END_CONTROL       -> END_CONTROL,
      (END_CONTROL       -> FOR_APPROVAL)         keepStates Set(PUBLISHED),
      (END_CONTROL       -> IN_PROGRESS)          keepStates Set(PUBLISHED),
      (END_CONTROL       -> INTERNAL_REVIEW)      keepStates Set(PUBLISHED),
      (END_CONTROL       -> PUBLISHED)            keepStates Set() require UserInfo.PublishRoles withSideEffect publishConcept,
    )
    // format: on

    private def getTransition(
        from: ConceptStatus.Value,
        to: ConceptStatus.Value,
        user: UserInfo
    ): Option[StateTransition] =
      StateTransitions
        .find(transition => transition.from == from && transition.to == to)
        .filter(t => user.hasRoles(t.requiredRoles))

    private[service] def doTransitionWithoutSideEffect(
        current: domain.Concept,
        to: ConceptStatus.Value,
        user: UserInfo
    ): (Try[domain.Concept], SideEffect) = {
      getTransition(current.status.current, to, user) match {
        case Some(t) =>
          val currentToOther =
            if (t.addCurrentStateToOthersOnTransition) Set(current.status.current)
            else Set.empty

          val containsIllegalStatuses = current.status.other.intersect(t.illegalStatuses)
          if (containsIllegalStatuses.nonEmpty) {
            val illegalStateTransition = IllegalStatusStateTransition(
              s"Cannot go to $to when concept contains $containsIllegalStatuses"
            )
            return (Failure(illegalStateTransition), SideEffect.fromOutput(Failure(illegalStateTransition)))
          }
          val other            = current.status.other.intersect(t.otherStatesToKeepOnTransition) ++ currentToOther
          val newStatus        = domain.Status(to, other)
          val convertedArticle = current.copy(status = newStatus)
          (Success(convertedArticle), t.sideEffect)
        case None =>
          val illegalStateTransition = IllegalStatusStateTransition(
            s"Cannot go to $to when concept is ${current.status.current}"
          )
          (Failure(illegalStateTransition), SideEffect.fromOutput(Failure(illegalStateTransition)))
      }
    }

    def doTransition(
        current: domain.Concept,
        to: ConceptStatus.Value,
        user: UserInfo
    ): IO[Try[domain.Concept]] = {
      val (convertedArticle, sideEffect) = doTransitionWithoutSideEffect(current, to, user)
      val requestInfo                    = RequestInfo()
      IO {
        requestInfo.setRequestInfo()
        convertedArticle.flatMap(concept => sideEffect(concept))
      }
    }
  }
}
