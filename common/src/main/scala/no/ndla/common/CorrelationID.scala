/*
 * Part of NDLA network.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common

import no.ndla.common.configuration.Constants
import org.apache.logging.log4j.ThreadContext

import java.util.UUID
import javax.servlet.http.HttpServletRequest

object CorrelationID {
  private val correlationID = new ThreadLocal[String]

  def set(request: HttpServletRequest): Unit = {
    val maybeHeaderValue = Option(request.getHeader(Constants.CorrelationIdHeader))
    this.set(maybeHeaderValue)
  }

  def set(correlationId: Option[String]): Unit = {
    val idToSet = correlationId match {
      case Some(x) => x
      case None    => UUID.randomUUID().toString
    }

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
