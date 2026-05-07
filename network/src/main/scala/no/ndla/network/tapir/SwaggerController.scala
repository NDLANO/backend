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
import sttp.apispec.openapi.{Contact, Info, License}
import sttp.tapir.*
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.server.ServerEndpoint

import java.nio.file.{Files, Paths}
import scala.util.Try

class SwaggerController(services: TapirController*)(using
    swaggerInfo: SwaggerInfo,
    myNDLAApiClient: MyNDLAProvider,
    errorHelpers: ErrorHelpers,
    errorHandling: ErrorHandling,
    props: BaseProps,
) extends TapirController {
  val allServices: List[TapirController] = services.toList :+ this

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

  private val docs: Json = OpenAPIDocsInterpreter().serverEndpointsToOpenAPI(swaggerEndpoints, info).asJson

  def saveSwagger(): Try[Unit] = {
    Try(Files.write(Paths.get(s"${props.ApplicationName}.json"), docs.noSpaces.getBytes)).map(_ => ())
  }

  private def addCorsHeaders[A, I, X, O, R](end: Endpoint[A, I, X, O, R]) =
    if (props.Environment == "local") end.out(header("Access-Control-Allow-Origin", "*"))
    else end

  override val enableSwagger: Boolean       = false
  protected val prefix: EndpointInput[Unit] = swaggerInfo.prefix / "api-docs"

  override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
    addCorsHeaders(endpoint.get)
      .out(stringJsonBody)
      .serverLogicPure { _ =>
        Right(docs.noSpaces)
      }
  )
}
