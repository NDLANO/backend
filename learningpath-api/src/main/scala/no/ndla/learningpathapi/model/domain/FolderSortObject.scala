/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

import enumeratum._

import java.util.UUID

sealed trait FolderSortObject extends EnumEntry

object FolderSortObject extends Enum[FolderSortObject] {
  val values: IndexedSeq[FolderSortObject] = findValues
  case class ResourceSorting(parentId: UUID) extends FolderSortObject
  case class FolderSorting(parentId: UUID)   extends FolderSortObject
  case class RootFolderSorting()             extends FolderSortObject
}
