/*
 * Part of NDLA network
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.model

import no.ndla.common.model.domain.myndla.UserRole.EMPLOYEE
import no.ndla.network.tapir.auth.TokenUser

sealed trait CombinedUser {
  val tokenUser: Option[TokenUser]
  val myndlaUser: Option[FeideUserWrapper]
  def isMyNDLAUser: Boolean = myndlaUser.isDefined && tokenUser.isEmpty
  def isEmployee: Boolean   = myndlaUser.exists(_.user.userRole == EMPLOYEE)
}

case class OptionalCombinedUser(
    override val tokenUser: Option[TokenUser],
    override val myndlaUser: Option[FeideUserWrapper],
) extends CombinedUser

trait CombinedUserRequired extends CombinedUser {
  def id: String
}

case class CombinedUserWithTokenUser(ndlaUser: TokenUser, override val myndlaUser: Option[FeideUserWrapper])
    extends CombinedUserRequired {
  override def id: FeideID                  = ndlaUser.id
  override val tokenUser: Option[TokenUser] = Some(ndlaUser)
}

case class CombinedUserWithMyNDLAUser(override val tokenUser: Option[TokenUser], feideUser: FeideUserWrapper)
    extends CombinedUserRequired {
  override def id: String                           = feideUser.user.feideId
  override val myndlaUser: Option[FeideUserWrapper] = Some(feideUser)
}

case class CombinedUserWithBoth(ndlaUser: TokenUser, feideUser: FeideUserWrapper) extends CombinedUserRequired {
  override def id: String                           = ndlaUser.id
  override val tokenUser: Option[TokenUser]         = Some(ndlaUser)
  override val myndlaUser: Option[FeideUserWrapper] = Some(feideUser)
}
