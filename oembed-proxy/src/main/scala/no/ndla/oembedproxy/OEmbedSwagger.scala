/*
 * Part of NDLA oembed-proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy

import org.scalatra.ScalatraServlet
import org.scalatra.swagger._

class ResourcesApp(using override val swagger: Swagger) extends ScalatraServlet with NativeSwaggerBase {
  get("/") {
    renderSwagger2(swagger.docs.toList)
  }
}

  class OEmbedProxyInfo(props: OEmbedProxyProperties) {

    val contactInfo: ContactInfo = ContactInfo(
      props.ContactName,
      props.ContactUrl,
      props.ContactEmail
    )

    val licenseInfo: LicenseInfo = LicenseInfo(
      "GPL v3.0",
      "http://www.gnu.org/licenses/gpl-3.0.en.html"
    )

    val apiInfo: ApiInfo = ApiInfo(
      "OEmbed Proxy",
      "Convert any NDLA resource to an oEmbed embeddable resource.",
      props.TermsUrl,
      contactInfo,
      licenseInfo
    )
  }

class OEmbedSwagger(using props: OEmbedProxyProperties) extends Swagger("2.0", "1.0", new OEmbedProxyInfo(props).apiInfo) {
  addAuthorization(
    OAuth(List(), List(ImplicitGrant(LoginEndpoint(props.Auth0LoginEndpoint), "access_token")))
  )
}
