/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi

import org.scalatra.ScalatraServlet
import org.scalatra.swagger._

class ResourcesApp(implicit val swagger: Swagger) extends ScalatraServlet with NativeSwaggerBase {
  get("/") {
    renderSwagger2(swagger.docs.toList)
  }
}

object ConceptApiInfo {

  val contactInfo: ContactInfo = ContactInfo(
    ConceptApiProperties.ContactName,
    ConceptApiProperties.ContactUrl,
    ConceptApiProperties.ContactEmail
  )

  val licenseInfo: LicenseInfo = LicenseInfo(
    "GPL v3.0",
    "http://www.gnu.org/licenses/gpl-3.0.en.html"
  )

  val apiInfo: ApiInfo = ApiInfo(
    "Concept API",
    "Services for accessing concepts",
    ConceptApiProperties.TermsUrl,
    contactInfo,
    licenseInfo
  )
}

class ConceptSwagger extends Swagger("2.0", "1.0", ConceptApiInfo.apiInfo) {

  private def writeRolesInTest: List[String] = {
    List(ConceptApiProperties.ConceptRoleWithWriteAccess)
  }

  addAuthorization(
    OAuth(writeRolesInTest,
          List(ImplicitGrant(LoginEndpoint(ConceptApiProperties.Auth0LoginEndpoint), "access_token"))))
}
