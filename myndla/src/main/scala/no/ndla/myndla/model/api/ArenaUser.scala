/*
 * Part of NDLA myndla
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla.model.api

import no.ndla.myndla.model.domain.ArenaGroup
import sttp.tapir.Schema.annotations.description

@description("Arena owner data")
case class ArenaUser(
    @description("The owners id") id: Long,
    @description("The name") displayName: String,
    @description("The username") username: String,
    @description("The location") location: String,
    @description("Which groups the user belongs to") groups: List[ArenaGroup]
)

object ArenaUser {

  def from(user: no.ndla.myndla.model.domain.MyNDLAUser): ArenaUser = {
    val location = user.groups
      .find(_.isPrimarySchool)
      .map(_.displayName)
      .getOrElse(user.organization)
    ArenaUser(
      id = user.id,
      displayName = user.displayName,
      username = user.username,
      location = location,
      groups = user.arenaGroups
    )
  }

}
