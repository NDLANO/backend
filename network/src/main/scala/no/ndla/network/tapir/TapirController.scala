/*
 * Part of NDLA network
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */
package no.ndla.network.tapir

import cats.implicits.catsSyntaxEitherId
import com.typesafe.scalalogging.StrictLogging
import io.circe.{Decoder, Encoder}
import no.ndla.common.Clock
import no.ndla.common.configuration.HasBaseProps
import no.ndla.network.tapir.auth.{Permission, TokenUser}
import sttp.client3.Identity
import sttp.model.StatusCode
import sttp.monad.MonadError
import sttp.tapir.*
import sttp.tapir.server.{PartialServerEndpoint, ServerEndpoint}
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody

trait TapirController {
  this: HasBaseProps & Clock & TapirErrorHelpers =>
  trait TapirController extends StrictLogging {
    type Eff[A] = Identity[A]
    val enableSwagger: Boolean = true
    val serviceName: String    = this.getClass.getSimpleName
    protected val prefix: EndpointInput[Unit]
    val endpoints: List[ServerEndpoint[Any, Eff]]

    lazy val builtEndpoints: List[ServerEndpoint[Any, Eff]] = {
      this.endpoints.map(e => {
        ServerEndpoint(
          endpoint = e.endpoint.prependIn(this.prefix).tag(this.serviceName),
          securityLogic = e.securityLogic,
          logic = e.logic
        )
      })
    }

    /** Helper to simplify returning _both_ NoContent and some json body T from an endpoint */
    def noContentOrBodyOutput[T: Encoder: Decoder: Schema]: EndpointOutput.OneOf[Option[T], Option[T]] =
      oneOf[Option[T]](
        oneOfVariantValueMatcher(statusCode(StatusCode.Ok).and(jsonBody[Option[T]])) { case Some(_) => true },
        oneOfVariantValueMatcher(statusCode(StatusCode.NoContent).and(emptyOutputAs[Option[T]](None))) { case None =>
          true
        }
      )

    /** Helper function that returns function one can pass to `serverSecurityLogicPure` to require a specific scope for
      * some endpoint.
      */
    def requireScope(scope: Permission*): Option[TokenUser] => Either[AllErrors, TokenUser] = {
      case Some(user) if user.hasPermissions(scope) => user.asRight
      case Some(_)                                  => ErrorHelpers.forbidden.asLeft
      case None                                     => ErrorHelpers.unauthorized.asLeft
    }

    implicit class authlessEndpoint[A, I, E, O, R](self: Endpoint[Unit, I, AllErrors, O, R]) {
      def requirePermission[F[_]](
          requiredPermission: Permission*
      ): PartialServerEndpoint[Option[TokenUser], TokenUser, I, AllErrors, O, R, F] = {
        val newEndpoint   = self.securityIn(TokenUser.oauth2Input(requiredPermission))
        val authFunc      = requireScope(requiredPermission *)
        val securityLogic = (m: MonadError[F]) => (a: Option[TokenUser]) => m.unit(authFunc(a))
        PartialServerEndpoint(newEndpoint, securityLogic)
      }
    }

    implicit class authlessErrorlessEndpoint[A, I, E, O, R, X](self: Endpoint[Unit, I, X, O, R]) {
      def withOptionalUser[F[_]]: PartialServerEndpoint[Option[TokenUser], Option[TokenUser], I, X, O, R, F] = {
        val newEndpoint   = self.securityIn(TokenUser.oauth2Input(Seq.empty))
        val authFunc      = (tokenUser: Option[TokenUser]) => Right(tokenUser): Either[X, Option[TokenUser]]
        val securityLogic = (m: MonadError[F]) => (a: Option[TokenUser]) => m.unit(authFunc(a))
        PartialServerEndpoint(newEndpoint, securityLogic)
      }
    }
  }

}
