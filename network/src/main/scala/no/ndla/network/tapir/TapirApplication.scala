/*
 * Part of NDLA backend.network.main
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.tapir

import no.ndla.common.Clock
import no.ndla.common.configuration.HasBaseProps

trait TapirApplication
    extends TapirController
    with TapirErrorHandling
    with Clock
    with HasBaseProps
    with Routes
    with SwaggerControllerConfig
    with TapirHealthController
