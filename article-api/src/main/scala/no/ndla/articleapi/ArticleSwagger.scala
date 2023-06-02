/*
 * Part of NDLA article-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi

import org.scalatra.ScalatraServlet
import org.scalatra.swagger._

class ResourcesApp(implicit val swagger: Swagger) extends ScalatraServlet with NativeSwaggerBase {
  get("/") {
    renderSwagger2(swagger.docs.toList)
  }: Unit
}
trait ArticleApiInfo {
  this: Props =>

  object ArticleApiInfo {

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
      "Article API",
      "Searching and fetching all articles published on the NDLA platform.\n\n" +
        "The Article API provides an endpoint for searching and fetching articles. Different meta-data is attached to the " +
        "returned articles, and typical examples of this are language and license.\n" +
        "Includes endpoints to filter Articles on different levels, and retrieve single articles.",
      props.TermsUrl,
      contactInfo,
      licenseInfo
    )
  }

  class ArticleSwagger extends Swagger("2.0", "1.0", ArticleApiInfo.apiInfo) {

    private def writeRolesInTest: List[String] = {
      List(props.DraftRoleWithWriteAccess, props.RoleWithWriteAccess)
    }

    addAuthorization(
      OAuth(
        writeRolesInTest,
        List(ImplicitGrant(LoginEndpoint(props.Auth0LoginEndpoint), "access_token"))
      )
    )

  }

}
