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
import no.ndla.common.model.NDLADate
import scalikejdbc.*

import java.util.UUID
import scala.util.{Failure, Success, Try}

case class FolderResource(folderId: UUID, resourceId: UUID, rank: Int, favoritedDate: NDLADate) extends Rankable {
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
    import no.ndla.myndlaapi.uuidBinder
    for {
      folderId      <- rs.get[Try[UUID]](colNameWrapper("folder_id"))
      resourceId    <- rs.get[Try[UUID]](colNameWrapper("resource_id"))
      rank          <- Try(rs.int(colNameWrapper("rank")))
      favoritedDate <- Try(NDLADate.fromUtcDate(rs.localDateTime(colNameWrapper("favorited_date"))))
    } yield FolderResource(folderId = folderId, resourceId = resourceId, rank = rank, favoritedDate = favoritedDate)
  }

}
