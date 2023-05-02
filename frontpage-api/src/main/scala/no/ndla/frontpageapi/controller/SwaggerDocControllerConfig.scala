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
      description = "Service for fetching frontpage data",
      authUrl = props.Auth0LoginEndpoint,
      scopes = scopes
    )
  }
}
