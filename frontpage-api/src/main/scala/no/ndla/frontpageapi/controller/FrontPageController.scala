/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.controller

import cats.effect.{Effect, IO}
import no.ndla.frontpageapi.model.api._
import no.ndla.frontpageapi.service.{ReadService, WriteService}
import org.http4s.rho.RhoRoutes
import org.http4s.rho.swagger.SwaggerSyntax

trait FrontPageController {
  this: ReadService with WriteService =>
  val frontPageController: FrontPageController[IO]

  class FrontPageController[F[+ _]: Effect](swaggerSyntax: SwaggerSyntax[F]) extends RhoRoutes[F] {

    import swaggerSyntax._

    "Get data to display on the front page" **
      GET |>> { () =>
      {
        readService.frontPage match {
          case Some(s) => Ok(s)
          case None    => NotFound(Error.notFound)
        }
      }
    }

  }

}
