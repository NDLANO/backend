/*
 * Part of NDLA network
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir

import sttp.tapir.DecodeResult
import sttp.tapir.EndpointIO

object Parameters {
  val feideHeader: EndpointIO.Header[Option[String]] = sttp.tapir
    .header[Option[String]]("FeideAuthorization")
    .description("Header containing FEIDE access token.")
    .mapDecode(mbHeader => DecodeResult.Value(mbHeader.map(_.replaceFirst("Bearer ", ""))))(x => x)

}
