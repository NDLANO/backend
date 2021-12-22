/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.auth

case class UserInfo(id: String, roles: Set[Role.Value]) {
  def canWrite: Boolean = hasRoles(Set(Role.WRITE))

  def hasRoles(rolesToCheck: Set[Role.Value]): Boolean = rolesToCheck.subsetOf(roles)

}
