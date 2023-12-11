/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.controller

import no.ndla.myndlaapi.Props
import no.ndla.network.tapir.auth.Permission
import no.ndla.network.tapir.{SwaggerControllerConfig, SwaggerInfo}
import sttp.tapir._

trait SwaggerDocControllerConfig extends SwaggerControllerConfig {
  this: Props =>

  object SwaggerDocControllerConfig {
    private val scopes = Permission.toSwaggerMap(List.empty)

    val swaggerInfo: SwaggerInfo = SwaggerInfo(
      mountPoint = "myndla-api" / "api-docs",
      description = "NDLA API to manage users and groups related to MyNDLA",
      authUrl = props.Auth0LoginEndpoint,
      scopes = scopes
    )
  }
}
