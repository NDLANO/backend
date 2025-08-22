/*
 * Part of NDLA network
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir

import no.ndla.common.configuration.BaseProps

trait TapirApplication[PropType <: BaseProps] {
  given props: PropType
  given routes: Routes
  given swagger: SwaggerController
  given healthController: TapirHealthController
}
