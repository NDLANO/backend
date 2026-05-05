/*
 * Part of NDLA oembed-proxy
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.controller

import no.ndla.common.Clock
import no.ndla.network.model.HttpRequestException
import no.ndla.network.tapir.{AllErrors, ErrorBody, ErrorHandling, ErrorHelpers}
import no.ndla.oembedproxy.model.{InvalidUrlException, ProviderNotSupportedException}

class ControllerErrorHandling(using clock: Clock, errorHelpers: ErrorHelpers) extends ErrorHandling {
  import errorHelpers.*

  private val statusCodesToPassAlong = List(401, 403, 404, 410)

  override def handleErrors: PartialFunction[Throwable, AllErrors] = {
    case ivu: InvalidUrlException                       => ErrorBody(INVALID_URL, ivu.getMessage, clock.now(), 400)
    case pnse: ProviderNotSupportedException            => ErrorBody(PROVIDER_NOT_SUPPORTED, pnse.getMessage, clock.now(), 422)
    case ex @ HttpRequestException(exMessage, response) =>
      val statusCode = response.code.code
      val msg        = s"'$exMessage': Received '${response.code}' '${response.statusText}'. Body was '${response.body}'"

      if (statusCodesToPassAlong.contains(statusCode)) {
        logger.info(s"Remote service returned $statusCode: $msg")
        ErrorBody(REMOTE_ERROR, exMessage, clock.now(), statusCode)
      } else {
        logger.error(s"Could not fetch remote: $msg", ex)
        ErrorBody(REMOTE_ERROR, exMessage, clock.now(), 502)
      }
  }
}
