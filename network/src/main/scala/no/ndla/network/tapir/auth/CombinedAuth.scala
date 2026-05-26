/*
 * Part of NDLA network
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir.auth

import cats.implicits.*
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.model.api.myndla.MyNDLAUserDTO
import no.ndla.network.clients.MyNDLAProvider
import no.ndla.network.model.*
import no.ndla.network.tapir.{AllErrors, ErrorHelpers}
import sttp.tapir.*
import sttp.tapir.server.PartialServerEndpoint

import scala.util.{Failure, Success}

case class CombinedAuth()(using
    ndlaAuth: NdlaAuth,
    feideAuth: FeideAuth,
    errorHelpers: ErrorHelpers,
    myNDLAApiClient: MyNDLAProvider,
) extends StrictLogging {

  extension [INPUT, OUTPUT, R](self: Endpoint[Unit, INPUT, AllErrors, OUTPUT, R]) {
    def withOptionalMyNDLAUserOrTokenUser[F[_]]
        : PartialServerEndpoint[(Option[TokenUser], Option[String]), CombinedUser, INPUT, AllErrors, OUTPUT, R, F] =
      self
        .securityIn(ndlaAuth.ndlaOptionalAuth)
        .securityIn(feideAuth.feideOptionalUncheckedAuth)
        .serverSecurityLogicPure { (maybeUser, maybeToken) =>
          val maybeMyNdlaUser = maybeToken.flatMap(getMyNdlaUser)
          val combinedUser    = OptionalCombinedUser(maybeUser, maybeMyNdlaUser)
          Right(combinedUser)
        }

    def withRequiredMyNDLAUserOrTokenUser[F[_]]: PartialServerEndpoint[
      (Option[TokenUser], Option[String]),
      CombinedUserRequired,
      INPUT,
      AllErrors,
      OUTPUT,
      R,
      F,
    ] = self
      .securityIn(ndlaAuth.ndlaOptionalAuth)
      .securityIn(feideAuth.feideOptionalUncheckedAuth)
      .serverSecurityLogicPure { (maybeUser, maybeToken) =>
        val maybeMyNdlaUser = maybeToken.flatMap(getMyNdlaUser)
        (maybeUser, maybeMyNdlaUser) match {
          case (Some(tokenUser), Some(ndlaUser)) => CombinedUserWithBoth(tokenUser, ndlaUser).asRight
          case (Some(tokenUser), None)           => tokenUser.toCombined.asRight
          case (None, Some(ndlaUser))            => CombinedUserWithMyNDLAUser(None, ndlaUser).asRight
          case _                                 => errorHelpers.unauthorized.asLeft
        }
      }
  }

  private def getMyNdlaUser(token: String): Option[MyNDLAUserDTO] = myNDLAApiClient.getUserWithFeideToken(token) match {
    case Failure(ex: HttpRequestException) if ex.code == 401 || ex.code == 403 => None
    case Failure(ex)                                                           =>
      logger.warn("Got unexpected exception when fetching myndla user", ex)
      None
    case Success(user) => Some(user)
  }
}
