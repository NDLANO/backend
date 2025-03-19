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
import no.ndla.common.configuration.HasBaseProps
import no.ndla.common.model.api.UpdateOrDelete
import sttp.apispec
import sttp.apispec.openapi.{Components, Contact, Info, License}
import sttp.apispec.{AnySchema, OAuthFlow, OAuthFlows, SchemaLike, SecurityScheme}
import sttp.tapir.*
import sttp.tapir.docs.openapi.{OpenAPIDocsInterpreter, OpenAPIDocsOptions}
import sttp.tapir.server.ServerEndpoint

import scala.collection.immutable.ListMap

trait SwaggerControllerConfig {
  this: HasBaseProps & TapirController =>

  class SwaggerController(services: List[TapirController], swaggerInfo: SwaggerInfo) extends TapirController {
    import props.*

    def getServices(): List[TapirController] = services :+ this

    val info: Info = Info(
      title = props.ApplicationName,
      version = "1.0",
      description = swaggerInfo.description.some,
      termsOfService = TermsUrl.some,
      contact = Contact(name = ContactName.some, url = ContactUrl.some, email = ContactEmail.some).some,
      license = License("GPL v3.0", "https://www.gnu.org/licenses/gpl-3.0.en.html".some).some
    )

    import io.circe.syntax.*
    import sttp.apispec.openapi.circe.*

    private val swaggerEndpoints = services.collect {
      case svc: TapirController if svc.enableSwagger => svc.builtEndpoints
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

    /** NOTE: This is a hack to allow us to create nullable types in the specification. If possible this should probably
      * be replaced by a tapir alternative when that is possible.
      *
      * https://github.com/softwaremill/tapir/issues/2953
      */
    private val schemaPostProcessingFunctions: List[apispec.Schema => Option[apispec.Schema]] = List(
      UpdateOrDelete.replaceSchema
    )

    private def postProcessSchema(schema: SchemaLike): SchemaLike = {
      schema match {
        case schema: AnySchema => schema
        case schema: apispec.Schema =>
          val convertedSchema = schemaPostProcessingFunctions.foldLeft(None: Option[apispec.Schema]) {
            case (None, f)                  => f(schema)
            case (Some(convertedSchema), _) => Some(convertedSchema)
          }

          convertedSchema match {
            case Some(value) => value
            case None =>
              val props = schema.properties.map {
                case (k, v: apispec.Schema) => k -> postProcessSchema(v)
                case (k, v)                 => k -> v
              }
              schema.copy(properties = props)
          }
      }
    }

    private val docs: Json = {
      val options             = OpenAPIDocsOptions.default
      val docs                = OpenAPIDocsInterpreter(options).serverEndpointsToOpenAPI(swaggerEndpoints, info)
      val generatedComponents = docs.components.getOrElse(Components.Empty)
      val newSchemas          = generatedComponents.schemas.map { case (k, v) => k -> postProcessSchema(v) }
      val newComponents = generatedComponents.copy(
        securitySchemes = ListMap("oauth2" -> Right(securityScheme)),
        schemas = newSchemas
      )
      val docsWithComponents = docs.components(newComponents).asJson
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
}
