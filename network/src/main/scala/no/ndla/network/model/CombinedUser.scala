/*
 * Part of NDLA network
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.model

import no.ndla.common.model.api.myndla.MyNDLAUser
import no.ndla.network.tapir.auth.TokenUser

sealed trait CombinedUser {
  val tokenUser: Option[TokenUser]
  val myndlaUser: Option[MyNDLAUser]
}

case class OptionalCombinedUser(tokenUser: Option[TokenUser], myndlaUser: Option[MyNDLAUser]) extends CombinedUser

trait CombinedUserRequired extends CombinedUser {
  def id: String
}

case class CombinedUserWithTokenUser(user: TokenUser, myndlaUser: Option[MyNDLAUser]) extends CombinedUserRequired {
  override def id: FeideID         = user.id
  val tokenUser: Option[TokenUser] = Some(user)
}

case class CombinedUserWithMyNDLAUser(tokenUser: Option[TokenUser], user: MyNDLAUser) extends CombinedUserRequired {
  override def id: String            = user.feideId
  val myndlaUser: Option[MyNDLAUser] = Some(user)
}

case class CombinedUserWithBoth(user: TokenUser, ndlaUser: MyNDLAUser) extends CombinedUserRequired {
  override def id: String            = user.id
  val tokenUser: Option[TokenUser]   = Some(user)
  val myndlaUser: Option[MyNDLAUser] = Some(ndlaUser)
}
