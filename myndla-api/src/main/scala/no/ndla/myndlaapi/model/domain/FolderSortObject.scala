/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.domain

import enumeratum._

import java.util.UUID

sealed trait FolderSortObject extends EnumEntry

object FolderSortObject extends Enum[FolderSortObject] with CirceEnum[FolderSortObject] {
  val values: IndexedSeq[FolderSortObject] = findValues
  case class ResourceSorting(parentId: UUID) extends FolderSortObject
  case class FolderSorting(parentId: UUID)   extends FolderSortObject
  case class RootFolderSorting()             extends FolderSortObject
  case class SharedFolderSorting()           extends FolderSortObject
}
