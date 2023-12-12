/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla.model.domain

import no.ndla.common.model.NDLADate
import no.ndla.network.model.FeideID
import org.json4s.FieldSerializer._
import org.json4s.ext.EnumNameSerializer
import org.json4s.{DefaultFormats, FieldSerializer, Formats}
import scalikejdbc._

import java.util.UUID
import scala.util.{Failure, Success, Try}

case class NewFolderData(
    parentId: Option[UUID],
    name: String,
    status: FolderStatus.Value,
    rank: Option[Int],
    description: Option[String]
) {
  def toFullFolder(
      id: UUID,
      feideId: FeideID,
      resources: List[Resource],
      subfolders: List[Folder],
      created: NDLADate,
      updated: NDLADate,
      shared: Option[NDLADate]
  ): Folder = {
    Folder(
      id = id,
      feideId = feideId,
      parentId = parentId,
      name = name,
      status = status,
      description = description,
      resources = resources,
      subfolders = subfolders,
      rank = rank,
      created = created,
      updated = updated,
      shared = shared
    )
  }
}

case class Folder(
    id: UUID,
    feideId: FeideID,
    parentId: Option[UUID],
    name: String,
    status: FolderStatus.Value,
    description: Option[String],
    rank: Option[Int],
    created: NDLADate,
    updated: NDLADate,
    resources: List[Resource],
    subfolders: List[Folder],
    shared: Option[NDLADate]
) extends FeideContent
    with Rankable
    with CopyableFolder {
  override val sortId: UUID          = id
  override val sortRank: Option[Int] = rank

  def isPrivate: Boolean = this.status == FolderStatus.PRIVATE
  def isShared: Boolean  = this.status == FolderStatus.SHARED

  def isClonable: Try[Folder] = {
    if (this.isShared) Success(this)
    else Failure(InvalidStatusException(s"Only folders with status ${FolderStatus.SHARED.toString} can be cloned"))
  }
}

object DBFolder extends SQLSyntaxSupport[Folder] {
  implicit val jsonEncoder: Formats = DefaultFormats + new EnumNameSerializer(FolderStatus)
  override val tableName            = "folders"

  val repositorySerializer: Formats = jsonEncoder + FieldSerializer[Folder](
    ignore("id") orElse
      ignore("feideId") orElse
      ignore("parentId")
  )

  def fromResultSet(lp: SyntaxProvider[Folder])(rs: WrappedResultSet): Try[Folder] =
    fromResultSet((s: String) => lp.resultName.c(s))(rs)

  def fromResultSet(rs: WrappedResultSet): Try[Folder] = fromResultSet((s: String) => s)(rs)

  def fromResultSet(colNameWrapper: String => String)(rs: WrappedResultSet): Try[Folder] = {
    import no.ndla.myndla.{maybeUuidBinder, uuidBinder}

    val id          = rs.get[Try[UUID]](colNameWrapper("id"))
    val parentId    = rs.get[Option[UUID]](colNameWrapper("parent_id"))
    val feideId     = rs.string(colNameWrapper("feide_id"))
    val name        = rs.string(colNameWrapper("name"))
    val status      = FolderStatus.valueOfOrError(rs.string(colNameWrapper("status")))
    val description = rs.stringOpt(colNameWrapper("description"))
    val rank        = rs.intOpt(colNameWrapper("rank"))
    val created     = NDLADate.fromUtcDate(rs.localDateTime(colNameWrapper("created")))
    val updated     = NDLADate.fromUtcDate(rs.localDateTime(colNameWrapper("updated")))
    val shared      = rs.localDateTimeOpt(colNameWrapper("shared")).map(NDLADate.fromUtcDate)

    for {
      id     <- id
      status <- status
    } yield Folder(
      id = id,
      parentId = parentId,
      feideId = feideId,
      name = name,
      status = status,
      description = description,
      resources = List.empty,
      subfolders = List.empty,
      rank = rank,
      created = created,
      updated = updated,
      shared = shared
    )
  }
}
