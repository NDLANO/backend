/*
 * Part of NDLA network
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.network.tapir

import cats.implicits.catsSyntaxEitherId
import com.typesafe.scalalogging.StrictLogging
import io.circe.{Decoder, Encoder}
import no.ndla.common.SchemaImplicits
import no.ndla.common.model.api.myndla.MyNDLAUserDTO
import no.ndla.common.model.domain.myndla.auth.AuthUtility
import no.ndla.network.clients.MyNDLAProvider
import no.ndla.network.model.{
  CombinedUser,
  CombinedUserRequired,
  CombinedUserWithBoth,
  CombinedUserWithMyNDLAUser,
  FeideUserWrapper,
  HttpRequestException,
  OptionalCombinedUser,
}
import no.ndla.network.tapir.auth.{Permission, TokenUser}
import sttp.client3.Identity
import sttp.model.StatusCode
import sttp.monad.MonadError
import sttp.tapir.*
import sttp.tapir.server.{PartialServerEndpoint, ServerEndpoint}
import no.ndla.network.tapir.NoNullJsonPrinter.jsonBody
import no.ndla.network.tapir.auth.TokenUser.{filterHeaders, stringPrefixWithSpace}
import sttp.model.headers.WWWAuthenticateChallenge
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.EndpointInput.{AuthInfo, AuthType}

import scala.collection.immutable.ListMap
import scala.util.{Failure, Success}

abstract class TapirController(using
    myNDLAApiClient: MyNDLAProvider,
    errorHelpers: ErrorHelpers,
    errorHandling: ErrorHandling,
) extends TapirErrorHandling
    with StrictLogging
    with SchemaImplicits {
  type Eff[A] = Identity[A]
  val enableSwagger: Boolean = true
  val serviceName: String    = this.getClass.getSimpleName
  protected val prefix: EndpointInput[Unit]
  val endpoints: List[ServerEndpoint[Any, Eff]]

  lazy val builtEndpoints: List[ServerEndpoint[Any, Eff]] = {
    this
      .endpoints
      .map(e => {
        ServerEndpoint(
          endpoint = e.endpoint.prependIn(this.prefix).tag(this.serviceName),
          securityLogic = e.securityLogic,
          logic = e.logic,
        )
      })
  }

  /** Helper to simplify returning _both_ NoContent and some json body T from an endpoint */
  def noContentOrBodyOutput[T: Encoder: Decoder: Schema]: EndpointOutput.OneOf[Option[T], Option[T]] = {
    val noContentVariant = noContent.and(emptyOutputAs[Option[T]](None))
    val okVariant        = statusCode(StatusCode.Ok).and(jsonBody[Option[T]])
    oneOf[Option[T]](
      oneOfVariantValueMatcher(okVariant) { case Some(_) =>
        true
      },
      oneOfVariantValueMatcher(noContentVariant) { case None =>
        true
      },
    )
  }

  /** Helper function that returns function one can pass to `serverSecurityLogicPure` to require a specific scope for
    * some endpoint.
    */
  def requireScope(scope: Permission*): Option[TokenUser] => Either[AllErrors, TokenUser] = {
    case Some(user) if user.hasPermissions(scope) => user.asRight
    case Some(_)                                  => errorHelpers.forbidden.asLeft
    case None                                     => errorHelpers.unauthorized.asLeft
  }

  private def encodeFeideUserWrapper(user: FeideUserWrapper): String            = user.token
  private def decodeFeideUserWrapper(s: String): DecodeResult[FeideUserWrapper] = {
    myNDLAApiClient.getDomainUser(s) match {
      case Failure(ex)   => DecodeResult.Error(s, ex)
      case Success(user) => DecodeResult.Value(FeideUserWrapper(s, Some(user)))
    }
  }

  private implicit val userinfoCodec: Codec[String, FeideUserWrapper, TextPlain] = Codec
    .string
    .mapDecode(decodeFeideUserWrapper)(encodeFeideUserWrapper)
  private val feideHeaderCodec                                                    = implicitly[Codec[List[String], Option[FeideUserWrapper], CodecFormat.TextPlain]]
  private val authCodec: Codec[List[String], Option[FeideUserWrapper], TextPlain] = Codec
    .id[List[String], CodecFormat.TextPlain](feideHeaderCodec.format, Schema.binary)
    .map(filterHeaders)(identity)
    .map(stringPrefixWithSpace)
    .mapDecode(feideHeaderCodec.decode)(feideHeaderCodec.encode)
    .schema(feideHeaderCodec.schema)

  private val feideWrapperAuth: EndpointInput.Auth[Option[FeideUserWrapper], AuthType.OAuth2] = {
    EndpointInput.Auth(
      input = sttp.tapir.header("FeideAuthorization")(using authCodec),
      challenge = WWWAuthenticateChallenge.bearer,
      authType = EndpointInput.AuthType.OAuth2(None, None, ListMap.empty, None),
      info = AuthInfo.Empty.securitySchemeName("oauth2"),
    )
  }

  implicit class authlessEndpoint[A, I, E, O, R](self: Endpoint[Unit, I, AllErrors, O, R]) {
    def requirePermission[F[_]](
        requiredPermission: Permission*
    ): PartialServerEndpoint[Option[TokenUser], TokenUser, I, AllErrors, O, R, F] = {
      val newEndpoint   = self.securityIn(TokenUser.oauth2Input(requiredPermission))
      val authFunc      = requireScope(requiredPermission*)
      val securityLogic = (m: MonadError[F]) => (a: Option[TokenUser]) => m.unit(authFunc(a))
      PartialServerEndpoint(newEndpoint, securityLogic)
    }

    def withFeideUser[F[_]]
        : PartialServerEndpoint[Option[FeideUserWrapper], FeideUserWrapper, I, AllErrors, O, R, F] = {
      val newEndpoint                                                               = self.securityIn(feideWrapperAuth)
      val authFunc: Option[FeideUserWrapper] => Either[AllErrors, FeideUserWrapper] = {
        case Some(value) => value.asRight
        case None        => errorHelpers.unauthorized.asLeft
      }
      val securityLogic = (m: MonadError[F]) => (a: Option[FeideUserWrapper]) => m.unit(authFunc(a))
      PartialServerEndpoint(newEndpoint, securityLogic)
    }

    def withOptionalFeideUser[F[_]]
        : PartialServerEndpoint[Option[FeideUserWrapper], Option[FeideUserWrapper], I, AllErrors, O, R, F] = {
      val newEndpoint                                                                       = self.securityIn(feideWrapperAuth)
      val authFunc: Option[FeideUserWrapper] => Either[AllErrors, Option[FeideUserWrapper]] = {
        case None     => None.asRight
        case someUser => someUser.asRight
      }
      val securityLogic = (m: MonadError[F]) => (a: Option[FeideUserWrapper]) => m.unit(authFunc(a))
      PartialServerEndpoint(newEndpoint, securityLogic)
    }

    def withOptionalMyNDLAUser[F[_]]
        : PartialServerEndpoint[Option[String], Option[MyNDLAUserDTO], I, AllErrors, O, R, F] = {
      val newEndpoint                                                          = self.securityIn(AuthUtility.feideOauth())
      val authFunc: Option[String] => Either[AllErrors, Option[MyNDLAUserDTO]] = (maybeToken: Option[String]) => {
        maybeToken match {
          case None        => Right(None)
          case Some(token) => myNDLAApiClient.getUserWithFeideToken(token) match {
              case Failure(ex: HttpRequestException) if ex.code == 401 => errorHelpers.unauthorized.asLeft
              case Failure(ex: HttpRequestException) if ex.code == 403 => errorHelpers.forbidden.asLeft
              case Failure(ex)                                         =>
                logger.error("Got exception when fetching user", ex)
                errorHelpers.generic.asLeft
              case Success(user) => Some(user).asRight
            }
        }
      }
      val securityLogic = (m: MonadError[F]) => (a: Option[String]) => m.unit(authFunc(a))
      PartialServerEndpoint(newEndpoint, securityLogic)
    }

    def withOptionalMyNDLAUserOrTokenUser[F[_]]
        : PartialServerEndpoint[(Option[TokenUser], Option[String]), CombinedUser, I, AllErrors, O, R, F] = {
      val newEndpoint = self.securityIn(TokenUser.oauth2Input(Seq.empty)).securityIn(AuthUtility.feideOauth())

      val authFunc: ((Option[TokenUser], Option[String])) => Either[AllErrors, CombinedUser] =
        (userInputOptions: (Option[TokenUser], Option[String])) => {
          val maybeUser  = userInputOptions._1
          val maybeToken = userInputOptions._2

          val myndlaUser = maybeToken.flatMap { token =>
            myNDLAApiClient.getUserWithFeideToken(token) match {
              case Failure(ex: HttpRequestException) if ex.code == 401 || ex.code == 403 => None
              case Failure(ex)                                                           =>
                logger.warn("Got unexpected exception when fetching myndla user", ex)
                None
              case Success(user) => Some(user)
            }
          }

          val combinedUser = OptionalCombinedUser(maybeUser, myndlaUser)
          Right(combinedUser)
        }
      val securityLogic = (m: MonadError[F]) => (a: (Option[TokenUser], Option[String])) => m.unit(authFunc(a))
      PartialServerEndpoint(newEndpoint, securityLogic)
    }

    def withRequiredMyNDLAUserOrTokenUser[F[_]]
        : PartialServerEndpoint[(Option[TokenUser], Option[String]), CombinedUserRequired, I, AllErrors, O, R, F] = {
      val newEndpoint = self.securityIn(TokenUser.oauth2Input(Seq.empty)).securityIn(AuthUtility.feideOauth())

      val authFunc: ((Option[TokenUser], Option[String])) => Either[AllErrors, CombinedUserRequired] =
        (userInputOptions: (Option[TokenUser], Option[String])) => {
          val maybeUser  = userInputOptions._1
          val maybeToken = userInputOptions._2

          val myndlaUser = maybeToken.flatMap { token =>
            myNDLAApiClient.getUserWithFeideToken(token) match {
              case Failure(ex: HttpRequestException) if ex.code == 401 || ex.code == 403 => None
              case Failure(ex)                                                           =>
                logger.warn("Got unexpected exception when fetching myndla user", ex)
                None
              case Success(user) => Some(user)
            }
          }

          (maybeUser, myndlaUser) match {
            case (Some(tokenUser), Some(ndlaUser)) => CombinedUserWithBoth(tokenUser, ndlaUser).asRight
            case (Some(tokenUser), None)           => tokenUser.toCombined.asRight
            case (None, Some(ndlaUser))            => CombinedUserWithMyNDLAUser(None, ndlaUser).asRight
            case _                                 => errorHelpers.unauthorized.asLeft
          }
        }
      val securityLogic = (m: MonadError[F]) => (a: (Option[TokenUser], Option[String])) => m.unit(authFunc(a))
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

  private val zeroNoContentHeader: EndpointIO.FixedHeader[Unit] = header("Content-Length", "0")

  // NOTE: We use our own emptyOutput to add the `Content-Length` header
  //       to signify no output body, since openapi-fetch doesn't react nicely
  //       200 OK responses without body and no `Content-Length` header.
  def emptyOutput: EndpointOutput[Unit] = sttp.tapir.emptyOutput.and(zeroNoContentHeader)
  def noContent: EndpointOutput[Unit]   = statusCode(StatusCode.NoContent)
}
