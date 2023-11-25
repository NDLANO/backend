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
import no.ndla.common.errors.{AccessDeniedException, ValidationException}
import no.ndla.common.implicits.TryQuestionMark
import no.ndla.common.model.{api => commonApi}
import no.ndla.learningpathapi.model.api._
import no.ndla.learningpathapi.model.domain.UserInfo.LearningpathTokenUser
import no.ndla.learningpathapi.model.domain.config.ConfigKey
import no.ndla.learningpathapi.model.domain.{StepStatus, LearningPathStatus => _, _}
import no.ndla.learningpathapi.model.{api, domain}
import no.ndla.learningpathapi.repository.{
  ConfigRepository,
  FolderRepository,
  LearningPathRepositoryComponent,
  UserRepository
}
import no.ndla.network.clients.{FeideApiClient, FeideGroup, RedisClient}
import no.ndla.network.tapir.auth.TokenUser
import scalikejdbc.{AutoSession, DBSession}

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
    with Clock
    with RedisClient =>
  val readService: ReadService

  class ReadService {

    def exportUserData(maybeFeideToken: Option[FeideAccessToken]): Try[api.ExportedUserData] = {
      withFeideId(maybeFeideToken)(feideId => exportUserDataAuthenticated(maybeFeideToken, feideId))
    }

    private def exportUserDataAuthenticated(
        maybeFeideAccessToken: Option[FeideAccessToken],
        feideId: FeideID
    ): Try[api.ExportedUserData] =
      for {
        folders   <- getFoldersAuthenticated(includeSubfolders = true, includeResources = true, feideId)
        feideUser <- getFeideUserDataAuthenticated(feideId, maybeFeideAccessToken)
      } yield ExportedUserData(
        userData = feideUser,
        folders = folders
      )

    def tags: List[LearningPathTags] = {
      learningPathRepository.allPublishedTags.map(tags => LearningPathTags(tags.tags, tags.language))
    }

    def contributors: List[commonApi.Author] = {
      learningPathRepository.allPublishedContributors.map(author => commonApi.Author(author.`type`, author.name))
    }

    def withOwnerV2(user: TokenUser): List[LearningPathSummaryV2] = {
      learningPathRepository
        .withOwner(user.id)
        .flatMap(value => converterService.asApiLearningpathSummaryV2(value, user).toOption)
    }

    def withIdV2List(
        ids: Seq[Long],
        language: String,
        fallback: Boolean,
        page: Int,
        pageSize: Int,
        userInfo: TokenUser
    ): Try[Seq[LearningPathV2]] = {
      if (ids.isEmpty) Failure(ValidationException("ids", "Query parameter 'ids' is missing"))
      else {
        val offset        = (page - 1) * pageSize
        val learningpaths = learningPathRepository.pageWithIds(ids, pageSize, offset)
        learningpaths.traverse(learningpath =>
          converterService.asApiLearningpathV2(learningpath, language, fallback, userInfo)
        )
      }
    }

    def withIdV2(
        learningPathId: Long,
        language: String,
        fallback: Boolean,
        user: TokenUser
    ): Try[LearningPathV2] = {
      withIdAndAccessGranted(learningPathId, user).flatMap(lp =>
        converterService.asApiLearningpathV2(lp, language, fallback, user)
      )
    }

    def statusFor(learningPathId: Long, user: TokenUser): Try[LearningPathStatus] = {
      withIdAndAccessGranted(learningPathId, user).map(lp => LearningPathStatus(lp.status.toString))
    }

    def learningStepStatusForV2(
        learningPathId: Long,
        learningStepId: Long,
        language: String,
        fallback: Boolean,
        user: TokenUser
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
        user: TokenUser
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
        user: TokenUser
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

    def withIdAndAccessGranted(learningPathId: Long, user: TokenUser): Try[domain.LearningPath] = {
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

    def learningPathWithStatus(status: String, user: TokenUser): Try[List[LearningPathV2]] = {
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
      configRepository
        .getConfigWithKey(ConfigKey.LearningpathWriteRestricted)
        .map(_.value)
        .collectFirst { case domain.config.BooleanValue(value) => value }
        .getOrElse(false)

    def isMyNDLAWriteRestricted: Boolean =
      configRepository
        .getConfigWithKey(ConfigKey.MyNDLAWriteRestricted)
        .map(_.value)
        .collectFirst { case domain.config.BooleanValue(value) => value }
        .getOrElse(false)

    def getMyNDLAEnabledOrgs: Try[List[String]] = {
      Try {
        configRepository
          .getConfigWithKey(ConfigKey.ArenaEnabledOrgs)
          .map(_.value)
          .collectFirst { case domain.config.StringListValue(value) => value }
          .getOrElse(List.empty)
      }
    }

    def getConfig(configKey: ConfigKey): Try[api.config.ConfigMetaRestricted] = {
      configRepository.getConfigWithKey(configKey) match {
        case None      => Failure(NotFoundException(s"Configuration with key $configKey does not exist"))
        case Some(key) => Success(converterService.asApiConfigRestricted(key))
      }
    }

    def canWriteNow(userInfo: TokenUser): Boolean =
      userInfo.canWriteDuringWriteRestriction || !readService.isWriteRestricted

    def getAllResources(
        size: Int,
        feideAccessToken: Option[FeideAccessToken] = None
    ): Try[List[api.Resource]] = {
      for {
        feideId            <- feideApiClient.getFeideID(feideAccessToken)
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
        rank = 1.some,
        description = None
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
        feideId           <- feideApiClient.getFeideID(feideAccessToken)
        folderWithContent <- getSingleFolderWithContent(id, includeSubfolders, includeResources)
        _                 <- folderWithContent.isOwner(feideId)
        feideUser         <- userRepository.userWithFeideId(folderWithContent.feideId)
        breadcrumbs       <- getBreadcrumbs(folderWithContent)
        converted         <- converterService.toApiFolder(folderWithContent, breadcrumbs, feideUser)
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

    private def withFeideId[T](maybeToken: Option[FeideAccessToken])(func: FeideID => Try[T]): Try[T] =
      feideApiClient.getFeideID(maybeToken).flatMap(feideId => func(feideId))

    def getFolders(
        includeSubfolders: Boolean,
        includeResources: Boolean,
        feideAccessToken: Option[FeideAccessToken]
    ): Try[List[api.Folder]] = {
      withFeideId(feideAccessToken)(getFoldersAuthenticated(includeSubfolders, includeResources, _))
    }

    private def getFoldersAuthenticated(
        includeSubfolders: Boolean,
        includeResources: Boolean,
        feideId: FeideID
    ): Try[List[api.Folder]] = {
      implicit val session: DBSession = folderRepository.getSession(true)
      for {
        topFolders   <- folderRepository.foldersWithFeideAndParentID(None, feideId)
        withFavorite <- mergeWithFavorite(topFolders, feideId)
        withData     <- getSubfolders(withFavorite, includeSubfolders, includeResources)
        feideUser    <- userRepository.userWithFeideId(feideId)
        apiFolders <- converterService.domainToApiModel(
          withData,
          v => converterService.toApiFolder(v, List(api.Breadcrumb(id = v.id.toString, name = v.name)), feideUser)
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
        feideUser   <- userRepository.userWithFeideId(folderWithContent.feideId)
        converted   <- converterService.toApiFolder(folderAsTopFolder, breadcrumbs, feideUser)
      } yield converted
    }

    private def createMyNDLAUser(feideId: FeideID, feideAccessToken: Option[FeideAccessToken])(implicit
        session: DBSession
    ): Try[domain.MyNDLAUser] = {
      for {
        feideExtendedUserData <- feideApiClient.getFeideExtendedUser(feideAccessToken)
        organization          <- feideApiClient.getOrganization(feideAccessToken)
        groups                <- feideApiClient.getFeideGroups(feideAccessToken)
        newUser = domain
          .MyNDLAUserDocument(
            favoriteSubjects = Seq.empty,
            userRole = if (feideExtendedUserData.isTeacher) UserRole.TEACHER else UserRole.STUDENT,
            lastUpdated = clock.now().plusDays(1),
            organization = organization,
            groups = groups,
            email = feideExtendedUserData.email,
            arenaEnabled = false,
            shareName = false,
            displayName = feideExtendedUserData.displayName
          )
        inserted <- userRepository.insertUser(feideId, newUser)(session)
      } yield inserted
    }

    private def fetchDataAndUpdateMyNDLAUser(
        feideId: FeideID,
        feideAccessToken: Option[FeideAccessToken],
        userData: domain.MyNDLAUser
    )(implicit
        session: DBSession
    ): Try[domain.MyNDLAUser] = {
      val feideUser    = feideApiClient.getFeideExtendedUser(feideAccessToken).?
      val organization = feideApiClient.getOrganization(feideAccessToken).?
      val groups       = feideApiClient.getFeideGroups(feideAccessToken).?
      val updatedMyNDLAUser = domain.MyNDLAUser(
        id = userData.id,
        feideId = userData.feideId,
        favoriteSubjects = userData.favoriteSubjects,
        userRole = if (feideUser.isTeacher) UserRole.TEACHER else UserRole.STUDENT,
        lastUpdated = clock.now().plusDays(1),
        organization = organization,
        groups = groups.filter(g => g.`type` == FeideGroup.FC_ORG),
        email = feideUser.email,
        arenaEnabled = userData.arenaEnabled,
        shareName = userData.shareName,
        displayName = feideUser.displayName
      )
      userRepository.updateUser(feideId, updatedMyNDLAUser)(session)
    }

    def getOrCreateMyNDLAUserIfNotExist(
        feideId: FeideID,
        feideAccessToken: Option[FeideAccessToken]
    )(implicit session: DBSession): Try[domain.MyNDLAUser] = {
      userRepository.userWithFeideId(feideId)(session).flatMap {
        case None =>
          createMyNDLAUser(feideId, feideAccessToken)(session)
        case Some(userData) =>
          if (userData.wasUpdatedLast24h) Success(userData)
          else fetchDataAndUpdateMyNDLAUser(feideId, feideAccessToken, userData)(session)
      }
    }

    private def getFeideUserDataAuthenticated(
        feideId: FeideID,
        feideAccessToken: Option[FeideAccessToken]
    ): Try[api.MyNDLAUser] =
      for {
        user <- getOrCreateMyNDLAUserIfNotExist(feideId, feideAccessToken)(AutoSession)
        orgs <- readService.getMyNDLAEnabledOrgs
      } yield converterService.toApiUserData(user, orgs)

    def getMyNDLAUserData(feideAccessToken: Option[FeideAccessToken]): Try[api.MyNDLAUser] = {
      for {
        feideId  <- feideApiClient.getFeideID(feideAccessToken)
        userData <- getOrCreateMyNDLAUserIfNotExist(feideId, feideAccessToken)(AutoSession)
        orgs     <- readService.getMyNDLAEnabledOrgs
        api = converterService.toApiUserData(userData, orgs)
      } yield api
    }

    def getStats: Option[Stats] = {
      implicit val session: DBSession = folderRepository.getSession(true)
      val groupedResources            = folderRepository.numberOfResourcesGrouped()
      val favouritedResources         = groupedResources.map(gr => ResourceStats(gr._2, gr._1))
      for {
        numberOfUsers         <- userRepository.numberOfUsers()
        numberOfFolders       <- folderRepository.numberOfFolders()
        numberOfResources     <- folderRepository.numberOfResources()
        numberOfTags          <- folderRepository.numberOfTags()
        numberOfSubjects      <- userRepository.numberOfFavouritedSubjects()
        numberOfSharedFolders <- folderRepository.numberOfSharedFolders()
        stats = Stats(
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
  }
}
