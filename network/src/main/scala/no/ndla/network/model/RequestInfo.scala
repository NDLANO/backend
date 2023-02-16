/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.model

import no.ndla.common.CorrelationID
import no.ndla.network.{ApplicationUrl, AuthUser, TaxonomyData}

import javax.servlet.http.HttpServletRequest

/** Helper class to help keep Thread specific request information in futures. */
case class RequestInfo(
    correlationId: Option[String],
    authUser: AuthUser,
    taxonomyVersion: String,
    applicationUrl: String
) {
  def setRequestInfo(): Unit = {
    TaxonomyData.set(taxonomyVersion)
    authUser.setThreadContext()
    CorrelationID.set(correlationId)
    ApplicationUrl.set(applicationUrl)
  }

}

object RequestInfo {

  def fromRequest(request: HttpServletRequest): RequestInfo = {
    new RequestInfo(
      correlationId = CorrelationID.fromRequest(request),
      authUser = AuthUser.fromRequest(request),
      taxonomyVersion = TaxonomyData.getFromRequest(request),
      applicationUrl = ApplicationUrl.fromRequest(request)
    )
  }

  def fromThreadContext(): RequestInfo = {
    val correlationId   = CorrelationID.get
    val authUser        = AuthUser.fromThreadContext()
    val taxonomyVersion = TaxonomyData.get
    val applicationUrl  = ApplicationUrl.get

    new RequestInfo(correlationId, authUser, taxonomyVersion, applicationUrl)
  }

  def clear(): Unit = {
    TaxonomyData.clear()
    CorrelationID.clear()
    AuthUser.clear()
    ApplicationUrl.clear()
  }

}
