/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi

import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.ResourceType
import no.ndla.common.model.domain.myndla.{FolderStatus, MyNDLAUser, UserRole}
import no.ndla.myndlaapi.model.api
import no.ndla.myndlaapi.model.domain.{NewFolderData, Resource, ResourceDocument}
import no.ndla.myndlaapi.model.domain

import java.util.UUID

object TestData {
  val today: NDLADate = NDLADate.now()

  val emptyDomainResource: Resource = Resource(
    id = UUID.randomUUID(),
    feideId = "",
    resourceType = ResourceType.Article,
    path = "",
    created = NDLADate.now(),
    tags = List.empty,
    resourceId = "1",
    connection = None,
  )

  val emptyDomainFolder: domain.Folder = domain.Folder(
    id = UUID.randomUUID(),
    feideId = "",
    parentId = None,
    name = "",
    status = FolderStatus.PRIVATE,
    subfolders = List.empty,
    resources = List.empty,
    rank = 1,
    created = today,
    updated = today,
    shared = None,
    description = None,
    user = None,
  )

  val baseFolderDocument: NewFolderData =
    NewFolderData(parentId = None, name = "some-name", status = FolderStatus.PRIVATE, rank = 1, description = None)

  val baseResourceDocument: ResourceDocument = ResourceDocument(tags = List.empty, resourceId = "1")

  val emptyApiFolder: api.FolderDTO = api.FolderDTO(
    id = UUID.randomUUID(),
    name = "",
    status = "",
    subfolders = List.empty,
    resources = List.empty,
    breadcrumbs = List.empty,
    parentId = None,
    rank = 1,
    created = today,
    updated = today,
    shared = None,
    description = None,
    owner = None,
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
    displayName = "",
    email = "",
    arenaEnabled = false,
    lastSeen = today,
  )

}
