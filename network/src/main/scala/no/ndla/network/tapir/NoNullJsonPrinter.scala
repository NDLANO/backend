/*
 * Part of NDLA network
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.tapir

import io.circe.Printer
import io.circe.generic.extras.Configuration
import sttp.tapir.json.circe.TapirJsonCirce

object NoNullJsonPrinter extends TapirJsonCirce {
  implicit val config: Configuration = Configuration.default.withDefaults
  override def jsonPrinter: Printer  = Printer.noSpaces.copy(dropNullValues = true)
}
