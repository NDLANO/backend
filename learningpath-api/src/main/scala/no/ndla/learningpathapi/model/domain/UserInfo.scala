/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import no.ndla.common.auth.Permission
import no.ndla.common.errors.AccessDeniedException
import no.ndla.network.model.CombinedUser
import no.ndla.common.auth.Permission.{LEARNINGPATH_API_ADMIN, LEARNINGPATH_API_PUBLISH, LEARNINGPATH_API_WRITE}
import no.ndla.network.tapir.auth.TokenUser

import scala.util.{Failure, Success}
import scala.util.Try

object UserInfo {
  def getWithUserIdOrAdmin(user: CombinedUser): Try[CombinedUser] = user match {
    case user if user.isAdmin                                    => Success(user)
    case user if user.tokenUser.flatMap(_.jwt.ndla_id).isDefined => Success(user)
    case _                                                       => Failure(AccessDeniedException("You do not have access to the requested resource."))
  }

  extension (self: TokenUser) {
    def isAdmin: Boolean     = self.permissions.contains(LEARNINGPATH_API_ADMIN)
    def isPublisher: Boolean = self.permissions.contains(LEARNINGPATH_API_PUBLISH)
    def isWriter: Boolean    = self.permissions.contains(LEARNINGPATH_API_WRITE)
    def canWrite: Boolean    = isAdmin || isPublisher || isWriter
    def canPublish: Boolean  = isAdmin || isPublisher
    def isNdla: Boolean      = self.permissions.nonEmpty
  }

  extension (self: CombinedUser) {
    def id: Option[String] = {
      val tokenId = self.tokenUser.map(_.id)
      val feideId = self.myndlaUser.map(_.user.feideId)
      tokenId.orElse(feideId)
    }

    def permissions: Set[Permission] = self.tokenUser.map(_.permissions).getOrElse(Set.empty)
    def isAdmin: Boolean             = self.permissions.contains(LEARNINGPATH_API_ADMIN)
    def isPublisher: Boolean         = self.permissions.contains(LEARNINGPATH_API_PUBLISH)
    def isWriter: Boolean            = self.permissions.contains(LEARNINGPATH_API_WRITE)
    def canWrite: Boolean            = self.isEmployee || isAdmin || isPublisher || isWriter
    def canPublish: Boolean          = isAdmin || isPublisher
    def isNdla: Boolean              = self.permissions.nonEmpty
  }
}
