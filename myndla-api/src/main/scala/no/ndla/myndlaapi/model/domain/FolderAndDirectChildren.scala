/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.domain

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
