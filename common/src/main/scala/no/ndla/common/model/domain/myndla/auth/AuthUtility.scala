/*
 * Part of NDLA common
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.domain.myndla.auth

import sttp.model.headers.{AuthenticationScheme, WWWAuthenticateChallenge}
import sttp.tapir.*
import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.EndpointInput.{AuthInfo, AuthType}

import scala.collection.immutable.ListMap

object AuthUtility {
  private val authScheme                                                  = AuthenticationScheme.Bearer.name
  private def filterHeaders(headers: List[String])                        = headers.filter(_.toLowerCase.startsWith(authScheme.toLowerCase))
  private def stringPrefixWithSpace                                       = Mapping.stringPrefixCaseInsensitiveForList(authScheme + " ")
  val feideTokenAuthCodec: Codec[List[String], Option[String], TextPlain] = {
    val codec = implicitly[Codec[List[String], Option[String], CodecFormat.TextPlain]]
    Codec
      .id[List[String], CodecFormat.TextPlain](codec.format, Schema.binary)
      .map(filterHeaders(_))(identity)
      .map(stringPrefixWithSpace)
      .mapDecode(codec.decode)(codec.encode)
      .schema(codec.schema)
  }

  def feideOauth() = {
    val authType: AuthType.ScopedOAuth2 = EndpointInput
      .AuthType
      .OAuth2(None, None, ListMap.empty, None)
      .requiredScopes(Seq.empty)

    EndpointInput.Auth(
      input = sttp.tapir.header("FeideAuthorization")(using feideTokenAuthCodec),
      challenge = WWWAuthenticateChallenge.bearer,
      authType = authType,
      info = AuthInfo.Empty.securitySchemeName("oauth2"),
    )
  }

}
