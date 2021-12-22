/*
 * Part of NDLA network.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network

import java.util.UUID

object CorrelationID {
  private val correlationID = new ThreadLocal[String]

  def set(correlationId: Option[String]) = {
    correlationId match {
      case Some(x) => correlationID.set(x)
      case None    => correlationID.set(UUID.randomUUID().toString)
    }
  }

  def get: Option[String] = {
    Option(correlationID.get)
  }

  def clear() = {
    correlationID.remove()
  }
}
