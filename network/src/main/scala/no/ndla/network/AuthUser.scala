/*
 * Part of NDLA network.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.network

import javax.servlet.http.HttpServletRequest
import no.ndla.network.jwt.JWTExtractor
import no.ndla.network.model.NdlaHttpRequest

object AuthUser {

  def getAuth0HostForEnv(env: String): String = {
    Map(
      "prod" -> "ndla.eu.auth0.com",
      "ff" -> "ndla.eu.auth0.com",
      "staging" -> "ndla-staging.eu.auth0.com",
      "test" -> "ndla-test.eu.auth0.com",
      "local" -> "ndla-test.eu.auth0.com"
    ).withDefaultValue("ndla-test.eu.auth0.com")(env)
  }

  private val userId = ThreadLocal.withInitial[Option[String]](() => None)
  private val userRoles = ThreadLocal.withInitial[List[String]](() => List.empty)
  private val userName = ThreadLocal.withInitial[Option[String]](() => None)
  private val clientId = ThreadLocal.withInitial[Option[String]](() => None)
  private val authHeader = ThreadLocal.withInitial[Option[String]](() => None)

  def set(request: HttpServletRequest): Unit = set(NdlaHttpRequest(request))

  def set(request: NdlaHttpRequest): Unit = {
    val jWTExtractor = new JWTExtractor(request)
    jWTExtractor.extractUserId().foreach(setId)
    setRoles(jWTExtractor.extractUserRoles())
    jWTExtractor.extractUserName().foreach(setName)
    jWTExtractor.extractClientId().foreach(setClientId)
    request.getHeader("Authorization").foreach(setHeader)
  }

  def setId(user: String): Unit = userId.set(Option(user))
  def setRoles(roles: List[String]): Unit = userRoles.set(roles)
  def setName(name: String): Unit = userName.set(Option(name))
  def setClientId(client: String): Unit = clientId.set(Option(client))
  def setHeader(header: String): Unit = authHeader.set(Option(header))

  def get: Option[String] = userId.get
  def getRoles: List[String] = userRoles.get
  def getName: Option[String] = userName.get
  def getClientId: Option[String] = clientId.get
  def getHeader: Option[String] = authHeader.get

  def hasRole(role: String): Boolean = getRoles.contains(role)

  def clear(): Unit = {
    userId.remove()
    userRoles.remove()
    userName.remove()
    clientId.remove()
    authHeader.remove()
  }
}
