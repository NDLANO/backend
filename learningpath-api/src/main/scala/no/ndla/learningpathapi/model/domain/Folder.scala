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

case class Resource(
    id: Option[Long],
    feideId: Option[FeideID],
    resourceId: Long,
    resourceType: String,
    tags: List[String]
) extends Content

trait DBResource {
  this: Props =>

  object DBResource extends SQLSyntaxSupport[Resource] {
    implicit val formats         = DefaultFormats
    override val tableName       = "resources"
    lazy override val schemaName = Some(props.MetaSchema)

    val JSonSerializer = FieldSerializer[Resource](ignore("id") orElse ignore("feideId"))

    def fromResultSetOpt(ls: ResultName[Resource])(rs: WrappedResultSet): Option[Resource] =
      rs.longOpt(ls.c("id")).map(_ => fromResultSet(ls)(rs))

    def fromResultSet(lp: SyntaxProvider[Resource])(rs: WrappedResultSet): Resource =
      fromResultSet(lp.resultName)(rs)

    def fromResultSetOpt(lp: SyntaxProvider[Resource])(rs: WrappedResultSet): Option[Resource] =
      fromResultSetOpt(lp.resultName)(rs)

    def fromResultSet(lp: ResultName[Resource])(rs: WrappedResultSet): Resource = {
      val metaData = read[Resource](rs.string(lp.c("document")))

      Resource(
        id = Some(rs.long(lp.c("id"))),
        feideId = Some(rs.string(lp.c("feide_id"))),
        resourceId = metaData.resourceId,
        resourceType = metaData.resourceType,
        tags = metaData.tags
      )
    }
  }
}

case class Folder(
    id: Option[Long],
    feideId: Option[FeideID],
    parentId: Option[Long] = None,
    name: String,
    status: FolderStatus.Value = FolderStatus.PRIVATE,
    data: List[FolderData]
) extends FolderContent

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

    def fromResultSetOpt(ls: ResultName[Folder])(rs: WrappedResultSet): Option[Folder] =
      rs.longOpt(ls.c("id")).map(_ => fromResultSet(ls)(rs))

    def fromResultSet(lp: SyntaxProvider[Folder])(rs: WrappedResultSet): Folder =
      fromResultSet(lp.resultName)(rs)

    def fromResultSet(lp: ResultName[Folder])(rs: WrappedResultSet): Folder = {
      val metaData = read[Folder](rs.string(lp.c("document")))

      Folder(
        id = Some(rs.long(lp.c("id"))),
        feideId = Some(rs.string(lp.c("feide_id"))),
        parentId = rs.longOpt(lp.c("parent_id")),
        name = metaData.name,
        status = metaData.status,
        data = List.empty
      )
    }
  }
}

case class FolderResource(folder_id: Long, resource_id: Long, feideId: FeideID)

trait DBFolderResource {
  this: Props =>

  object DBFolderResource extends SQLSyntaxSupport[FolderResource] {
    implicit val formats         = DefaultFormats
    override val tableName       = "folder_resources"
    lazy override val schemaName = Some(props.MetaSchema)
  }
}

object FolderStatus extends Enumeration {
  val PRIVATE: FolderStatus.Value = Value("private")
  val PUBLIC: FolderStatus.Value  = Value("public")

  def valueOf(s: String): Option[FolderStatus.Value] = {
    FolderStatus.values.find(_.toString == s)
  }

  def valueOf(s: Option[String]): Option[FolderStatus.Value] = {
    s match {
      case None    => None
      case Some(s) => valueOf(s)
    }
  }
}
