/*
 * Part of NDLA draft-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.controller

import no.ndla.draftapi.DraftApiProperties
import no.ndla.network.tapir.auth.Permission
import no.ndla.network.tapir.SwaggerInfo
import sttp.tapir.*

object SwaggerDocControllerConfig {
  def swaggerInfo(using props: DraftApiProperties): SwaggerInfo = {
    val scopes = Permission.toSwaggerMap(Permission.thatStartsWith("drafts"))

    SwaggerInfo(
      mountPoint = "draft-api" / "api-docs",
      description = "Services for accessing draft articles, draft and agreements.",
      authUrl = props.Auth0LoginEndpoint,
      scopes = scopes,
    )
  }
}
