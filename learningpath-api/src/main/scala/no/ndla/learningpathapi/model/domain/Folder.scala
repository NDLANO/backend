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
import java.time.LocalDateTime
import scala.util.Try
import cats.implicits._

case class ResourceDocument(tags: List[String], resourceId: Option[Long]) {
  def toFullResource(
      id: UUID,
      path: String,
      resourceType: String,
      feideId: String,
      created: LocalDateTime
  ): Resource =
    Resource(
      id = id,
      feideId = feideId,
      path = path,
      resourceType = resourceType,
      tags = tags,
      created = created,
      resourceId = resourceId
    )
}

case class Resource(
    id: UUID,
    feideId: FeideID,
    created: LocalDateTime,
    path: String,
    resourceType: String,
    tags: List[String],
    resourceId: Option[Long]
) extends FeideContent

trait DBResource {
  this: Props =>

  object DBResource extends SQLSyntaxSupport[Resource] {
    implicit val formats: Formats = DefaultFormats
    override val tableName        = "resources"
    lazy override val schemaName  = Some(props.MetaSchema)

    val JSonSerializer = FieldSerializer[Resource](
      ignore("id") orElse
        ignore("feideId") orElse
        ignore("created")
    )

    def fromResultSet(lp: SyntaxProvider[Resource])(rs: WrappedResultSet): Try[Resource] =
      fromResultSet(lp.resultName)(rs)
    def fromResultSet(lp: ResultName[Resource])(rs: WrappedResultSet): Try[Resource] =
      fromResultSet(s => lp.c(s))(rs)
    def fromResultSet(rs: WrappedResultSet): Try[Resource] =
      fromResultSet(s => s)(rs)
    def fromResultSetOpt(rs: WrappedResultSet): Try[Option[Resource]] =
      rs.get[Option[UUID]]("resource_id").traverse(_ => fromResultSet(rs))

    def fromResultSet(colNameWrapper: String => String)(rs: WrappedResultSet): Try[Resource] = {
      val jsonString   = rs.string(colNameWrapper("document"))
      val metaData     = read[ResourceDocument](jsonString)
      val id           = rs.get[Try[UUID]](colNameWrapper("id"))
      val feideId      = rs.string(colNameWrapper("feide_id"))
      val created      = rs.localDateTime(colNameWrapper("created"))
      val path         = rs.string(colNameWrapper("path"))
      val resourceType = rs.string(colNameWrapper("resource_type"))

      id.map(id => metaData.toFullResource(id, path, resourceType, feideId, created))
    }
  }
}

case class FolderDocument(isFavorite: Boolean, name: String, status: FolderStatus.Value) {
  def toFullFolder(
      id: UUID,
      feideId: FeideID,
      parentId: Option[UUID],
      resources: List[Resource],
      subfolders: List[Folder]
  ): Folder = {
    Folder(
      id = id,
      feideId = feideId,
      parentId = parentId,
      name = name,
      status = status,
      isFavorite = isFavorite,
      resources = resources,
      subfolders = subfolders
    )
  }
}

case class Folder(
    id: UUID,
    feideId: FeideID,
    parentId: Option[UUID],
    name: String,
    status: FolderStatus.Value,
    isFavorite: Boolean,
    subfolders: List[Folder],
    resources: List[Resource]
) extends FolderContent {
  def toDocument(): FolderDocument = FolderDocument(
    isFavorite = isFavorite,
    name = name,
    status = status
  )
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
      id.map(id =>
        metaData.toFullFolder(
          id,
          feideId,
          parentId,
          List.empty,
          List.empty
        )
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
