/*
 * Part of NDLA search-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.controller

import no.ndla.network.tapir.auth.Permission
import no.ndla.network.tapir.{SwaggerControllerConfig, SwaggerInfo}
import no.ndla.searchapi.Props
import sttp.tapir.*

trait SwaggerDocControllerConfig extends SwaggerControllerConfig {
  this: Props =>

  object SwaggerDocControllerConfig {
    private val scopes = Permission.toSwaggerMap(List.empty)

    val swaggerInfo: SwaggerInfo = SwaggerInfo(
      mountPoint = "search-api" / "api-docs",
      description = "A common endpoint for searching across article, draft, learningpath, image and audio APIs.\n\n" +
        "The Search API provides a common endpoint for searching across the article, draft, learningpath, image and audio APIs. " +
        "The search does a free text search in data and metadata. It is also possible to search targeted at specific " +
        "meta-data fields like language or license.\n" +
        "Note that the query parameter is based on the Elasticsearch simple search language. For more information, see " +
        "https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-simple-query-string-query.html",
      authUrl = props.Auth0LoginEndpoint,
      scopes = scopes
    )
  }
}
