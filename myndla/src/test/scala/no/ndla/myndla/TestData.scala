/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla

import no.ndla.common.model.NDLADate
import no.ndla.myndla.model.api
import no.ndla.myndla.model.domain.{
  Folder,
  FolderStatus,
  MyNDLAUser,
  NewFolderData,
  Resource,
  ResourceDocument,
  UserRole
}

import java.util.UUID

object TestData {
  val today: NDLADate = NDLADate.now()

  val emptyDomainResource: Resource = Resource(
    id = UUID.randomUUID(),
    feideId = "",
    resourceType = "",
    path = "",
    created = NDLADate.now(),
    tags = List.empty,
    resourceId = "1",
    connection = None
  )

  val emptyDomainFolder: Folder = Folder(
    id = UUID.randomUUID(),
    feideId = "",
    parentId = None,
    name = "",
    status = FolderStatus.PRIVATE,
    subfolders = List.empty,
    resources = List.empty,
    rank = None,
    created = today,
    updated = today,
    shared = None,
    description = None
  )

  val baseFolderDocument: NewFolderData = NewFolderData(
    parentId = None,
    name = "some-name",
    status = FolderStatus.PRIVATE,
    rank = None,
    description = None
  )

  val baseResourceDocument: ResourceDocument = ResourceDocument(
    tags = List.empty,
    resourceId = "1"
  )

  val emptyApiFolder: api.Folder = api.Folder(
    id = "",
    name = "",
    status = "",
    subfolders = List.empty,
    resources = List.empty,
    breadcrumbs = List.empty,
    parentId = None,
    rank = None,
    created = today,
    updated = today,
    shared = None,
    description = None,
    owner = None
  )

  val emptyMyNDLAUser: MyNDLAUser = MyNDLAUser(
    id = 1,
    feideId = "",
    favoriteSubjects = Seq.empty,
    userRole = UserRole.EMPLOYEE,
    lastUpdated = today,
    organization = "",
    groups = Seq.empty,
    username = "",
    email = "",
    arenaEnabled = false,
    displayName = "",
    shareName = false
  )

}
