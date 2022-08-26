/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import no.ndla.learningpathapi.Props
import org.json4s.DefaultFormats
import scalikejdbc._

import java.util.UUID
import scala.util.Try

case class FolderResource(folderId: UUID, resourceId: UUID, rank: Int) extends Rankable {
  override val sortId: UUID = resourceId
  override val sortRank: Option[Int] = Some(rank)
}

trait DBFolderResource {
  this: Props =>

  object DBFolderResource extends SQLSyntaxSupport[FolderResource] {
    implicit val formats         = DefaultFormats
    override val tableName       = "folder_resources"
    lazy override val schemaName = Some(props.MetaSchema)

    def fromResultSet(lp: SyntaxProvider[FolderResource])(rs: WrappedResultSet): Try[FolderResource] =
      fromResultSet(s => lp.resultName.c(s), rs)

    def fromResultSet(colNameWrapper: String => String, rs: WrappedResultSet): Try[FolderResource] = {
      for {
        folderId   <- rs.get[Try[UUID]](colNameWrapper("folder_id"))
        resourceId <- rs.get[Try[UUID]](colNameWrapper("resource_id"))
        rank       <- Try(rs.int(colNameWrapper("rank")))
      } yield FolderResource(folderId = folderId, resourceId = resourceId, rank = rank)
    }

  }
}
