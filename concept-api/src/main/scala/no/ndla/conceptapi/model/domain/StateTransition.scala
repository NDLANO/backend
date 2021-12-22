/*
 * Part of NDLA concept-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.model.domain

import no.ndla.conceptapi.auth.{Role, UserInfo}
import no.ndla.conceptapi.model.domain.SideEffect.SideEffect

import scala.language.implicitConversions

case class StateTransition(from: ConceptStatus.Value,
                           to: ConceptStatus.Value,
                           otherStatesToKeepOnTransition: Set[ConceptStatus.Value],
                           sideEffect: SideEffect,
                           addCurrentStateToOthersOnTransition: Boolean,
                           requiredRoles: Set[Role.Value],
                           illegalStatuses: Set[ConceptStatus.Value]) {

  def keepCurrentOnTransition: StateTransition = copy(addCurrentStateToOthersOnTransition = true)
  def keepStates(toKeep: Set[ConceptStatus.Value]): StateTransition = copy(otherStatesToKeepOnTransition = toKeep)
  def withSideEffect(sideEffect: SideEffect): StateTransition = copy(sideEffect = sideEffect)
  def require(roles: Set[Role.Value]): StateTransition = copy(requiredRoles = roles)

  def illegalStatuses(illegalStatuses: Set[ConceptStatus.Value]): StateTransition =
    copy(illegalStatuses = illegalStatuses)
}

object StateTransition {
  implicit def tupleToStateTransition(fromTo: (ConceptStatus.Value, ConceptStatus.Value)): StateTransition = {
    val (from, to) = fromTo
    StateTransition(from,
                    to,
                    Set(ConceptStatus.PUBLISHED),
                    SideEffect.none,
                    addCurrentStateToOthersOnTransition = false,
                    UserInfo.WriteRoles,
                    Set())
  }
}
