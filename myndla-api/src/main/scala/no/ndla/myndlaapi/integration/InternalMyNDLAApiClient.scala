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
import no.ndla.myndlaapi.service.UserService

import scala.util.Try

class InternalMyNDLAApiClient(using userService: UserService) extends MyNDLAProvider {
  def getUserWithFeideToken(feideToken: String): Try[MyNDLAUserDTO] = {
    userService.getMyNdlaUserDataDTO(Some(feideToken))
  }

  override def getDomainUser(feideToken: String): Try[MyNDLAUser] = {
    userService.getMyNdlaUserDataDomain(Some(feideToken))
  }
}
