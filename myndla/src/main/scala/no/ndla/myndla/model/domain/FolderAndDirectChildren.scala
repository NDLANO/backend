/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla.model.domain

import java.util.UUID

case class FolderAndDirectChildren(
    folder: Option[Folder],
    childrenFolders: Seq[Folder],
    childrenResources: Seq[FolderResource]
) {
  def withoutChild(childId: UUID): FolderAndDirectChildren = {
    val filteredChildren = childrenFolders.filterNot(_.id == childId)
    copy(childrenFolders = filteredChildren)
  }
}
