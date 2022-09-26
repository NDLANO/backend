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
import org.json4s.{DefaultFormats, FieldSerializer, Formats}
import org.json4s.ext.EnumNameSerializer
import scalikejdbc._

import java.util.UUID
import scala.util.Try

case class NewFolderData(
    parentId: Option[UUID],
    name: String,
    status: FolderStatus.Value,
    rank: Option[Int]
) {
  def toFullFolder(
      id: UUID,
      feideId: FeideID,
      resources: List[Resource],
      subfolders: List[Folder]
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
    rank: Option[Int],
    resources: List[Resource],
    subfolders: List[Folder]
) extends FeideContent
    with Rankable {
  override val sortId: UUID          = id
  override val sortRank: Option[Int] = rank

  def isPrivate: Boolean = this.status == FolderStatus.PRIVATE
  def isShared: Boolean  = this.status == FolderStatus.SHARED
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
      val id       = rs.get[Try[UUID]](colNameWrapper("id"))
      val parentId = rs.get[Option[UUID]](colNameWrapper("parent_id"))
      val feideId  = rs.string(colNameWrapper("feide_id"))
      val name     = rs.string(colNameWrapper("name"))
      val status   = FolderStatus.valueOfOrError(rs.string(colNameWrapper("status")))
      val rank     = rs.intOpt(colNameWrapper("rank"))

      for {
        id     <- id
        status <- status
      } yield Folder(
        id = id,
        parentId = parentId,
        feideId = feideId,
        name = name,
        status = status,
        resources = List.empty,
        subfolders = List.empty,
        rank = rank
      )
    }
  }
}
