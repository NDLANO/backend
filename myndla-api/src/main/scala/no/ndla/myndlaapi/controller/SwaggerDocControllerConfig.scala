/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.controller

import no.ndla.myndlaapi.Props
import no.ndla.network.tapir.auth.Permission
import no.ndla.network.tapir.SwaggerInfo
import sttp.tapir.*

object SwaggerDocControllerConfig {
  private val scopes = Permission.toSwaggerMap(List.empty)

  def swaggerInfo(using props: Props): SwaggerInfo = SwaggerInfo(
    mountPoint = "myndla-api" / "api-docs",
    description = "NDLA API to manage users and groups related to MyNDLA",
    authUrl = props.Auth0LoginEndpoint,
    scopes = scopes
  )
}
