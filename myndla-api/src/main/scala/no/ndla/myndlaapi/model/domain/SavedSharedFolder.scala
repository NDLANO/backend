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

case class SavedSharedFolder(folderId: UUID, feideId: FeideID) {}

object SavedSharedFolder extends SQLSyntaxSupport[SavedSharedFolder] {
  implicit val encoder: Encoder[SavedSharedFolder] = deriveEncoder
  implicit val decoder: Decoder[SavedSharedFolder] = deriveDecoder

  override val tableName = "saved_shared_folder"
}
