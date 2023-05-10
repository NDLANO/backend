/*
 * Part of NDLA network.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common

import cats.effect.IO
import no.ndla.common.configuration.Constants
import org.apache.logging.log4j.ThreadContext
import org.http4s.Request
import org.typelevel.ci.CIString
import sttp.tapir.model.ServerRequest

import java.util.UUID
import javax.servlet.http.HttpServletRequest

object CorrelationID {
  private val correlationID = new ThreadLocal[String]

  def set(request: HttpServletRequest): Unit = {
    val maybeHeaderValue = fromRequest(request)
    this.set(maybeHeaderValue)
  }

  def fromRequest(request: HttpServletRequest): Option[String] = {
    Option(request.getHeader(Constants.CorrelationIdHeader))
  }

  def fromRequest(request: ServerRequest): String = {
    getOrGenerate(request.header(Constants.CorrelationIdHeader))
  }

  def fromRequest(request: Request[IO]): String = {
    getOrGenerate(request.headers.get(CIString(Constants.CorrelationIdHeader)).map(_.head.value))
  }

  def getOrGenerate(x: Option[String]): String = x match {
    case Some(x) => x
    case None    => UUID.randomUUID().toString
  }

  def set(correlationId: Option[String]): Unit = {
    val idToSet = getOrGenerate(correlationId)
    correlationID.set(idToSet)
    ThreadContext.put(Constants.CorrelationIdKey, idToSet)
  }

  def get: Option[String] = {
    Option(correlationID.get)
  }

  def clear(): Unit = {
    correlationID.remove()
    ThreadContext.remove(Constants.CorrelationIdKey)
  }
}
