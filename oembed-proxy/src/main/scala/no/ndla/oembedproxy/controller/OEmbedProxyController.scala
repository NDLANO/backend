/*
 * Part of NDLA oembed-proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.controller

import com.typesafe.scalalogging.LazyLogging
import no.ndla.network.ApplicationUrl
import no.ndla.network.model.HttpRequestException
import no.ndla.oembedproxy.model._
import no.ndla.oembedproxy.service.OEmbedServiceComponent
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport, SwaggerSupportSyntax}
import org.scalatra.util.NotNothing
import org.scalatra._

import scala.util.{Failure, Success}

trait OEmbedProxyController {
  this: OEmbedServiceComponent with ErrorHelpers with CorrelationIdSupport =>
  val oEmbedProxyController: OEmbedProxyController

  class OEmbedProxyController(implicit val swagger: Swagger)
      extends ScalatraServlet
      with NativeJsonSupport
      with SwaggerSupport
      with LazyLogging
      with CorrelationIdSupport {
    protected implicit override val jsonFormats: Formats = DefaultFormats

    protected val applicationDescription =
      "API wrapper for oembed.com adding support for ndla.no."

    registerModel[Error]()

    val response400: ResponseMessage = ResponseMessage(400, "Validation error", Some("Error"))
    val response401: ResponseMessage = ResponseMessage(401, "Unauthorized")
    val response404: ResponseMessage = ResponseMessage(404, "Url not found")
    val response500: ResponseMessage = ResponseMessage(500, "Unknown error", Some("Error"))

    val response501: ResponseMessage =
      ResponseMessage(501, "Provider Not Supported", Some("Error"))
    val response502: ResponseMessage = ResponseMessage(502, "Bad Gateway", Some("Error"))

    case class Param(paramName: String, description: String)

    private val correlationId =
      Param("X-Correlation-ID", "User supplied correlation-id. May be omitted.")
    private val urlParam =
      Param("url", "The URL to retrieve embedding information for")
    private val maxWidth =
      Param("maxwidth", "The maximum width of the embedded resource")
    private val maxHeight =
      Param("maxheight", "The maximum height of the embedded resource")

    protected def asQueryParam[T: Manifest: NotNothing](param: Param): SwaggerSupportSyntax.ParameterBuilder[T] =
      queryParam[T](param.paramName).description(param.description)
    protected def asHeaderParam[T: Manifest: NotNothing](param: Param): SwaggerSupportSyntax.ParameterBuilder[T] =
      headerParam[T](param.paramName).description(param.description)

    before() {
      contentType = formats("json")
      ApplicationUrl.set(request)
      logger.info(
        "{} {}{}",
        request.getMethod,
        request.getRequestURI,
        Option(request.getQueryString).map(s => s"?$s").getOrElse("")
      )
    }

    after() {
      ApplicationUrl.clear()
    }

    import ErrorHelpers._

    error {
      case pme: ParameterMissingException =>
        BadRequest(Error(PARAMETER_MISSING, pme.getMessage))
      case pnse: ProviderNotSupportedException =>
        NotImplemented(Error(PROVIDER_NOT_SUPPORTED, pnse.getMessage))
      case hre: HttpRequestException if hre.is404 =>
        logger.info(s"Could not fetch remote: '${hre.getMessage}'")
        NotFound(Error(REMOTE_ERROR, hre.getMessage))
      case hre: HttpRequestException =>
        val msg = hre.httpResponse.map(response =>
          s": Received '${response.code}' '${response.statusLine}'. Body was '${response.body}'"
        )
        logger.error(s"Could not fetch remote: '${hre.getMessage}'${msg.getOrElse("")}", hre)
        BadGateway(Error(REMOTE_ERROR, hre.getMessage))
      case t: Throwable => {
        t.printStackTrace()
        logger.error(t.getMessage)
        InternalServerError(GenericError)
      }
    }

    get(
      "/",
      operation(
        apiOperation[OEmbed]("oembed")
          .summary("Returns oEmbed information for a given url.")
          .description("Returns oEmbed information for a given url.")
          .parameters(
            asHeaderParam[Option[String]](correlationId),
            asQueryParam[String](urlParam),
            asQueryParam[Option[String]](maxWidth),
            asQueryParam[Option[String]](maxHeight)
          )
          .responseMessages(response400, response401, response500, response501, response502)
      )
    ) {
      val maxWidth  = params.get(this.maxWidth.paramName)
      val maxHeight = params.get(this.maxHeight.paramName)

      params.get(urlParam.paramName) match {
        case None =>
          errorHandler(new ParameterMissingException(s"The required parameter '${urlParam.paramName}' is missing."))
        case Some(url) =>
          oEmbedService.get(url, maxWidth, maxHeight) match {
            case Success(oembed) => oembed
            case Failure(ex)     => errorHandler(ex)
          }
      }
    }
  }

}
