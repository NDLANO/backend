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

import no.ndla.common.Clock
import no.ndla.common.configuration.HasBaseProps
import no.ndla.network.NdlaClient
import no.ndla.network.clients.MyNDLAApiClient

class TapirApplication(
    tapirController: TapirController,
    myNDLAApiClient: MyNDLAApiClient,
    ndlaClient: NdlaClient,
    tapirErrorHandling: TapirErrorHandling,
    clock: Clock,
    baseProps: HasBaseProps,
    routes: Routes,
    swaggerControllerConfig: SwaggerControllerConfig,
    tapirHealthController: TapirHealthController,
    val swagger: SwaggerController
)
