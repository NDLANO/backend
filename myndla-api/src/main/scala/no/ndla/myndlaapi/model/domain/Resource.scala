/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.domain

import cats.implicits.*
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import no.ndla.common.CirceUtil
import no.ndla.common.implicits.*
import no.ndla.common.model.NDLADate
import no.ndla.myndlaapi.Props
import no.ndla.common.model.domain.ResourceType
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
      connection: Option[ResourceConnection],
  ): Resource = Resource(
    id = id,
    feideId = feideId,
    path = path,
    resourceType = resourceType,
    tags = tags,
    created = created,
    resourceId = resourceId,
    connection = connection,
  )
}

object ResourceDocument {
  implicit val encoder: Encoder[ResourceDocument] = deriveEncoder
  implicit val decoder: Decoder[ResourceDocument] = deriveDecoder
}

case class Resource(
    id: UUID,
    feideId: FeideID,
    created: NDLADate,
    path: String,
    resourceType: ResourceType,
    tags: List[String],
    resourceId: String,
    connection: Option[ResourceConnection],
) extends FeideContent
    with Rankable
    with CopyableResource {
  override val sortId: UUID          = id
  override val sortRank: Option[Int] = connection.map(_.rank)
  override val rank: Option[Int]     = sortRank
}

object Resource {

  implicit val encoder: Encoder[Resource] = deriveEncoder
  implicit val decoder: Decoder[Resource] = deriveDecoder

  def fromResultSet(lp: SyntaxProvider[Resource], withConnection: Boolean)(rs: WrappedResultSet): Try[Resource] =
    fromResultSet(s => lp.resultName.c(s), withConnection)(rs)

  def fromResultSetSyntaxProviderWithConnection(lp: SyntaxProvider[Resource], sp: SyntaxProvider[ResourceConnection])(
      rs: WrappedResultSet
  ): Try[Option[Resource]] = {
    import no.ndla.myndlaapi.maybeUuidBinder
    rs.get[Option[UUID]](sp.resultName.c("resource_id"))
      .traverse(_ => fromResultSetSyntaxProvider(s => lp.resultName.c(s), sp)(rs))
  }

  def fromResultSetOpt(rs: WrappedResultSet, withConnection: Boolean): Try[Option[Resource]] = {
    import no.ndla.myndlaapi.maybeUuidBinder
    rs.get[Option[UUID]]("resource_id").traverse(_ => fromResultSet(rs, withConnection))
  }

  private def fromResultSet(rs: WrappedResultSet, withConnection: Boolean): Try[Resource] =
    fromResultSet(s => s, withConnection)(rs)

  private def fromResultSetSyntaxProvider(colNameWrapper: String => String, sp: SyntaxProvider[ResourceConnection])(
      rs: WrappedResultSet
  ): Try[Resource] = {
    val connection = ResourceConnection.fromResultSet(sp)(rs).toOption
    toResource(colNameWrapper, connection)(rs)
  }

  private def fromResultSet(colNameWrapper: String => String, withConnection: Boolean)(
      rs: WrappedResultSet
  ): Try[Resource] = {
    val connection =
      if (withConnection) ResourceConnection.fromResultSet(colNameWrapper, rs).toOption
      else None
    toResource(colNameWrapper, connection)(rs)
  }

  private def toResource(colNameWrapper: String => String, connection: Option[ResourceConnection])(
      rs: WrappedResultSet
  ): Try[Resource] = {
    import no.ndla.myndlaapi.uuidBinder
    for {
      id             <- rs.get[Try[UUID]](colNameWrapper("id"))
      jsonString      = rs.string(colNameWrapper("document"))
      feideId         = rs.string(colNameWrapper("feide_id"))
      created         = NDLADate.fromUtcDate(rs.localDateTime(colNameWrapper("created")))
      path            = rs.string(colNameWrapper("path"))
      resourceTypeStr = rs.string(colNameWrapper("resource_type"))
      resourceType   <- ResourceType
        .withNameOption(resourceTypeStr)
        .toTry(NDLASQLException(s"Invalid resource type when reading resource with id '$id' from database"))
      metaData <- CirceUtil.tryParseAs[ResourceDocument](jsonString)
    } yield metaData.toFullResource(id, path, resourceType, feideId, created, connection)
  }
}

class DBResource(using props: Props) extends SQLSyntaxSupport[Resource] {
  override val tableName: String          = "resources"
  override val schemaName: Option[String] = Some(props.MetaSchema)
}
