/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.articleapi.controller

import no.ndla.articleapi.Props
import no.ndla.network.tapir.auth.Permission
import no.ndla.network.tapir.{SwaggerControllerConfig, SwaggerInfo}
import sttp.tapir.*

trait SwaggerDocControllerConfig {
  this: Props & SwaggerControllerConfig =>

  object SwaggerDocControllerConfig {
    private val scopes = Permission.toSwaggerMap(
      Permission.thatStartsWith("articles") :+ Permission.DRAFT_API_WRITE
    )

    val swaggerInfo: SwaggerInfo = SwaggerInfo(
      mountPoint = "article-api" / "api-docs",
      description = "Searching and fetching all articles published on the NDLA platform.\n\n" +
        "The Article API provides an endpoint for searching and fetching articles. Different meta-data is attached to the " +
        "returned articles, and typical examples of this are language and license.\n" +
        "Includes endpoints to filter Articles on different levels, and retrieve single articles.",
      authUrl = props.Auth0LoginEndpoint,
      scopes = scopes
    )
  }
}
