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
  }: Unit
}
trait ConceptApiInfo {
  this: Props =>

  object ConceptApiInfo {

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
      "Concept API",
      "Services for accessing concepts",
      props.TermsUrl,
      contactInfo,
      licenseInfo
    )
  }

  class ConceptSwagger extends Swagger("2.0", "1.0", ConceptApiInfo.apiInfo) {

    private def writeRolesInTest: List[String] = {
      List(props.ConceptRoleWithWriteAccess)
    }

    addAuthorization(
      OAuth(
        writeRolesInTest,
        List(ImplicitGrant(LoginEndpoint(props.Auth0LoginEndpoint), "access_token"))
      )
    )
  }
}
