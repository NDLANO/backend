/*
 * Part of NDLA network
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir

import cats.implicits.*
import io.circe.Json
import no.ndla.common.configuration.BaseProps
import no.ndla.network.clients.MyNDLAProvider
import sttp.apispec.openapi.{Components, Contact, Info, License}
import sttp.apispec.{OAuthFlow, OAuthFlows, SecurityScheme}
import sttp.tapir.*
import sttp.tapir.docs.openapi.{OpenAPIDocsInterpreter, OpenAPIDocsOptions}
import sttp.tapir.server.ServerEndpoint

import scala.collection.immutable.ListMap

class SwaggerController(services: List[TapirController], swaggerInfo: SwaggerInfo)(using
    props: BaseProps,
    myNDLAApiClient: MyNDLAProvider,
    errorHelpers: ErrorHelpers,
    errorHandling: ErrorHandling,
) extends TapirController {
  def getServices(): List[TapirController] = services :+ this

  override def handleErrors: PartialFunction[Throwable, AllErrors] = { case e: Throwable =>
    errorHelpers.generic
  }

  val info: Info = Info(
    title = props.ApplicationName,
    version = "1.0",
    description = swaggerInfo.description.some,
    termsOfService = props.TermsUrl.some,
    contact = Contact(name = props.ContactName.some, url = props.ContactUrl.some, email = props.ContactEmail.some).some,
    license = License("GPL v3.0", "https://www.gnu.org/licenses/gpl-3.0.en.html".some).some,
  )

  import io.circe.syntax.*
  import sttp.apispec.openapi.circe.*

  private val swaggerEndpoints = services
    .collect {
      case svc: TapirController if svc.enableSwagger => svc.builtEndpoints
    }
    .flatten

  private val securityScheme: SecurityScheme = SecurityScheme(
    `type` = "oauth2",
    description = None,
    name = None,
    in = None,
    scheme = None,
    bearerFormat = None,
    flows = OAuthFlows(`implicit` =
      OAuthFlow(
        authorizationUrl = swaggerInfo.authUrl.some,
        tokenUrl = None,
        refreshUrl = None,
        scopes = swaggerInfo.scopes,
      ).some
    ).some,
    openIdConnectUrl = None,
  )

  private val docs: Json = {
    val options             = OpenAPIDocsOptions.default
    val docs                = OpenAPIDocsInterpreter(options).serverEndpointsToOpenAPI(swaggerEndpoints, info)
    val generatedComponents = docs.components.getOrElse(Components.Empty)
    val newComponents       = generatedComponents.copy(securitySchemes = ListMap("oauth2" -> Right(securityScheme)))
    val docsWithComponents  = docs.components(newComponents).asJson
    docsWithComponents.asJson
  }

  def saveSwagger(): Unit = {
    import java.io.*
    val swaggerLocation = new File(s"./typescript/types-backend/openapi")
    val jsonFile        = new File(swaggerLocation, s"${props.ApplicationName}.json")

    swaggerLocation.mkdir()

    val pw = new PrintWriter(jsonFile)
    pw.write(docs.noSpaces)
    pw.close()
  }

  private def addCorsHeaders[A, I, X, O, R](end: Endpoint[A, I, X, O, R]) =
    if (props.Environment == "local") end.out(header("Access-Control-Allow-Origin", "*"))
    else end

  override val enableSwagger: Boolean       = false
  protected val prefix: EndpointInput[Unit] = swaggerInfo.mountPoint

  override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
    addCorsHeaders(endpoint.get)
      .out(stringJsonBody)
      .serverLogicPure { _ =>
        Right(docs.noSpaces)
      }
  )
}
