/*
 * Part of NDLA network
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.network.tapir.auth

import sttp.tapir.CodecFormat.TextPlain
import no.ndla.network.jwt.JWTExtractor
import sttp.tapir._

import scala.util.{Failure, Success, Try}

case class TokenUser(id: String, scopes: Set[Scope]) {
  def hasScope(scope: Scope): Boolean = scopes.contains(scope)
}

object TokenUser {
  case class UserInfoException() extends RuntimeException("Could not build `TokenUser` from token.")

  def fromToken(token: String): Try[TokenUser] = {
    val jWTExtractor = JWTExtractor(token)
    val userId       = jWTExtractor.extractUserId()
    val roles        = jWTExtractor.extractUserRoles()
    val userName     = jWTExtractor.extractUserName()
    val clientId     = jWTExtractor.extractClientId()

    userId.orElse(clientId).orElse(userName) match {
      case Some(userInfoName) => Success(TokenUser(userInfoName, Scope.fromStrings(roles)))
      case None               => Failure(UserInfoException())
    }
  }

  def encode(user: TokenUser): String = user.id
  def decode(s: String): DecodeResult[TokenUser] = fromToken(s) match {
    case Failure(ex)    => DecodeResult.Error(s, ex)
    case Success(value) => DecodeResult.Value(value)
  }
  implicit val userinfoCodec: Codec[String, TokenUser, TextPlain] = Codec.string.mapDecode(decode)(encode)
}
