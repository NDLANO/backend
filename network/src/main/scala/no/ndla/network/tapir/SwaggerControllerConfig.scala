/*
 * Part of NDLA network
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.tapir

import cats.implicits._
import no.ndla.common.configuration.HasBaseProps
import sttp.apispec.openapi.{Components, Contact, Info, License}
import sttp.apispec.{OAuthFlow, OAuthFlows, SecurityScheme}
import sttp.tapir._
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.server.ServerEndpoint

import scala.collection.immutable.ListMap

trait SwaggerControllerConfig {
  this: HasBaseProps =>

  class SwaggerController[F[_]](services: List[Service[F]], swaggerInfo: SwaggerInfo) extends Service[F] {
    import props._

    def getServices(): List[Service[F]] = services :+ this

    val info: Info = Info(
      title = props.ApplicationName,
      version = "1.0",
      description = swaggerInfo.description.some,
      termsOfService = TermsUrl.some,
      contact = Contact(name = ContactName.some, url = ContactUrl.some, email = ContactEmail.some).some,
      license = License("GPL v3.0", "https://www.gnu.org/licenses/gpl-3.0.en.html".some).some
    )

    import io.circe.syntax._
    import sttp.apispec.openapi.circe._

    private val swaggerEndpoints = services.collect {
      case svc: Service[F] if svc.enableSwagger => svc.builtEndpoints
    }.flatten

    private val securityScheme: SecurityScheme = SecurityScheme(
      `type` = "oauth2",
      description = None,
      name = None,
      in = None,
      scheme = None,
      bearerFormat = None,
      flows = OAuthFlows(
        `implicit` = OAuthFlow(
          authorizationUrl = swaggerInfo.authUrl.some,
          tokenUrl = None,
          refreshUrl = None,
          scopes = swaggerInfo.scopes
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

    private def addCorsHeaders[A, I, X, O, R](end: Endpoint[A, I, X, O, R]) =
      if (props.Environment == "local") end.out(header("Access-Control-Allow-Origin", "*"))
      else end

    override val enableSwagger: Boolean       = false
    protected val prefix: EndpointInput[Unit] = swaggerInfo.mountPoint

    override val endpoints: List[ServerEndpoint[Any, F]] = List(
      addCorsHeaders(endpoint.get)
        .out(stringJsonBody)
        .serverLogicPure { _ =>
          Right(docs.noSpaces)
        }
    )
  }
}
