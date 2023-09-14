/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.Props
import no.ndla.network.tapir.auth.Permission
import no.ndla.network.tapir.{SwaggerControllerConfig, SwaggerInfo}
import sttp.tapir._

trait SwaggerDocControllerConfig extends SwaggerControllerConfig {
  this: Props =>

  object SwaggerDocControllerConfig {
    private val scopes = Permission.toSwaggerMap(Permission.thatStartsWith("audio"))

    val swaggerInfo: SwaggerInfo = SwaggerInfo(
      mountPoint = "audio-api" / "api-docs",
      description = "Searching and fetching all audio used in the NDLA platform.\n\n" +
        "The Audio API provides an endpoint for searching and fetching audio used in NDLA resources. " +
        "Meta-data like title, tags, language and license are searchable and also provided in the results. " +
        "The media file is provided as an URL with the mime type.",
      authUrl = props.Auth0LoginEndpoint,
      scopes = scopes
    )
  }
}
