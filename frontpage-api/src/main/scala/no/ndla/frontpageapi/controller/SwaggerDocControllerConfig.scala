/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.controller

import no.ndla.frontpageapi.Props
import no.ndla.network.tapir.auth.Scope
import no.ndla.network.tapir.{Service, SwaggerControllerConfig, SwaggerInfo}

trait SwaggerDocControllerConfig extends SwaggerControllerConfig {
  this: Service with Props =>

  object SwaggerDocControllerConfig {
    private val scopes = Scope.toSwaggerMap(Scope.thatStartsWith("frontpage"))

    val swaggerInfo: SwaggerInfo = SwaggerInfo(
      mountPoint = "/frontpage-api/api-docs",
      description = "Service for fetching frontpage data",
      authUrl = props.Auth0LoginEndpoint,
      scopes = scopes
    )
  }
}
