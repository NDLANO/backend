/*
 * Part of NDLA network
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.model

import no.ndla.common.CorrelationID
import no.ndla.network.{ApplicationUrl, AuthUser, TaxonomyData}
import sttp.tapir.model.ServerRequest

/** Helper class to help keep Thread specific request information in futures. */
case class RequestInfo(
    correlationId: Option[String],
    authUser: AuthUser,
    taxonomyVersion: String,
    applicationUrl: String,
) {
  def setThreadContextRequestInfo(): Unit = {
    TaxonomyData.set(taxonomyVersion)
    authUser.setThreadContext()
    CorrelationID.set(correlationId)
    ApplicationUrl.set(applicationUrl)
  }
}

object RequestInfo {
  def fromRequest(request: ServerRequest): RequestInfo = {
    val ndlaRequest = NdlaHttpRequest.from(request)
    new RequestInfo(
      correlationId = Some(CorrelationID.fromRequest(request)),
      authUser = AuthUser.fromRequest(ndlaRequest),
      taxonomyVersion = TaxonomyData.fromRequest(ndlaRequest),
      applicationUrl = ApplicationUrl.fromRequest(ndlaRequest),
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
