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
    requiredPermissions: Set[Permission],
    illegalStatuses: Set[DraftStatus],
    private val ignorePermissionsIf: Option[(Set[Permission], IgnoreFunction)],
    requiredStatuses: Set[DraftStatus]
) {

  def keepCurrentOnTransition: StateTransition                = copy(addCurrentStateToOthersOnTransition = true)
  def keepStates(toKeep: Set[DraftStatus]): StateTransition   = copy(otherStatesToKeepOnTransition = toKeep)
  def withSideEffect(sideEffect: SideEffect): StateTransition = copy(sideEffects = sideEffects :+ sideEffect)
  def requireStatusesForTransition(required: Set[DraftStatus]): StateTransition = copy(requiredStatuses = required)

  def require(permissions: Set[Permission], ignoreRoleRequirementIf: Option[IgnoreFunction] = None): StateTransition =
    copy(requiredPermissions = permissions, ignorePermissionsIf = ignoreRoleRequirementIf.map(requiredPermissions -> _))

  def hasRequiredProperties(user: TokenUser, maybeArticle: Option[Draft]): Boolean = {
    val ignore = ignorePermissionsIf match {
      case Some((oldRoles, ignoreFunc)) => ignoreFunc(maybeArticle, this) && user.hasPermissions(oldRoles)
      case None                         => false
    }
    val hasRequiredStatuses =
      maybeArticle match {
        case Some(article) => requiredStatuses.forall(draftStatus => article.status.other.contains(draftStatus))
        case None          => requiredStatuses.isEmpty
      }
    (ignore || user.hasPermissions(this.requiredPermissions)) && hasRequiredStatuses
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
      None,
      Set()
    )
  }
}
