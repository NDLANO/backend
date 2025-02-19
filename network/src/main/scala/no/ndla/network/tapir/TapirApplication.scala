/*
 * Part of NDLA network
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir

import no.ndla.common.Clock
import no.ndla.common.configuration.HasBaseProps
import no.ndla.network.NdlaClient
import no.ndla.network.clients.MyNDLAApiClient

trait TapirApplication
    extends TapirController
    with MyNDLAApiClient
    with NdlaClient
    with TapirErrorHandling
    with Clock
    with HasBaseProps
    with Routes
    with SwaggerControllerConfig
    with TapirHealthController
