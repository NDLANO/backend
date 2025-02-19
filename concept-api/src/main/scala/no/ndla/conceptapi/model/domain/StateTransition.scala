/*
 * Part of NDLA concept-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.model.domain

import no.ndla.common.model.domain.concept.ConceptStatus
import no.ndla.conceptapi.model.domain.SideEffect.SideEffect
import no.ndla.network.tapir.auth.Permission
import no.ndla.network.tapir.auth.Permission.CONCEPT_API_WRITE

case class StateTransition(
    from: ConceptStatus,
    to: ConceptStatus,
    otherStatesToKeepOnTransition: Set[ConceptStatus],
    sideEffects: Seq[SideEffect],
    addCurrentStateToOthersOnTransition: Boolean,
    requiredPermissions: Set[Permission],
    illegalStatuses: Set[ConceptStatus]
) {

  def keepCurrentOnTransition: StateTransition                = copy(addCurrentStateToOthersOnTransition = true)
  def keepStates(toKeep: Set[ConceptStatus]): StateTransition = copy(otherStatesToKeepOnTransition = toKeep)
  def withSideEffect(sideEffect: SideEffect): StateTransition = copy(sideEffects = sideEffects :+ sideEffect)
  def require(permissions: Set[Permission]): StateTransition  = copy(requiredPermissions = permissions)

  def withIllegalStatuses(illegalStatuses: Set[ConceptStatus]): StateTransition =
    copy(illegalStatuses = illegalStatuses)
}

object StateTransition {
  implicit def tupleToStateTransition(fromTo: (ConceptStatus, ConceptStatus)): StateTransition = {
    val (from, to) = fromTo
    StateTransition(
      from,
      to,
      Set(ConceptStatus.PUBLISHED),
      Seq.empty[SideEffect],
      addCurrentStateToOthersOnTransition = false,
      Set(CONCEPT_API_WRITE),
      Set()
    )
  }
}
