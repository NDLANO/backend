/*
 * Part of NDLA concept-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.service

import cats.effect.IO
import no.ndla.conceptapi.auth.UserInfo
import no.ndla.conceptapi.model.api.IllegalStatusStateTransition
import no.ndla.conceptapi.model.domain.SideEffect.SideEffect
import no.ndla.conceptapi.model.domain
import no.ndla.conceptapi.model.domain.{ConceptStatus, SideEffect, StateTransition}
import no.ndla.conceptapi.repository.{DraftConceptRepository, PublishedConceptRepository}
import no.ndla.conceptapi.service.search.DraftConceptIndexService
import no.ndla.conceptapi.validation.ContentValidator
import no.ndla.conceptapi.model.domain.ConceptStatus._

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
    with PublishedConceptRepository =>

  object StateTransitionRules {

    private[service] val unpublishConcept: SideEffect =
      (concept: domain.Concept) => writeService.unpublishConcept(concept)

    private[service] val publishConcept: SideEffect =
      (concept: domain.Concept) => writeService.publishConcept(concept)

    import StateTransition._

    // format: off
    val StateTransitions: Set[StateTransition] = Set(
       DRAFT                      -> DRAFT,
       DRAFT                      -> ARCHIVED                    require UserInfo.WriteRoles illegalStatuses Set(PUBLISHED),
       DRAFT                      -> QUEUED_FOR_LANGUAGE         keepStates Set(PUBLISHED),
      (DRAFT                      -> PUBLISHED)                  keepStates Set() require UserInfo.PublishRoles withSideEffect publishConcept,
       ARCHIVED                   -> ARCHIVED,
       ARCHIVED                   -> DRAFT,
      (ARCHIVED                   -> PUBLISHED)                  keepStates Set() require UserInfo.PublishRoles withSideEffect publishConcept,
      (DRAFT                      -> PUBLISHED)                  keepStates Set() require UserInfo.PublishRoles withSideEffect publishConcept,
       QUALITY_ASSURED            -> QUALITY_ASSURED             keepStates Set(PUBLISHED),
      (QUALITY_ASSURED            -> PUBLISHED)                  keepStates Set() require UserInfo.PublishRoles withSideEffect publishConcept,
       QUALITY_ASSURED            -> DRAFT                       keepStates Set(PUBLISHED),
      (PUBLISHED                  -> DRAFT)                      keepCurrentOnTransition,
      (PUBLISHED                  -> UNPUBLISHED)                keepStates Set() require UserInfo.PublishRoles withSideEffect unpublishConcept,
      (UNPUBLISHED                -> PUBLISHED)                  keepStates Set() require UserInfo.PublishRoles withSideEffect publishConcept,
       UNPUBLISHED                -> DRAFT,
       UNPUBLISHED                -> UNPUBLISHED,
       UNPUBLISHED                -> ARCHIVED                    require UserInfo.WriteRoles illegalStatuses Set(PUBLISHED),
       QUEUED_FOR_LANGUAGE        -> QUEUED_FOR_LANGUAGE,
       QUEUED_FOR_LANGUAGE        -> TRANSLATED                  keepStates Set(PUBLISHED),
       QUEUED_FOR_LANGUAGE        -> DRAFT                       keepStates Set(PUBLISHED),
      (QUEUED_FOR_LANGUAGE        -> PUBLISHED)                  keepStates Set() require UserInfo.PublishRoles withSideEffect publishConcept,
       TRANSLATED                 -> TRANSLATED,
       TRANSLATED                 -> QUALITY_ASSURED             keepStates Set(PUBLISHED),
       TRANSLATED                 -> DRAFT                       keepStates Set(PUBLISHED),
      (TRANSLATED                 -> PUBLISHED)                  keepStates Set() require UserInfo.PublishRoles withSideEffect publishConcept,
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
              s"Cannot go to $to when concept contains $containsIllegalStatuses")
            return (Failure(illegalStateTransition), SideEffect.fromOutput(Failure(illegalStateTransition)))
          }
          val other = current.status.other.intersect(t.otherStatesToKeepOnTransition) ++ currentToOther
          val newStatus = domain.Status(to, other)
          val convertedArticle = current.copy(status = newStatus)
          (Success(convertedArticle), t.sideEffect)
        case None =>
          val illegalStateTransition = IllegalStatusStateTransition(
            s"Cannot go to $to when concept is ${current.status.current}")
          (Failure(illegalStateTransition), SideEffect.fromOutput(Failure(illegalStateTransition)))
      }
    }

    def doTransition(
        current: domain.Concept,
        to: ConceptStatus.Value,
        user: UserInfo
    ): IO[Try[domain.Concept]] = {
      val (convertedArticle, sideEffect) = doTransitionWithoutSideEffect(current, to, user)
      IO { convertedArticle.flatMap(concept => sideEffect(concept)) }
    }
  }
}
