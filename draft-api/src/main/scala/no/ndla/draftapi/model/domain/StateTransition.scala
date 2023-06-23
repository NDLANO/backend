/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.domain

import no.ndla.common.model.domain.draft.{Draft, DraftStatus}
import no.ndla.draftapi.service.SideEffect.SideEffect
import no.ndla.network.tapir.auth.Permission.DRAFT_API_WRITE
import no.ndla.network.tapir.auth.{Permission, TokenUser}

case class StateTransition(
    from: DraftStatus,
    to: DraftStatus,
    otherStatesToKeepOnTransition: Set[DraftStatus],
    sideEffects: Seq[SideEffect],
    addCurrentStateToOthersOnTransition: Boolean,
    requiredRoles: Set[Permission],
    illegalStatuses: Set[DraftStatus],
    private val ignoreRolesIf: Option[(Set[Permission], IgnoreFunction)]
) {

  def keepCurrentOnTransition: StateTransition                = copy(addCurrentStateToOthersOnTransition = true)
  def keepStates(toKeep: Set[DraftStatus]): StateTransition   = copy(otherStatesToKeepOnTransition = toKeep)
  def withSideEffect(sideEffect: SideEffect): StateTransition = copy(sideEffects = sideEffects :+ sideEffect)

  def require(roles: Set[Permission], ignoreRoleRequirementIf: Option[IgnoreFunction] = None): StateTransition =
    copy(requiredRoles = roles, ignoreRolesIf = ignoreRoleRequirementIf.map(requiredRoles -> _))

  def hasRequiredRoles(user: TokenUser, article: Option[Draft]): Boolean = {
    val ignore = ignoreRolesIf match {
      case Some((oldRoles, ignoreFunc)) => ignoreFunc(article, this) && user.hasPermissions(oldRoles)
      case None                         => false
    }
    ignore || user.hasPermissions(this.requiredRoles)
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
      Set(DRAFT_API_WRITE),
      Set(),
      None
    )
  }
}
