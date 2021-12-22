/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain

import no.ndla.network.{AuthUser, CorrelationID}
import no.ndla.searchapi.SearchApiProperties
import org.apache.logging.log4j.ThreadContext

/** Helper class to help keep Thread specific request information in futures. */
case class RequestInfo(CorrelationId: Option[String],
                       AuthHeader: Option[String],
                       UserId: Option[String],
                       Roles: List[String],
                       Name: Option[String],
                       ClientId: Option[String]) {

  def setRequestInfo(): Unit = {
    ThreadContext.put(SearchApiProperties.CorrelationIdKey, CorrelationId.getOrElse(""))
    CorrelationID.set(CorrelationId)
    AuthUser.setHeader(AuthHeader.getOrElse(""))
    UserId.foreach(AuthUser.setId)
    AuthUser.setRoles(Roles)
    Name.foreach(AuthUser.setName)
    ClientId.foreach(AuthUser.setClientId)
  }
}

object RequestInfo {

  def apply(): RequestInfo = {
    val correlationId = CorrelationID.get
    val authHeader = AuthUser.getHeader
    val userId = AuthUser.get
    val roles = AuthUser.getRoles
    val name = AuthUser.getName
    val clientId = AuthUser.getClientId

    RequestInfo(correlationId, authHeader, userId, roles, name, clientId)
  }
}
