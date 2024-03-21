/*
 * Part of NDLA myndla-api.
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.domain

import cats.implicits.*
import com.scalatsi.TypescriptType.{TSLiteralString, TSUnion}
import com.scalatsi.{TSNamedType, TSType}
import enumeratum.*
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.CirceUtil
import no.ndla.common.implicits.OptionImplicit
import no.ndla.common.model.NDLADate
import no.ndla.network.model.FeideID
import scalikejdbc.*

import java.util.UUID
import scala.util.Try

case class ResourceDocument(tags: List[String], resourceId: String) {
  def toFullResource(
      id: UUID,
      path: String,
      resourceType: ResourceType,
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

object ResourceDocument {
  implicit val encoder: Encoder[ResourceDocument] = deriveEncoder
  implicit val decoder: Decoder[ResourceDocument] = deriveDecoder
}

sealed abstract class ResourceType(override val entryName: String) extends EnumEntry {}

object ResourceType extends Enum[ResourceType] with CirceEnum[ResourceType] {
  override val values: IndexedSeq[ResourceType] = findValues

  implicit val enumTsType: TSNamedType[ResourceType] =
    TSType.alias[ResourceType]("ResourceType", TSUnion(values.map(e => TSLiteralString(e.entryName))))

  case object Concept           extends ResourceType("concept")
  case object Image             extends ResourceType("image")
  case object Audio             extends ResourceType("audio")
  case object Multidisciplinary extends ResourceType("multidisciplinary")
  case object Article           extends ResourceType("article")
  case object Learningpath      extends ResourceType("learningpath")
  case object Video             extends ResourceType("video")
  case object Folder            extends ResourceType("folder")
}

case class Resource(
    id: UUID,
    feideId: FeideID,
    created: NDLADate,
    path: String,
    resourceType: ResourceType,
    tags: List[String],
    resourceId: String,
    connection: Option[FolderResource]
) extends FeideContent
    with Rankable
    with CopyableResource {
  override val sortId: UUID          = id
  override val sortRank: Option[Int] = connection.map(_.rank)
}

object Resource extends SQLSyntaxSupport[Resource] {
  override val tableName = "resources"

  implicit val encoder: Encoder[Resource] = deriveEncoder
  implicit val decoder: Decoder[Resource] = deriveDecoder

  def fromResultSet(lp: SyntaxProvider[Resource], withConnection: Boolean)(rs: WrappedResultSet): Try[Resource] =
    fromResultSet(s => lp.resultName.c(s), withConnection)(rs)

  def fromResultSetOpt(rs: WrappedResultSet, withConnection: Boolean): Try[Option[Resource]] = {
    import no.ndla.myndlaapi.maybeUuidBinder
    rs.get[Option[UUID]]("resource_id").traverse(_ => fromResultSet(rs, withConnection))
  }

  private def fromResultSet(rs: WrappedResultSet, withConnection: Boolean): Try[Resource] =
    fromResultSet(s => s, withConnection)(rs)

  private def fromResultSet(
      colNameWrapper: String => String,
      withConnection: Boolean
  )(rs: WrappedResultSet): Try[Resource] = {
    import no.ndla.myndlaapi.uuidBinder

    val connection =
      if (withConnection) FolderResource.fromResultSet(colNameWrapper, rs).toOption
      else None

    for {
      id <- rs.get[Try[UUID]](colNameWrapper("id"))
      jsonString      = rs.string(colNameWrapper("document"))
      feideId         = rs.string(colNameWrapper("feide_id"))
      created         = NDLADate.fromUtcDate(rs.localDateTime(colNameWrapper("created")))
      path            = rs.string(colNameWrapper("path"))
      resourceTypeStr = rs.string(colNameWrapper("resource_type"))
      resourceType <- ResourceType
        .withNameOption(resourceTypeStr)
        .toTry(NDLASQLException(s"Invalid resource type when reading resource with id '$id' from database"))
      metaData <- CirceUtil.tryParseAs[ResourceDocument](jsonString)

    } yield metaData.toFullResource(id, path, resourceType, feideId, created, connection)
  }
}
