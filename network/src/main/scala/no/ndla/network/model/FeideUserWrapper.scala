/*
 * Part of NDLA network
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.model

import no.ndla.common.model.domain.myndla.MyNDLAUser
import no.ndla.common.implicits.toTry
import no.ndla.network.clients.FeideApiClient

import scala.util.Try

case class FeideUserWrapper(token: FeideAccessToken, user: Option[MyNDLAUser]) {
  def userOrAccessDenied: Try[MyNDLAUser] = user.toTry(FeideApiClient.accessDeniedException)
}

extension (maybeUser: Option[FeideUserWrapper]) {
  def userOrAccessDenied: Try[MyNDLAUser] = maybeUser.flatMap(_.user).toTry(FeideApiClient.accessDeniedException)

}
