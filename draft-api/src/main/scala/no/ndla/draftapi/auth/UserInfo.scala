/*
 * Part of NDLA draft-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.auth

import no.ndla.network.AuthUser

case class UserInfo(id: String, roles: Set[Role.Value]) {
  def canPublish: Boolean = hasRoles(UserInfo.PublishRoles)
  def canWrite: Boolean = hasRoles(UserInfo.WriteRoles)
  def canRead: Boolean = hasRoles(UserInfo.ReadRoles)

  def hasRoles(rolesToCheck: Set[Role.Value]): Boolean = rolesToCheck.subsetOf(roles)
}

object UserInfo {
  val UnauthorizedUser = UserInfo("unauthorized", Set.empty)

  val PublishRoles = Set(Role.WRITE, Role.PUBLISH)
  val DirectPublishRoles = PublishRoles + Role.ADMIN
  val WriteRoles = Set(Role.WRITE)
  val ReadRoles = Set(Role.WRITE)

  def apply(name: String): UserInfo = UserInfo(name, AuthUser.getRoles.flatMap(Role.valueOf).toSet)

  def get: Option[UserInfo] = (AuthUser.get orElse AuthUser.getClientId).map(UserInfo.apply)
}

trait User {
  val user: User

  class User {
    def getUser: UserInfo = UserInfo.get.getOrElse(UserInfo.UnauthorizedUser)
  }
}
