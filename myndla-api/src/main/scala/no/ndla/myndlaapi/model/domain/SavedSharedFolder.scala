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
import scala.util.Try

case class SavedSharedFolder(
    folderId: UUID,
    feideId: FeideID,
    rank: Int
) {}

object SavedSharedFolder extends SQLSyntaxSupport[SavedSharedFolder] {
  implicit val encoder: Encoder[SavedSharedFolder] = deriveEncoder
  implicit val decoder: Decoder[SavedSharedFolder] = deriveDecoder

  override val tableName = "saved_shared_folder"

  def fromResultSet(sp: SyntaxProvider[SavedSharedFolder])(rs: WrappedResultSet): Try[SavedSharedFolder] =
    fromResultSet((s: String) => sp.resultName.c(s))(rs)

  def fromResultSet(rs: WrappedResultSet): Try[SavedSharedFolder] = fromResultSet((s: String) => s)(rs)

  def fromResultSet(colNameWrapper: String => String)(rs: WrappedResultSet): Try[SavedSharedFolder] = {
    import no.ndla.myndlaapi.uuidBinder
    for {
      folderId <- rs.get[Try[UUID]](colNameWrapper("folder_id"))
      feideId  <- Try(rs.string(colNameWrapper("feide_id")))
      rank     <- Try(rs.int(colNameWrapper("rank")))
    } yield SavedSharedFolder(
      folderId = folderId,
      feideId = feideId,
      rank = rank
    )
  }
}
