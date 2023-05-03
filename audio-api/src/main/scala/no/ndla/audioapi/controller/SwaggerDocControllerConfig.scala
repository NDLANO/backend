/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.controller

import no.ndla.audioapi.Props
import no.ndla.network.tapir.{Service, SwaggerControllerConfig, SwaggerInfo}

import scala.collection.immutable.ListMap

trait SwaggerDocControllerConfig extends SwaggerControllerConfig {
  this: Service with Props =>

  object SwaggerDocControllerConfig {
    private val scopes = ListMap.from(props.roles.map(role => role -> role))

    val swaggerInfo: SwaggerInfo = SwaggerInfo(
      mountPoint = "/audio-api/api-docs",
      description = "Service for fetching frontpage data",
      authUrl = props.Auth0LoginEndpoint,
      scopes = scopes
    )
  }
}
