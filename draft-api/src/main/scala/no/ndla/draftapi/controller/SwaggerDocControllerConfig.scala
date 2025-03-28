/*
 * Part of NDLA draft-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.controller

import no.ndla.draftapi.Props
import no.ndla.network.tapir.auth.Permission
import no.ndla.network.tapir.{SwaggerControllerConfig, SwaggerInfo}
import sttp.tapir.*

trait SwaggerDocControllerConfig {
  this: Props & SwaggerControllerConfig =>

  object SwaggerDocControllerConfig {
    private val scopes = Permission.toSwaggerMap(Permission.thatStartsWith("drafts"))

    val swaggerInfo: SwaggerInfo = SwaggerInfo(
      mountPoint = "draft-api" / "api-docs",
      description = "Services for accessing draft articles, draft and agreements.",
      authUrl = props.Auth0LoginEndpoint,
      scopes = scopes
    )
  }
}
