/*
 * Part of NDLA search-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.auth

import no.ndla.network.AuthUser

case class UserInfo(id: String, roles: Set[Role.Value]) {
  def hasRoles(rolesToCheck: Set[Role.Value]): Boolean = rolesToCheck.subsetOf(roles)
}

object UserInfo {
  val UnauthorizedUser = UserInfo("unauthorized", Set.empty)
  def apply(name: String): UserInfo = UserInfo(name, AuthUser.getRoles.flatMap(Role.valueOf).toSet)
  def get: Option[UserInfo] = (AuthUser.get orElse AuthUser.getClientId).map(UserInfo.apply)
}

trait User {
  val user: User

  class User {
    def getUser: UserInfo = UserInfo.get.getOrElse(UserInfo.UnauthorizedUser)
  }
}
