/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import no.ndla.learningpathapi.Props
import org.json4s.FieldSerializer._
import org.json4s.native.Serialization._
import org.json4s.{DefaultFormats, FieldSerializer, Formats}
import org.json4s.ext.EnumNameSerializer
import scalikejdbc._

import java.util.UUID
import scala.util.Try

case class FolderDocument(name: String, status: FolderStatus.Value) {
  def toFullFolder(
      id: UUID,
      feideId: FeideID,
      parentId: Option[UUID],
      resources: List[Resource],
      subfolders: List[Folder],
      rank: Option[Int]
  ): Folder = {
    Folder(
      id = id,
      feideId = feideId,
      parentId = parentId,
      name = name,
      status = status,
      resources = resources,
      subfolders = subfolders,
      rank = rank
    )
  }
}

case class Folder(
    id: UUID,
    feideId: FeideID,
    parentId: Option[UUID],
    name: String,
    status: FolderStatus.Value,
    subfolders: List[Folder],
    resources: List[Resource],
    rank: Option[Int]
) extends FeideContent
    with Rankable {
  override val sortId: UUID          = id
  override val sortRank: Option[Int] = rank

  def toDocument: FolderDocument = FolderDocument(name = name, status = status)

  def isPrivate: Boolean = this.status == FolderStatus.PRIVATE

}

trait DBFolder {
  this: Props =>

  object DBFolder extends SQLSyntaxSupport[Folder] {
    implicit val jsonEncoder: Formats            = DefaultFormats + new EnumNameSerializer(FolderStatus)
    override val tableName                       = "folders"
    override lazy val schemaName: Option[String] = Some(props.MetaSchema)

    val repositorySerializer: Formats = jsonEncoder + FieldSerializer[Folder](
      ignore("id") orElse
        ignore("feideId") orElse
        ignore("parentId")
    )

    def fromResultSet(lp: SyntaxProvider[Folder])(rs: WrappedResultSet): Try[Folder] =
      fromResultSet((s: String) => lp.resultName.c(s))(rs)

    def fromResultSet(rs: WrappedResultSet): Try[Folder] = fromResultSet((s: String) => s)(rs)

    def fromResultSet(colNameWrapper: String => String)(rs: WrappedResultSet): Try[Folder] = {
      val metaData = read[FolderDocument](rs.string(colNameWrapper("document")))
      val id       = rs.get[Try[UUID]](colNameWrapper("id"))
      val feideId  = rs.string(colNameWrapper("feide_id"))
      val parentId = rs.get[Option[UUID]](colNameWrapper("parent_id"))
      val rank     = rs.intOpt(colNameWrapper("rank"))

      id.map(id =>
        metaData.toFullFolder(
          id = id,
          feideId = feideId,
          parentId = parentId,
          resources = List.empty,
          subfolders = List.empty,
          rank = rank
        )
      )
    }
  }
}
