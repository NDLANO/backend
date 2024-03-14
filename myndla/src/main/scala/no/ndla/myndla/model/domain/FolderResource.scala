/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla.model.domain

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import scalikejdbc.*

import java.util.UUID
import scala.util.Try

case class FolderResource(folderId: UUID, resourceId: UUID, rank: Int) extends Rankable {
  override val sortId: UUID          = resourceId
  override val sortRank: Option[Int] = Some(rank)
}

object FolderResource extends SQLSyntaxSupport[FolderResource] {
  implicit val encoder: Encoder[FolderResource] = deriveEncoder
  implicit val decoder: Decoder[FolderResource] = deriveDecoder

  override val tableName = "folder_resources"

  def fromResultSet(lp: SyntaxProvider[FolderResource])(rs: WrappedResultSet): Try[FolderResource] =
    fromResultSet(s => lp.resultName.c(s), rs)

  def fromResultSet(colNameWrapper: String => String, rs: WrappedResultSet): Try[FolderResource] = {
    import no.ndla.myndla.uuidBinder
    for {
      folderId   <- rs.get[Try[UUID]](colNameWrapper("folder_id"))
      resourceId <- rs.get[Try[UUID]](colNameWrapper("resource_id"))
      rank       <- Try(rs.int(colNameWrapper("rank")))
    } yield FolderResource(folderId = folderId, resourceId = resourceId, rank = rank)
  }

}
