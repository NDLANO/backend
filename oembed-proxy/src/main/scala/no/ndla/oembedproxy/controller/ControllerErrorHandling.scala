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

  private val statusCodesToPassAlong                                                      = List(401, 403, 404, 410)
  private def getRequestExceptionStatusCode(exception: HttpRequestException): Option[Int] =
    exception.httpResponse.map(_.code.code) match {
      case Some(value) if statusCodesToPassAlong.contains(value) => Some(value)
      case _                                                     => None
    }

  override def handleErrors: PartialFunction[Throwable, AllErrors] = {
    case ivu: InvalidUrlException            => ErrorBody(INVALID_URL, ivu.getMessage, clock.now(), 400)
    case pnse: ProviderNotSupportedException => ErrorBody(PROVIDER_NOT_SUPPORTED, pnse.getMessage, clock.now(), 422)
    case hre: HttpRequestException           =>
      val msg = hre
        .httpResponse
        .map(response => s": Received '${response.code}' '${response.statusText}'. Body was '${response.body}'")
      getRequestExceptionStatusCode(hre) match {
        case None =>
          logger.error(s"Could not fetch remote: '${hre.getMessage}'${msg.getOrElse("")}", hre)
          ErrorBody(REMOTE_ERROR, hre.getMessage, clock.now(), 502)
        case Some(statusCode) if statusCodesToPassAlong.contains(statusCode) =>
          logger.info(s"Remote service returned $statusCode: '${hre.getMessage}'${msg.getOrElse("")}")
          ErrorBody(REMOTE_ERROR, hre.getMessage, clock.now(), statusCode)
        case Some(statusCode) =>
          logger.error(s"Remote service returned $statusCode: '${hre.getMessage}'${msg.getOrElse("")}")
          ErrorBody(REMOTE_ERROR, hre.getMessage, clock.now(), statusCode)
      }
  }
}
