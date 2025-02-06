/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.controller

import no.ndla.conceptapi.Props
import no.ndla.network.tapir.auth.Permission
import no.ndla.network.tapir.{SwaggerControllerConfig, SwaggerInfo}
import sttp.tapir.*

trait SwaggerDocControllerConfig {
  this: Props & SwaggerControllerConfig =>

  object SwaggerDocControllerConfig {
    private val scopes = Permission.toSwaggerMap(Permission.thatStartsWith("concept"))

    val swaggerInfo: SwaggerInfo = SwaggerInfo(
      mountPoint = "concept-api" / "api-docs",
      description = "Services for accessing concepts",
      authUrl = props.Auth0LoginEndpoint,
      scopes = scopes
    )
  }
}
