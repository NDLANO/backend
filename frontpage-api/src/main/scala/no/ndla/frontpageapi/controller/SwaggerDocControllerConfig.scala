/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.controller

import no.ndla.frontpageapi.Props
import no.ndla.frontpageapi.auth.Role
import no.ndla.network.tapir.{Service, SwaggerControllerConfig, SwaggerInfo}

import scala.collection.immutable.ListMap

trait SwaggerDocControllerConfig extends SwaggerControllerConfig {
  this: Service with Props =>

  object SwaggerDocControllerConfig {
    private val scopes = ListMap.from(Role.values.map(role => {
      val fullRole = s"${Role.prefix}$role".toLowerCase
      fullRole -> fullRole
    }))

    val swaggerInfo: SwaggerInfo = SwaggerInfo(
      mountPoint = "/frontpage-api/api-docs",
      description = "Searching and fetching all audio used in the NDLA platform.\n\n" +
        "The Audio API provides an endpoint for searching and fetching audio used in NDLA resources. " +
        "Meta-data like title, tags, language and license are searchable and also provided in the results. " +
        "The media file is provided as an URL with the mime type.",
      authUrl = props.Auth0LoginEndpoint,
      scopes = scopes
    )
  }
}
