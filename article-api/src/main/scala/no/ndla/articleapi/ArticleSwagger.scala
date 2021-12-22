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
  }
}

object ArticleApiInfo {

  val contactInfo = ContactInfo(
    ArticleApiProperties.ContactName,
    ArticleApiProperties.ContactUrl,
    ArticleApiProperties.ContactEmail
  )

  val licenseInfo = LicenseInfo(
    "GPL v3.0",
    "http://www.gnu.org/licenses/gpl-3.0.en.html"
  )

  val apiInfo = ApiInfo(
    "Article API",
    "Searching and fetching all articles published on the NDLA platform.\n\n" +
      "The Article API provides an endpoint for searching and fetching articles. Different meta-data is attached to the " +
      "returned articles, and typical examples of this are language and license.\n" +
      "Includes endpoints to filter Articles on different levels, and retrieve single articles.",
    ArticleApiProperties.TermsUrl,
    contactInfo,
    licenseInfo
  )
}

class ArticleSwagger extends Swagger("2.0", "1.0", ArticleApiInfo.apiInfo) {

  private def writeRolesInTest: List[String] = {
    val writeRoles = List(ArticleApiProperties.DraftRoleWithWriteAccess, ArticleApiProperties.RoleWithWriteAccess)
    writeRoles.map(_.replace(":", "-test:"))
  }

  addAuthorization(
    OAuth(writeRolesInTest,
          List(ImplicitGrant(LoginEndpoint(ArticleApiProperties.Auth0LoginEndpoint), "access_token"))))

}
