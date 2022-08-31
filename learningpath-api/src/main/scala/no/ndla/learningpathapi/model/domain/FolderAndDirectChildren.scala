/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.model.domain

case class FolderAndDirectChildren(
    folder: Folder,
    childrenFolders: Seq[Folder],
    childrenResources: Seq[FolderResource]
)
