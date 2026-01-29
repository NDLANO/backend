/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.service

import cats.implicits.*
import no.ndla.common.errors.NotFoundException
import no.ndla.common.implicits.*
import no.ndla.common.model.api.SingleResourceStatsDTO
import no.ndla.common.model.api.learningpath.LearningPathStatsDTO
import no.ndla.common.model.domain.TryMaybe.*
import no.ndla.common.model.domain.{ResourceType, TryMaybe, myndla}
import no.ndla.common.model.domain.myndla.{FolderStatus, UserRole}
import no.ndla.database.DBUtility
import no.ndla.myndlaapi.FavoriteFolderDefaultName
import no.ndla.myndlaapi.integration.LearningPathApiClient
import no.ndla.myndlaapi.model.api.{ExportedUserDataDTO, FolderDTO, ResourceDTO, StatsDTO, UserFolderDTO, UserStatsDTO}
import no.ndla.myndlaapi.model.{api, domain}
import no.ndla.myndlaapi.repository.{FolderRepository, UserRepository}
import no.ndla.network.model.{FeideID, FeideUserWrapper}
import scalikejdbc.DBSession

import java.util.UUID
import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

class FolderReadService(using
    folderConverterService: FolderConverterService,
    folderRepository: FolderRepository,
    userRepository: UserRepository,
    dbUtility: DBUtility,
    learningPathApiClient: LearningPathApiClient,
) {
  private def getSubFoldersAndResources(
      topFolders: List[domain.Folder],
      includeSubfolders: Boolean,
      includeResources: Boolean,
      feideId: FeideID,
  )(session: DBSession): Try[List[FolderDTO]] = {
    for {
      withFavorite <- mergeWithFavorite(topFolders, feideId)
      withData     <- getSubfolders(withFavorite, includeSubfolders, includeResources)(using session)
      feideUser    <- userRepository.userWithFeideId(feideId)(using session)
      apiFolders   <- folderConverterService.domainToApiModel(
        withData,
        v =>
          folderConverterService.toApiFolder(
            v,
            List(api.BreadcrumbDTO(id = v.id, name = v.name)),
            feideUser,
            feideUser.exists(_.feideId == v.feideId),
          ),
      )
      sorted = apiFolders.sortBy(_.rank)
    } yield sorted
  }

  private def getSharedSubFoldersAndResources(topFolders: List[domain.Folder])(session: DBSession) = {
    for {
      withData <- topFolders.traverse(f => {
        val folderWithContent = folderRepository.getSharedFolderAndChildrenSubfoldersWithResources(f.id)(using session)
        getWith404IfNone(f.id, folderWithContent)
      })
      apiFolders <- folderConverterService.domainToApiModel(
        withData,
        (v: domain.Folder) => {
          folderConverterService.toApiFolder(
            v,
            List(api.BreadcrumbDTO(id = v.id, name = v.name)),
            v.user,
            v.user.exists(_.feideId == v.feideId),
          )
        },
      )
      sorted = apiFolders.sortBy(_.rank)
    } yield sorted

  }
  private def getFoldersAuthenticated(includeSubfolders: Boolean, includeResources: Boolean, feideId: FeideID) = {
    dbUtility.rollbackOnFailure(session => {
      for {
        myFolders          <- folderRepository.foldersWithFeideAndParentID(None, feideId)(using session)
        savedSharedFolders <- folderRepository.getSavedSharedFolders(feideId)(using session)
        folders            <- getSubFoldersAndResources(myFolders, includeSubfolders, includeResources, feideId)(session)
        sharedFolders      <- getSharedSubFoldersAndResources(savedSharedFolders)(session)

      } yield UserFolderDTO(folders = folders, sharedFolders = sharedFolders)
    })
  }

  def getSharedFolder(id: UUID, feide: FeideUserWrapper): Try[FolderDTO] = {
    dbUtility.writeSession { implicit session =>
      for {
        user                <- feide.userOrAccessDenied
        folderWithResources <-
          folderRepository.getFolderAndChildrenSubfoldersWithResources(id, FolderStatus.SHARED, Some(user.feideId))
        folderWithContent <- getWith404IfNone(id, Success(folderWithResources))
        _                 <-
          if (folderWithContent.isShared || user.feideId == folderWithContent.feideId) Success(())
          else Failure(NotFoundException("Folder does not exist"))
        folderAsTopFolder = folderWithContent.copy(parentId = None)
        breadcrumbs      <- getBreadcrumbs(folderAsTopFolder)
        feideUser        <- userRepository.userWithFeideId(folderWithContent.feideId)
        converted        <- folderConverterService.toApiFolder(
          folderAsTopFolder,
          breadcrumbs,
          feideUser,
          user.feideId == folderWithContent.feideId,
        )
      } yield converted
    }
  }

  private[service] def mergeWithFavorite(folders: List[domain.Folder], feideId: FeideID): Try[List[domain.Folder]] = {
    for {
      favorite <-
        if (folders.isEmpty) createFavorite(feideId).map(_.some)
        else Success(None)
      combined = favorite.toList ++ folders
    } yield combined
  }

  private def withResources(folderId: UUID, shouldIncludeResources: Boolean)(implicit
      session: DBSession
  ): Try[domain.Folder] = folderRepository
    .folderWithId(folderId)
    .flatMap(folder => {
      val folderResources =
        if (shouldIncludeResources) folderRepository.getFolderResources(folderId)
        else Success(List.empty)

      folderResources.map(res => folder.copy(resources = res))
    })

  def getSingleFolderWithContent(folderId: UUID, includeSubfolders: Boolean, includeResources: Boolean)(implicit
      session: DBSession
  ): Try[domain.Folder] = {
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

  private def getSubfolders(folders: List[domain.Folder], includeSubfolders: Boolean, includeResources: Boolean)(
      implicit session: DBSession
  ): Try[List[domain.Folder]] =
    folders.traverse(f => getSingleFolderWithContent(f.id, includeSubfolders, includeResources))

  private def withFeideId[T](user: FeideUserWrapper)(func: FeideID => Try[T]): Try[T] = {
    for {
      user   <- user.userOrAccessDenied
      result <- func(user.feideId)
    } yield result
  }

  def getFolders(includeSubfolders: Boolean, includeResources: Boolean, feide: FeideUserWrapper): Try[UserFolderDTO] = {
    withFeideId(feide)((feideId: FeideID) => getFoldersAuthenticated(includeSubfolders, includeResources, feideId))
  }

  def getBreadcrumbs(folder: domain.Folder)(implicit session: DBSession): Try[List[api.BreadcrumbDTO]] = {
    @tailrec
    def getParentRecursively(folder: domain.Folder, crumbs: List[api.BreadcrumbDTO]): Try[List[api.BreadcrumbDTO]] = {
      folder.parentId match {
        case None           => Success(crumbs)
        case Some(parentId) => folderRepository.folderWithId(parentId) match {
            case Failure(ex) => Failure(ex)
            case Success(p)  =>
              val newCrumb = api.BreadcrumbDTO(id = p.id, name = p.name)
              getParentRecursively(p, newCrumb +: crumbs)
          }
      }
    }

    getParentRecursively(folder, List.empty) match {
      case Failure(ex)    => Failure(ex)
      case Success(value) =>
        val newCrumb = api.BreadcrumbDTO(id = folder.id, name = folder.name)
        Success(value :+ newCrumb)
    }
  }

  def getSingleFolder(
      id: UUID,
      includeSubfolders: Boolean,
      includeResources: Boolean,
      feide: FeideUserWrapper,
  ): Try[FolderDTO] = {
    dbUtility.writeSession { implicit session =>
      for {
        user              <- feide.userOrAccessDenied
        folderWithContent <- getSingleFolderWithContent(id, includeSubfolders, includeResources)
        _                 <- folderWithContent.isOwner(user.feideId)
        feideUser         <- userRepository.userWithFeideId(folderWithContent.feideId)
        breadcrumbs       <- getBreadcrumbs(folderWithContent)
        converted         <- folderConverterService.toApiFolder(
          folderWithContent,
          breadcrumbs,
          feideUser,
          user.feideId == folderWithContent.feideId,
        )
      } yield converted
    }
  }

  def getAllResources(size: Int, feide: FeideUserWrapper): Try[List[ResourceDTO]] = {
    dbUtility.writeSession { implicit session =>
      for {
        user               <- feide.userOrAccessDenied
        resources          <- folderRepository.resourcesWithFeideId(user.feideId, size)
        convertedResources <- folderConverterService.domainToApiModel(
          resources,
          resource => folderConverterService.toApiResource(resource, isOwner = true),
        )
      } yield convertedResources
    }
  }

  private def createFavorite(feideId: FeideID): Try[domain.Folder] = {
    val favoriteFolder = domain.NewFolderData(
      parentId = None,
      name = FavoriteFolderDefaultName,
      status = myndla.FolderStatus.PRIVATE,
      rank = 1,
      description = None,
    )
    folderRepository.insertFolder(feideId, favoriteFolder)
  }

  private def getUserStats(numberOfUsersWithLearningpath: Long, session: DBSession): Try[Option[UserStatsDTO]] = {
    for {
      numberOfUsersWithFavourites    <- folderRepository.numberOfUsersWithFavourites(using session).toTryMaybe
      numberOfUsersWithoutFavourites <- folderRepository.numberOfUsersWithoutFavourites(using session).toTryMaybe
      numberOfUsersInArena           <- userRepository.numberOfUsersInArena(using session).toTryMaybe
      usersGrouped                   <- userRepository.usersGrouped().toTrySome
      numberOfEmployees               = usersGrouped(UserRole.EMPLOYEE)
      numberOfStudents                = usersGrouped(UserRole.STUDENT)
      numberOfUsers                   = numberOfStudents + numberOfEmployees
    } yield UserStatsDTO(
      numberOfUsers,
      numberOfEmployees,
      numberOfStudents,
      numberOfUsersWithFavourites,
      numberOfUsersWithoutFavourites,
      numberOfUsersWithLearningpath,
      numberOfUsersInArena,
    )
  }.value

  def getStats: TryMaybe[api.StatsDTO] = {
    implicit val session: DBSession = folderRepository.getSession(true)
    for {
      groupedResources      <- folderRepository.numberOfResourcesGrouped().toTrySome
      favouritedResources    = groupedResources.map(gr => api.ResourceStatsDTO(gr._2, gr._1))
      favourited             = groupedResources.map(gr => gr._2 -> gr._1).toMap
      learningPathStats      = learningPathApiClient.getStats.getOrElse(LearningPathStatsDTO(0, 0))
      numberOfFolders       <- folderRepository.numberOfFolders().toTryMaybe
      numberOfResources     <- folderRepository.numberOfResources().toTryMaybe
      numberOfTags          <- folderRepository.numberOfTags().toTryMaybe
      numberOfSubjects      <- userRepository.numberOfFavouritedSubjects().toTryMaybe
      numberOfSharedFolders <- folderRepository.numberOfSharedFolders().toTryMaybe
      userStats             <- getUserStats(learningPathStats.numberOfMyNdlaLearningPathOwners, session).toTryMaybe
    } yield StatsDTO(
      userStats.total,
      numberOfFolders,
      numberOfResources,
      numberOfTags,
      numberOfSubjects,
      numberOfSharedFolders,
      learningPathStats.numberOfMyNdlaLearningPaths,
      favouritedResources,
      favourited,
      userStats,
    )
  }

  def exportUserData(feide: FeideUserWrapper): Try[ExportedUserDataDTO] = {
    for {
      user    <- feide.userOrAccessDenied
      folders <- getFoldersAuthenticated(includeSubfolders = true, includeResources = true, user.feideId)
    } yield api.ExportedUserDataDTO(userData = folderConverterService.toApiUserData(user), folders = folders.folders)
  }

  def getAllTheFavorites: Try[Map[String, Map[String, Long]]] = {
    implicit val session: DBSession = folderRepository.getSession(true)
    folderRepository.getAllFavorites(using session)
  }

  def getRecentFavorite(size: Option[Int], excludeResourceTypes: List[ResourceType]): Try[List[ResourceDTO]] = {
    implicit val session: DBSession = folderRepository.getSession(true)
    folderRepository.getRecentFavorited(size, excludeResourceTypes)(using session) match {
      case Failure(ex)    => Failure(ex)
      case Success(value) => value.traverse(r => folderConverterService.toApiResource(r, isOwner = false))
    }
  }

  def getFavouriteStatsForResource(
      resourceIds: List[String],
      resourceTypes: List[String],
  ): Try[List[SingleResourceStatsDTO]] = permitTry {
    implicit val session: DBSession = folderRepository.getSession(true)

    val result = resourceIds.map(id => {
      val countList = resourceTypes.map(rt => {
        rt -> folderRepository.numberOfFavouritesForResource(id, rt).?
      })
      countList.map(cl => SingleResourceStatsDTO(cl._1, id, cl._2))
    })

    Success(result.flatten)
  }

}
