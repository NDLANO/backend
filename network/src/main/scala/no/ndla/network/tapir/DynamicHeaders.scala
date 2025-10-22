/*
 * Part of NDLA network
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir

import sttp.model.Header
import sttp.tapir.EndpointIO.annotations.headers

case class DynamicHeaders(
    @headers
    headers: List[Header]
)

object DynamicHeaders {
  def fromMaybeValue(name: String, s: Option[String]): DynamicHeaders = new DynamicHeaders(fromOpt(name, s).toList)

  def fromOpt(name: String, s: Option[String]): Option[Header] = s.map(Header(name, _))

  def fromValue(name: String, value: String): DynamicHeaders = {
    new DynamicHeaders(List(Header(name, value)))
  }
}
