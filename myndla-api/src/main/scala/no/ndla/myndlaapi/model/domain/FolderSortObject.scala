/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.domain

import enumeratum.*

import java.util.UUID

sealed trait FolderSortObject extends EnumEntry

object FolderSortObject extends Enum[FolderSortObject] with CirceEnum[FolderSortObject] {
  val values: IndexedSeq[FolderSortObject] = findValues
  sealed case class ResourceSorting(parentId: Option[UUID]) extends FolderSortObject
  sealed case class FolderSorting(parentId: UUID)           extends FolderSortObject
  sealed case class RootFolderSorting()                     extends FolderSortObject
  sealed case class SharedFolderSorting()                   extends FolderSortObject
}
