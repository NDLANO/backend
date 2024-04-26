/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.domain

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import no.ndla.network.model.FeideID
import scalikejdbc.*

import java.util.UUID

case class FolderUser(folderId: UUID, feideId: FeideID) {}

object FolderUser extends SQLSyntaxSupport[FolderUser] {
  implicit val encoder: Encoder[FolderUser] = deriveEncoder
  implicit val decoder: Decoder[FolderUser] = deriveDecoder

  override val tableName = "folder_users"
}
