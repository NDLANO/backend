/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.controller

import cats.Monad
import cats.data.{Kleisli, OptionT}
import cats.effect.Effect
import no.ndla.frontpageapi.auth.{Role, UserInfo}
import no.ndla.network.jwt.JWTExtractor
import org.http4s.rho.{AuthedContext, RhoRoutes}
import org.http4s.server.AuthMiddleware
import org.http4s.{AuthedRoutes, Request, Response, Status}

abstract class AuthController[F[+ _]: Effect](implicit F: Monad[F]) extends RhoRoutes[F] {

  protected val AuthOptions: Map[String, List[String]] = Map("oauth2" -> List("frontpage:write"))

  object Auth extends AuthedContext[F, Option[UserInfo]]

  protected val authUser: Kleisli[F, Request[F], Either[String, Option[UserInfo]]] = Kleisli { req =>
    val ndlaRequest = NdlaMiddleware.asNdlaHttpRequest(req)
    val jWTExtractor = new JWTExtractor(ndlaRequest)

    val userId = jWTExtractor.extractUserId()
    val roles = jWTExtractor.extractUserRoles()
    val userName = jWTExtractor.extractUserName()
    val clientId = jWTExtractor.extractClientId()

    userId.orElse(clientId).orElse(userName) match {
      case Some(userInfoName) => F.pure(Right(Some(UserInfo(userInfoName, roles.flatMap(Role.valueOf).toSet))))
      case None               => F.pure(Right(None))
    }
  }

  /** This will only happen if [[authUser]] returns [[Left]] so probably never
    *  since it currently only returns [[Right]]
    *  but we fail it for some reason later it will default to responding with 401 */
  private val onFailure: AuthedRoutes[String, F] = Kleisli { _ =>
    OptionT.liftF(
      F.pure(Response[F](Status.Unauthorized))
    )
  }

  val authMiddleware: AuthMiddleware[F, Option[UserInfo]] = AuthMiddleware(authUser, onFailure)

}
