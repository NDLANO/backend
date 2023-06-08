/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.domain

import no.ndla.common.model.domain.draft.{Draft, DraftStatus}
import no.ndla.draftapi.auth.{Role, UserInfo}
import no.ndla.draftapi.service.SideEffect.SideEffect

case class StateTransition(
    from: DraftStatus,
    to: DraftStatus,
    otherStatesToKeepOnTransition: Set[DraftStatus],
    sideEffects: Seq[SideEffect],
    addCurrentStateToOthersOnTransition: Boolean,
    requiredRoles: Set[Role.Value],
    illegalStatuses: Set[DraftStatus],
    private val ignoreRolesIf: Option[(Set[Role.Value], IgnoreFunction)]
) {

  def keepCurrentOnTransition: StateTransition                = copy(addCurrentStateToOthersOnTransition = true)
  def keepStates(toKeep: Set[DraftStatus]): StateTransition   = copy(otherStatesToKeepOnTransition = toKeep)
  def withSideEffect(sideEffect: SideEffect): StateTransition = copy(sideEffects = sideEffects :+ sideEffect)

  def require(roles: Set[Role.Value], ignoreRoleRequirementIf: Option[IgnoreFunction] = None): StateTransition =
    copy(requiredRoles = roles, ignoreRolesIf = ignoreRoleRequirementIf.map(requiredRoles -> _))

  def hasRequiredRoles(user: UserInfo, article: Option[Draft]): Boolean = {
    val ignore = ignoreRolesIf match {
      case Some((oldRoles, ignoreFunc)) => ignoreFunc(article, this) && user.hasRoles(oldRoles)
      case None                         => false
    }
    ignore || user.hasRoles(this.requiredRoles)
  }

  def withIllegalStatuses(illegalStatuses: Set[DraftStatus]): StateTransition =
    copy(illegalStatuses = illegalStatuses)
}

object StateTransition {
  implicit def tupleToStateTransition(fromTo: (DraftStatus, DraftStatus)): StateTransition = {
    val (from, to) = fromTo
    StateTransition(
      from,
      to,
      Set(DraftStatus.PUBLISHED),
      Seq.empty[SideEffect],
      addCurrentStateToOthersOnTransition = false,
      UserInfo.WriteRoles,
      Set(),
      None
    )
  }
}
