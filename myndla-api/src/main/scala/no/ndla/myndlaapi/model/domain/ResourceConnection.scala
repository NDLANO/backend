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
import scala.util.Try

case class ResourceConnection(folderId: Option[UUID], resourceId: UUID, rank: Int, favoritedDate: NDLADate)
    extends Rankable {
  override val sortId: UUID          = resourceId
  override val sortRank: Option[Int] = Some(rank)
}

object ResourceConnection extends SQLSyntaxSupport[ResourceConnection] {
  implicit val encoder: Encoder[ResourceConnection] = deriveEncoder
  implicit val decoder: Decoder[ResourceConnection] = deriveDecoder

  override val tableName = "resource_connections"

  def fromResultSet(lp: SyntaxProvider[ResourceConnection])(rs: WrappedResultSet): Try[ResourceConnection] =
    fromResultSet(s => lp.resultName.c(s), rs)

  def fromResultSet(colNameWrapper: String => String, rs: WrappedResultSet): Try[ResourceConnection] = {
    import no.ndla.myndlaapi.{uuidBinder, maybeUuidBinder}
    val folderId = rs.get[Option[UUID]](colNameWrapper("folder_id"))
    for {
      resourceId    <- rs.get[Try[UUID]](colNameWrapper("resource_id"))
      rank          <- Try(rs.int(colNameWrapper("rank")))
      favoritedDate <- Try(NDLADate.fromUtcDate(rs.localDateTime(colNameWrapper("favorited_date"))))
    } yield ResourceConnection(folderId = folderId, resourceId = resourceId, rank = rank, favoritedDate = favoritedDate)
  }

}
