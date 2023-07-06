/*
 * Part of NDLA network
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.tapir

import io.circe.generic.extras.Configuration
import io.circe.{Decoder, Encoder, Printer}
import sttp.tapir.json.circe.TapirJsonCirce
import sttp.tapir.{EndpointIO, Schema}

object NoNullJsonPrinter extends TapirJsonCirce {
  implicit val config: Configuration = Configuration.default.withDefaults
  override def jsonPrinter: Printer  = Printer.noSpaces.copy(dropNullValues = true)

  override def jsonBody[T: Encoder: Decoder: Schema]: EndpointIO.Body[String, T] = {
    super.jsonBody[T]
  }
}
