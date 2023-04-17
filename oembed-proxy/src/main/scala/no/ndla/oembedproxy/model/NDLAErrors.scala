/*
 * Part of NDLA oembed-proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.model

import no.ndla.common.Clock
import no.ndla.network.model.HttpRequestException
import no.ndla.network.tapir._
import no.ndla.oembedproxy.Props

trait ErrorHelpers extends TapirErrorHelpers {
  this: Props with Clock =>

  import ErrorHelpers._

  override def returnError(ex: Throwable): ErrorBody = {
    ex match {
      case pme: ParameterMissingException =>
        BadRequestBody(PARAMETER_MISSING, pme.getMessage, clock.now())
      case pnse: ProviderNotSupportedException =>
        NotImplementedBody(PROVIDER_NOT_SUPPORTED, pnse.getMessage, clock.now())
      case hre: HttpRequestException if hre.is404 =>
        val msg = hre.getMessage
        logger.info(s"Could not fetch remote: '$msg'")
        NotFoundBody(REMOTE_ERROR, msg, clock.now())
      case hre: HttpRequestException =>
        val msg = hre.httpResponse.map(response =>
          s": Received '${response.code}' '${response.statusText}'. Body was '${response.body}'"
        )
        logger.error(hre)(s"Could not fetch remote: '${hre.getMessage}'${msg.getOrElse("")}")
        BadGatewayBody(REMOTE_ERROR, hre.getMessage, clock.now())
      case t: Throwable =>
        logger.error(t)(t.getMessage)
        generic
    }
  }
}

class ParameterMissingException(message: String)          extends RuntimeException(message)
case class ProviderNotSupportedException(message: String) extends RuntimeException(message)
class DoNotUpdateMemoizeException(message: String)        extends RuntimeException(message)
