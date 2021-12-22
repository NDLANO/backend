/*
 * Part of NDLA audio-api
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

  val contactInfo: ContactInfo = ContactInfo(
    AudioApiProperties.ContactName,
    AudioApiProperties.ContactUrl,
    AudioApiProperties.ContactEmail
  )

  val licenseInfo: LicenseInfo = LicenseInfo(
    "GPL v3.0",
    "http://www.gnu.org/licenses/gpl-3.0.en.html"
  )

  val apiInfo: ApiInfo = ApiInfo(
    "Audio API",
    "Searching and fetching all audio used in the NDLA platform.\n\n" +
      "The Audio API provides an endpoint for searching and fetching audio used in NDLA resources. " +
      "Meta-data like title, tags, language and license are searchable and also provided in the results. " +
      "The media file is provided as an URL with the mime type.",
    AudioApiProperties.TermsUrl,
    contactInfo,
    licenseInfo
  )
}

class AudioSwagger extends Swagger("2.0", "1.0", AudioApiInfo.apiInfo) {
  addAuthorization(
    OAuth(List(AudioApiProperties.RoleWithWriteAccess),
          List(ImplicitGrant(LoginEndpoint(AudioApiProperties.Auth0LoginEndpoint), "access_token"))))
}
