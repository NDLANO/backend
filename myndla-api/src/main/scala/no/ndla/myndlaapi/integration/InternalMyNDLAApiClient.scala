/*
 * Part of NDLA myndla-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.integration

import no.ndla.network.clients.MyNDLAProvider
import no.ndla.common.model.api.myndla.MyNDLAUserDTO
import no.ndla.common.model.domain.myndla.MyNDLAUser

import scala.util.Try

trait InternalMyNDLAApiClient extends MyNDLAProvider {
  def getUserWithFeideToken(feideToken: String): Try[MyNDLAUserDTO]
  override def getDomainUser(feideToken: String): Try[MyNDLAUser]
}
