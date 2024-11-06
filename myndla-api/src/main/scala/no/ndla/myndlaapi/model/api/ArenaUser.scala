/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.api

import com.typesafe.scalalogging.StrictLogging
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.common.model.domain
import sttp.tapir.Schema.annotations.description

@description("Arena owner data")
case class ArenaUser(
    @description("The owners id") id: Long,
    @description("The name") displayName: String,
    @description("The username") username: String,
    @description("The location") location: String,
    @description("Which groups the user belongs to") groups: List[domain.myndla.ArenaGroup]
)

object ArenaUser extends StrictLogging {

  def from(user: domain.myndla.MyNDLAUser): ArenaUser = {
    val location = user.groups
      .find(_.isPrimarySchool)
      .map(_.displayName)
      .getOrElse(user.organization.getOrElse(""))

    ArenaUser(
      id = user.id,
      displayName = user.displayName,
      username = user.username,
      location = location,
      groups = user.arenaGroups
    )
  }

  implicit val arenaUserEncoder: Encoder[ArenaUser] = deriveEncoder
  implicit val arenaUserDecoder: Decoder[ArenaUser] = deriveDecoder

}
