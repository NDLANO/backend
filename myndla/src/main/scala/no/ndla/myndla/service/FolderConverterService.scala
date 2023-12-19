/*
 * Part of NDLA myndla.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndla.service

import cats.implicits._
import no.ndla.common.Clock
import no.ndla.common.errors.ValidationException
import no.ndla.myndla.model.{api, domain}
import no.ndla.network.tapir.auth.Permission.LEARNINGPATH_API_ADMIN
import no.ndla.network.tapir.auth.TokenUser

import java.util.UUID
import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

trait FolderConverterService {
  this: Clock =>

  val folderConverterService: FolderConverterService

  class FolderConverterService {
    def toApiFolder(
        domainFolder: domain.Folder,
        breadcrumbs: List[api.Breadcrumb],
        feideUser: Option[domain.MyNDLAUser]
    ): Try[api.Folder] = {
      def loop(
          folder: domain.Folder,
          crumbs: List[api.Breadcrumb],
          feideUser: Option[domain.MyNDLAUser]
      ): Try[api.Folder] = folder.subfolders
        .traverse(folder => {
          val newCrumb = api.Breadcrumb(
            id = folder.id.toString,
            name = folder.name
          )
          val newCrumbs = crumbs :+ newCrumb
          loop(folder, newCrumbs, feideUser)
        })
        .flatMap(subFolders =>
          folder.resources
            .traverse(toApiResource)
            .map(resources => {
              api.Folder(
                id = folder.id.toString,
                name = folder.name,
                status = folder.status.toString,
                subfolders = subFolders.sortBy(_.rank),
                resources = resources.sortBy(_.rank),
                breadcrumbs = crumbs,
                parentId = folder.parentId.map(_.toString),
                rank = folder.rank,
                created = folder.created,
                updated = folder.updated,
                shared = folder.shared,
                description = folder.description,
                owner = feideUser.flatMap(user => if (user.shareName) Some(api.Owner(user.displayName)) else None)
              )
            })
        )

      loop(domainFolder, breadcrumbs, feideUser)
    }

    def mergeFolder(existing: domain.Folder, updated: api.UpdatedFolder): domain.Folder = {
      val name        = updated.name.getOrElse(existing.name)
      val status      = updated.status.flatMap(domain.FolderStatus.valueOf).getOrElse(existing.status)
      val description = updated.description.orElse(existing.description)

      val shared = (existing.status, status) match {
        case (domain.FolderStatus.PRIVATE, domain.FolderStatus.SHARED) => Some(clock.now())
        case (domain.FolderStatus.SHARED, domain.FolderStatus.SHARED)  => existing.shared
        case (domain.FolderStatus.SHARED, domain.FolderStatus.PRIVATE) => None
        case _                                                         => None
      }

      domain.Folder(
        id = existing.id,
        resources = existing.resources,
        subfolders = existing.subfolders,
        feideId = existing.feideId,
        parentId = existing.parentId,
        name = name,
        status = status,
        rank = existing.rank,
        created = existing.created,
        updated = clock.now(),
        shared = shared,
        description = description
      )
    }

    def mergeResource(existing: domain.Resource, updated: api.UpdatedResource): domain.Resource = {
      val tags       = updated.tags.getOrElse(existing.tags)
      val resourceId = updated.resourceId.getOrElse(existing.resourceId)

      domain.Resource(
        id = existing.id,
        feideId = existing.feideId,
        resourceType = existing.resourceType,
        path = existing.path,
        created = existing.created,
        tags = tags,
        resourceId = resourceId,
        connection = None
      )
    }

    def mergeResource(existing: domain.Resource, newResource: api.NewResource): domain.Resource = {
      val tags = newResource.tags.getOrElse(existing.tags)

      domain.Resource(
        id = existing.id,
        feideId = existing.feideId,
        resourceType = existing.resourceType,
        path = existing.path,
        created = existing.created,
        tags = tags,
        resourceId = newResource.resourceId,
        connection = existing.connection
      )
    }

    def toApiResource(domainResource: domain.Resource): Try[api.Resource] = {
      val resourceType = domainResource.resourceType
      val path         = domainResource.path
      val created      = domainResource.created
      val tags         = domainResource.tags
      val resourceId   = domainResource.resourceId

      Success(
        api.Resource(
          id = domainResource.id.toString,
          resourceType = resourceType,
          path = path,
          created = created,
          tags = tags,
          resourceId = resourceId,
          rank = domainResource.connection.map(_.rank)
        )
      )
    }

    def toNewFolderData(
        newFolder: api.NewFolder,
        parentId: Option[UUID],
        newRank: Option[Int]
    ): Try[domain.NewFolderData] = {
      val newStatus = domain.FolderStatus.valueOf(newFolder.status).getOrElse(domain.FolderStatus.PRIVATE)

      Success(
        domain.NewFolderData(
          parentId = parentId,
          name = newFolder.name,
          status = newStatus,
          rank = newRank,
          description = newFolder.description
        )
      )
    }

    def toApiUserData(domainUserData: domain.MyNDLAUser, arenaEnabledOrgs: List[String]): api.MyNDLAUser = {
      api.MyNDLAUser(
        id = domainUserData.id,
        feideId = domainUserData.feideId,
        username = domainUserData.username,
        email = domainUserData.email,
        displayName = domainUserData.displayName,
        favoriteSubjects = domainUserData.favoriteSubjects,
        role = domainUserData.userRole.toString,
        organization = domainUserData.organization,
        groups = domainUserData.groups.map(toApiGroup),
        arenaEnabled = getArenaEnabled(domainUserData, arenaEnabledOrgs),
        shareName = domainUserData.shareName
      )
    }

    def getArenaEnabled(userData: domain.MyNDLAUser, arenaEnabledOrgs: List[String]): Boolean =
      userData.arenaEnabled || arenaEnabledOrgs.contains(userData.organization)

    def domainToApiModel[Domain, Api](
        domainObjects: List[Domain],
        f: Domain => Try[Api]
    ): Try[List[Api]] = {

      @tailrec
      def loop(domainObjects: List[Domain], acc: List[Api]): Try[List[Api]] = {
        domainObjects match {
          case ::(head, next) =>
            f(head) match {
              case Failure(exception) => Failure(exception)
              case Success(apiObject) => loop(next, acc :+ apiObject)
            }
          case Nil => Success(acc)
        }
      }
      loop(domainObjects, List())
    }

    private def toApiGroup(group: domain.MyNDLAGroup): api.MyNDLAGroup = {
      api.MyNDLAGroup(
        id = group.id,
        displayName = group.displayName,
        isPrimarySchool = group.isPrimarySchool,
        parentId = group.parentId
      )
    }

    def mergeUserData(
        domainUserData: domain.MyNDLAUser,
        updatedUser: api.UpdatedMyNDLAUser,
        updaterToken: Option[TokenUser],
        updaterUser: Option[domain.MyNDLAUser]
    ): domain.MyNDLAUser = {
      val favoriteSubjects = updatedUser.favoriteSubjects.getOrElse(domainUserData.favoriteSubjects)
      val shareName        = updatedUser.shareName.getOrElse(domainUserData.shareName)
      val arenaEnabled = {
        if (updaterToken.hasPermission(LEARNINGPATH_API_ADMIN) || updaterUser.exists(_.isAdmin))
          updatedUser.arenaEnabled.getOrElse(domainUserData.arenaEnabled)
        else domainUserData.arenaEnabled
      }

      val arenaGroups =
        if (updaterUser.exists(_.isAdmin)) updatedUser.arenaGroups.getOrElse(domainUserData.arenaGroups)
        else domainUserData.arenaGroups

      domain.MyNDLAUser(
        id = domainUserData.id,
        feideId = domainUserData.feideId,
        favoriteSubjects = favoriteSubjects,
        userRole = domainUserData.userRole,
        lastUpdated = domainUserData.lastUpdated,
        organization = domainUserData.organization,
        groups = domainUserData.groups,
        username = domainUserData.username,
        email = domainUserData.email,
        arenaEnabled = arenaEnabled,
        shareName = shareName,
        displayName = domainUserData.displayName,
        arenaGroups = arenaGroups
      )
    }

    def toDomainResource(newResource: api.NewResource): domain.ResourceDocument = {
      val tags = newResource.tags.getOrElse(List.empty)
      domain.ResourceDocument(
        tags = tags,
        resourceId = newResource.resourceId
      )
    }

    def toUUIDValidated(maybeValue: Option[String], paramName: String): Try[UUID] = {
      val maybeUUID = maybeValue.map(value => Try(UUID.fromString(value)))
      maybeUUID match {
        case Some(Success(uuid)) => Success(uuid)
        case _ =>
          Failure(
            ValidationException(
              paramName,
              s"Invalid value for $paramName. Only UUID's allowed."
            )
          )
      }
    }

  }

}
