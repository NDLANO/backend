/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.model

import cats.effect.{IO, IOLocal}
import no.ndla.common.CorrelationID
import no.ndla.common.logging.{LoggerContext, LoggerInfo}
import no.ndla.network.{ApplicationUrl, AuthUser, TaxonomyData}
import org.http4s.Request

import javax.servlet.http.HttpServletRequest

/** Helper class to help keep Thread specific request information in futures. */
case class RequestInfo(
    correlationId: Option[String],
    authUser: AuthUser,
    taxonomyVersion: String,
    applicationUrl: String
) {
  def setRequestInfo(): IO[Unit] = {
    TaxonomyData.set(taxonomyVersion)
    authUser.setThreadContext()
    CorrelationID.set(correlationId)
    ApplicationUrl.set(applicationUrl)
    RequestInfo.set(this)
  }

}

object RequestInfo {
  private val requestLocalState = {
    import cats.effect.unsafe.implicits.global
    IOLocal(None: Option[RequestInfo]).unsafeRunSync()
  }
  private val accessOutsideContextError = new IllegalStateException(
    "Tried to access IOLocal `RequestInfo` outside somewhere with context."
  )

  def get: IO[RequestInfo] = requestLocalState.get.flatMap {
    case Some(value) => IO.pure(value)
    case None        => IO.raiseError(accessOutsideContextError)
  }

  def set(v: RequestInfo): IO[Unit] = requestLocalState.set(Some(v))
  def reset: IO[Unit]               = requestLocalState.reset

  /** Implicit context used to derive required [[LoggerInfo]] */
  implicit val ioLoggerContext: LoggerContext[IO] = new LoggerContext[IO] {
    override def get: IO[LoggerInfo] = RequestInfo.get.map(info => LoggerInfo(correlationId = info.correlationId))
    override def map[T](f: LoggerInfo => T): IO[T] = get.map(f)
  }

  def fromRequest(request: HttpServletRequest): RequestInfo = {
    val ndlaRequest = NdlaHttpRequest(request)
    new RequestInfo(
      correlationId = CorrelationID.fromRequest(request),
      authUser = AuthUser.fromRequest(ndlaRequest),
      taxonomyVersion = TaxonomyData.fromRequest(ndlaRequest),
      applicationUrl = ApplicationUrl.fromRequest(ndlaRequest)
    )
  }

  def fromRequest(request: Request[IO]): RequestInfo = {
    val ndlaRequest = NdlaHttpRequest.from(request)
    new RequestInfo(
      correlationId = Some(CorrelationID.fromRequest(request)),
      authUser = AuthUser.fromRequest(ndlaRequest),
      taxonomyVersion = TaxonomyData.fromRequest(ndlaRequest),
      applicationUrl = ApplicationUrl.fromRequest(ndlaRequest)
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
    reset
    TaxonomyData.clear()
    CorrelationID.clear()
    AuthUser.clear()
    ApplicationUrl.clear()
  }

}
