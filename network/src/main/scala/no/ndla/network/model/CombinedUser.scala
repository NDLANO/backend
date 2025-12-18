/*
 * Part of NDLA network
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.model

import no.ndla.common.model.api.myndla.MyNDLAUserDTO
import no.ndla.common.model.domain.myndla.UserRole.EMPLOYEE
import no.ndla.network.tapir.auth.TokenUser

sealed trait CombinedUser {
  val tokenUser: Option[TokenUser]
  val myndlaUser: Option[MyNDLAUserDTO]
  def isMyNDLAUser: Boolean = myndlaUser.isDefined && tokenUser.isEmpty
  def isEmployee: Boolean   = myndlaUser.exists(_.role == EMPLOYEE)
}

case class OptionalCombinedUser(tokenUser: Option[TokenUser], myndlaUser: Option[MyNDLAUserDTO]) extends CombinedUser

trait CombinedUserRequired extends CombinedUser {
  def id: String
}

case class CombinedUserWithTokenUser(user: TokenUser, myndlaUser: Option[MyNDLAUserDTO]) extends CombinedUserRequired {
  override def id: FeideID         = user.id
  val tokenUser: Option[TokenUser] = Some(user)
}

case class CombinedUserWithMyNDLAUser(tokenUser: Option[TokenUser], user: MyNDLAUserDTO) extends CombinedUserRequired {
  override def id: String               = user.feideId
  val myndlaUser: Option[MyNDLAUserDTO] = Some(user)
}

case class CombinedUserWithBoth(user: TokenUser, ndlaUser: MyNDLAUserDTO) extends CombinedUserRequired {
  override def id: String               = user.id
  val tokenUser: Option[TokenUser]      = Some(user)
  val myndlaUser: Option[MyNDLAUserDTO] = Some(ndlaUser)
}
