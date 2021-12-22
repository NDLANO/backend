/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.domain

import no.ndla.draftapi.auth.{Role, UserInfo}
import no.ndla.draftapi.service.SideEffect
import no.ndla.draftapi.service.SideEffect.SideEffect

import scala.language.implicitConversions

case class StateTransition(from: ArticleStatus.Value,
                           to: ArticleStatus.Value,
                           otherStatesToKeepOnTransition: Set[ArticleStatus.Value],
                           sideEffects: Seq[SideEffect],
                           addCurrentStateToOthersOnTransition: Boolean,
                           requiredRoles: Set[Role.Value],
                           illegalStatuses: Set[ArticleStatus.Value]) {

  def keepCurrentOnTransition: StateTransition = copy(addCurrentStateToOthersOnTransition = true)
  def keepStates(toKeep: Set[ArticleStatus.Value]): StateTransition = copy(otherStatesToKeepOnTransition = toKeep)
  def withSideEffect(sideEffect: SideEffect): StateTransition = copy(sideEffects = sideEffects :+ sideEffect)
  def require(roles: Set[Role.Value]): StateTransition = copy(requiredRoles = roles)

  def illegalStatuses(illegalStatuses: Set[ArticleStatus.Value]): StateTransition =
    copy(illegalStatuses = illegalStatuses)
}

object StateTransition {
  implicit def tupleToStateTransition(fromTo: (ArticleStatus.Value, ArticleStatus.Value)): StateTransition = {
    val (from, to) = fromTo
    StateTransition(from,
                    to,
                    Set(ArticleStatus.IMPORTED, ArticleStatus.PUBLISHED),
                    Seq.empty[SideEffect],
                    addCurrentStateToOthersOnTransition = false,
                    UserInfo.WriteRoles,
                    Set())
  }
}
