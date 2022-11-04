/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import cats.implicits._
import no.ndla.common.Clock
import no.ndla.learningpathapi.caching.Memoize
import no.ndla.learningpathapi.model.api
import no.ndla.learningpathapi.model.api._
import no.ndla.learningpathapi.model.domain
import no.ndla.learningpathapi.model.domain.config.ConfigKey
import no.ndla.learningpathapi.model.domain.{
  StepStatus,
  UserInfo,
  Author => _,
  LearningPathStatus => _,
  LearningPathTags => _,
  _
}
import no.ndla.learningpathapi.repository.{
  ConfigRepository,
  FolderRepository,
  LearningPathRepositoryComponent,
  UserRepository
}
import no.ndla.network.clients.FeideApiClient
import no.ndla.common.errors.AccessDeniedException
import scalikejdbc.DBSession

import java.util.UUID
import scala.annotation.tailrec
import scala.math.max
import scala.util.{Failure, Success, Try}

trait ReadService {
  this: LearningPathRepositoryComponent
    with FeideApiClient
    with ConfigRepository
    with ConverterService
    with UserRepository
    with FolderRepository
    with Clock =>
  val readService: ReadService

  class ReadService {

    private val getUserFeideID = Memoize(feideApiClient.getUserFeideID)

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
        LearningStepStatus(ls.status)
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

    def getConfig(configKey: ConfigKey): Try[api.config.ConfigMetaRestricted] = {
      configRepository.getConfigWithKey(configKey) match {
        case None      => Failure(NotFoundException(s"Configuration with key $configKey does not exist"))
        case Some(key) => Success(converterService.asApiConfigRestricted(key))
      }
    }

    def canWriteNow(userInfo: UserInfo): Boolean =
      userInfo.canWriteDuringWriteRestriction || !readService.isWriteRestricted

    def getAllResources(
        size: Int,
        feideAccessToken: Option[FeideAccessToken] = None
    ): Try[List[api.Resource]] = {
      for {
        feideId            <- getUserFeideID(feideAccessToken)
        resources          <- folderRepository.resourcesWithFeideId(feideId, size)
        convertedResources <- converterService.domainToApiModel(resources, converterService.toApiResource)
      } yield convertedResources
    }

    private def createFavorite(
        feideId: domain.FeideID
    ): Try[domain.Folder] = {
      val favoriteFolder = domain.NewFolderData(
        parentId = None,
        name = FavoriteFolderDefaultName,
        status = domain.FolderStatus.PRIVATE,
        rank = 1.some
      )
      folderRepository.insertFolder(feideId, favoriteFolder)
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
    ): Try[api.Folder] = {
      implicit val session: DBSession = folderRepository.getSession(true)
      for {
        feideId           <- getUserFeideID(feideAccessToken)
        folderWithContent <- getSingleFolderWithContent(id, includeSubfolders, includeResources)
        _                 <- folderWithContent.isOwner(feideId)
        breadcrumbs       <- getBreadcrumbs(folderWithContent)
        converted         <- converterService.toApiFolder(folderWithContent, breadcrumbs)
      } yield converted
    }

    private[service] def mergeWithFavorite(folders: List[domain.Folder], feideId: FeideID): Try[List[domain.Folder]] = {
      for {
        favorite <- if (folders.isEmpty) createFavorite(feideId).map(_.some) else Success(None)
        combined = favorite.toList ++ folders
      } yield combined
    }

    def withResources(folderId: UUID, shouldIncludeResources: Boolean)(implicit
        session: DBSession
    ): Try[domain.Folder] = folderRepository
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

    def getFolders(
        includeSubfolders: Boolean,
        includeResources: Boolean,
        feideAccessToken: Option[FeideAccessToken] = None
    ): Try[List[api.Folder]] = {
      implicit val session: DBSession = folderRepository.getSession(true)
      for {
        feideId      <- getUserFeideID(feideAccessToken)
        topFolders   <- folderRepository.foldersWithFeideAndParentID(None, feideId)
        withFavorite <- mergeWithFavorite(topFolders, feideId)
        withData     <- getSubfolders(withFavorite, includeSubfolders, includeResources)
        apiFolders <- converterService.domainToApiModel(
          withData,
          v => converterService.toApiFolder(v, List(api.Breadcrumb(id = v.id.toString, name = v.name)))
        )
        sorted = apiFolders.sortBy(_.rank)
      } yield sorted
    }

    def getSharedFolder(id: UUID): Try[api.Folder] = {
      implicit val session: DBSession = folderRepository.getSession(true)
      val folderWithResources = folderRepository.getFolderAndChildrenSubfoldersWithResources(id, FolderStatus.SHARED)
      for {
        folderWithContent <- getWith404IfNone(id, folderWithResources)
        _ <- if (folderWithContent.isShared) Success(()) else Failure(NotFoundException("Folder does not exist"))
        folderAsTopFolder = folderWithContent.copy(parentId = None)
        breadcrumbs <- getBreadcrumbs(folderAsTopFolder)
        converted   <- converterService.toApiFolder(folderAsTopFolder, breadcrumbs)
      } yield converted
    }

    private def createFeideUser(feideId: FeideID, feideAccessToken: FeideAccessToken): Try[domain.FeideUser] = {
      for {
        feideExtendedUserData <- feideApiClient.getUser(feideAccessToken)
        newUser = domain
          .FeideUserDocument(
            favoriteSubjects = Seq.empty,
            userRole = if (feideExtendedUserData.isTeacher) UserRole.TEACHER else UserRole.STUDENT,
            lastUpdated = clock.now().plusDays(1)
          )
        inserted <- userRepository.insertUser(feideId, newUser)
      } yield inserted
    }

    def getOrCreateFeideUserIfNotExist(
        feideId: FeideID,
        feideAccessToken: Option[FeideAccessToken]
    ): Try[domain.FeideUser] = {
      userRepository.userWithFeideId(feideId).flatMap {
        case None =>
          feideApiClient.getFeideAccessTokenOrFail(feideAccessToken).flatMap(token => createFeideUser(feideId, token))
        case Some(userData) =>
          if (userData.wasUpdatedLast24h) Success(userData)
          else userRepository.updateUser(feideId, userData.copy(lastUpdated = clock.now().plusDays(1)))
      }
    }

    def getFeideUserData(feideAccessToken: Option[FeideAccessToken]): Try[api.FeideUser] = {
      for {
        feideId  <- getUserFeideID(feideAccessToken)
        userData <- getOrCreateFeideUserIfNotExist(feideId, feideAccessToken)
        api = converterService.toApiUserData(userData)
      } yield api
    }
  }
}
