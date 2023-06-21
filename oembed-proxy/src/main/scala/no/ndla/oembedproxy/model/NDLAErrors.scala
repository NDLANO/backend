/*
 * Part of NDLA oembed-proxy.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.oembedproxy.model

import cats.effect.IO
import no.ndla.common.Clock
import no.ndla.network.logging.FLogging
import no.ndla.network.model.HttpRequestException
import no.ndla.network.tapir._
import no.ndla.oembedproxy.Props

trait ErrorHelpers extends TapirErrorHelpers with FLogging {
  this: Props with Clock =>

  import ErrorHelpers._

  override def handleErrors: PartialFunction[Throwable, IO[ErrorBody]] = {
    case pnse: ProviderNotSupportedException =>
      IO(ErrorBody(PROVIDER_NOT_SUPPORTED, pnse.getMessage, clock.now(), 501))
    case hre: HttpRequestException if hre.is404 =>
      val msg = hre.getMessage
      logger.info(s"Could not fetch remote: '$msg'") >>
        IO(ErrorBody(REMOTE_ERROR, msg, clock.now(), 404))
    case hre: HttpRequestException =>
      val msg = hre.httpResponse.map(response =>
        s": Received '${response.code}' '${response.statusText}'. Body was '${response.body}'"
      )
      logger.error(hre)(s"Could not fetch remote: '${hre.getMessage}'${msg.getOrElse("")}") >>
        IO(ErrorBody(REMOTE_ERROR, hre.getMessage, clock.now(), 502))
  }
}

case class ProviderNotSupportedException(message: String) extends RuntimeException(message)
class DoNotUpdateMemoizeException(message: String)        extends RuntimeException(message)
