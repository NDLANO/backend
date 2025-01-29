/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.service

import cats.implicits.*
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.{Clock, model}
import no.ndla.common.errors.ValidationException
import no.ndla.common.implicits.{OptionImplicit, TryQuestionMark}
import no.ndla.common.model.api.myndla.UpdatedMyNDLAUserDTO
import no.ndla.common.model.domain.myndla
import no.ndla.common.model.domain.myndla.{
  FolderStatus,
  MyNDLAGroup as DomainMyNDLAGroup,
  MyNDLAUser as DomainMyNDLAUser
}
import no.ndla.myndlaapi.integration.nodebb.NodeBBClient
import no.ndla.myndlaapi.model.api.{FolderDTO, OwnerDTO}
import no.ndla.myndlaapi.model.{api, domain}
import no.ndla.network.model.FeideAccessToken
import no.ndla.network.tapir.auth.Permission.LEARNINGPATH_API_ADMIN
import no.ndla.network.tapir.auth.TokenUser

import java.util.UUID
import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

trait FolderConverterService {
  this: Clock & NodeBBClient =>

  val folderConverterService: FolderConverterService

  class FolderConverterService extends StrictLogging {
    def toApiFolder(
        domainFolder: domain.Folder,
        breadcrumbs: List[api.BreadcrumbDTO],
        feideUser: Option[DomainMyNDLAUser],
        isOwner: Boolean
    ): Try[FolderDTO] = {
      def loop(
          folder: domain.Folder,
          crumbs: List[api.BreadcrumbDTO],
          feideUser: Option[DomainMyNDLAUser]
      ): Try[FolderDTO] = folder.subfolders
        .traverse(folder => {
          val newCrumb = api.BreadcrumbDTO(
            id = folder.id.toString,
            name = folder.name
          )
          val newCrumbs = crumbs :+ newCrumb
          loop(folder, newCrumbs, feideUser)
        })
        .flatMap(subFolders =>
          folder.resources
            .traverse(r => toApiResource(r, isOwner))
            .map(resources => {
              api.FolderDTO(
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
                owner = feideUser.map(user => OwnerDTO(user.displayName))
              )
            })
        )

      loop(domainFolder, breadcrumbs, feideUser)
    }

    def mergeFolder(existing: domain.Folder, updated: api.UpdatedFolderDTO): domain.Folder = {
      val name        = updated.name.getOrElse(existing.name)
      val status      = updated.status.flatMap(FolderStatus.valueOf).getOrElse(existing.status)
      val description = updated.description.orElse(existing.description)

      val shared = (existing.status, status) match {
        case (myndla.FolderStatus.PRIVATE, myndla.FolderStatus.SHARED) => Some(clock.now())
        case (myndla.FolderStatus.SHARED, myndla.FolderStatus.SHARED)  => existing.shared
        case (myndla.FolderStatus.SHARED, myndla.FolderStatus.PRIVATE) => None
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
        description = description,
        user = existing.user
      )
    }

    def mergeResource(existing: domain.Resource, updated: api.UpdatedResourceDTO): domain.Resource = {
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

    def mergeResource(existing: domain.Resource, newResource: api.NewResourceDTO): domain.Resource = {
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

    def toApiResource(domainResource: domain.Resource, isOwner: Boolean): Try[api.ResourceDTO] = {
      val resourceType = domainResource.resourceType
      val path         = domainResource.path
      val created      = domainResource.created
      val tags         = if (isOwner) domainResource.tags else List.empty
      val resourceId   = domainResource.resourceId

      Success(
        api.ResourceDTO(
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
        newFolder: api.NewFolderDTO,
        parentId: Option[UUID],
        newRank: Int
    ): Try[domain.NewFolderData] = {
      val newStatus = myndla.FolderStatus.valueOf(newFolder.status).getOrElse(myndla.FolderStatus.PRIVATE)

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

    def toApiUserData(
        domainUserData: DomainMyNDLAUser,
        arenaEnabledOrgs: List[String]
    ): model.api.myndla.MyNDLAUserDTO = {
      val arenaEnabled = getArenaEnabled(domainUserData, arenaEnabledOrgs)
      model.api.myndla.MyNDLAUserDTO(
        id = domainUserData.id,
        feideId = domainUserData.feideId,
        username = domainUserData.username,
        email = domainUserData.email,
        displayName = domainUserData.displayName,
        favoriteSubjects = domainUserData.favoriteSubjects,
        role = domainUserData.userRole.toString,
        organization = domainUserData.organization,
        groups = domainUserData.groups.map(toApiGroup),
        arenaEnabled = arenaEnabled,
        arenaAccepted = domainUserData.arenaAccepted,
        arenaGroups = domainUserData.arenaGroups
      )
    }

    def getArenaEnabled(userData: DomainMyNDLAUser, arenaEnabledOrgs: List[String]): Boolean =
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

    private def toApiGroup(group: DomainMyNDLAGroup): model.api.myndla.MyNDLAGroupDTO = {
      model.api.myndla.MyNDLAGroupDTO(
        id = group.id,
        displayName = group.displayName,
        isPrimarySchool = group.isPrimarySchool,
        parentId = group.parentId
      )
    }

    private def getArenaAccepted(
        arenaEnabled: Boolean,
        domainUserData: DomainMyNDLAUser,
        updatedUser: UpdatedMyNDLAUserDTO,
        feideToken: Option[FeideAccessToken]
    ): Try[Boolean] = {
      val arenaAccepted = updatedUser.arenaAccepted match {
        case Some(true) if arenaEnabled => true
        case Some(false)                => false
        case _                          => domainUserData.arenaAccepted
      }

      def getToken = feideToken.toTry(
        ValidationException(
          "arenaAccepted",
          "Tried to update arenaAccepted without a token connected to the feide user."
        )
      )

      (domainUserData.arenaAccepted, arenaAccepted) match {
        case (true, false) =>
          logger.info("User went from `arenaAccepted` true to false, calling nodebb to delete user.")
          for {
            token  <- getToken
            userId <- nodebb.getUserId(token)
            _      <- nodebb.deleteUser(userId, token)
          } yield arenaAccepted
        case (false, true) =>
          logger.info(
            s"User with ndla user id ${domainUserData.id} went from `arenaAccepted` false to true, calling nodebb."
          )
          for {
            token <- getToken
            _     <- nodebb.getUserId(token)
          } yield arenaAccepted
        case _ => Success(arenaAccepted)
      }
    }

    def mergeUserData(
        domainUserData: DomainMyNDLAUser,
        updatedUser: UpdatedMyNDLAUserDTO,
        updaterToken: Option[TokenUser],
        updaterUser: Option[DomainMyNDLAUser],
        arenaEnabledUsers: List[String],
        feideToken: Option[FeideAccessToken]
    ): Try[DomainMyNDLAUser] = {
      val favoriteSubjects = updatedUser.favoriteSubjects.getOrElse(domainUserData.favoriteSubjects)
      val arenaEnabled = {
        if (updaterToken.hasPermission(LEARNINGPATH_API_ADMIN) || updaterUser.exists(_.isAdmin))
          updatedUser.arenaEnabled.getOrElse(domainUserData.arenaEnabled)
        else
          domainUserData.arenaEnabled || arenaEnabledUsers.map(_.toLowerCase).contains(domainUserData.email.toLowerCase)
      }

      val arenaAccepted = getArenaAccepted(arenaEnabled, domainUserData, updatedUser, feideToken).?

      val arenaGroups =
        if (updaterUser.exists(_.isAdmin)) updatedUser.arenaGroups.getOrElse(domainUserData.arenaGroups)
        else domainUserData.arenaGroups

      Success(
        DomainMyNDLAUser(
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
          displayName = domainUserData.displayName,
          arenaGroups = arenaGroups,
          arenaAccepted = arenaAccepted
        )
      )
    }

    def toDomainResource(newResource: api.NewResourceDTO): domain.ResourceDocument = {
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
