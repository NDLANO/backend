/*
 * Part of NDLA network
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir.auth

import sttp.tapir.*

object TapirAuthUtil {
  def makeOptionalMapping[T](typeMapping: Mapping[String, T]): Mapping[Option[String], Option[T]] =
    Mapping.fromDecode[Option[String], Option[T]] {
      case Some(v) => typeMapping.decode(v).map(Some(_))
      case None    => DecodeResult.Value(None)
    } {
      case Some(v) => Some(typeMapping.encode(v))
      case None    => None
    }
}
