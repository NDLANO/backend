/*
 * Part of NDLA myndla
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla.model.api

import sttp.tapir.Schema.annotations.description

@description("Arena owner data")
case class ArenaOwner(
    @description("The owners id") id: Long,
    @description("The name") displayName: String,
    @description("The username") username: String,
    @description("The location") location: String
)

object ArenaOwner {

  def from(user: no.ndla.myndla.model.domain.MyNDLAUser): ArenaOwner = {
    val location = user.groups
      .find(_.isPrimarySchool)
      .map(_.displayName)
      .getOrElse(user.organization)
    ArenaOwner(
      id = user.id,
      displayName = user.displayName,
      username = user.username,
      location = location
    )
  }

}
