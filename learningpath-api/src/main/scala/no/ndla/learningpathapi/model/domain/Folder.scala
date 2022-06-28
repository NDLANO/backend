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

case class ResourceDocument(tags: List[String]) {
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
      created = created
    )
}

case class Resource(
    id: UUID,
    feideId: FeideID,
    created: LocalDateTime,
    path: String,
    resourceType: String,
    tags: List[String]
) extends FeideContent

trait DBResource {
  this: Props =>

  object DBResource extends SQLSyntaxSupport[Resource] {
    implicit val formats         = DefaultFormats
    override val tableName       = "resources"
    lazy override val schemaName = Some(props.MetaSchema)

    val JSonSerializer = FieldSerializer[Resource](
      ignore("id") orElse
        ignore("feideId") orElse
        ignore("created")
    )

    def fromResultSetOpt(ls: ResultName[Resource])(rs: WrappedResultSet): Try[Option[Resource]] =
      rs.get[Option[UUID]](ls.c("id")).traverse(_ => fromResultSet(ls)(rs))

    def fromResultSet(lp: SyntaxProvider[Resource])(rs: WrappedResultSet): Try[Resource] =
      fromResultSet(lp.resultName)(rs)

    def fromResultSetOpt(lp: SyntaxProvider[Resource])(rs: WrappedResultSet): Try[Option[Resource]] =
      fromResultSetOpt(lp.resultName)(rs)

    def fromResultSet(lp: ResultName[Resource])(rs: WrappedResultSet): Try[Resource] = {
      val metaData     = read[ResourceDocument](rs.string(lp.c("document")))
      val id           = rs.get[Try[UUID]](lp.c("id"))
      val feideId      = rs.string(lp.c("feide_id"))
      val created      = rs.localDateTime(lp.c("created"))
      val path         = rs.string(lp.c("path"))
      val resourceType = rs.string(lp.c("resource_type"))

      id.map(id => metaData.toFullResource(id, path, resourceType, feideId, created))
    }
  }
}

case class FolderDocument(isFavorite: Boolean, name: String, status: FolderStatus.Value, data: List[FolderData]) {
  def toFullFolder(id: UUID, feideId: FeideID, parentId: Option[UUID]): Folder = {
    Folder(
      id = id,
      feideId = feideId,
      parentId = parentId,
      name = name,
      status = status,
      isFavorite = isFavorite,
      data = data
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

    def fromResultSetOpt(ls: ResultName[Folder])(rs: WrappedResultSet): Try[Option[Folder]] =
      rs.get[Option[UUID]](ls.c("id")).traverse(_ => fromResultSet(ls)(rs))

    def fromResultSet(lp: SyntaxProvider[Folder])(rs: WrappedResultSet): Try[Folder] =
      fromResultSet(lp.resultName)(rs)

    def fromResultSet(lp: ResultName[Folder])(rs: WrappedResultSet): Try[Folder] = {
      val metaData = read[FolderDocument](rs.string(lp.c("document")))
      val id       = rs.get[Try[UUID]](lp.c("id"))
      val feideId  = rs.string(lp.c("feide_id"))
      val parentId = rs.get[Option[UUID]](lp.c("parent_id"))

      id.map(id =>
        metaData.toFullFolder(
          id,
          feideId,
          parentId
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
