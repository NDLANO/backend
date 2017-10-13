/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi

import org.scalatra.ScalatraServlet
import org.scalatra.swagger._

class ResourcesApp(implicit val swagger: Swagger) extends ScalatraServlet with NativeSwaggerBase {
  get("/") {
    renderSwagger2(swagger.docs.toList)
  }
}

object AudioApiInfo {
  val apiInfo = ApiInfo(
    "Audio Api",
    "Documentation for the Audio API of NDLA.no",
    "http://ndla.no",
    AudioApiProperties.ContactEmail,
    "GPL v3.0",
    "http://www.gnu.org/licenses/gpl-3.0.en.html")
}

class AudioSwagger extends Swagger("2.0", "0.8", AudioApiInfo.apiInfo) {
  addAuthorization(OAuth(List("openid"), List(ApplicationGrant(TokenEndpoint("https://ndla.eu.auth0.com/oauth/token", "access_token")))))
}
