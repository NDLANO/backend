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
import no.ndla.network.model.*
import no.ndla.network.tapir.{AllErrors, ErrorHelpers}
import sttp.tapir.*
import sttp.tapir.server.PartialServerEndpoint

case class CombinedAuth()(using ndlaAuth: NdlaAuth, feideAuth: FeideAuth, errorHelpers: ErrorHelpers)
    extends StrictLogging {

  extension [INPUT, OUTPUT, R](self: Endpoint[Unit, INPUT, AllErrors, OUTPUT, R]) {
    def withOptionalMyNDLAUserOrTokenUser[F[_]]: PartialServerEndpoint[
      (Option[TokenUser], Option[FeideUserWrapper]),
      CombinedUser,
      INPUT,
      AllErrors,
      OUTPUT,
      R,
      F,
    ] = self
      .securityIn(ndlaAuth.ndlaOptionalAuth)
      .securityIn(feideAuth.feideOptionalAuth)
      .serverSecurityLogicPure(OptionalCombinedUser.apply.tupled.andThen(Right(_)))

    def withRequiredMyNDLAUserOrTokenUser[F[_]]: PartialServerEndpoint[
      (Option[TokenUser], Option[FeideUserWrapper]),
      CombinedUserRequired,
      INPUT,
      AllErrors,
      OUTPUT,
      R,
      F,
    ] = self
      .securityIn(ndlaAuth.ndlaOptionalAuth)
      .securityIn(feideAuth.feideOptionalAuth)
      .serverSecurityLogicPure {
        case (Some(ndlaUser), Some(feideUser)) => CombinedUserWithBoth(ndlaUser, feideUser).asRight
        case (Some(ndlaUser), None)            => ndlaUser.toCombined.asRight
        case (None, Some(feideUser))           => CombinedUserWithMyNDLAUser(None, feideUser).asRight
        case (None, None)                      => errorHelpers.unauthorized.asLeft
      }
  }
}
