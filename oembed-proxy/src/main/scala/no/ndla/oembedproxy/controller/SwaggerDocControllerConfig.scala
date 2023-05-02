/*
 * Part of NDLA oembed-proxy
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.oembedproxy.controller

import no.ndla.network.tapir.{Service, SwaggerControllerConfig, SwaggerInfo}
import no.ndla.oembedproxy.Props

import scala.collection.immutable.ListMap

trait SwaggerDocControllerConfig extends SwaggerControllerConfig {
  this: Service with Props =>

  object SwaggerDocControllerConfig {
    val swaggerInfo: SwaggerInfo = SwaggerInfo(
      mountPoint = "/oembed-proxy/api-docs",
      description = "Convert any NDLA resource to an oEmbed embeddable resource.",
      authUrl = props.Auth0LoginEndpoint,
      scopes = ListMap.empty
    )
  }
}
