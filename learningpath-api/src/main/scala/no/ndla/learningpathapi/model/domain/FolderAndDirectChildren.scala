/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

case class FolderAndDirectChildren(
    folder: Option[Folder],
    childrenFolders: Seq[Folder],
    childrenResources: Seq[FolderResource]
)
