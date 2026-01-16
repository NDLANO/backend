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
import no.ndla.common.Clock
import no.ndla.common.errors.{AccessDeniedException, NotFoundException, ValidationException}
import no.ndla.common.implicits.*
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.ResourceType
import no.ndla.common.model.domain.myndla.{FolderStatus, MyNDLAUser}
import no.ndla.database.DBUtility
import no.ndla.myndlaapi.integration.SearchApiClient
import no.ndla.myndlaapi.model.domain.FolderSortObject.{
  FolderSorting,
  ResourceSorting,
  RootFolderSorting,
  SharedFolderSorting,
}
import no.ndla.myndlaapi.model.api.{
  ExportedUserDataDTO,
  FolderDTO,
  FolderSortRequestDTO,
  NewFolderDTO,
  NewResourceDTO,
  ResourceDTO,
  UpdatedFolderDTO,
  UpdatedResourceDTO,
}
import no.ndla.myndlaapi.model.{api, domain}
import no.ndla.myndlaapi.model.domain.{
  BulkInserts,
  CopyableFolder,
  FolderAndDirectChildren,
  FolderSortException,
  Rankable,
  SavedSharedFolder,
}
import no.ndla.myndlaapi.repository.{FolderRepository, UserRepository}
import no.ndla.network.clients.FeideApiClient
import no.ndla.network.model.{FeideAccessToken, FeideID}
import scalikejdbc.{DBSession, ReadOnlyAutoSession}

import java.util.UUID
import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

class FolderWriteService(using
    folderReadService: FolderReadService,
    clock: Clock,
    feideApiClient: FeideApiClient,
    folderRepository: FolderRepository,
    folderConverterService: FolderConverterService,
    userRepository: UserRepository,
    configService: ConfigService,
    userService: => UserService,
    searchApiClient: SearchApiClient,
    dbUtility: DBUtility,
) extends StrictLogging {
  val MaxFolderDepth = 5L

  private[service] def isOperationAllowedOrAccessDenied(
      feideId: FeideID,
      feideAccessToken: Option[FeideAccessToken],
      updatedFolder: UpdatedFolderDTO,
  )(implicit session: DBSession = ReadOnlyAutoSession): Try[?] = {
    userService
      .getMyNDLAUser(feideId, feideAccessToken)(using session)
      .flatMap(myNDLAUser => {
        if (myNDLAUser.isStudent && updatedFolder.status.contains(FolderStatus.SHARED.toString))
          Failure(AccessDeniedException("You do not have necessary permissions to share folders."))
        else canWriteNow(myNDLAUser).flatMap {
          case true  => Success(())
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
      oldStatus: FolderStatus.Value,
  )(implicit session: DBSession): Try[?] = {
    (oldStatus, newStatus) match {
      case (FolderStatus.SHARED, FolderStatus.PRIVATE) => folderRepository.deleteFolderUserConnections(folderIds)
      case _                                           => Success(())
    }
  }

  def changeStatusOfFolderAndItsSubfolders(
      folderId: UUID,
      newStatus: FolderStatus.Value,
      feideAccessToken: Option[FeideAccessToken],
  ): Try[List[UUID]] = dbUtility.rollbackOnFailure({ implicit session =>
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

  private def buildInsertList(
      source: CopyableFolder,
      destination: domain.Folder,
      toInsert: BulkInserts,
      feideId: FeideID,
      now: NDLADate,
      isOwner: Boolean,
  ): BulkInserts = {
    val withResources = source
      .resources
      .sortBy(_.rank)
      .zipWithIndex
      .foldLeft(toInsert) { case (acc, cur) =>
        val (curResource, idx) = cur
        val newResourceId      = UUID.randomUUID()
        acc
          .addResource(
            domain.Resource(
              id = newResourceId,
              feideId = feideId,
              created = now,
              path = curResource.path,
              resourceType = curResource.resourceType,
              tags =
                if (isOwner) curResource.tags
                else List.empty,
              resourceId = curResource.resourceId,
              connection = None,
            )
          )
          .addConnection(
            domain.FolderResource(
              folderId = destination.id,
              resourceId = newResourceId,
              rank = idx + 1,
              favoritedDate = now,
            )
          )
      }
    source
      .subfolders
      .foldLeft(withResources) { case (acc, cur) =>
        val newFolder = domain.Folder(
          id = UUID.randomUUID(),
          feideId = feideId,
          parentId = destination.id.some,
          name = cur.name,
          status = FolderStatus.PRIVATE,
          description = cur.description,
          rank = cur.rank,
          created = now,
          updated = now,
          resources = List.empty,
          subfolders = List.empty,
          shared = None,
          user = None,
        )
        val wf = acc.addFolder(newFolder)
        buildInsertList(cur, newFolder, wf, feideId, now, isOwner)
      }
  }

  private[service] def cloneChildrenRecursively(
      sourceFolder: CopyableFolder,
      destinationFolder: domain.Folder,
      feideId: FeideID,
      isOwner: Boolean,
  )(implicit session: DBSession): Try[Unit] = {
    val now      = clock.now()
    val toInsert = buildInsertList(sourceFolder, destinationFolder, BulkInserts.empty, feideId, now, isOwner)
    for {
      _ <- folderRepository.insertFolderInBulk(toInsert)(using session)
      _ <- folderRepository.insertResourcesInBulk(toInsert)(using session)
      _ <- folderRepository.insertResourceConnectionInBulk(toInsert)(using session)
    } yield ()
  }

  private def cloneRecursively(
      sourceFolder: CopyableFolder,
      destinationId: Option[UUID],
      feideId: FeideID,
      makeUniqueRootNamesWithPostfix: Option[String],
      isOwner: Boolean,
  )(implicit session: DBSession): Try[domain.Folder] = {
    val sourceFolderCopy = NewFolderDTO(
      name = sourceFolder.name,
      parentId = None,
      status = FolderStatus.PRIVATE.toString.some,
      description = sourceFolder.description,
    )

    for {
      maybeExistingFolder <- destinationId.traverse(id => folderRepository.folderWithId(id))
      clonedSourceFolder   = sourceFolderCopy.copy(parentId = maybeExistingFolder.map(_.id.toString))
      createdFolder       <- createNewFolder(clonedSourceFolder, feideId, makeUniqueRootNamesWithPostfix, isCloning = true)
      _                   <- cloneChildrenRecursively(sourceFolder, createdFolder, feideId, isOwner)
      maybeFolder         <- folderRepository.getFolderAndChildrenSubfoldersWithResources(createdFolder.id)
      clonedFolder        <- maybeFolder.toTry(new IllegalStateException("Folder not found after cloning. This is a bug"))
    } yield clonedFolder
  }

  def cloneFolder(
      sourceId: UUID,
      destinationId: Option[UUID],
      feideAccessToken: Option[FeideAccessToken],
  ): Try[FolderDTO] = {
    dbUtility.rollbackOnFailure { implicit session =>
      for {
        feideId    <- feideApiClient.getFeideID(feideAccessToken)
        _          <- canWriteOrAccessDenied(feideId, feideAccessToken)
        maybeFolder =
          folderRepository.getFolderAndChildrenSubfoldersWithResources(sourceId, FolderStatus.SHARED, Some(feideId))
        sourceFolder <- folderReadService.getWith404IfNone(sourceId, maybeFolder)
        isOwner       = sourceFolder.feideId == feideId
        _            <- sourceFolder.isClonable
        clonedFolder <- cloneRecursively(sourceFolder, destinationId, feideId, "_Kopi".some, isOwner)(using session)
        breadcrumbs  <- folderReadService.getBreadcrumbs(clonedFolder)
        feideUser    <- userRepository.userWithFeideId(feideId)
        converted    <- folderConverterService.toApiFolder(clonedFolder, breadcrumbs, feideUser, isOwner)
      } yield converted
    }
  }

  private def importFolders(toImport: Seq[FolderDTO], feideId: FeideID)(implicit
      session: DBSession
  ): Try[Seq[domain.Folder]] = toImport.traverse(folder =>
    cloneRecursively(folder, None, feideId, makeUniqueRootNamesWithPostfix = " (Fra import)".some, isOwner = true)
  )

  private def importUserDataAuthenticated(
      toImport: ExportedUserDataDTO,
      feideId: FeideID,
      maybeFeideToken: Option[FeideAccessToken],
  ): Try[ExportedUserDataDTO] = {
    dbUtility.rollbackOnFailure { session =>
      for {
        _ <- canWriteOrAccessDenied(feideId, maybeFeideToken)
        _ <- userService.importUser(toImport.userData, feideId, maybeFeideToken)(using session)
        _ <- importFolders(toImport.folders, feideId)(using session)
      } yield toImport
    }
  }

  def importUserData(
      toImport: ExportedUserDataDTO,
      maybeFeideToken: Option[FeideAccessToken],
  ): Try[ExportedUserDataDTO] = {
    feideApiClient
      .getFeideID(maybeFeideToken)
      .flatMap(feideId => importUserDataAuthenticated(toImport, feideId, maybeFeideToken))
  }

  private def connectIfNotConnected(folderId: UUID, resourceId: UUID, rank: Int, favoritedDate: NDLADate)(implicit
      session: DBSession
  ): Try[domain.FolderResource] = folderRepository.getConnection(folderId, resourceId) match {
    case Success(Some(connection)) => Success(connection)
    case Success(None)             => folderRepository.createFolderResourceConnection(folderId, resourceId, rank, favoritedDate)
    case Failure(ex)               => Failure(ex)
  }

  def updateFolder(
      id: UUID,
      updatedFolder: UpdatedFolderDTO,
      feideAccessToken: Option[FeideAccessToken],
  ): Try[FolderDTO] = {
    implicit val session: DBSession = folderRepository.getSession(readOnly = false)
    for {
      feideId        <- feideApiClient.getFeideID(feideAccessToken)
      _              <- isOperationAllowedOrAccessDenied(feideId, feideAccessToken, updatedFolder)
      existingFolder <- folderRepository.folderWithId(id)
      _              <- existingFolder.isOwner(feideId)
      converted      <- folderConverterService.mergeFolder(existingFolder, updatedFolder)
      maybeSiblings  <- getFolderWithDirectChildren(converted.parentId, feideId)
      _              <- validateUpdatedFolder(converted.name, converted.parentId, maybeSiblings, converted)
      updated        <- folderRepository.updateFolder(id, feideId, converted)
      crumbs         <- folderReadService.getBreadcrumbs(updated)(using ReadOnlyAutoSession)
      siblingsToSort <- getFolderWithDirectChildren(updated.parentId, feideId)
      sortRequest     = FolderSortRequestDTO(sortedIds = siblingsToSort.childrenFolders.map(_.id))
      _              <- performSort(siblingsToSort.childrenFolders, sortRequest, feideId, sharedFolderSort = false)
      feideUser      <- userRepository.userWithFeideId(feideId)
      api            <- folderConverterService.toApiFolder(updated, crumbs, feideUser, isOwner = true)
    } yield api
  }

  def updateResource(
      id: UUID,
      updatedResource: UpdatedResourceDTO,
      feideAccessToken: Option[FeideAccessToken] = None,
  ): Try[ResourceDTO] = {
    for {
      feideId          <- feideApiClient.getFeideID(feideAccessToken)
      _                <- canWriteOrAccessDenied(feideId, feideAccessToken)
      existingResource <- folderRepository.resourceWithId(id)
      _                <- existingResource.isOwner(feideId)
      converted         = folderConverterService.mergeResource(existingResource, updatedResource)
      updated          <- folderRepository.updateResource(converted)
      api              <- folderConverterService.toApiResource(updated, isOwner = true)
    } yield api
  }

  private def deleteResourceIfNoConnection(folderId: UUID, resourceId: UUID)(implicit session: DBSession): Try[UUID] = {
    folderRepository.folderResourceConnectionCount(resourceId) match {
      case Failure(exception)            => Failure(exception)
      case Success(count) if count == 1L => folderRepository.deleteResource(resourceId)
      case Success(_)                    => folderRepository.deleteFolderResourceConnection(folderId, resourceId)
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
      feideId        <- feideApiClient.getFeideID(feideAccessToken)
      _              <- canWriteOrAccessDenied(feideId, feideAccessToken)
      folder         <- folderRepository.folderWithId(id)
      _              <- folder.isOwner(feideId)
      parent         <- getFolderWithDirectChildren(folder.parentId, feideId)
      folderWithData <-
        folderReadService.getSingleFolderWithContent(id, includeSubfolders = true, includeResources = true)
      deletedFolderId <- deleteRecursively(folderWithData, feideId)
      siblingsToSort   = parent.childrenFolders.filterNot(_.id == deletedFolderId)
      sortRequest      = FolderSortRequestDTO(sortedIds = siblingsToSort.map(_.id))
      _               <- performSort(siblingsToSort, sortRequest, feideId, sharedFolderSort = false)
    } yield deletedFolderId
  }

  def deleteConnection(folderId: UUID, resourceId: UUID, feideAccessToken: Option[FeideAccessToken]): Try[UUID] = {
    implicit val session: DBSession = folderRepository.getSession(readOnly = false)
    for {
      feideId       <- feideApiClient.getFeideID(feideAccessToken)
      _             <- canWriteOrAccessDenied(feideId, feideAccessToken)
      folder        <- folderRepository.folderWithId(folderId)
      _             <- folder.isOwner(feideId)
      resource      <- folderRepository.resourceWithId(resourceId)
      _             <- resource.isOwner(feideId)
      id            <- deleteResourceIfNoConnection(folderId, resourceId)
      parent        <- getFolderWithDirectChildren(folder.id.some, feideId)
      siblingsToSort = parent.childrenResources.filterNot(c => c.resourceId == resourceId && c.folderId == folderId)
      sortRequest    = api.FolderSortRequestDTO(sortedIds = siblingsToSort.map(_.resourceId))
      _              = updateSearchApi(resource)
      _             <- performSort(siblingsToSort, sortRequest, feideId, sharedFolderSort = false)
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
      sortRequest: api.FolderSortRequestDTO,
      feideId: FeideID,
      sharedFolderSort: Boolean,
  ): Try[Unit] = {
    val allIds     = rankables.map(_.sortId)
    val hasEveryId = allIds.forall(sortRequest.sortedIds.contains)
    if (!hasEveryId || allIds.size != sortRequest.sortedIds.size)
      return Failure(ValidationException("ids", s"You need to supply _every_ direct child of the folder when sorting."))

    folderRepository.withTx { session =>
      sortRequest
        .sortedIds
        .mapWithIndex((id, idx) => {
          val newRank = idx + 1
          val found   = rankables.find(_.sortId == id)
          found match {
            case Some(domain.Folder(folderId, _, _, _, _, _, _, _, _, _, _, _, _)) if sharedFolderSort =>
              folderRepository.setSharedFolderRank(folderId, newRank, feideId)(using session)
            case Some(domain.Folder(folderId, _, _, _, _, _, _, _, _, _, _, _, _)) =>
              folderRepository.setFolderRank(folderId, newRank, feideId)(using session)
            case Some(domain.FolderResource(folderId, resourceId, _, _)) =>
              folderRepository.setResourceConnectionRank(folderId, resourceId, newRank)(using session)
            case _ => Failure(FolderSortException("Something went wrong when sorting! This seems like a bug!"))
          }
        })
        .sequence
        .map(_ => ())
    }
  }

  private def sortRootFolders(sortRequest: api.FolderSortRequestDTO, feideId: FeideID): Try[Unit] = {
    val session = folderRepository.getSession(true)
    folderRepository
      .foldersWithFeideAndParentID(None, feideId)(using session)
      .flatMap(rootFolders => performSort(rootFolders, sortRequest, feideId, sharedFolderSort = false))
  }

  private def sortSavedSharedFolders(sortRequest: api.FolderSortRequestDTO, feideId: FeideID): Try[Unit] = {
    val session = folderRepository.getSession(true)
    folderRepository
      .getSavedSharedFolders(feideId)(using session)
      .flatMap(savedFolders => performSort(savedFolders, sortRequest, feideId, sharedFolderSort = true))
  }

  private def sortNonRootFolderResources(folderId: UUID, sortRequest: api.FolderSortRequestDTO, feideId: FeideID)(
      implicit session: DBSession
  ): Try[Unit] = getFolderWithDirectChildren(folderId.some, feideId).flatMap {
    case FolderAndDirectChildren(_, _, resources) =>
      performSort(resources, sortRequest, feideId, sharedFolderSort = false)
  }

  private def sortNonRootFolderSubfolders(folderId: UUID, sortRequest: api.FolderSortRequestDTO, feideId: FeideID)(
      implicit session: DBSession
  ): Try[Unit] = getFolderWithDirectChildren(folderId.some, feideId).flatMap {
    case FolderAndDirectChildren(_, subfolders, _) =>
      performSort(subfolders, sortRequest, feideId, sharedFolderSort = false)
  }

  def sortFolder(
      folderSortObject: domain.FolderSortObject,
      sortRequest: api.FolderSortRequestDTO,
      feideAccessToken: Option[FeideAccessToken],
  ): Try[Unit] = permitTry {
    implicit val session: DBSession = folderRepository.getSession(readOnly = false)
    val feideId                     = feideApiClient.getFeideID(feideAccessToken).?
    canWriteOrAccessDenied(feideId, feideAccessToken).??
    folderSortObject match {
      case ResourceSorting(parentId) => sortNonRootFolderResources(parentId, sortRequest, feideId)
      case FolderSorting(parentId)   => sortNonRootFolderSubfolders(parentId, sortRequest, feideId)
      case RootFolderSorting()       => sortRootFolders(sortRequest, feideId)
      case SharedFolderSorting()     => sortSavedSharedFolders(sortRequest, feideId)
    }
  }

  private def checkDepth(parentId: Option[UUID]): Try[Unit] = {
    parentId match {
      case None      => Success(())
      case Some(pid) => folderRepository.getFoldersDepth(pid) match {
          case Failure(ex)                                             => Failure(ex)
          case Success(currentDepth) if currentDepth >= MaxFolderDepth =>
            Failure(
              ValidationException(
                "MAX_DEPTH_LIMIT_REACHED",
                s"Folder can not be created, max folder depth limit of $MaxFolderDepth reached.",
              )
            )
          case _ => Success(())
        }
    }
  }

  private def getFolderWithDirectChildren(maybeParentId: Option[UUID], feideId: FeideID)(implicit
      session: DBSession
  ): Try[FolderAndDirectChildren] = maybeParentId match {
    case None => folderRepository
        .foldersWithFeideAndParentID(None, feideId)
        .map(siblingFolders => {
          domain.FolderAndDirectChildren(None, siblingFolders, Seq.empty)
        })
    case Some(parentId) => folderRepository.folderWithFeideId(parentId, feideId) match {
        case Failure(ex)     => Failure(ex)
        case Success(parent) => for {
            siblingFolders   <- folderRepository.foldersWithFeideAndParentID(parentId.some, feideId)
            siblingResources <- folderRepository.getConnections(parentId)
          } yield domain.FolderAndDirectChildren(Some(parent), siblingFolders, siblingResources)
      }
  }

  private def validateSiblingNames(name: String, maybeParentAndSiblings: domain.FolderAndDirectChildren): Try[Unit] = {
    val domain.FolderAndDirectChildren(_, siblings, _) = maybeParentAndSiblings
    val hasNameDuplicate                               = siblings.map(_.name).exists(_.toLowerCase == name.toLowerCase)
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
      updatedFolder: domain.Folder,
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
      maybeParentAndSiblings: domain.FolderAndDirectChildren,
  ): Try[Option[UUID]] = for {
    validatedParentId <- validateParentId(parentId, maybeParentAndSiblings.folder)
    _                 <- validateSiblingNames(folderName, maybeParentAndSiblings)
    _                 <- checkDepth(validatedParentId)
  } yield validatedParentId

  private def getNextRank(siblings: Seq[?]): Int = siblings.length + 1

  private[service] def changeStatusToSharedIfParentIsShared(
      newFolder: NewFolderDTO,
      parentFolder: Option[domain.Folder],
      isCloning: Boolean,
  ): NewFolderDTO = {
    import FolderStatus.SHARED

    parentFolder match {
      case Some(parent) if parent.status == SHARED && !isCloning => newFolder.copy(status = SHARED.toString.some)
      case _                                                     => newFolder
    }
  }

  private def createNewFolder(
      newFolder: NewFolderDTO,
      feideId: FeideID,
      makeUniqueNamePostfix: Option[String],
      isCloning: Boolean,
  )(implicit session: DBSession): Try[domain.Folder] = permitTry {
    val parentId       = getMaybeParentId(newFolder.parentId).?
    val maybeSiblings  = getFolderWithDirectChildren(parentId, feideId).?
    val nextRank       = getNextRank(maybeSiblings.childrenFolders)
    val withStatus     = changeStatusToSharedIfParentIsShared(newFolder, maybeSiblings.folder, isCloning)
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
      maybeParentAndSiblings: domain.FolderAndDirectChildren,
  ): String = {
    makeUniqueNamePostfix match {
      case None          => folderName
      case Some(postfix) =>
        @tailrec
        def getCopyUntilValid(folderName: String): String =
          if (validateSiblingNames(folderName, maybeParentAndSiblings).isFailure) {
            getCopyUntilValid(s"$folderName$postfix")
          } else {
            folderName
          }

        getCopyUntilValid(folderName)
    }
  }

  def newFolder(newFolder: NewFolderDTO, feideAccessToken: Option[FeideAccessToken]): Try[FolderDTO] = {
    implicit val session: DBSession = folderRepository.getSession(readOnly = false)
    for {
      feideId   <- feideApiClient.getFeideID(feideAccessToken)
      _         <- canWriteOrAccessDenied(feideId, feideAccessToken)
      inserted  <- createNewFolder(newFolder, feideId, makeUniqueNamePostfix = None, isCloning = false)
      crumbs    <- folderReadService.getBreadcrumbs(inserted)(using ReadOnlyAutoSession)
      feideUser <- userRepository.userWithFeideId(feideId)
      api       <- folderConverterService.toApiFolder(inserted, crumbs, feideUser, isOwner = true)
    } yield api
  }

  private def createOrUpdateFolderResourceConnection(folderId: UUID, newResource: NewResourceDTO, feideId: FeideID)(
      implicit session: DBSession
  ): Try[domain.Resource] = for {
    _ <- folderRepository
      .folderWithFeideId(folderId, feideId)
      .orElse(Failure(NotFoundException(s"Can't connect resource to non-existing folder")))
    siblings          <- getFolderWithDirectChildren(folderId.some, feideId)
    insertedOrUpdated <- createNewResourceOrUpdateExisting(newResource, folderId, siblings, feideId)
    _                  = updateSearchApi(insertedOrUpdated)
  } yield insertedOrUpdated

  private def updateSearchApi(resource: domain.Resource): Unit = {
    resource.resourceType match {
      case ResourceType.Multidisciplinary                               => searchApiClient.reindexDraft(resource.resourceId)
      case ResourceType.Article                                         => searchApiClient.reindexDraft(resource.resourceId)
      case ResourceType.Topic                                           => searchApiClient.reindexDraft(resource.resourceId)
      case ResourceType.Learningpath                                    => searchApiClient.reindexLearningpath(resource.resourceId)
      case ResourceType.Concept                                         => searchApiClient.reindexConcept(resource.resourceId)
      case ResourceType.Audio | ResourceType.Image | ResourceType.Video =>
    }
  }

  def newFolderResourceConnection(
      folderId: UUID,
      newResource: NewResourceDTO,
      feideAccessToken: Option[FeideAccessToken],
  ): Try[ResourceDTO] = {
    implicit val session: DBSession = folderRepository.getSession(readOnly = false)
    for {
      feideId   <- feideApiClient.getFeideID(feideAccessToken)
      _         <- canWriteOrAccessDenied(feideId, feideAccessToken)
      resource  <- createOrUpdateFolderResourceConnection(folderId, newResource, feideId)
      converted <- folderConverterService.toApiResource(resource, isOwner = true)
    } yield converted
  }

  private[service] def createNewResourceOrUpdateExisting(
      newResource: NewResourceDTO,
      folderId: UUID,
      siblings: domain.FolderAndDirectChildren,
      feideId: FeideID,
  )(implicit session: DBSession): Try[domain.Resource] = {
    val rank = getNextRank(siblings.childrenResources)
    val date = clock.now()
    folderRepository
      .resourceWithPathAndTypeAndFeideId(newResource.path, newResource.resourceType, feideId)
      .flatMap {
        case None =>
          val document = folderConverterService.toDomainResource(newResource)
          for {
            inserted <-
              folderRepository.insertResource(feideId, newResource.path, newResource.resourceType, date, document)
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
            s"Invalid value for $paramName. The UUID specified does not exist or is not writable by you.",
          )
        )
      case _ => Success(parentId)
    }

  def canWriteOrAccessDenied(feideId: FeideID, feideAccessToken: Option[FeideAccessToken])(
      implicit session: DBSession = ReadOnlyAutoSession
  ): Try[?] = {
    userService
      .getMyNDLAUser(feideId, feideAccessToken)(using session)
      .flatMap(myNDLAUser =>
        canWriteNow(myNDLAUser).flatMap {
          case true  => Success(())
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
      newRank       = savedFolders.length + 1
      folderUser   <- folderRepository.createFolderUserConnection(folder.id, feideId, newRank)
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

  private def isTeacherOrAccessDenied(feideId: FeideID, feideAccessToken: Option[FeideAccessToken])(implicit
      session: DBSession
  ): Try[?] = {
    userService
      .getMyNDLAUser(feideId, feideAccessToken)(using session)
      .flatMap(myNDLAUser => {
        if (myNDLAUser.isTeacher) Success(())
        else Failure(AccessDeniedException("You do not have necessary permissions to share folders."))
      })
  }
}
