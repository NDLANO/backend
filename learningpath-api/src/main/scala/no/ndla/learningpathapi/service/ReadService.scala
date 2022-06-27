/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import cats.implicits._
import no.ndla.learningpathapi.integration.FeideApiClient
import no.ndla.learningpathapi.model.api
import no.ndla.learningpathapi.model.api._
import no.ndla.learningpathapi.model.domain
import no.ndla.learningpathapi.model.domain.config.ConfigKey
import no.ndla.learningpathapi.model.domain.{
  FolderData,
  StepStatus,
  UserInfo,
  Author => _,
  LearningPathStatus => _,
  LearningPathTags => _,
  _
}
import no.ndla.learningpathapi.repository.{ConfigRepository, FolderRepository, LearningPathRepositoryComponent}
import scalikejdbc.{DBSession}

import java.util.UUID
import scala.annotation.tailrec
import scala.math.max
import scala.util.{Failure, Success, Try}

trait ReadService {
  this: LearningPathRepositoryComponent
    with FeideApiClient
    with ConfigRepository
    with ConverterService
    with FolderRepository =>
  val readService: ReadService

  class ReadService {

    def tags: List[LearningPathTags] = {
      learningPathRepository.allPublishedTags.map(tags => LearningPathTags(tags.tags, tags.language))
    }

    def contributors: List[Author] = {
      learningPathRepository.allPublishedContributors.map(author => Author(author.`type`, author.name))
    }

    def withOwnerV2(user: UserInfo = UserInfo.getUserOrPublic): List[LearningPathSummaryV2] = {
      learningPathRepository
        .withOwner(user.userId)
        .flatMap(value => converterService.asApiLearningpathSummaryV2(value, user).toOption)
    }

    def withIdV2(
        learningPathId: Long,
        language: String,
        fallback: Boolean,
        user: UserInfo = UserInfo.getUserOrPublic
    ): Try[LearningPathV2] = {
      withIdAndAccessGranted(learningPathId, user).flatMap(lp =>
        converterService.asApiLearningpathV2(lp, language, fallback, user)
      )
    }

    def statusFor(learningPathId: Long, user: UserInfo = UserInfo.getUserOrPublic): Try[LearningPathStatus] = {
      withIdAndAccessGranted(learningPathId, user).map(lp => LearningPathStatus(lp.status.toString))
    }

    def learningStepStatusForV2(
        learningPathId: Long,
        learningStepId: Long,
        language: String,
        fallback: Boolean,
        user: UserInfo = UserInfo.getUserOrPublic
    ): Try[LearningStepStatus] = {
      learningstepV2For(learningPathId, learningStepId, language, fallback, user).map(ls =>
        LearningStepStatus(ls.status.toString)
      )
    }

    def learningstepsForWithStatusV2(
        learningPathId: Long,
        status: StepStatus,
        language: String,
        fallback: Boolean,
        user: UserInfo = UserInfo.getUserOrPublic
    ): Try[LearningStepContainerSummary] = {
      withIdAndAccessGranted(learningPathId, user) match {
        case Success(lp) => converterService.asLearningStepContainerSummary(status, lp, language, fallback)
        case Failure(ex) => Failure(ex)
      }
    }

    def learningstepV2For(
        learningPathId: Long,
        learningStepId: Long,
        language: String,
        fallback: Boolean,
        user: UserInfo = UserInfo.getUserOrPublic
    ): Try[LearningStepV2] = {
      withIdAndAccessGranted(learningPathId, user) match {
        case Success(lp) =>
          learningPathRepository
            .learningStepWithId(learningPathId, learningStepId)
            .map(ls => converterService.asApiLearningStepV2(ls, lp, language, fallback, user)) match {
            case Some(value) => value
            case None =>
              Failure(
                NotFoundException(
                  s"Learningstep with id $learningStepId for learningpath with id $learningPathId not found"
                )
              )
          }
        case Failure(ex) => Failure(ex)
      }
    }

    def withIdAndAccessGranted(learningPathId: Long, user: UserInfo): Try[domain.LearningPath] = {
      val learningPath = learningPathRepository.withId(learningPathId)
      learningPath.map(_.isOwnerOrPublic(user)) match {
        case Some(Success(lp)) => Success(lp)
        case Some(Failure(ex)) => Failure(ex)
        case None              => Failure(NotFoundException(s"Could not find learningPath with id $learningPathId"))
      }
    }

    def getLearningPathDomainDump(pageNo: Int, pageSize: Int, onlyIncludePublished: Boolean): LearningPathDomainDump = {
      val (safePageNo, safePageSize) = (max(pageNo, 1), max(pageSize, 0))

      val resultFunc =
        if (onlyIncludePublished) learningPathRepository.getPublishedLearningPathByPage _
        else learningPathRepository.getAllLearningPathsByPage _

      val count =
        if (onlyIncludePublished) learningPathRepository.publishedLearningPathCount
        else learningPathRepository.learningPathCount

      val results = resultFunc(safePageSize, (safePageNo - 1) * safePageSize)

      LearningPathDomainDump(count, safePageNo, safePageSize, results)
    }

    def learningPathWithStatus(status: String, user: UserInfo): Try[List[LearningPathV2]] = {
      if (user.isAdmin) {
        domain.LearningPathStatus.valueOf(status) match {
          case Some(ps) =>
            Success(
              learningPathRepository
                .learningPathsWithStatus(ps)
                .flatMap(lp => converterService.asApiLearningpathV2(lp, "all", fallback = true, user).toOption)
            )
          case _ => Failure(InvalidStatusException(s"Parameter '$status' is not a valid status"))
        }
      } else { Failure(AccessDeniedException("You do not have access to this resource.")) }
    }

    def isWriteRestricted: Boolean =
      Try(
        configRepository
          .getConfigWithKey(ConfigKey.IsWriteRestricted)
          .map(_.value.toBoolean)
      ).toOption.flatten.getOrElse(false)

    def canWriteNow(userInfo: UserInfo): Boolean =
      userInfo.canWriteDuringWriteRestriction || !readService.isWriteRestricted

    def getAllResources(size: Int, feideAccessToken: Option[FeideAccessToken] = None): Try[List[api.Resource]] = {
      for {
        feideId            <- feideApiClient.getUserFeideID(feideAccessToken)
        resources          <- folderRepository.resourcesWithFeideId(feideId, size)
        convertedResources <- converterService.domainToApiModel(resources, converterService.toApiResource)
      } yield convertedResources
    }

    private def getFolderResources(
        id: UUID,
        includeResources: Boolean
    )(implicit session: DBSession): Try[List[FolderData]] = {
      if (includeResources)
        folderRepository.getFolderResources(id).map(_.map(_.asRight))
      else
        Success(List.empty)
    }

    private[service] def getSubFoldersRecursively(
        folder: domain.Folder,
        includeResources: Boolean
    )(implicit session: DBSession): Try[domain.Folder] = {
      for {
        directSubfolders <- folderRepository.foldersWithParentID(parentId = folder.id.some)
        folderResources  <- getFolderResources(folder.id, includeResources)
        subFolders <- directSubfolders.traverse(subFolder => getSubFoldersRecursively(subFolder, includeResources))
        combined = folderResources ++ subFolders.map(_.asLeft)
      } yield folder.copy(data = combined)
    }

    private def createFavorite(
        feideId: domain.FeideID
    ): Try[domain.Folder] = {
      val favoriteFolder = domain.FolderDocument(
        name = FavoriteFolderDefaultName,
        status = domain.FolderStatus.PRIVATE,
        isFavorite = true,
        data = List.empty
      )
      folderRepository.insertFolder(feideId, None, favoriteFolder)
    }

    def getBreadcrumbs(folder: domain.Folder)(implicit session: DBSession): Try[List[String]] = {
      @tailrec
      def getParentRecursively(folder: domain.Folder, crumbs: List[String]): Try[List[String]] = {
        folder.parentId match {
          case None => Success(crumbs)
          case Some(parentId) =>
            folderRepository.folderWithId(parentId) match {
              case Failure(ex) => Failure(ex)
              case Success(p)  => getParentRecursively(p, p.name +: crumbs)
            }
        }
      }

      getParentRecursively(folder, List.empty) match {
        case Failure(ex)    => Failure(ex)
        case Success(value) => Success(value :+ folder.name)
      }
    }

    def getFolder(
        id: UUID,
        includeSubfolders: Boolean,
        includeResources: Boolean,
        feideAccessToken: Option[FeideAccessToken]
    ): Try[api.Folder] = {
      implicit val session: DBSession = folderRepository.getSession(true)
      for {
        feideId           <- feideApiClient.getUserFeideID(feideAccessToken)
        mainFolder        <- folderRepository.folderWithId(id)
        _                 <- mainFolder.hasReadAccess(feideId)
        folderWithContent <- folderWithContent(mainFolder, includeSubfolders, includeResources)
        breadcrumbs       <- getBreadcrumbs(mainFolder)
        converted         <- converterService.toApiFolder(folderWithContent, breadcrumbs)
      } yield converted
    }

    private def folderWithContent(
        folder: domain.Folder,
        includeSubfolders: Boolean,
        includeResources: Boolean
    )(implicit session: DBSession): Try[domain.Folder] =
      if (includeSubfolders) getSubFoldersRecursively(folder, includeResources)
      else
        getFolderResources(folder.id, includeResources).map(resources => {
          folder.copy(data = resources)
        })

    private[service] def mergeWithFavorite(folders: List[domain.Folder], feideId: FeideID): Try[List[domain.Folder]] = {
      val maybeFavorite = folders.find(_.isFavorite)
      for {
        favorite <- if (maybeFavorite.isEmpty) createFavorite(feideId).map(_.some) else Success(None)
        combined = favorite.toList ++ folders
      } yield combined
    }

    private[service] def injectResourcesToFolders(
        folders: List[domain.Folder],
        includeResources: Boolean
    )(implicit session: DBSession): Try[List[domain.Folder]] =
      folders.traverse(folder => {
        val folderResources = getFolderResources(folder.id, includeResources)
        folderResources.map(res => folder.copy(data = res))
      })

    private def getSubfolders(
        folders: List[domain.Folder],
        includeSubfolders: Boolean,
        includeResources: Boolean
    )(implicit session: DBSession): Try[List[domain.Folder]] = {
      if (includeSubfolders)
        folders.traverse(folder => getSubFoldersRecursively(folder, includeResources))
      else {
        // getSubFoldersRecursively already injects resources into folders, this prevents double-up
        injectResourcesToFolders(folders, includeResources)
      }
    }

    def getFolders(
        includeSubfolders: Boolean,
        includeResources: Boolean,
        feideAccessToken: Option[FeideAccessToken] = None
    ): Try[List[api.Folder]] = {
      implicit val session: DBSession = folderRepository.getSession(true)
      for {
        feideId      <- feideApiClient.getUserFeideID(feideAccessToken)
        topFolders   <- folderRepository.foldersWithFeideAndParentID(None, feideId)
        withFavorite <- mergeWithFavorite(topFolders, feideId)
        withData     <- getSubfolders(withFavorite, includeSubfolders, includeResources)
        apiFolders   <- converterService.domainToApiModel(withData, v => converterService.toApiFolder(v, List(v.name)))
      } yield apiFolders
    }
  }
}
