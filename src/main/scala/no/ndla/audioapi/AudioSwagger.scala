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

  val apiInfo = ApiInfo("Audio API",
                        "Services for accessing audio",
                        "http://ndla.no",
                        AudioApiProperties.ContactEmail,
                        "GPL v3.0",
                        "http://www.gnu.org/licenses/gpl-3.0.en.html")
}

class AudioSwagger extends Swagger("2.0", "1.0", AudioApiInfo.apiInfo) {
  val roleWithWriteAccessInTest = AudioApiProperties.RoleWithWriteAccess.replace(":", "-test:")
  addAuthorization(
    OAuth(List(roleWithWriteAccessInTest),
          List(ImplicitGrant(LoginEndpoint(AudioApiProperties.Auth0LoginEndpoint), "access_token"))))
}
