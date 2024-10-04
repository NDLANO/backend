/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.domain

import cats.implicits.catsSyntaxOptionId
import no.ndla.common.model.NDLADate
import no.ndla.network.model.FeideID
import scalikejdbc.*

import java.util.UUID
import scala.util.{Failure, Success, Try}

case class NewFolderData(
    parentId: Option[UUID],
    name: String,
    status: FolderStatus.Value,
    rank: Int,
    description: Option[String]
) {
  def toFullFolder(
      id: UUID,
      feideId: FeideID,
      resources: List[Resource],
      subfolders: List[Folder],
      created: NDLADate,
      updated: NDLADate,
      shared: Option[NDLADate],
      user: Option[MyNDLAUser]
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
      shared = shared,
      user = user
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
    rank: Int,
    created: NDLADate,
    updated: NDLADate,
    resources: List[Resource],
    subfolders: List[Folder],
    shared: Option[NDLADate],
    user: Option[MyNDLAUser]
) extends FeideContent
    with Rankable
    with CopyableFolder {
  override val sortId: UUID          = id
  override val sortRank: Option[Int] = rank.some

  def isPrivate: Boolean = this.status == FolderStatus.PRIVATE
  def isShared: Boolean  = this.status == FolderStatus.SHARED

  def isClonable: Try[Folder] = {
    if (this.isShared || this.feideId.contains(feideId)) Success(this)
    else Failure(InvalidStatusException(s"Only folders with status ${FolderStatus.SHARED.toString} can be cloned"))
  }
}

object Folder extends SQLSyntaxSupport[Folder] {
  override val tableName = "folders"

  def fromResultSet(lp: SyntaxProvider[Folder])(rs: WrappedResultSet): Try[Folder] = {
    val wrapper: String => String = (s: String) => lp.resultName.c(s)
    fromResultSet(wrapper)(rs)
  }

  def fromResultSet(rs: WrappedResultSet): Try[Folder] = fromResultSet((s: String) => s)(rs)

  def fromResultSet(colNameWrapper: String => String)(rs: WrappedResultSet): Try[Folder] = {
    import no.ndla.myndlaapi.{maybeUuidBinder, uuidBinder}

    val id          = rs.get[Try[UUID]](colNameWrapper("id"))
    val parentId    = rs.get[Option[UUID]](colNameWrapper("parent_id"))
    val feideId     = rs.string(colNameWrapper("feide_id"))
    val name        = rs.string(colNameWrapper("name"))
    val status      = FolderStatus.valueOfOrError(rs.string(colNameWrapper("status")))
    val description = rs.stringOpt(colNameWrapper("description"))
    val rank        = rs.int(colNameWrapper("rank"))
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
      shared = shared,
      user = None
    )
  }
}
