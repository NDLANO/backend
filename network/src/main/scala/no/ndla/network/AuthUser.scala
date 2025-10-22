/*
 * Part of NDLA network
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network

import no.ndla.network.jwt.JWTExtractor
import no.ndla.network.model.NdlaHttpRequest

case class AuthUser(
    userId: Option[String],
    userRoles: List[String],
    userName: Option[String],
    clientId: Option[String],
    authHeader: Option[String],
) {
  def setThreadContext(): Unit = {
    userId.foreach(AuthUser.setId)
    AuthUser.setRoles(userRoles)
    userName.foreach(AuthUser.setName)
    clientId.foreach(AuthUser.setClientId)
    authHeader.foreach(AuthUser.setHeader)
  }
}

object AuthUser {

  def getAuth0HostForEnv(env: String): String = {
    Map(
      "prod"    -> "ndla.eu.auth0.com",
      "ff"      -> "ndla.eu.auth0.com",
      "staging" -> "ndla-staging.eu.auth0.com",
      "test"    -> "ndla-test.eu.auth0.com",
      "local"   -> "ndla-test.eu.auth0.com",
    ).withDefaultValue("ndla-test.eu.auth0.com")(env)
  }

  private val userId     = ThreadLocal.withInitial[Option[String]](() => None)
  private val userRoles  = ThreadLocal.withInitial[List[String]](() => List.empty)
  private val userName   = ThreadLocal.withInitial[Option[String]](() => None)
  private val clientId   = ThreadLocal.withInitial[Option[String]](() => None)
  private val authHeader = ThreadLocal.withInitial[Option[String]](() => None)

  def fromRequest(request: NdlaHttpRequest): AuthUser = {
    val jWTExtractor = JWTExtractor(request)
    new AuthUser(
      userId = jWTExtractor.extractUserId(),
      userRoles = jWTExtractor.extractUserRoles(),
      userName = jWTExtractor.extractUserName(),
      clientId = jWTExtractor.extractClientId(),
      authHeader = request.getHeader("Authorization"),
    )
  }

  def fromThreadContext(): AuthUser = {
    new AuthUser(
      userId = AuthUser.get,
      userRoles = AuthUser.getRoles,
      userName = AuthUser.getName,
      clientId = AuthUser.getClientId,
      authHeader = AuthUser.getHeader,
    )
  }

  def set(request: NdlaHttpRequest): Unit = fromRequest(request).setThreadContext()

  private def setId(user: String): Unit           = userId.set(Option(user))
  private def setRoles(roles: List[String]): Unit = userRoles.set(roles)
  private def setName(name: String): Unit         = userName.set(Option(name))
  private def setClientId(client: String): Unit   = clientId.set(Option(client))
  def setHeader(header: String): Unit             = authHeader.set(Option(header))

  def get: Option[String]         = userId.get
  def getRoles: List[String]      = userRoles.get
  def getName: Option[String]     = userName.get
  def getClientId: Option[String] = clientId.get
  def getHeader: Option[String]   = authHeader.get

  def hasRole(role: String): Boolean = getRoles.contains(role)

  def clear(): Unit = {
    userId.remove()
    userRoles.remove()
    userName.remove()
    clientId.remove()
    authHeader.remove()
  }
}
