/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.controller

import no.ndla.frontpageapi.auth.Role
import no.ndla.frontpageapi.Props

import cats.effect.IO
import cats.implicits._
import org.http4s.headers.`Content-Type`
import org.http4s.{Header, Headers, HttpRoutes, MediaType}
import org.typelevel.ci.CIString
import sttp.apispec.openapi.{Components, Contact, Info, License}
import sttp.apispec.{OAuthFlow, OAuthFlows, SecurityScheme}
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import scala.collection.immutable.ListMap

trait SwaggerDocController {
  this: Service with Props =>

  val swaggerDocController: SwaggerDocController

  class SwaggerDocController(services: List[Service]) extends NoDocService {
    val mountPoint = "/frontpage-api/api-docs"

    import props._

    val info: Info = Info(
      title = props.ApplicationName,
      version = "1.0",
      description = "Service for fetching frontpage data".some,
      termsOfService = TermsUrl.some,
      contact = Contact(name = ContactName.some, url = ContactUrl.some, email = ContactEmail.some).some,
      license = License("GPL v3.0", "https://www.gnu.org/licenses/gpl-3.0.en.html".some).some
    )

    import io.circe.syntax._
    import org.http4s.circe._
    import org.http4s.dsl.io._
    import sttp.apispec.openapi.circe._

    private val swaggerEndpoints = services.collect {
      case svc: SwaggerService if svc.enableSwagger => svc.builtEndpoints
    }.flatten

    val securityScheme: SecurityScheme = SecurityScheme(
      `type` = "oauth2",
      description = None,
      name = None,
      in = None,
      scheme = None,
      bearerFormat = None,
      flows = OAuthFlows(`implicit` =
        OAuthFlow(
          authorizationUrl = props.Auth0LoginEndpoint.some,
          tokenUrl = None,
          refreshUrl = None,
          scopes = ListMap.from(Role.values.map(role => {
            val fullRole = s"${Role.prefix}$role".toLowerCase
            fullRole -> fullRole
          }))
        ).some
      ).some,
      openIdConnectUrl = None
    )

    private val docs = {
      val docs                = OpenAPIDocsInterpreter().serverEndpointsToOpenAPI(swaggerEndpoints, info)
      val generatedComponents = docs.components.getOrElse(Components.Empty)
      val newComponents       = generatedComponents.copy(securitySchemes = ListMap("oauth2" -> Right(securityScheme)))
      val docsWithComponents  = docs.components(newComponents).asJson
      docsWithComponents.asJson
    }
    private val corsHeaders: Headers =
      if (props.Environment == "local") Headers(Header.Raw(CIString("Access-Control-Allow-Origin"), "*"))
      else Headers.empty

    val route: HttpRoutes[IO] = HttpRoutes.of { case GET -> Root =>
      Ok(
        docs,
        `Content-Type`(MediaType.application.json),
        corsHeaders
      )
    }
    override def getBinding: (String, HttpRoutes[IO]) = mountPoint -> route
  }
}
