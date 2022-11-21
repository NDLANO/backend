/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */
package no.ndla.frontpageapi.controller

import cats.effect.IO
import io.circe.Printer
import io.circe.generic.auto._
import io.circe.syntax._
import no.ndla.frontpageapi.model.api.ErrorHelpers
import org.http4s.headers.`Content-Type`
import org.http4s.{Headers, MediaType, Response}

trait FallbackRoute {
  this: ErrorHelpers =>

  def getFallbackRoute: Response[IO] = {
    val body: String = Printer.noSpaces.print(ErrorHelpers.notFound.asJson)
    val headers      = Headers(`Content-Type`(MediaType.application.json))
    Response.notFound[IO].withEntity(body).withHeaders(headers)
  }
}
