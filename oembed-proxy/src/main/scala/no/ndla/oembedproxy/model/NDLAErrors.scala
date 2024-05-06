/*
 * Part of NDLA oembed-proxy
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.model

import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.Clock
import no.ndla.network.model.HttpRequestException
import no.ndla.network.tapir._
import no.ndla.oembedproxy.Props

trait ErrorHelpers extends TapirErrorHelpers with StrictLogging {
  this: Props with Clock =>

  import ErrorHelpers._

  private val statusCodesToPassAlong = List(401, 403, 404, 410)
  def getRequestExceptionStatusCode(exception: HttpRequestException): Int =
    exception.httpResponse.map(_.code.code) match {
      case Some(value) if statusCodesToPassAlong.contains(value) => value
      case _                                                     => 502
    }

  override def handleErrors: PartialFunction[Throwable, ErrorBody] = {
    case ivu: InvalidUrlException =>
      ErrorBody(INVALID_URL, ivu.getMessage, clock.now(), 400)
    case pnse: ProviderNotSupportedException =>
      ErrorBody(PROVIDER_NOT_SUPPORTED, pnse.getMessage, clock.now(), 422)
    case hre: HttpRequestException =>
      val statusCode = getRequestExceptionStatusCode(hre)
      val msg = hre.httpResponse.map(response =>
        s": Received '${response.code}' '${response.statusText}'. Body was '${response.body}'"
      )
      logger.error(s"Could not fetch remote: '${hre.getMessage}'${msg.getOrElse("")}", hre)
      ErrorBody(REMOTE_ERROR, hre.getMessage, clock.now(), statusCode)
  }
}

case class InvalidUrlException(message: String)           extends RuntimeException(message)
case class ProviderNotSupportedException(message: String) extends RuntimeException(message)
class DoNotUpdateMemoizeException(message: String)        extends RuntimeException(message)
