/*
 * Part of NDLA network
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */
package no.ndla.network.tapir

import sttp.tapir._
import sttp.tapir.server.ServerEndpoint

trait Service[F[_]] {
  val enableSwagger: Boolean = true
  val serviceName: String    = this.getClass.getSimpleName
  protected val prefix: EndpointInput[Unit]
  val endpoints: List[ServerEndpoint[Any, F]]

  lazy val builtEndpoints: List[ServerEndpoint[Any, F]] = {
    this.endpoints.map(e => {
      ServerEndpoint(
        endpoint = e.endpoint.prependIn(this.prefix).tag(this.serviceName),
        securityLogic = e.securityLogic,
        logic = e.logic
      )
    })
  }
}
