/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

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
