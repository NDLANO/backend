/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi

import io.circe.{Json, Printer}

package object repository {
  extension (json: Json) {
    def noSpacesDropNull: String = Printer.noSpaces.copy(dropNullValues = true).print(json)
  }
}
