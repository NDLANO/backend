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
import no.ndla.common.errors.{AccessDeniedException, NotFoundException, ValidationException}
import no.ndla.common.implicits.TryQuestionMark
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.ResourceType
import no.ndla.common.model.domain.myndla.{FolderStatus, MyNDLAUser}
import no.ndla.myndlaapi.integration.SearchApiClient
import no.ndla.myndlaapi.model.domain.FolderSortObject.{
  FolderSorting,
  ResourceSorting,
  RootFolderSorting,
  SharedFolderSorting
}
import no.ndla.myndlaapi.model.api.{
  ExportedUserData,
  Folder,
  FolderSortRequest,
  NewFolder,
  NewResource,
  Resource,
  UpdatedFolder,
  UpdatedResource
}
import no.ndla.myndlaapi.model.{api, domain}
import no.ndla.myndlaapi.model.domain.{
  CopyableFolder,
  FolderAndDirectChildren,
  FolderSortException,
  Rankable,
  SavedSharedFolder
}
import no.ndla.myndlaapi.repository.{FolderRepository, UserRepository}
import no.ndla.network.clients.FeideApiClient
import no.ndla.network.model.{FeideAccessToken, FeideID}
import scalikejdbc.{DBSession, ReadOnlyAutoSession}

import java.util.UUID
import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

trait FolderWriteService {
  this: FolderReadService
    with Clock
    with FeideApiClient
    with FolderRepository
    with FolderConverterService
    with UserRepository
    with ConfigService
    with UserService
    with SearchApiClient =>

  val folderWriteService: FolderWriteService
  class FolderWriteService {

    val MaxFolderDepth = 5L

    private def getMyNDLAUser(feideId: FeideID, feideAccessToken: Option[FeideAccessToken]): Try[MyNDLAUser] = {
      userRepository.rollbackOnFailure(session =>
        userService.getOrCreateMyNDLAUserIfNotExist(feideId, feideAccessToken, List.empty)(session)
      )
    }

    private[service] def isOperationAllowedOrAccessDenied(
        feideId: FeideID,
        feideAccessToken: Option[FeideAccessToken],
        updatedFolder: UpdatedFolder
    ): Try[?] = {
      getMyNDLAUser(feideId, feideAccessToken).flatMap(myNDLAUser => {
        if (myNDLAUser.isStudent && updatedFolder.status.contains(FolderStatus.SHARED.toString))
          Failure(AccessDeniedException("You do not have necessary permissions to share folders."))
        else
          canWriteNow(myNDLAUser).flatMap {
            case true => Success(())
            case false =>
              Failure(AccessDeniedException("You do not have write access while write restriction is active."))
          }
      })
    }

    private def canWriteNow(myNDLAUser: MyNDLAUser): Try[Boolean] = {
      if (myNDLAUser.isTeacher) return Success(true)
      configService.isMyNDLAWriteRestricted.map(!_)
    }

    private def handleFolderUserConnectionsOnUnShare(
        folderIds: List[UUID],
        newStatus: FolderStatus.Value,
        oldStatus: FolderStatus.Value
    )(implicit session: DBSession): Try[?] = {
      (oldStatus, newStatus) match {
        case (FolderStatus.SHARED, FolderStatus.PRIVATE) => folderRepository.deleteFolderUserConnections(folderIds)
        case _                                           => Success(())
      }
    }

    def changeStatusOfFolderAndItsSubfolders(
        folderId: UUID,
        newStatus: FolderStatus.Value,
        feideAccessToken: Option[FeideAccessToken]
    ): Try[List[UUID]] =
      folderRepository.rollbackOnFailure({ implicit session =>
        for {
          feideId    <- feideApiClient.getFeideID(feideAccessToken)
          _          <- isTeacherOrAccessDenied(feideId, feideAccessToken)
          folder     <- folderRepository.folderWithId(folderId)
          _          <- folder.isOwner(feideId)
          ids        <- folderRepository.getFoldersAndSubfoldersIds(folderId)
          updatedIds <- folderRepository.updateFolderStatusInBulk(ids, newStatus)
          _          <- handleFolderUserConnectionsOnUnShare(ids, newStatus, folder.status)
        } yield updatedIds
      })

    private[service] def cloneChildrenRecursively(
        sourceFolder: CopyableFolder,
        destinationFolder: domain.Folder,
        feideId: FeideID,
        isOwner: Boolean
    )(implicit session: DBSession): Try[domain.Folder] = {

      val clonedResources = sourceFolder.resources.traverse(res => {
        val newResource =
          NewResource(
            resourceType = res.resourceType,
            path = res.path,
            tags = if (isOwner) res.tags.some else None,
            resourceId = res.resourceId
          )
        createOrUpdateFolderResourceConnection(destinationFolder.id, newResource, feideId)
      })

      val clonedSubfolders = sourceFolder.subfolders.traverse(childFolder => {
        val newFolder = domain.NewFolderData(
          parentId = destinationFolder.id.some,
          name = childFolder.name,
          status = FolderStatus.PRIVATE,
          rank = childFolder.rank,
          description = childFolder.description
        )
        folderRepository
          .insertFolder(feideId, newFolder)
          .flatMap(newFolder => cloneChildrenRecursively(childFolder, newFolder, feideId, isOwner))
      })

      for {
        resources <- clonedResources
        folders   <- clonedSubfolders
      } yield destinationFolder.copy(subfolders = folders, resources = resources)
    }

    private def cloneRecursively(
        sourceFolder: CopyableFolder,
        destinationId: Option[UUID],
        feideId: FeideID,
        makeUniqueRootNamesWithPostfix: Option[String],
        isOwner: Boolean
    )(implicit
        session: DBSession
    ): Try[domain.Folder] = {
      val sourceFolderCopy = NewFolder(
        name = sourceFolder.name,
        parentId = None,
        status = FolderStatus.PRIVATE.toString.some,
        description = sourceFolder.description
      )

      destinationId match {
        case None =>
          for {
            createdFolder <- createNewFolder(
              sourceFolderCopy,
              feideId,
              makeUniqueRootNamesWithPostfix,
              isCloning = true
            )
            clonedFolder <- cloneChildrenRecursively(sourceFolder, createdFolder, feideId, isOwner)
          } yield clonedFolder
        case Some(id) =>
          for {
            existingFolder <- folderRepository.folderWithId(id)
            clonedSourceFolder = sourceFolderCopy.copy(parentId = existingFolder.id.toString.some)
            createdFolder <- createNewFolder(
              clonedSourceFolder,
              feideId,
              makeUniqueRootNamesWithPostfix,
              isCloning = true
            )
            clonedFolder <- cloneChildrenRecursively(sourceFolder, createdFolder, feideId, isOwner)
          } yield clonedFolder
      }
    }

    def cloneFolder(
        sourceId: UUID,
        destinationId: Option[UUID],
        feideAccessToken: Option[FeideAccessToken]
    ): Try[Folder] = {
      folderRepository.rollbackOnFailure { implicit session =>
        for {
          feideId <- feideApiClient.getFeideID(feideAccessToken)
          _       <- canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied(feideId, feideAccessToken)
          maybeFolder = folderRepository.getFolderAndChildrenSubfoldersWithResources(
            sourceId,
            FolderStatus.SHARED,
            Some(feideId)
          )
          sourceFolder <- folderReadService.getWith404IfNone(sourceId, maybeFolder)
          isOwner = sourceFolder.feideId == feideId
          _            <- sourceFolder.isClonable
          clonedFolder <- cloneRecursively(sourceFolder, destinationId, feideId, "_Kopi".some, isOwner)(session)
          breadcrumbs  <- folderReadService.getBreadcrumbs(clonedFolder)
          feideUser    <- userRepository.userWithFeideId(feideId)
          converted    <- folderConverterService.toApiFolder(clonedFolder, breadcrumbs, feideUser, isOwner)
        } yield converted
      }
    }

    private def importFolders(toImport: Seq[Folder], feideId: FeideID)(implicit
        session: DBSession
    ): Try[Seq[domain.Folder]] =
      toImport.traverse(folder =>
        cloneRecursively(
          folder,
          None,
          feideId,
          makeUniqueRootNamesWithPostfix = " (Fra import)".some,
          isOwner = true
        )
      )

    private def importUserDataAuthenticated(
        toImport: ExportedUserData,
        feideId: FeideID,
        maybeFeideToken: Option[FeideAccessToken]
    ): Try[ExportedUserData] = {
      folderRepository.rollbackOnFailure { session =>
        for {
          _ <- canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied(feideId, maybeFeideToken)
          _ <- userService.importUser(toImport.userData, feideId, maybeFeideToken)(session)
          _ <- importFolders(toImport.folders, feideId)(session)
        } yield toImport
      }
    }

    def importUserData(
        toImport: ExportedUserData,
        maybeFeideToken: Option[FeideAccessToken]
    ): Try[ExportedUserData] = {
      feideApiClient
        .getFeideID(maybeFeideToken)
        .flatMap(feideId => importUserDataAuthenticated(toImport, feideId, maybeFeideToken))
    }

    private def connectIfNotConnected(folderId: UUID, resourceId: UUID, rank: Int, favoritedDate: NDLADate)(implicit
        session: DBSession
    ): Try[domain.FolderResource] =
      folderRepository.getConnection(folderId, resourceId) match {
        case Success(Some(connection)) => Success(connection)
        case Success(None) => folderRepository.createFolderResourceConnection(folderId, resourceId, rank, favoritedDate)
        case Failure(ex)   => Failure(ex)
      }

    def updateFolder(
        id: UUID,
        updatedFolder: UpdatedFolder,
        feideAccessToken: Option[FeideAccessToken]
    ): Try[Folder] = {
      implicit val session: DBSession = folderRepository.getSession(readOnly = false)
      for {
        feideId        <- feideApiClient.getFeideID(feideAccessToken)
        _              <- isOperationAllowedOrAccessDenied(feideId, feideAccessToken, updatedFolder)
        existingFolder <- folderRepository.folderWithId(id)
        _              <- existingFolder.isOwner(feideId)
        converted      <- Try(folderConverterService.mergeFolder(existingFolder, updatedFolder))
        maybeSiblings  <- getFolderWithDirectChildren(converted.parentId, feideId)
        _              <- validateUpdatedFolder(converted.name, converted.parentId, maybeSiblings, converted)
        updated        <- folderRepository.updateFolder(id, feideId, converted)
        crumbs         <- folderReadService.getBreadcrumbs(updated)(ReadOnlyAutoSession)
        feideUser      <- userRepository.userWithFeideId(feideId)
        api            <- folderConverterService.toApiFolder(updated, crumbs, feideUser, isOwner = true)
      } yield api
    }

    def updateResource(
        id: UUID,
        updatedResource: UpdatedResource,
        feideAccessToken: Option[FeideAccessToken] = None
    ): Try[Resource] = {
      for {
        feideId          <- feideApiClient.getFeideID(feideAccessToken)
        _                <- canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied(feideId, feideAccessToken)
        existingResource <- folderRepository.resourceWithId(id)
        _                <- existingResource.isOwner(feideId)
        converted = folderConverterService.mergeResource(existingResource, updatedResource)
        updated <- folderRepository.updateResource(converted)
        api     <- folderConverterService.toApiResource(updated, isOwner = true)
      } yield api
    }

    private def deleteResourceIfNoConnection(folderId: UUID, resourceId: UUID)(implicit
        session: DBSession
    ): Try[UUID] = {
      folderRepository.folderResourceConnectionCount(resourceId) match {
        case Failure(exception)           => Failure(exception)
        case Success(count) if count == 1 => folderRepository.deleteResource(resourceId)
        case Success(_)                   => folderRepository.deleteFolderResourceConnection(folderId, resourceId)
      }
    }

    private def deleteRecursively(folder: domain.Folder, feideId: FeideID)(implicit session: DBSession): Try[UUID] = {
      for {
        _ <- folder.resources.traverse(res => deleteResourceIfNoConnection(folder.id, res.id))
        _ <- folder.subfolders.traverse(childFolder => deleteRecursively(childFolder, feideId))
        _ <- folderRepository.deleteFolder(folder.id)
        _ <- folderRepository.deleteFolderUserConnection(folder.id.some, None)
      } yield folder.id
    }

    def deleteFolder(id: UUID, feideAccessToken: Option[FeideAccessToken]): Try[UUID] = {
      implicit val session: DBSession = folderRepository.getSession(readOnly = false)
      for {
        feideId <- feideApiClient.getFeideID(feideAccessToken)
        _       <- canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied(feideId, feideAccessToken)
        folder  <- folderRepository.folderWithId(id)
        _       <- folder.isOwner(feideId)
        parent  <- getFolderWithDirectChildren(folder.parentId, feideId)
        folderWithData <- folderReadService.getSingleFolderWithContent(
          id,
          includeSubfolders = true,
          includeResources = true
        )
        deletedFolderId <- deleteRecursively(folderWithData, feideId)
        siblingsToSort = parent.childrenFolders.filterNot(_.id == deletedFolderId)
        sortRequest    = FolderSortRequest(sortedIds = siblingsToSort.map(_.id))
        _ <- performSort(siblingsToSort, sortRequest, feideId, sharedFolderSort = false)
      } yield deletedFolderId
    }

    def deleteConnection(
        folderId: UUID,
        resourceId: UUID,
        feideAccessToken: Option[FeideAccessToken]
    ): Try[UUID] = {
      implicit val session: DBSession = folderRepository.getSession(readOnly = false)
      for {
        feideId  <- feideApiClient.getFeideID(feideAccessToken)
        _        <- canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied(feideId, feideAccessToken)
        folder   <- folderRepository.folderWithId(folderId)
        _        <- folder.isOwner(feideId)
        resource <- folderRepository.resourceWithId(resourceId)
        _        <- resource.isOwner(feideId)
        id       <- deleteResourceIfNoConnection(folderId, resourceId)
        parent   <- getFolderWithDirectChildren(folder.id.some, feideId)
        siblingsToSort = parent.childrenResources.filterNot(c => c.resourceId == resourceId && c.folderId == folderId)
        sortRequest    = api.FolderSortRequest(sortedIds = siblingsToSort.map(_.resourceId))
        _              = updateSearchApi(resource)
        _ <- performSort(siblingsToSort, sortRequest, feideId, sharedFolderSort = false)
      } yield id
    }

    def deleteAllUserData(feideAccessToken: Option[FeideAccessToken]): Try[Unit] = {
      for {
        feideId <- feideApiClient.getFeideID(feideAccessToken)
        _       <- folderRepository.deleteAllUserFolders(feideId)
        _       <- folderRepository.deleteAllUserResources(feideId)
        _       <- userRepository.deleteUser(feideId)
        _       <- folderRepository.deleteFolderUserConnection(None, feideId.some)
      } yield ()
    }

    private def performSort(
        rankables: Seq[Rankable],
        sortRequest: api.FolderSortRequest,
        feideId: FeideID,
        sharedFolderSort: Boolean
    ): Try[Unit] = {
      val allIds     = rankables.map(_.sortId)
      val hasEveryId = allIds.forall(sortRequest.sortedIds.contains)
      if (!hasEveryId || allIds.size != sortRequest.sortedIds.size)
        return Failure(
          ValidationException(
            "ids",
            s"You need to supply _every_ direct child of the folder when sorting."
          )
        )

      folderRepository.withTx { session =>
        sortRequest.sortedIds
          .mapWithIndex((id, idx) => {
            val newRank = idx + 1
            val found   = rankables.find(_.sortId == id)
            found match {
              case Some(domain.Folder(folderId, _, _, _, _, _, _, _, _, _, _, _, _)) if sharedFolderSort =>
                folderRepository.setSharedFolderRank(folderId, newRank, feideId)(session)
              case Some(domain.Folder(folderId, _, _, _, _, _, _, _, _, _, _, _, _)) =>
                folderRepository.setFolderRank(folderId, newRank, feideId)(session)
              case Some(domain.FolderResource(folderId, resourceId, _, _)) =>
                folderRepository.setResourceConnectionRank(folderId, resourceId, newRank)(session)
              case _ => Failure(FolderSortException("Something went wrong when sorting! This seems like a bug!"))
            }
          })
          .sequence
          .map(_ => ())
      }
    }

    private def sortRootFolders(sortRequest: api.FolderSortRequest, feideId: FeideID): Try[Unit] = {
      val session = folderRepository.getSession(true)
      folderRepository
        .foldersWithFeideAndParentID(None, feideId)(session)
        .flatMap(rootFolders => performSort(rootFolders, sortRequest, feideId, sharedFolderSort = false))
    }

    private def sortSavedSharedFolders(sortRequest: api.FolderSortRequest, feideId: FeideID): Try[Unit] = {
      val session = folderRepository.getSession(true)
      folderRepository
        .getSavedSharedFolders(feideId)(session)
        .flatMap(savedFolders => performSort(savedFolders, sortRequest, feideId, sharedFolderSort = true))
    }

    private def sortNonRootFolderResources(
        folderId: UUID,
        sortRequest: api.FolderSortRequest,
        feideId: FeideID
    )(implicit
        session: DBSession
    ): Try[Unit] = getFolderWithDirectChildren(folderId.some, feideId).flatMap {
      case FolderAndDirectChildren(_, _, resources) =>
        performSort(resources, sortRequest, feideId, sharedFolderSort = false)
    }

    private def sortNonRootFolderSubfolders(
        folderId: UUID,
        sortRequest: api.FolderSortRequest,
        feideId: FeideID
    )(implicit
        session: DBSession
    ): Try[Unit] = getFolderWithDirectChildren(folderId.some, feideId).flatMap {
      case FolderAndDirectChildren(_, subfolders, _) =>
        performSort(subfolders, sortRequest, feideId, sharedFolderSort = false)
    }

    def sortFolder(
        folderSortObject: domain.FolderSortObject,
        sortRequest: api.FolderSortRequest,
        feideAccessToken: Option[FeideAccessToken]
    ): Try[Unit] = {
      implicit val session: DBSession = folderRepository.getSession(readOnly = false)
      val feideId                     = feideApiClient.getFeideID(feideAccessToken).?
      canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied(feideId, feideAccessToken).??
      folderSortObject match {
        case ResourceSorting(parentId) => sortNonRootFolderResources(parentId, sortRequest, feideId)
        case FolderSorting(parentId)   => sortNonRootFolderSubfolders(parentId, sortRequest, feideId)
        case RootFolderSorting()       => sortRootFolders(sortRequest, feideId)
        case SharedFolderSorting()     => sortSavedSharedFolders(sortRequest, feideId)
      }
    }

    private def checkDepth(parentId: Option[UUID]): Try[Unit] = {
      parentId match {
        case None => Success(())
        case Some(pid) =>
          folderRepository.getFoldersDepth(pid) match {
            case Failure(ex) => Failure(ex)
            case Success(currentDepth) if currentDepth >= MaxFolderDepth =>
              Failure(
                ValidationException(
                  "MAX_DEPTH_LIMIT_REACHED",
                  s"Folder can not be created, max folder depth limit of $MaxFolderDepth reached."
                )
              )
            case _ => Success(())
          }
      }
    }

    private def getFolderWithDirectChildren(maybeParentId: Option[UUID], feideId: FeideID)(implicit
        session: DBSession
    ): Try[FolderAndDirectChildren] = maybeParentId match {
      case None =>
        folderRepository
          .foldersWithFeideAndParentID(None, feideId)
          .map(siblingFolders => {
            domain.FolderAndDirectChildren(None, siblingFolders, Seq.empty)
          })
      case Some(parentId) =>
        folderRepository.folderWithFeideId(parentId, feideId) match {
          case Failure(ex) => Failure(ex)
          case Success(parent) =>
            for {
              siblingFolders   <- folderRepository.foldersWithFeideAndParentID(parentId.some, feideId)
              siblingResources <- folderRepository.getConnections(parentId)
            } yield domain.FolderAndDirectChildren(Some(parent), siblingFolders, siblingResources)
        }
    }

    private def validateSiblingNames(
        name: String,
        maybeParentAndSiblings: domain.FolderAndDirectChildren
    ): Try[Unit] = {
      val domain.FolderAndDirectChildren(_, siblings, _) = maybeParentAndSiblings
      val hasNameDuplicate = siblings.map(_.name).exists(_.toLowerCase == name.toLowerCase)
      if (hasNameDuplicate) {
        Failure(ValidationException("name", s"The folder name must be unique within its parent."))
      } else Success(())
    }

    private def getMaybeParentId(parentId: Option[String]): Try[Option[UUID]] = {
      parentId.traverse(pid => folderConverterService.toUUIDValidated(pid.some, "parentId"))
    }

    private def validateUpdatedFolder(
        folderName: String,
        parentId: Option[UUID],
        maybeParentAndSiblings: domain.FolderAndDirectChildren,
        updatedFolder: domain.Folder
    ): Try[Option[UUID]] = {
      val folderTreeWithoutTheUpdatee = maybeParentAndSiblings.withoutChild(updatedFolder.id)
      for {
        validatedParentId <- validateParentId(parentId, maybeParentAndSiblings.folder)
        _                 <- validateSiblingNames(folderName, folderTreeWithoutTheUpdatee)
        _                 <- checkDepth(validatedParentId)
      } yield validatedParentId
    }

    private def validateNewFolder(
        folderName: String,
        parentId: Option[UUID],
        maybeParentAndSiblings: domain.FolderAndDirectChildren
    ): Try[Option[UUID]] = for {
      validatedParentId <- validateParentId(parentId, maybeParentAndSiblings.folder)
      _                 <- validateSiblingNames(folderName, maybeParentAndSiblings)
      _                 <- checkDepth(validatedParentId)
    } yield validatedParentId

    private def getNextRank(siblings: Seq[_]): Int = siblings.length + 1

    private[service] def changeStatusToSharedIfParentIsShared(
        newFolder: NewFolder,
        parentFolder: Option[domain.Folder],
        isCloning: Boolean
    ): NewFolder = {
      import FolderStatus.SHARED

      parentFolder match {
        case Some(parent) if parent.status == SHARED && !isCloning => newFolder.copy(status = SHARED.toString.some)
        case _                                                     => newFolder
      }
    }

    private def createNewFolder(
        newFolder: NewFolder,
        feideId: FeideID,
        makeUniqueNamePostfix: Option[String],
        isCloning: Boolean
    )(implicit
        session: DBSession
    ): Try[domain.Folder] = {

      val parentId      = getMaybeParentId(newFolder.parentId).?
      val maybeSiblings = getFolderWithDirectChildren(parentId, feideId).?
      val nextRank      = getNextRank(maybeSiblings.childrenFolders)
      val withStatus    = changeStatusToSharedIfParentIsShared(newFolder, maybeSiblings.folder, isCloning)
      val folderWithName =
        withStatus.copy(name = getFolderValidName(makeUniqueNamePostfix, newFolder.name, maybeSiblings))
      val validatedParentId = validateNewFolder(folderWithName.name, parentId, maybeSiblings).?
      val newFolderData     = folderConverterService.toNewFolderData(folderWithName, validatedParentId, nextRank).?
      val inserted          = folderRepository.insertFolder(feideId, newFolderData).?

      Success(inserted)
    }

    private def getFolderValidName(
        makeUniqueNamePostfix: Option[String],
        folderName: String,
        maybeParentAndSiblings: domain.FolderAndDirectChildren
    ): String = {
      makeUniqueNamePostfix match {
        case None => folderName
        case Some(postfix) =>
          @tailrec
          def getCopyUntilValid(folderName: String): String =
            if (validateSiblingNames(folderName, maybeParentAndSiblings).isFailure) {
              getCopyUntilValid(s"$folderName$postfix")
            } else { folderName }

          getCopyUntilValid(folderName)
      }
    }

    def newFolder(newFolder: NewFolder, feideAccessToken: Option[FeideAccessToken]): Try[Folder] = {
      implicit val session: DBSession = folderRepository.getSession(readOnly = false)
      for {
        feideId   <- feideApiClient.getFeideID(feideAccessToken)
        _         <- canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied(feideId, feideAccessToken)
        inserted  <- createNewFolder(newFolder, feideId, makeUniqueNamePostfix = None, isCloning = false)
        crumbs    <- folderReadService.getBreadcrumbs(inserted)(ReadOnlyAutoSession)
        feideUser <- userRepository.userWithFeideId(feideId)
        api       <- folderConverterService.toApiFolder(inserted, crumbs, feideUser, isOwner = true)
      } yield api
    }

    private def createOrUpdateFolderResourceConnection(
        folderId: UUID,
        newResource: NewResource,
        feideId: FeideID
    )(implicit
        session: DBSession
    ): Try[domain.Resource] =
      for {
        _ <- folderRepository
          .folderWithFeideId(folderId, feideId)
          .orElse(Failure(NotFoundException(s"Can't connect resource to non-existing folder")))
        siblings          <- getFolderWithDirectChildren(folderId.some, feideId)
        insertedOrUpdated <- createNewResourceOrUpdateExisting(newResource, folderId, siblings, feideId)
        _ = updateSearchApi(insertedOrUpdated)
      } yield insertedOrUpdated

    private def updateSearchApi(resource: domain.Resource): Unit = {
      resource.resourceType match {
        case ResourceType.Multidisciplinary => searchApiClient.reindexDraft(resource.resourceId)
        case ResourceType.Article           => searchApiClient.reindexDraft(resource.resourceId)
        case ResourceType.Learningpath      => searchApiClient.reindexLearningpath(resource.resourceId)
        case _                              =>
      }
    }

    def newFolderResourceConnection(
        folderId: UUID,
        newResource: NewResource,
        feideAccessToken: Option[FeideAccessToken]
    ): Try[Resource] = {
      implicit val session: DBSession = folderRepository.getSession(readOnly = false)
      for {
        feideId   <- feideApiClient.getFeideID(feideAccessToken)
        _         <- canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied(feideId, feideAccessToken)
        resource  <- createOrUpdateFolderResourceConnection(folderId, newResource, feideId)
        converted <- folderConverterService.toApiResource(resource, isOwner = true)
      } yield converted
    }

    private[service] def createNewResourceOrUpdateExisting(
        newResource: NewResource,
        folderId: UUID,
        siblings: domain.FolderAndDirectChildren,
        feideId: FeideID
    )(implicit session: DBSession): Try[domain.Resource] = {
      val rank = getNextRank(siblings.childrenResources)
      val date = clock.now()
      folderRepository
        .resourceWithPathAndTypeAndFeideId(newResource.path, newResource.resourceType, feideId)
        .flatMap {
          case None =>
            val document = folderConverterService.toDomainResource(newResource)
            for {
              inserted <- folderRepository.insertResource(
                feideId,
                newResource.path,
                newResource.resourceType,
                date,
                document
              )
              connection <- folderRepository.createFolderResourceConnection(folderId, inserted.id, rank, date)
            } yield inserted.copy(connection = connection.some)
          case Some(existingResource) =>
            val mergedResource = folderConverterService.mergeResource(existingResource, newResource)
            for {
              updated    <- folderRepository.updateResource(mergedResource)
              connection <- connectIfNotConnected(folderId, mergedResource.id, rank, date)
            } yield updated.copy(connection = connection.some)
        }
    }

    private def validateParentId(parentId: Option[UUID], parent: Option[domain.Folder]): Try[Option[UUID]] =
      (parentId, parent) match {
        case (Some(_), None) =>
          val paramName = "parentId"
          Failure(
            ValidationException(
              paramName,
              s"Invalid value for $paramName. The UUID specified does not exist or is not writable by you."
            )
          )
        case _ => Success(parentId)
      }

    def canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied(
        feideId: FeideID,
        feideAccessToken: Option[FeideAccessToken]
    ): Try[_] = {
      getMyNDLAUser(feideId, feideAccessToken)
        .flatMap(myNDLAUser =>
          canWriteNow(myNDLAUser).flatMap {
            case true => Success(())
            case false =>
              Failure(AccessDeniedException("You do not have write access while write restriction is active."))
          }
        )
    }

    def newSaveSharedFolder(folderId: UUID, feideAccessToken: Option[FeideAccessToken]): Try[Unit] = {
      implicit val session: DBSession = folderRepository.getSession(readOnly = false)
      for {
        feideId <- feideApiClient.getFeideID(feideAccessToken)
        _       <- createSharedFolderUserConnection(folderId, feideId)
      } yield ()
    }

    private def createSharedFolderUserConnection(folderId: UUID, feideId: FeideID)(implicit
        session: DBSession
    ): Try[SavedSharedFolder] = {
      for {
        folder       <- folderRepository.folderWithId(folderId).filter(f => f.isShared)
        savedFolders <- folderRepository.getSavedSharedFolders(feideId)
        newRank = savedFolders.length + 1
        folderUser <- folderRepository.createFolderUserConnection(folder.id, feideId, newRank)
      } yield folderUser
    }

    def deleteSavedSharedFolder(folderId: UUID, feideAccessToken: Option[FeideAccessToken]): Try[Unit] = {
      implicit val session: DBSession = folderRepository.getSession(readOnly = false)
      for {
        feideId <- feideApiClient.getFeideID(feideAccessToken)
        _       <- deleteFolderUserConnection(folderId, feideId)
      } yield ()
    }

    private def deleteFolderUserConnection(folderId: UUID, feideId: FeideID)(implicit session: DBSession): Try[Int] = {
      folderRepository.deleteFolderUserConnection(folderId.some, feideId.some)
    }

    private def isTeacherOrAccessDenied(feideId: FeideID, feideAccessToken: Option[FeideAccessToken]): Try[_] = {
      getMyNDLAUser(feideId, feideAccessToken)
        .flatMap(myNDLAUser => {
          if (myNDLAUser.isTeacher) Success(())
          else Failure(AccessDeniedException("You do not have necessary permissions to share folders."))
        })
    }

  }
}
