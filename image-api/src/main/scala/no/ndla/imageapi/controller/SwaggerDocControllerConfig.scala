/*
 * Part of NDLA image-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.imageapi.Props
import no.ndla.network.tapir.auth.Permission
import no.ndla.network.tapir.SwaggerInfo
import sttp.tapir.*

object SwaggerDocControllerConfig {
  private val scopes = Permission.toSwaggerMap(Permission.thatStartsWith("concept"))

  def swaggerInfo(using props: Props): SwaggerInfo = SwaggerInfo(
    mountPoint = "image-api" / "api-docs",
    description = "Searching and fetching all images used in the NDLA platform.\n\n" +
      "The Image API provides an endpoint for searching in and fetching images used in NDLA resources. Meta-data are " +
      "also searched and returned in the results. Examples of meta-data are title, alt-text, language and license.\n" +
      "The API can resize and crop transitions on the returned images to enable use in special contexts, e.g. " +
      "low bandwidth scenarios",
    authUrl = props.Auth0LoginEndpoint,
    scopes = scopes,
  )
}
