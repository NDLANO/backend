/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.service

import cats.implicits.*
import no.ndla.common.Clock
import no.ndla.common.errors.NotFoundException
import no.ndla.common.implicits.TryQuestionMark
import no.ndla.common.model.api.SingleResourceStats
import no.ndla.common.model.domain.ResourceType
import no.ndla.myndlaapi.FavoriteFolderDefaultName
import no.ndla.myndlaapi.model.api.{ExportedUserData, Folder, Resource}
import no.ndla.myndlaapi.model.{api, domain}
import no.ndla.myndlaapi.model.domain.FolderStatus
import no.ndla.myndlaapi.repository.{FolderRepository, UserRepository}
import no.ndla.network.clients.FeideApiClient
import no.ndla.network.model.{FeideAccessToken, FeideID}
import scalikejdbc.DBSession

import java.util.UUID
import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

trait FolderReadService {
  this: FolderConverterService
    with FolderRepository
    with UserRepository
    with FeideApiClient
    with Clock
    with ConfigService
    with UserService =>

  val folderReadService: FolderReadService

  class FolderReadService {

    private def getFoldersAuthenticated(
        includeSubfolders: Boolean,
        includeResources: Boolean,
        feideId: FeideID
    ): Try[List[Folder]] = {
      folderRepository.rollbackOnFailure(session => {
        for {
          topFolders   <- folderRepository.foldersWithFeideAndParentID(None, feideId)(session)
          withFavorite <- mergeWithFavorite(topFolders, feideId)
          withData     <- getSubfolders(withFavorite, includeSubfolders, includeResources)(session)
          feideUser    <- userRepository.userWithFeideId(feideId)(session)
          apiFolders <- folderConverterService.domainToApiModel(
            withData,
            v =>
              folderConverterService.toApiFolder(
                v,
                List(api.Breadcrumb(id = v.id.toString, name = v.name)),
                feideUser,
                feideUser.exists(_.feideId == v.feideId)
              )
          )
          sorted = apiFolders.sortBy(_.rank)
        } yield sorted
      })
    }

    def getSharedFolder(id: UUID, maybeFeideToken: Option[FeideAccessToken]): Try[Folder] = {
      implicit val session: DBSession = folderRepository.getSession(true)
      for {
        feideId <- maybeFeideToken.traverse(token => feideApiClient.getFeideID(Some(token)))
        folderWithResources <- folderRepository.getFolderAndChildrenSubfoldersWithResources(
          id,
          FolderStatus.SHARED,
          feideId
        )
        folderWithContent <- getWith404IfNone(id, Success(folderWithResources))
        _ <-
          if (folderWithContent.isShared || feideId.contains(folderWithContent.feideId)) Success(())
          else Failure(NotFoundException("Folder does not exist"))
        folderAsTopFolder = folderWithContent.copy(parentId = None)
        breadcrumbs <- getBreadcrumbs(folderAsTopFolder)
        feideUser   <- userRepository.userWithFeideId(folderWithContent.feideId)
        converted <- folderConverterService.toApiFolder(
          folderAsTopFolder,
          breadcrumbs,
          feideUser,
          feideId.contains(folderWithContent.feideId)
        )
      } yield converted
    }

    private[service] def mergeWithFavorite(folders: List[domain.Folder], feideId: FeideID): Try[List[domain.Folder]] = {
      for {
        favorite <- if (folders.isEmpty) createFavorite(feideId).map(_.some) else Success(None)
        combined = favorite.toList ++ folders
      } yield combined
    }

    private def withResources(folderId: UUID, shouldIncludeResources: Boolean)(implicit
        session: DBSession
    ): Try[domain.Folder] =
      folderRepository
        .folderWithId(folderId)
        .flatMap(folder => {
          val folderResources =
            if (shouldIncludeResources) folderRepository.getFolderResources(folderId)
            else Success(List.empty)

          folderResources.map(res => folder.copy(resources = res))
        })

    def getSingleFolderWithContent(
        folderId: UUID,
        includeSubfolders: Boolean,
        includeResources: Boolean
    )(implicit session: DBSession): Try[domain.Folder] = {
      val folderWithContent = (includeSubfolders, includeResources) match {
        case (true, true)                    => folderRepository.getFolderAndChildrenSubfoldersWithResources(folderId)
        case (true, false)                   => folderRepository.getFolderAndChildrenSubfolders(folderId)
        case (false, shouldIncludeResources) => withResources(folderId, shouldIncludeResources).map(_.some)
      }

      getWith404IfNone(folderId, folderWithContent)
    }

    def getWith404IfNone(folderId: UUID, maybeFolder: Try[Option[domain.Folder]]): Try[domain.Folder] = {
      maybeFolder match {
        case Failure(ex)           => Failure(ex)
        case Success(Some(folder)) => Success(folder)
        case Success(None)         => Failure(NotFoundException(s"Folder with id $folderId does not exist"))
      }
    }

    private def getSubfolders(
        folders: List[domain.Folder],
        includeSubfolders: Boolean,
        includeResources: Boolean
    )(implicit session: DBSession): Try[List[domain.Folder]] =
      folders
        .traverse(f => getSingleFolderWithContent(f.id, includeSubfolders, includeResources))

    private def withFeideId[T](maybeToken: Option[FeideAccessToken])(func: FeideID => Try[T]): Try[T] =
      feideApiClient.getFeideID(maybeToken).flatMap(feideId => func(feideId))

    def getFolders(
        includeSubfolders: Boolean,
        includeResources: Boolean,
        feideAccessToken: Option[FeideAccessToken]
    ): Try[List[Folder]] = {
      withFeideId(feideAccessToken)(getFoldersAuthenticated(includeSubfolders, includeResources, _))
    }

    def getBreadcrumbs(folder: domain.Folder)(implicit session: DBSession): Try[List[api.Breadcrumb]] = {
      @tailrec
      def getParentRecursively(folder: domain.Folder, crumbs: List[api.Breadcrumb]): Try[List[api.Breadcrumb]] = {
        folder.parentId match {
          case None => Success(crumbs)
          case Some(parentId) =>
            folderRepository.folderWithId(parentId) match {
              case Failure(ex) => Failure(ex)
              case Success(p) =>
                val newCrumb = api.Breadcrumb(
                  id = p.id.toString,
                  name = p.name
                )
                getParentRecursively(p, newCrumb +: crumbs)
            }
        }
      }

      getParentRecursively(folder, List.empty) match {
        case Failure(ex) => Failure(ex)
        case Success(value) =>
          val newCrumb = api.Breadcrumb(
            id = folder.id.toString,
            name = folder.name
          )
          Success(value :+ newCrumb)
      }
    }

    def getSingleFolder(
        id: UUID,
        includeSubfolders: Boolean,
        includeResources: Boolean,
        feideAccessToken: Option[FeideAccessToken]
    ): Try[Folder] = {
      implicit val session: DBSession = folderRepository.getSession(true)
      for {
        feideId           <- feideApiClient.getFeideID(feideAccessToken)
        folderWithContent <- getSingleFolderWithContent(id, includeSubfolders, includeResources)
        _                 <- folderWithContent.isOwner(feideId)
        feideUser         <- userRepository.userWithFeideId(folderWithContent.feideId)
        breadcrumbs       <- getBreadcrumbs(folderWithContent)
        converted <- folderConverterService.toApiFolder(
          folderWithContent,
          breadcrumbs,
          feideUser,
          feideId == folderWithContent.feideId
        )
      } yield converted
    }

    def getAllResources(
        size: Int,
        feideAccessToken: Option[FeideAccessToken] = None
    ): Try[List[Resource]] = {
      for {
        feideId   <- feideApiClient.getFeideID(feideAccessToken)
        resources <- folderRepository.resourcesWithFeideId(feideId, size)
        convertedResources <- folderConverterService.domainToApiModel(
          resources,
          resource => folderConverterService.toApiResource(resource, isOwner = true)
        )
      } yield convertedResources
    }

    private def createFavorite(
        feideId: FeideID
    ): Try[domain.Folder] = {
      val favoriteFolder = domain.NewFolderData(
        parentId = None,
        name = FavoriteFolderDefaultName,
        status = domain.FolderStatus.PRIVATE,
        rank = 1.some,
        description = None
      )
      folderRepository.insertFolder(feideId, favoriteFolder)
    }

    private def getFeideUserDataAuthenticated(
        feideId: FeideID,
        feideAccessToken: Option[FeideAccessToken]
    ): Try[api.MyNDLAUser] =
      for {
        users <- configService.getMyNDLAEnabledUsers
        user <- userRepository.rollbackOnFailure(session =>
          userService.getOrCreateMyNDLAUserIfNotExist(feideId, feideAccessToken, users)(session)
        )
        orgs <- configService.getMyNDLAEnabledOrgs
      } yield folderConverterService.toApiUserData(user, orgs)

    def getStats: Option[api.Stats] = {
      implicit val session: DBSession = folderRepository.getSession(true)
      val groupedResources            = folderRepository.numberOfResourcesGrouped()
      val favouritedResources         = groupedResources.map(gr => api.ResourceStats(gr._2, gr._1))
      for {
        numberOfUsers         <- userRepository.numberOfUsers()
        numberOfFolders       <- folderRepository.numberOfFolders()
        numberOfResources     <- folderRepository.numberOfResources()
        numberOfTags          <- folderRepository.numberOfTags()
        numberOfSubjects      <- userRepository.numberOfFavouritedSubjects()
        numberOfSharedFolders <- folderRepository.numberOfSharedFolders()
        stats = api.Stats(
          numberOfUsers,
          numberOfFolders,
          numberOfResources,
          numberOfTags,
          numberOfSubjects,
          numberOfSharedFolders,
          favouritedResources
        )
      } yield stats
    }

    def exportUserData(maybeFeideToken: Option[FeideAccessToken]): Try[ExportedUserData] = {
      withFeideId(maybeFeideToken)(feideId => exportUserDataAuthenticated(maybeFeideToken, feideId))
    }

    def getAllTheFavorites: Try[Map[String, Map[String, Long]]] = {
      implicit val session: DBSession = folderRepository.getSession(true)
      folderRepository.getAllFavorites(session)
    }

    def getRecentFavorite(size: Option[Int], excludeResourceTypes: List[ResourceType]): Try[List[Resource]] = {
      implicit val session: DBSession = folderRepository.getSession(true)
      folderRepository.getRecentFavorited(size, excludeResourceTypes)(session) match {
        case Failure(ex)    => Failure(ex)
        case Success(value) => value.traverse(r => folderConverterService.toApiResource(r, isOwner = false))
      }
    }

    def getFavouriteStatsForResource(
        resourceIds: List[String],
        resourceTypes: List[String]
    ): Try[List[SingleResourceStats]] = {
      implicit val session: DBSession = folderRepository.getSession(true)

      val result =
        resourceIds.map(id => {
          val countList = resourceTypes.map(rt => {
            folderRepository.numberOfFavouritesForResource(id, rt).?
          })
          SingleResourceStats(id, countList.sum)

        })

      Success(result)
    }

    private def exportUserDataAuthenticated(
        maybeFeideAccessToken: Option[FeideAccessToken],
        feideId: FeideID
    ): Try[ExportedUserData] =
      for {
        folders   <- getFoldersAuthenticated(includeSubfolders = true, includeResources = true, feideId)
        feideUser <- getFeideUserDataAuthenticated(feideId, maybeFeideAccessToken)
      } yield api.ExportedUserData(
        userData = feideUser,
        folders = folders
      )

  }
}
