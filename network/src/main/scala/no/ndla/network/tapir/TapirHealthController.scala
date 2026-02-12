/*
 * Part of NDLA network
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir

import no.ndla.network.clients.MyNDLAProvider
import no.ndla.network.model.ServerStatus
import sttp.model.StatusCode
import sttp.tapir.EndpointInput
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.*

import java.util.concurrent.atomic.AtomicReference

class TapirHealthController(using
    myNDLAApiClient: MyNDLAProvider,
    errorHelpers: ErrorHelpers,
    errorHandling: ErrorHandling,
) extends TapirController {
  private val serverStatus: AtomicReference[ServerStatus] = AtomicReference(ServerStatus.Starting)
  override val enableSwagger: Boolean                     = false
  val prefix: EndpointInput[Unit]                         = "health"

  def setRunning(): Unit  = serverStatus.compareAndSet(ServerStatus.Starting, ServerStatus.Running): Unit
  def setStopping(): Unit = serverStatus.set(ServerStatus.Stopping): Unit

  override def handleErrors: PartialFunction[Throwable, AllErrors] = { case e: Throwable =>
    errorHelpers.generic
  }

  private def checkLiveness(): Either[String, String] = Right("Healthy")

  protected def checkAppReadiness(): Either[String, String] = Right("Ready")

  private def checkReadiness(): Either[String, String] = serverStatus.get() match {
    case ServerStatus.Starting => Left("Service is starting up")
    case ServerStatus.Running  => checkAppReadiness()
    case ServerStatus.Stopping => Left("Service is shutting down")
  }

  override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
    endpoint
      .get
      .description("Readiness probe. Returns 200 if the service is ready to serve traffic.")
      .in("readiness")
      .out(stringBody)
      .errorOut(statusCode(StatusCode.InternalServerError).and(stringBody))
      .serverLogicPure(_ => checkReadiness()),
    endpoint
      .get
      .description("Liveness probe. Returns 200 if the service is alive, but not necessarily ready.")
      .in("liveness")
      .out(stringBody)
      .errorOut(statusCode(StatusCode.InternalServerError).and(stringBody))
      .serverLogicPure(_ => checkLiveness()),
    endpoint
      .get
      .out(stringBody)
      .errorOut(statusCode(StatusCode.InternalServerError).and(stringBody))
      .serverLogicPure(_ => checkLiveness()),
  )
}
