/*
 * Part of NDLA frontpage-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.frontpageapi.auth

import no.ndla.network.jwt.JWTExtractor
import sttp.tapir._
import sttp.tapir.CodecFormat.TextPlain

import scala.util.{Failure, Success, Try}

case class UserInfo(id: String, roles: Set[Role.Value]) {
  def canWrite: Boolean = hasRoles(Set(Role.WRITE))

  def hasRoles(rolesToCheck: Set[Role.Value]): Boolean = rolesToCheck.subsetOf(roles)

}

object UserInfo {

  case class UserInfoException() extends RuntimeException("Could not build `UserInfo` from token.")

  def fromToken(token: String): Try[UserInfo] = {
    val jWTExtractor = JWTExtractor(token)
    val userId       = jWTExtractor.extractUserId()
    val roles        = jWTExtractor.extractUserRoles()
    val userName     = jWTExtractor.extractUserName()
    val clientId     = jWTExtractor.extractClientId()

    userId.orElse(clientId).orElse(userName) match {
      case Some(userInfoName) => Success(UserInfo(userInfoName, roles.flatMap(Role.valueOf).toSet))
      case None               => Failure(UserInfoException())
    }
  }

  def encode(user: UserInfo): String = user.id
  def decode(s: String): DecodeResult[UserInfo] = fromToken(s) match {
    case Failure(ex)    => DecodeResult.Error(s, ex)
    case Success(value) => DecodeResult.Value(value)
  }
  implicit val userinfoCodec: Codec[String, UserInfo, TextPlain] = Codec.string.mapDecode(decode)(encode)
}
