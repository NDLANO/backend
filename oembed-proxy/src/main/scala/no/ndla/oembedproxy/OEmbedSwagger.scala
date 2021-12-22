/*
 * Part of NDLA oembed_proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy

import org.scalatra.ScalatraServlet
import org.scalatra.swagger._

class ResourcesApp(implicit val swagger: Swagger) extends ScalatraServlet with NativeSwaggerBase {
  get("/") {
    renderSwagger2(swagger.docs.toList)
  }
}

object OEmbedProxyInfo {

  val contactInfo: ContactInfo = ContactInfo(
    "NDLA",
    "ndla.no",
    OEmbedProxyProperties.ContactEmail
  )

  val licenseInfo: LicenseInfo = LicenseInfo(
    "GPL v3.0",
    "http://www.gnu.org/licenses/gpl-3.0.en.html"
  )

  val apiInfo: ApiInfo = ApiInfo(
    "OEmbed Proxy",
    "Convert any NDLA resource to an oEmbed embeddable resource.",
    "https://om.ndla.no/tos",
    contactInfo,
    licenseInfo
  )
}

class OEmbedSwagger extends Swagger("2.0", "1.0", OEmbedProxyInfo.apiInfo) {
  addAuthorization(
    OAuth(List(), List(ImplicitGrant(LoginEndpoint(OEmbedProxyProperties.Auth0LoginEndpoint), "access_token"))))
}
