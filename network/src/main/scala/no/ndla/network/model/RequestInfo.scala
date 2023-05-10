/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.model

import cats.effect.IO.{IOCont, Uncancelable}
import cats.effect.{IO, IOLocal}
import no.ndla.common.CorrelationID
import no.ndla.network.{ApplicationUrl, AuthUser, TaxonomyData}
import org.http4s.Request
import org.log4s.MDC
import sttp.tapir.model.ServerRequest
import sttp.tapir.server.interceptor.{EndpointInterceptor, RequestHandler, RequestInterceptor, Responder}

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
  private val localCid = {
    import cats.effect.unsafe.implicits.global
    IOLocal(None: Option[RequestInfo]).unsafeRunSync()
  }

  def get: IO[RequestInfo] = {
    // TODO: is there a smart way to deal with this pattern? (flatMap into IO.raiseError + IO.pure)
    //       i assume it happens quite a bit
    localCid.get.flatMap {
      case Some(value) => IO.pure(value)
      case None =>
        IO.raiseError(
          new IllegalStateException("Tried to access IOLocal `RequestInfo` outside somewhere with context.")
        )
    }
  }
  def set(v: RequestInfo): IO[Unit] = localCid.set(Some(v))
  def reset: IO[Unit]               = localCid.reset

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

  def fromRequest(request: ServerRequest): RequestInfo = {
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

object RequestInfoInterceptor extends RequestInterceptor[IO] {
  override def apply[R, B](
      responder: Responder[IO, B],
      requestHandler: EndpointInterceptor[IO] => RequestHandler[IO, R, B]
  ): RequestHandler[IO, R, B] =
    RequestHandler.from { case (request, endpoints, monad) =>
      val reqInfo = RequestInfo.fromRequest(request)
      val set     = RequestInfo.set(reqInfo)
      set >> requestHandler(EndpointInterceptor.noop)(request, endpoints)(monad)
    }
}
