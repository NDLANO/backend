/*
 * Part of NDLA network
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir

import no.ndla.common.configuration.BaseProps
import no.ndla.network.clients.MyNDLAProvider
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint

/** Re-exposes another controller's endpoints under a different prefix as a transitional alias while callers migrate to
  * the delegate's canonical prefix. Excluded from swagger docs and filtered out by the monolith merge so the alias only
  * exists when an *-api runs standalone. Remove the alias once all known callers have switched to the canonical prefix.
  */
class LegacyPrefixAlias(delegate: TapirController, legacyPrefix: String)(using
    myNDLAApiClient: MyNDLAProvider,
    errorHelpers: ErrorHelpers,
    errorHandling: ErrorHandling,
    props: BaseProps,
) extends TapirController {
  override val prefix: EndpointInput[Unit]               = legacyPrefix
  override val endpoints: List[ServerEndpoint[Any, Eff]] = delegate.endpoints
  override val enableSwagger: Boolean                    = false
  override val serviceName: String                       = s"${delegate.serviceName}LegacyAlias"
}
