/*
 * Part of NDLA concept-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.domain

import no.ndla.conceptapi.model.domain.SideEffect.SideEffect
import no.ndla.network.tapir.auth.Permission
import no.ndla.network.tapir.auth.Permission.CONCEPT_API_WRITE

case class StateTransition(
    from: ConceptStatus.Value,
    to: ConceptStatus.Value,
    otherStatesToKeepOnTransition: Set[ConceptStatus.Value],
    sideEffects: Seq[SideEffect],
    addCurrentStateToOthersOnTransition: Boolean,
    requiredRoles: Set[Permission],
    illegalStatuses: Set[ConceptStatus.Value]
) {

  def keepCurrentOnTransition: StateTransition                      = copy(addCurrentStateToOthersOnTransition = true)
  def keepStates(toKeep: Set[ConceptStatus.Value]): StateTransition = copy(otherStatesToKeepOnTransition = toKeep)
  def withSideEffect(sideEffect: SideEffect): StateTransition       = copy(sideEffects = sideEffects :+ sideEffect)
  def require(roles: Set[Permission]): StateTransition              = copy(requiredRoles = roles)

  def illegalStatuses(illegalStatuses: Set[ConceptStatus.Value]): StateTransition =
    copy(illegalStatuses = illegalStatuses)
}

object StateTransition {
  implicit def tupleToStateTransition(fromTo: (ConceptStatus.Value, ConceptStatus.Value)): StateTransition = {
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
