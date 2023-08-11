/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import cats.implicits._
import no.ndla.common.model.NDLADate
import no.ndla.learningpathapi.Props
import org.json4s.FieldSerializer._
import org.json4s.native.Serialization._
import org.json4s.{DefaultFormats, FieldSerializer, Formats}
import scalikejdbc._

import java.util.UUID
import scala.util.Try

case class ResourceDocument(tags: List[String], resourceId: String) {
  def toFullResource(
      id: UUID,
      path: String,
      resourceType: String,
      feideId: String,
      created: NDLADate,
      connection: Option[FolderResource]
  ): Resource =
    Resource(
      id = id,
      feideId = feideId,
      path = path,
      resourceType = resourceType,
      tags = tags,
      created = created,
      resourceId = resourceId,
      connection = connection
    )
}

case class Resource(
    id: UUID,
    feideId: FeideID,
    created: NDLADate,
    path: String,
    resourceType: String,
    tags: List[String],
    resourceId: String,
    connection: Option[FolderResource]
) extends FeideContent
    with Rankable
    with CopyableResource {
  override val sortId: UUID          = id
  override val sortRank: Option[Int] = connection.map(_.rank)
}

trait DBResource {
  this: Props with DBFolderResource =>
  object DBResource extends SQLSyntaxSupport[Resource] {
    implicit val formats: Formats = DefaultFormats
    override val tableName        = "resources"
    lazy override val schemaName  = Some(props.MetaSchema)

    val JSonSerializer: FieldSerializer[Resource] = FieldSerializer[Resource](
      ignore("id") orElse
        ignore("feideId") orElse
        ignore("created")
    )

    def fromResultSet(lp: SyntaxProvider[Resource], withConnection: Boolean)(rs: WrappedResultSet): Try[Resource] =
      fromResultSet(s => lp.resultName.c(s), withConnection)(rs)

    def fromResultSetOpt(rs: WrappedResultSet, withConnection: Boolean): Try[Option[Resource]] =
      rs.get[Option[UUID]]("resource_id").traverse(_ => fromResultSet(rs, withConnection))

    private def fromResultSet(rs: WrappedResultSet, withConnection: Boolean): Try[Resource] =
      fromResultSet(s => s, withConnection)(rs)

    private def fromResultSet(
        colNameWrapper: String => String,
        withConnection: Boolean
    )(rs: WrappedResultSet): Try[Resource] = {
      val connection =
        if (withConnection) DBFolderResource.fromResultSet(colNameWrapper, rs).toOption
        else None

      for {
        id <- rs.get[Try[UUID]](colNameWrapper("id"))
        jsonString   = rs.string(colNameWrapper("document"))
        feideId      = rs.string(colNameWrapper("feide_id"))
        created      = NDLADate.fromUtcDate(rs.localDateTime(colNameWrapper("created")))
        path         = rs.string(colNameWrapper("path"))
        resourceType = rs.string(colNameWrapper("resource_type"))
        metaData <- Try(read[ResourceDocument](jsonString))

      } yield metaData.toFullResource(id, path, resourceType, feideId, created, connection)
    }
  }
}
