/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.controller

import no.ndla.frontpageapi.Props
import no.ndla.network.tapir.auth.Permission
import no.ndla.network.tapir.SwaggerInfo
import sttp.tapir.*

object SwaggerDocControllerConfig {
  private val scopes = Permission.toSwaggerMap(Permission.thatStartsWith("frontpage"))

  def swaggerInfo(using props: Props): SwaggerInfo = SwaggerInfo(
    mountPoint = "frontpage-api" / "api-docs",
    description = "Service for fetching frontpage data",
    authUrl = props.Auth0LoginEndpoint,
    scopes = scopes
  )
}
