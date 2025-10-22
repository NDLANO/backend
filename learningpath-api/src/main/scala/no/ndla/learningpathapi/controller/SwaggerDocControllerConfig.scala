/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.controller

import no.ndla.learningpathapi.Props
import no.ndla.network.tapir.auth.Permission
import no.ndla.network.tapir.SwaggerInfo
import sttp.tapir.*

object SwaggerDocControllerConfig {
  private val scopes = Permission.toSwaggerMap(Permission.thatStartsWith("learningpath"))

  def swaggerInfo(using props: Props): SwaggerInfo = SwaggerInfo(
    mountPoint = "learningpath-api" / "api-docs",
    description = "Services for accessing learningpaths",
    authUrl = props.Auth0LoginEndpoint,
    scopes = scopes,
  )
}
