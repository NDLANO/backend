/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi

import org.scalatra.ScalatraServlet
import org.scalatra.swagger._

class ResourcesApp(implicit val swagger: Swagger) extends ScalatraServlet with NativeSwaggerBase {
  get("/") {
    renderSwagger2(swagger.docs.toList)
  }
}

object DraftApiInfo {

  val contactInfo = ContactInfo(
    DraftApiProperties.ContactName,
    DraftApiProperties.ContactUrl,
    DraftApiProperties.ContactEmail
  )

  val licenseInfo = LicenseInfo(
    "GPL v3.0",
    "http://www.gnu.org/licenses/gpl-3.0.en.html"
  )

  val apiInfo = ApiInfo(
    "Draft API",
    "Services for accessing draft articles, draft and agreements",
    DraftApiProperties.TermsUrl,
    contactInfo,
    licenseInfo
  )
}

class DraftSwagger extends Swagger("2.0", "1.0", DraftApiInfo.apiInfo) {

  private def writeRolesInTest: List[String] = {
    val writeRoles = List(DraftApiProperties.DraftRoleWithWriteAccess,
                          DraftApiProperties.DraftRoleWithPublishAccess,
                          DraftApiProperties.ArticleRoleWithPublishAccess)
    writeRoles.map(_.replace(":", "-test:"))
  }

  addAuthorization(
    OAuth(writeRolesInTest, List(ImplicitGrant(LoginEndpoint(DraftApiProperties.Auth0LoginEndpoint), "access_token"))))
}
