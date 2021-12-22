/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi

import cats.effect.IO
import cats.implicits._
import no.ndla.frontpageapi.FrontpageApiProperties._
import no.ndla.frontpageapi.auth.Role
import no.ndla.frontpageapi.controller.{AuthController, HealthController, NdlaMiddleware}
import org.http4s.HttpRoutes
import org.http4s.rho.RhoRoutes
import org.http4s.rho.bits.PathAST._
import org.http4s.rho.swagger.SwaggerMetadata
import org.http4s.rho.swagger.models.{Contact, Info, License, OAuth2Definition}
import org.http4s.rho.swagger.syntax.io._
import shapeless.HNil

trait ServiceWithMountPoint {
  val mountPoint: String
  def toRoutes: HttpRoutes[IO]
}

case class Service(service: HttpRoutes[IO], override val mountPoint: String) extends ServiceWithMountPoint {
  override def toRoutes: HttpRoutes[IO] = service
}

class SwaggerService(service: RhoRoutes[IO], override val mountPoint: String) extends ServiceWithMountPoint {
  override def toRoutes: HttpRoutes[IO] = NdlaMiddleware(service.toRoutes())
  def service(): RhoRoutes[IO] = this.service
}

class AuthedSwaggerService(override val service: AuthController[IO], override val mountPoint: String)
    extends SwaggerService(service, mountPoint) {
  override def toRoutes: HttpRoutes[IO] = NdlaMiddleware(
    service.authMiddleware.apply(service.Auth.toService(service.toRoutes()))
  )
}

object Routes {

  def buildRoutes(): List[ServiceWithMountPoint] = {
    val frontPage = new SwaggerService(ComponentRegistry.frontPageController, "/frontpage-api/v1/frontpage")
    val subjectPage = new AuthedSwaggerService(ComponentRegistry.subjectPageController, "/frontpage-api/v1/subjectpage")
    val filmFrontPage =
      new AuthedSwaggerService(ComponentRegistry.filmPageController, "/frontpage-api/v1/filmfrontpage")

    List(
      frontPage,
      subjectPage,
      filmFrontPage,
      new SwaggerService(ComponentRegistry.internController, "/intern"),
      Service(HealthController(), "/health"),
      Service(createSwaggerDocService(frontPage, subjectPage, filmFrontPage), "/frontpage-api/api-docs")
    )
  }

  private def toTypedPath[F[_]](prefix: String): TypedPath[F, HNil] = {
    val start: PathRule = PathMatch("")
    val newPath =
      prefix.split("/").foldLeft(start)((p, s) => PathAnd(p, PathMatch(s)))

    TypedPath(newPath)
  }

  private def createSwaggerDocService(services: SwaggerService*): HttpRoutes[IO] = {
    val info = Info(
      title = "frontpage-api",
      version = "1.0",
      description = "Service for fetching frontpage data".some,
      termsOfService = TermsUrl.some,
      contact = Contact(ContactName, Some(ContactUrl), Some(ContactEmail)).some,
      license = License("GPL v3.0", "http://www.gnu.org/licenses/gpl-3.0.en.html").some
    )

    val allRoles = Role.values.map(r => s"${Role.prefix}${r.toString}".toLowerCase)

    val oauth2Definition = OAuth2Definition(
      authorizationUrl = Auth0LoginEndpoint,
      tokenUrl = "",
      flow = "implicit",
      scopes = allRoles.map(r => r -> r).toMap
    )

    val swaggerMetadata = SwaggerMetadata(
      apiInfo = info,
      securityDefinitions = Map(
        "oauth2" -> oauth2Definition
      )
    )

    val routes = services.map(t => {
      t.service()./:(toTypedPath(t.mountPoint)).getRoutes
    })

    val swagRoutes = routes.flatten
    val swagger = createSwagger(swaggerMetadata = swaggerMetadata)(swagRoutes)

    createSwaggerRoute(swagger, TypedPath(PathMatch(""))).toRoutes()
  }
}
