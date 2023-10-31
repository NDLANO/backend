/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi

import org.scalatra.ScalatraServlet
import org.scalatra.swagger._

class ResourcesApp(implicit val swagger: Swagger) extends ScalatraServlet with NativeSwaggerBase {
  get("/") {
    renderSwagger2(swagger.docs.toList)
  }: Unit
}

trait LearningpathApiInfo {
  this: Props =>

  object LearningpathApiInfo {

    private val contactInfo: ContactInfo = ContactInfo(
      props.ContactName,
      props.ContactUrl,
      props.ContactEmail
    )

    private val licenseInfo: LicenseInfo = LicenseInfo(
      "GPL v3.0",
      "http://www.gnu.org/licenses/gpl-3.0.en.html"
    )

    val apiInfo: ApiInfo = ApiInfo(
      "Learningpath API",
      "Services for accessing learningpaths",
      props.TermsUrl,
      contactInfo,
      licenseInfo
    )
  }

  class LearningpathSwagger extends Swagger("2.0", "1.0", LearningpathApiInfo.apiInfo) {
    addAuthorization(
      OAuth(List(), List(ImplicitGrant(LoginEndpoint(props.Auth0LoginEndpoint), "access_token")))
    )
  }
}
