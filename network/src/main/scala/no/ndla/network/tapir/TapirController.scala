/*
 * Part of NDLA network
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */
package no.ndla.network.tapir

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.Clock
import no.ndla.common.configuration.HasBaseProps
import sttp.client3.Identity
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint

trait TapirController {
  this: HasBaseProps & Clock =>
  trait TapirController extends StrictLogging {
    type Eff[A] = Identity[A]
    val enableSwagger: Boolean = true
    val serviceName: String    = this.getClass.getSimpleName
    protected val prefix: EndpointInput[Unit]
    val endpoints: List[ServerEndpoint[Any, Eff]]

    lazy val builtEndpoints: List[ServerEndpoint[Any, Eff]] = {
      this.endpoints.map(e => {
        ServerEndpoint(
          endpoint = e.endpoint.prependIn(this.prefix).tag(this.serviceName),
          securityLogic = e.securityLogic,
          logic = e.logic
        )
      })
    }
  }

}
