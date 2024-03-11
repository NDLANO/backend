/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla.service

import no.ndla.common.errors.{AccessDeniedException, ValidationException}
import no.ndla.common.model.NDLADate
import no.ndla.myndla.TestData.{emptyDomainFolder, emptyDomainResource, emptyMyNDLAUser}
import no.ndla.myndla.{TestData, TestEnvironment}
import no.ndla.myndla.model.api.{FolderSortRequest, NewFolder, NewResource}
import no.ndla.myndla.model.api
import no.ndla.myndla.model.domain.FolderSortObject.FolderSorting
import no.ndla.myndla.model.domain.{
  Folder,
  FolderAndDirectChildren,
  FolderResource,
  FolderStatus,
  Resource,
  ResourceType,
  UserRole
}
import no.ndla.scalatestsuite.UnitTestSuite
import org.mockito.invocation.InvocationOnMock
import scalikejdbc.DBSession

import java.util.UUID
import scala.util.{Failure, Success, Try}

class FolderWriteServiceTest extends UnitTestSuite with TestEnvironment {

  val MaxFolderDepth = 5L

  val service = new FolderWriteService

  override def beforeEach(): Unit = {
    super.beforeEach()
    resetMocks()
    when(folderRepository.getSession(any)).thenReturn(mock[DBSession])
    when(userRepository.rollbackOnFailure(any)).thenAnswer((i: InvocationOnMock) => {
      val func = i.getArgument[DBSession => Try[Nothing]](0)
      func(mock[DBSession])
    })
  }

  test("that a user without access cannot delete a folder") {
    val id = UUID.randomUUID()
    val folderWithChildren =
      emptyDomainFolder.copy(
        id = id,
        feideId = "FEIDE",
        subfolders = List(emptyDomainFolder, emptyDomainFolder),
        resources = List(emptyDomainResource)
      )
    val wrongFeideId = "nope"

    when(folderRepository.foldersWithFeideAndParentID(any, any)(any)).thenReturn(Success(List.empty))
    when(feideApiClient.getFeideID(any)).thenReturn(Success(wrongFeideId))
    when(userService.getOrCreateMyNDLAUserIfNotExist(any, any, any)(any)).thenReturn(Success(emptyMyNDLAUser))
    when(folderRepository.folderResourceConnectionCount(any)(any)).thenReturn(Success(0))
    when(folderRepository.folderWithId(eqTo(id))(any)).thenReturn(Success(folderWithChildren))

    val x = service.deleteFolder(id, Some("token"))
    x.isFailure should be(true)
    x should be(Failure(AccessDeniedException("You do not have access to this entity.")))

    verify(folderRepository, times(0)).deleteFolder(any)(any[DBSession])
    verify(folderRepository, times(0)).folderResourceConnectionCount(any)(any[DBSession])
    verify(folderRepository, times(0)).deleteResource(any)(any[DBSession])
  }

  test("that a user with access can delete a folder") {
    val mainFolderId = UUID.randomUUID()
    val subFolder1Id = UUID.randomUUID()
    val subFolder2Id = UUID.randomUUID()
    val resourceId   = UUID.randomUUID()
    val folder =
      emptyDomainFolder.copy(id = mainFolderId, feideId = "FEIDE", resources = List.empty, subfolders = List.empty)
    val folderWithChildren =
      folder.copy(
        subfolders = List(
          emptyDomainFolder.copy(id = subFolder1Id),
          emptyDomainFolder.copy(id = subFolder2Id)
        ),
        resources = List(
          emptyDomainResource.copy(id = resourceId)
        )
      )
    val correctFeideId = "FEIDE"

    when(folderRepository.withTx(any[DBSession => Try[Unit]])).thenAnswer((i: InvocationOnMock) => {
      val func = i.getArgument[DBSession => Try[Unit]](0)
      func(mock[DBSession])
    })
    when(folderRepository.foldersWithFeideAndParentID(any, any)(any)).thenReturn(Success(List.empty))
    when(feideApiClient.getFeideID(any)).thenReturn(Success(correctFeideId))
    when(userService.getOrCreateMyNDLAUserIfNotExist(any, any, any)(any)).thenReturn(Success(emptyMyNDLAUser))
    when(folderRepository.folderResourceConnectionCount(any)(any[DBSession])).thenReturn(Success(1))
    when(folderRepository.folderWithId(eqTo(mainFolderId))(any)).thenReturn(Success(folder))
    when(folderReadService.getSingleFolderWithContent(eqTo(folder.id), any, eqTo(true))(any))
      .thenReturn(Success(folderWithChildren))
    when(folderRepository.deleteFolder(any)(any))
      .thenReturn(Success(mainFolderId), Success(subFolder1Id), Success(subFolder2Id))
    when(folderRepository.deleteResource(any)(any[DBSession])).thenReturn(Success(resourceId))

    service.deleteFolder(mainFolderId, Some("token")).get should be(mainFolderId)

    verify(folderRepository, times(1)).deleteFolder(eqTo(mainFolderId))(any)
    verify(folderRepository, times(1)).deleteFolder(eqTo(subFolder1Id))(any)
    verify(folderRepository, times(1)).deleteFolder(eqTo(subFolder2Id))(any)
    verify(folderRepository, times(1)).folderResourceConnectionCount(eqTo(resourceId))(any)
    verify(folderRepository, times(1)).deleteResource(eqTo(resourceId))(any)
    verify(folderReadService, times(1)).getSingleFolderWithContent(eqTo(folder.id), any, eqTo(true))(any)
  }

  test("that resource is not deleted if folderResourceConnectionCount() returns 0") {
    val mainFolderId = UUID.randomUUID()
    val subFolder1Id = UUID.randomUUID()
    val subFolder2Id = UUID.randomUUID()
    val resourceId   = UUID.randomUUID()
    val folder =
      emptyDomainFolder.copy(id = mainFolderId, feideId = "FEIDE", resources = List.empty, subfolders = List.empty)
    val folderWithChildren =
      folder.copy(
        subfolders = List(
          emptyDomainFolder.copy(id = subFolder1Id),
          emptyDomainFolder.copy(id = subFolder2Id)
        ),
        resources = List(
          emptyDomainResource.copy(id = resourceId)
        )
      )
    val correctFeideId = "FEIDE"

    when(folderRepository.withTx(any[DBSession => Try[Unit]])).thenAnswer((i: InvocationOnMock) => {
      val func = i.getArgument[DBSession => Try[Unit]](0)
      func(mock[DBSession])
    })
    when(folderRepository.foldersWithFeideAndParentID(any, any)(any)).thenReturn(Success(List.empty))
    when(feideApiClient.getFeideID(any)).thenReturn(Success(correctFeideId))
    when(userService.getOrCreateMyNDLAUserIfNotExist(any, any, any)(any)).thenReturn(Success(emptyMyNDLAUser))
    when(folderRepository.folderResourceConnectionCount(eqTo(resourceId))(any)).thenReturn(Success(5))
    when(folderRepository.folderWithId(eqTo(mainFolderId))(any)).thenReturn(Success(folder))
    when(folderReadService.getSingleFolderWithContent(eqTo(folder.id), any, eqTo(true))(any))
      .thenReturn(Success(folderWithChildren))
    when(folderRepository.deleteFolderResourceConnection(eqTo(mainFolderId), eqTo(resourceId))(any))
      .thenReturn(Success(resourceId))
    when(folderRepository.deleteFolder(any)(any)).thenReturn(Success(any))

    service.deleteFolder(mainFolderId, Some("token")) should be(Success(mainFolderId))

    verify(folderRepository, times(1)).deleteFolder(eqTo(mainFolderId))(any)
    verify(folderRepository, times(1)).deleteFolder(eqTo(subFolder1Id))(any)
    verify(folderRepository, times(1)).deleteFolder(eqTo(subFolder2Id))(any)
    verify(folderRepository, times(1)).folderResourceConnectionCount(eqTo(resourceId))(any)
    verify(folderRepository, times(0)).deleteResource(any)(any)
    verify(folderRepository, times(1)).deleteFolderResourceConnection(any, any)(any)
    verify(folderReadService, times(1)).getSingleFolderWithContent(eqTo(folder.id), any, eqTo(true))(any)
  }

  test("that deleteConnection only deletes connection when there are several references to a resource") {
    val folderId       = UUID.randomUUID()
    val resourceId     = UUID.randomUUID()
    val correctFeideId = "FEIDE"
    val folder         = emptyDomainFolder.copy(id = folderId, feideId = "FEIDE")
    val resource       = emptyDomainResource.copy(id = resourceId, feideId = "FEIDE")
    val folderResource = FolderResource(folderId = folder.id, resourceId = resource.id, rank = 1)

    when(feideApiClient.getFeideID(any)).thenReturn(Success(correctFeideId))
    when(userService.getOrCreateMyNDLAUserIfNotExist(any, any, any)(any)).thenReturn(Success(emptyMyNDLAUser))
    when(folderRepository.folderWithId(eqTo(folderId))(any)).thenReturn(Success(folder))
    when(folderRepository.resourceWithId(eqTo(resourceId))(any)).thenReturn(Success(resource))
    when(folderRepository.folderResourceConnectionCount(eqTo(resourceId))(any)).thenReturn(Success(2))
    when(folderRepository.withTx(any[DBSession => Try[Unit]])).thenAnswer((i: InvocationOnMock) => {
      val func = i.getArgument[DBSession => Try[Unit]](0)
      func(mock[DBSession])
    })
    when(folderRepository.foldersWithFeideAndParentID(any, any)(any)).thenReturn(Success(List.empty))
    when(folderRepository.folderWithFeideId(eqTo(folderId), any)(any)).thenReturn(Success(folder))
    when(folderRepository.foldersWithFeideAndParentID(eqTo(Some(folderId)), any)(any)).thenReturn(Success(List.empty))
    when(folderRepository.getConnections(eqTo(folderId))(any)).thenReturn(Success(List(folderResource)))
    when(folderRepository.deleteFolderResourceConnection(eqTo(folderId), eqTo(resourceId))(any))
      .thenReturn(Success(resourceId))

    service.deleteConnection(folderId, resourceId, None).failIfFailure

    verify(folderRepository, times(1)).folderResourceConnectionCount(eqTo(resourceId))(any)
    verify(folderRepository, times(1)).folderWithId(eqTo(folderId))(any)
    verify(folderRepository, times(1)).resourceWithId(eqTo(resourceId))(any)
    verify(folderRepository, times(1)).deleteFolderResourceConnection(eqTo(folderId), eqTo(resourceId))(any)
    verify(folderRepository, times(0)).deleteResource(any)(any)
  }

  test("that deleteConnection deletes the resource if there is only 1 references to a resource") {
    val folderId       = UUID.randomUUID()
    val resourceId     = UUID.randomUUID()
    val correctFeideId = "FEIDE"
    val folder         = emptyDomainFolder.copy(id = folderId, feideId = "FEIDE")
    val resource       = emptyDomainResource.copy(id = resourceId, feideId = "FEIDE")
    val folderResource = FolderResource(folderId = folder.id, resourceId = resource.id, rank = 1)

    when(feideApiClient.getFeideID(any)).thenReturn(Success(correctFeideId))
    when(userService.getOrCreateMyNDLAUserIfNotExist(any, any, any)(any)).thenReturn(Success(emptyMyNDLAUser))
    when(folderRepository.folderWithId(eqTo(folderId))(any)).thenReturn(Success(folder))
    when(folderRepository.resourceWithId(eqTo(resourceId))(any)).thenReturn(Success(resource))
    when(folderRepository.folderResourceConnectionCount(eqTo(resourceId))(any)).thenReturn(Success(1))
    when(folderRepository.deleteFolderResourceConnection(eqTo(folderId), eqTo(resourceId))(any))
      .thenReturn(Success(resourceId))
    when(folderRepository.deleteResource(eqTo(resourceId))(any)).thenReturn(Success(resourceId))
    when(folderRepository.withTx(any[DBSession => Try[Unit]])).thenAnswer((i: InvocationOnMock) => {
      val func = i.getArgument[DBSession => Try[Unit]](0)
      func(mock[DBSession])
    })
    when(folderRepository.folderWithFeideId(eqTo(folderId), any)(any)).thenReturn(Success(folder))
    when(folderRepository.foldersWithFeideAndParentID(eqTo(Some(folderId)), any)(any)).thenReturn(Success(List.empty))
    when(folderRepository.getConnections(eqTo(folderId))(any)).thenReturn(Success(List(folderResource)))
    when(folderRepository.foldersWithFeideAndParentID(any, any)(any)).thenReturn(Success(List.empty))

    service.deleteConnection(folderId, resourceId, None).failIfFailure should be(resourceId)

    verify(folderRepository, times(1)).folderResourceConnectionCount(eqTo(resourceId))(any)
    verify(folderRepository, times(1)).folderWithId(eqTo(folderId))(any)
    verify(folderRepository, times(1)).resourceWithId(eqTo(resourceId))(any)
    verify(folderRepository, times(0)).deleteFolderResourceConnection(eqTo(folderId), eqTo(resourceId))(any)
    verify(folderRepository, times(1)).deleteResource(eqTo(resourceId))(any)
  }

  test("that deleteConnection exits early if user is not the folder owner") {
    val folderId       = UUID.randomUUID()
    val resourceId     = UUID.randomUUID()
    val correctFeideId = "FEIDE"
    val folder         = emptyDomainFolder.copy(id = folderId, feideId = "asd")

    when(feideApiClient.getFeideID(any)).thenReturn(Success(correctFeideId))
    when(userService.getOrCreateMyNDLAUserIfNotExist(any, any, any)(any)).thenReturn(Success(emptyMyNDLAUser))
    when(folderRepository.folderWithId(eqTo(folderId))(any)).thenReturn(Success(folder))

    val res = service.deleteConnection(folderId, resourceId, None)
    res.isFailure should be(true)
    res should be(Failure(AccessDeniedException("You do not have access to this entity.")))

    verify(folderRepository, times(1)).folderWithId(eqTo(folderId))(any)
    verify(folderRepository, times(0)).resourceWithId(eqTo(resourceId))(any)
    verify(folderRepository, times(0)).folderResourceConnectionCount(eqTo(resourceId))(any)
    verify(folderRepository, times(0)).deleteFolderResourceConnection(eqTo(folderId), eqTo(resourceId))(any)
    verify(folderRepository, times(0)).deleteResource(eqTo(resourceId))(any)
  }

  test("that deleteConnection exits early if user is not the resource owner") {
    val folderId       = UUID.randomUUID()
    val resourceId     = UUID.randomUUID()
    val correctFeideId = "FEIDE"
    val folder         = emptyDomainFolder.copy(id = folderId, feideId = "FEIDE")
    val resource       = emptyDomainResource.copy(id = resourceId, feideId = "asd")

    when(feideApiClient.getFeideID(any)).thenReturn(Success(correctFeideId))
    when(userService.getOrCreateMyNDLAUserIfNotExist(any, any, any)(any)).thenReturn(Success(emptyMyNDLAUser))
    when(folderRepository.folderWithId(eqTo(folderId))(any)).thenReturn(Success(folder))
    when(folderRepository.resourceWithId(eqTo(resourceId))(any)).thenReturn(Success(resource))

    val res = service.deleteConnection(folderId, resourceId, None)
    res.isFailure should be(true)
    res should be(Failure(AccessDeniedException("You do not have access to this entity.")))

    verify(folderRepository, times(1)).folderWithId(eqTo(folderId))(any)
    verify(folderRepository, times(1)).resourceWithId(eqTo(resourceId))(any)
    verify(folderRepository, times(0)).folderResourceConnectionCount(eqTo(resourceId))(any[DBSession])
    verify(folderRepository, times(0)).deleteFolderResourceConnection(eqTo(folderId), eqTo(resourceId))(any[DBSession])
    verify(folderRepository, times(0)).deleteResource(eqTo(resourceId))(any[DBSession])
  }

  test("that createNewResourceOrUpdateExisting creates a resource if it does not already exist") {
    val created = NDLADate.now()
    when(clock.now()).thenReturn(created)

    val feideId      = "FEIDE"
    val folderId     = UUID.randomUUID()
    val resourceId   = UUID.randomUUID()
    val resourcePath = "/subject/1/topic/2/resource/3"
    val newResource =
      NewResource(resourceType = ResourceType.Article, path = resourcePath, tags = None, resourceId = "1")
    val resource =
      Resource(
        id = resourceId,
        feideId = feideId,
        path = resourcePath,
        resourceType = ResourceType.Article,
        created = created,
        tags = List.empty,
        resourceId = "1",
        connection = None
      )

    when(feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    when(folderRepository.resourceWithPathAndTypeAndFeideId(any, any, any)(any)).thenReturn(Success(None))
    when(folderRepository.insertResource(any, any, any, any, any)(any)).thenReturn(Success(resource))
    when(folderRepository.createFolderResourceConnection(any, any, any)(any)).thenAnswer((i: InvocationOnMock) => {
      Success(FolderResource(folderId = i.getArgument(0), resourceId = i.getArgument(1), rank = i.getArgument(2)))
    })

    service
      .createNewResourceOrUpdateExisting(
        newResource,
        folderId,
        FolderAndDirectChildren(None, Seq.empty, Seq.empty),
        feideId
      )(mock[DBSession])
      .isSuccess should be(true)

    verify(folderRepository, times(1)).resourceWithPathAndTypeAndFeideId(
      eqTo(resourcePath),
      eqTo(ResourceType.Article),
      eqTo(feideId)
    )(
      any
    )
    verify(folderConverterService, times(1)).toDomainResource(eqTo(newResource))
    verify(folderRepository, times(1)).insertResource(
      eqTo(feideId),
      eqTo(resourcePath),
      eqTo(ResourceType.Article),
      any,
      any
    )(any)
    verify(folderRepository, times(1)).createFolderResourceConnection(eqTo(folderId), eqTo(resourceId), any)(any)
    verify(folderConverterService, times(0)).mergeResource(any, any[NewResource])
    verify(folderRepository, times(0)).updateResource(any)(any)
  }

  test(
    "that createNewResourceOrUpdateExisting updates a resource and creates new connection if the resource already exist"
  ) {
    val created = NDLADate.now()
    when(clock.now()).thenReturn(created)

    val feideId      = "FEIDE"
    val folderId     = UUID.randomUUID()
    val resourceId   = UUID.randomUUID()
    val resourcePath = "/subject/1/topic/2/resource/3"
    val newResource =
      NewResource(resourceType = ResourceType.Article, path = resourcePath, tags = None, resourceId = "1")
    val resource =
      Resource(
        id = resourceId,
        feideId = feideId,
        path = resourcePath,
        resourceType = ResourceType.Article,
        created = created,
        tags = List.empty,
        resourceId = "1",
        connection = None
      )

    when(folderRepository.getConnection(any, any)(any)).thenReturn(Success(None))
    when(feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    when(folderRepository.resourceWithPathAndTypeAndFeideId(any, any, any)(any)).thenReturn(Success(Some(resource)))
    when(folderRepository.updateResource(eqTo(resource))(any)).thenReturn(Success(resource))
    when(folderRepository.createFolderResourceConnection(any, any, any)(any)).thenAnswer((i: InvocationOnMock) => {
      Success(FolderResource(folderId = i.getArgument(0), resourceId = i.getArgument(1), rank = i.getArgument(2)))
    })

    service
      .createNewResourceOrUpdateExisting(
        newResource,
        folderId,
        FolderAndDirectChildren(None, Seq.empty, Seq.empty),
        feideId
      )(mock[DBSession])
      .get

    verify(folderRepository, times(1)).resourceWithPathAndTypeAndFeideId(
      eqTo(resourcePath),
      eqTo(ResourceType.Article),
      eqTo(feideId)
    )(
      any
    )
    verify(folderConverterService, times(0)).toDomainResource(eqTo(newResource))
    verify(folderRepository, times(0)).insertResource(any, any, any, any, any)(any)
    verify(folderConverterService, times(1)).mergeResource(eqTo(resource), eqTo(newResource))
    verify(folderRepository, times(1)).updateResource(eqTo(resource))(any)
    verify(folderRepository, times(1)).createFolderResourceConnection(eqTo(folderId), eqTo(resourceId), any)(any)
  }

  test("that deleteFolder deletes correct number of folder-resource-connections and resources") {
    val folder1Id   = UUID.randomUUID()
    val folder2Id   = UUID.randomUUID()
    val folder3Id   = UUID.randomUUID()
    val resource1Id = UUID.randomUUID()
    val resource2Id = UUID.randomUUID()
    val resource3Id = UUID.randomUUID()
    val resource1   = emptyDomainResource.copy(id = resource1Id, feideId = "FEIDEF")
    val resource2   = emptyDomainResource.copy(id = resource2Id, feideId = "FEIDEF")
    val resource3   = emptyDomainResource.copy(id = resource3Id, feideId = "FEIDEF")
    val folder3 = emptyDomainFolder.copy(
      id = folder3Id,
      feideId = "FEIDEF",
      resources = List(resource2, resource3),
      subfolders = List.empty
    )
    val folder2 = emptyDomainFolder.copy(
      id = folder2Id,
      feideId = "FEIDEF",
      resources = List(resource1, resource2),
      subfolders = List(folder3)
    )
    val folder1 =
      emptyDomainFolder.copy(
        id = folder1Id,
        feideId = "FEIDEF",
        resources = List(resource1),
        subfolders = List(folder2)
      )

    when(feideApiClient.getFeideID(any)).thenReturn(Success("FEIDEF"))
    when(userService.getOrCreateMyNDLAUserIfNotExist(any, any, any)(any)).thenReturn(Success(emptyMyNDLAUser))
    when(folderRepository.folderWithId(eqTo(folder1Id))(any[DBSession])).thenReturn(Success(folder1))
    when(folderReadService.getSingleFolderWithContent(eqTo(folder1Id), eqTo(true), eqTo(true))(any[DBSession]))
      .thenReturn(Success(folder1))
    when(folderRepository.folderResourceConnectionCount(eqTo(resource1Id))(any[DBSession]))
      .thenReturn(Success(2), Success(1))
    when(folderRepository.deleteFolderResourceConnection(eqTo(folder1Id), eqTo(resource1Id))(any[DBSession]))
      .thenReturn(Success(resource1Id))
    when(folderRepository.deleteResource(eqTo(resource1Id))(any[DBSession])).thenReturn(Success(resource1Id))
    when(folderRepository.folderResourceConnectionCount(eqTo(resource2Id))(any[DBSession]))
      .thenReturn(Success(2), Success(1))
    when(folderRepository.deleteFolderResourceConnection(eqTo(folder2Id), eqTo(resource2Id))(any[DBSession]))
      .thenReturn(Success(resource2Id))
    when(folderRepository.deleteResource(eqTo(resource2Id))(any[DBSession])).thenReturn(Success(resource2Id))
    when(folderRepository.folderResourceConnectionCount(eqTo(resource3Id))(any[DBSession])).thenReturn(Success(1))
    when(folderRepository.deleteResource(eqTo(resource3Id))(any[DBSession])).thenReturn(Success(resource3Id))
    when(folderRepository.deleteFolder(eqTo(folder3Id))(any[DBSession])).thenReturn(Success(folder3Id))
    when(folderRepository.deleteFolder(eqTo(folder2Id))(any[DBSession])).thenReturn(Success(folder2Id))
    when(folderRepository.deleteFolder(eqTo(folder1Id))(any[DBSession])).thenReturn(Success(folder1Id))
    when(folderRepository.withTx(any[DBSession => Try[Unit]])).thenAnswer((i: InvocationOnMock) => {
      val func = i.getArgument[DBSession => Try[Unit]](0)
      func(mock[DBSession])
    })
    when(folderRepository.foldersWithFeideAndParentID(any, any)(any)).thenReturn(Success(List.empty))

    service.deleteFolder(folder1Id, Some("FEIDEF")) should be(Success(folder1Id))

    verify(folderReadService, times(1)).getSingleFolderWithContent(eqTo(folder1Id), eqTo(true), eqTo(true))(any)
    verify(folderRepository, times(5)).folderResourceConnectionCount(any)(any)
    verify(folderRepository, times(2)).folderResourceConnectionCount(eqTo(resource1Id))(any)
    verify(folderRepository, times(2)).folderResourceConnectionCount(eqTo(resource2Id))(any)
    verify(folderRepository, times(1)).folderResourceConnectionCount(eqTo(resource3Id))(any)

    verify(folderRepository, times(2)).deleteFolderResourceConnection(any, any)(any)
    verify(folderRepository, times(1)).deleteFolderResourceConnection(eqTo(folder1Id), eqTo(resource1Id))(any)
    verify(folderRepository, times(1)).deleteFolderResourceConnection(eqTo(folder2Id), eqTo(resource2Id))(any)

    verify(folderRepository, times(3)).deleteResource(any)(any)
    verify(folderRepository, times(1)).deleteResource(eqTo(resource1Id))(any)
    verify(folderRepository, times(1)).deleteResource(eqTo(resource2Id))(any)
    verify(folderRepository, times(1)).deleteResource(eqTo(resource3Id))(any)

    verify(folderRepository, times(3)).deleteFolder(any)(any)
    verify(folderRepository, times(1)).deleteFolder(eqTo(folder1Id))(any)
    verify(folderRepository, times(1)).deleteFolder(eqTo(folder2Id))(any)
    verify(folderRepository, times(1)).deleteFolder(eqTo(folder3Id))(any)
  }

  test("that folder is not created if depth limit is reached") {
    val feideId   = "FEIDE"
    val parentId  = UUID.randomUUID()
    val newFolder = NewFolder(name = "asd", parentId = Some(parentId.toString), status = None, description = None)

    when(feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    when(userService.getOrCreateMyNDLAUserIfNotExist(any, any, any)(any)).thenReturn(Success(emptyMyNDLAUser))
    when(folderConverterService.toUUIDValidated(eqTo(Some(parentId.toString)), eqTo("parentId")))
      .thenReturn(Success(parentId))
    when(folderRepository.folderWithFeideId(eqTo(parentId), eqTo(feideId))(any[DBSession]))
      .thenReturn(Success(emptyDomainFolder))
    when(folderRepository.foldersWithFeideAndParentID(eqTo(Some(parentId)), eqTo(feideId))(any))
      .thenReturn(Success(List.empty))
    when(folderRepository.getFoldersDepth(eqTo(parentId))(any[DBSession])).thenReturn(Success(MaxFolderDepth))
    when(folderRepository.getConnections(any)(any)).thenReturn(Success(List.empty))

    val Failure(result: ValidationException) = service.newFolder(newFolder, Some(feideId))
    result.errors.head.message should be(
      s"Folder can not be created, max folder depth limit of $MaxFolderDepth reached."
    )

    verify(folderRepository, times(0)).insertFolder(any, any)(any)
  }

  test("that folder is created if depth count is below the limit") {
    val created   = clock.now()
    val feideId   = "FEIDE"
    val folderId  = UUID.randomUUID()
    val parentId  = UUID.randomUUID()
    val newFolder = NewFolder(name = "asd", parentId = Some(parentId.toString), status = None, description = None)
    val domainFolder = Folder(
      id = folderId,
      feideId = feideId,
      parentId = Some(parentId),
      name = "asd",
      status = FolderStatus.PRIVATE,
      subfolders = List.empty,
      resources = List.empty,
      rank = None,
      created = created,
      updated = created,
      shared = None,
      description = None
    )
    val apiFolder = api.Folder(
      id = folderId.toString,
      name = "asd",
      status = "private",
      parentId = Some(parentId.toString),
      breadcrumbs = List.empty,
      subfolders = List.empty,
      resources = List.empty,
      rank = None,
      created = created,
      updated = created,
      shared = None,
      description = None,
      owner = None
    )
    val belowLimit: Long = MaxFolderDepth - 2

    when(feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    when(userService.getOrCreateMyNDLAUserIfNotExist(any, any, any)(any)).thenReturn(Success(emptyMyNDLAUser))
    when(folderConverterService.toUUIDValidated(eqTo(Some(parentId.toString)), eqTo("parentId")))
      .thenReturn(Success(parentId))
    when(folderRepository.folderWithFeideId(eqTo(parentId), eqTo(feideId))(any[DBSession]))
      .thenReturn(Success(emptyDomainFolder))
    when(folderRepository.getFoldersDepth(eqTo(parentId))(any[DBSession])).thenReturn(Success(belowLimit))
    when(folderRepository.insertFolder(any, any)(any[DBSession])).thenReturn(Success(domainFolder))
    when(folderRepository.getConnections(any)(any)).thenReturn(Success(List.empty))
    when(folderRepository.getConnections(any)(any)).thenReturn(Success(List.empty))
    when(folderReadService.getBreadcrumbs(any)(any)).thenReturn(Success(List.empty))
    when(folderRepository.foldersWithFeideAndParentID(eqTo(Some(parentId)), eqTo(feideId))(any))
      .thenReturn(Success(List.empty))
    when(userRepository.userWithFeideId(any)(any[DBSession])).thenReturn(Success(None))

    service.newFolder(newFolder, Some(feideId)) should be(Success(apiFolder))

    verify(folderRepository, times(1)).insertFolder(any, any)(any)
  }

  test("that folder is not created if name already exists as a sibling") {
    val created   = clock.now()
    val feideId   = "FEIDE"
    val folderId  = UUID.randomUUID()
    val parentId  = UUID.randomUUID()
    val newFolder = NewFolder(name = "asd", parentId = Some(parentId.toString), status = None, description = None)
    val domainFolder = Folder(
      id = folderId,
      feideId = feideId,
      parentId = Some(parentId),
      name = "asd",
      status = FolderStatus.PRIVATE,
      subfolders = List.empty,
      resources = List.empty,
      rank = None,
      created = created,
      updated = created,
      shared = None,
      description = None
    )
    val siblingFolder = Folder(
      id = UUID.randomUUID(),
      feideId = feideId,
      parentId = Some(parentId),
      name = "aSd",
      status = FolderStatus.PRIVATE,
      subfolders = List.empty,
      resources = List.empty,
      rank = None,
      created = created,
      updated = created,
      shared = None,
      description = None
    )
    val belowLimit = MaxFolderDepth - 2

    when(feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    when(userService.getOrCreateMyNDLAUserIfNotExist(any, any, any)(any)).thenReturn(Success(emptyMyNDLAUser))
    when(folderConverterService.toUUIDValidated(eqTo(Some(parentId.toString)), eqTo("parentId")))
      .thenReturn(Success(parentId))
    when(folderRepository.folderWithFeideId(eqTo(parentId), eqTo(feideId))(any[DBSession]))
      .thenReturn(Success(emptyDomainFolder))
    when(folderRepository.getFoldersDepth(eqTo(parentId))(any[DBSession])).thenReturn(Success(belowLimit))
    when(folderRepository.insertFolder(any, any)(any[DBSession])).thenReturn(Success(domainFolder))
    when(folderRepository.getConnections(any)(any)).thenReturn(Success(List.empty))
    when(folderReadService.getBreadcrumbs(any)(any)).thenReturn(Success(List.empty))
    when(folderRepository.foldersWithFeideAndParentID(eqTo(Some(parentId)), eqTo(feideId))(any))
      .thenReturn(Success(List(siblingFolder)))

    service.newFolder(newFolder, Some(feideId)) should be(
      Failure(
        ValidationException("name", s"The folder name must be unique within its parent.")
      )
    )

    verify(folderRepository, times(0)).insertFolder(any, any)(any)
  }

  test("that folder is not updated if name already exists as a sibling") {
    val created      = clock.now()
    val feideId      = "FEIDE"
    val folderId     = UUID.randomUUID()
    val parentId     = UUID.randomUUID()
    val updateFolder = api.UpdatedFolder(name = Some("asd"), status = None, description = None)

    val existingFolder = Folder(
      id = folderId,
      feideId = feideId,
      parentId = Some(parentId),
      name = "noe unikt",
      status = FolderStatus.PRIVATE,
      subfolders = List.empty,
      resources = List.empty,
      rank = None,
      created = created,
      updated = created,
      shared = None,
      description = None
    )
    val siblingFolder = Folder(
      id = UUID.randomUUID(),
      feideId = feideId,
      parentId = Some(parentId),
      name = "aSd",
      status = FolderStatus.PRIVATE,
      subfolders = List.empty,
      resources = List.empty,
      rank = None,
      created = created,
      updated = created,
      shared = None,
      description = None
    )
    val belowLimit = MaxFolderDepth - 2

    when(feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    when(userService.getOrCreateMyNDLAUserIfNotExist(any, any, any)(any)).thenReturn(Success(emptyMyNDLAUser))
    when(folderConverterService.toUUIDValidated(eqTo(Some(parentId.toString)), eqTo("parentId")))
      .thenReturn(Success(parentId))
    when(folderRepository.folderWithFeideId(eqTo(parentId), eqTo(feideId))(any[DBSession]))
      .thenReturn(Success(emptyDomainFolder))
    when(folderRepository.getFoldersDepth(eqTo(parentId))(any[DBSession])).thenReturn(Success(belowLimit))
    when(folderReadService.getBreadcrumbs(any)(any)).thenReturn(Success(List.empty))
    when(folderRepository.getConnections(any)(any)).thenReturn(Success(List.empty))
    when(folderRepository.foldersWithFeideAndParentID(eqTo(Some(parentId)), eqTo(feideId))(any))
      .thenReturn(Success(List(siblingFolder)))
    when(folderRepository.folderWithId(eqTo(folderId))(any)).thenReturn(Success(existingFolder))

    service.updateFolder(folderId, updateFolder, Some(feideId)) should be(
      Failure(
        ValidationException("name", s"The folder name must be unique within its parent.")
      )
    )

    verify(folderRepository, times(0)).insertFolder(any, any)(any)
    verify(folderRepository, times(0)).updateFolder(any, any, any)(any)
  }

  test("that folder status is updated even when name is not changed") {
    val created      = clock.now()
    val feideId      = "FEIDE"
    val folderId     = UUID.randomUUID()
    val parentId     = UUID.randomUUID()
    val updateFolder = api.UpdatedFolder(name = None, status = Some("shared"), description = None)

    val existingFolder = Folder(
      id = folderId,
      feideId = feideId,
      parentId = Some(parentId),
      name = "noe unikt",
      status = FolderStatus.PRIVATE,
      subfolders = List.empty,
      resources = List.empty,
      rank = None,
      created = created,
      updated = created,
      shared = None,
      description = None
    )
    val mergedFolder = existingFolder.copy(status = FolderStatus.SHARED)
    val siblingFolder = Folder(
      id = UUID.randomUUID(),
      feideId = feideId,
      parentId = Some(parentId),
      name = "aSd",
      status = FolderStatus.PRIVATE,
      subfolders = List.empty,
      resources = List.empty,
      rank = None,
      created = created,
      updated = created,
      shared = None,
      description = None
    )
    val expectedFolder = api.Folder(
      id = folderId.toString,
      name = "noe unikt",
      status = "shared",
      parentId = Some(parentId.toString),
      breadcrumbs = List.empty,
      subfolders = List.empty,
      resources = List.empty,
      rank = None,
      created = created,
      updated = created,
      shared = None,
      description = None,
      owner = None
    )
    val belowLimit = MaxFolderDepth - 2

    when(feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    when(userService.getOrCreateMyNDLAUserIfNotExist(any, any, any)(any)).thenReturn(Success(emptyMyNDLAUser))
    when(folderConverterService.toUUIDValidated(eqTo(Some(parentId.toString)), eqTo("parentId")))
      .thenReturn(Success(parentId))
    when(folderRepository.folderWithFeideId(eqTo(parentId), eqTo(feideId))(any[DBSession]))
      .thenReturn(Success(emptyDomainFolder))
    when(folderRepository.getFoldersDepth(eqTo(parentId))(any[DBSession])).thenReturn(Success(belowLimit))
    when(folderReadService.getBreadcrumbs(any)(any)).thenReturn(Success(List.empty))
    when(folderRepository.getConnections(any)(any)).thenReturn(Success(List.empty))
    when(folderRepository.foldersWithFeideAndParentID(eqTo(Some(parentId)), eqTo(feideId))(any))
      .thenReturn(Success(List(siblingFolder)))
    when(folderRepository.folderWithId(eqTo(folderId))(any)).thenReturn(Success(existingFolder))
    when(folderRepository.updateFolder(any, any, any)(any)).thenReturn(Success(mergedFolder))
    when(userRepository.userWithFeideId(any)(any[DBSession])).thenReturn(Success(None))

    service.updateFolder(folderId, updateFolder, Some(feideId)) should be(Success(expectedFolder))

    verify(folderRepository, times(1)).updateFolder(any, any, any)(any)
  }

  test("That deleteAllUserData works as expected") {
    val feideId = "feide"

    when(feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    when(folderRepository.deleteAllUserFolders(any)(any)).thenReturn(Success(1))
    when(folderRepository.deleteAllUserResources(any)(any)).thenReturn(Success(1))
    when(userRepository.deleteUser(any)(any)).thenReturn(Success(""))

    service.deleteAllUserData(Some(feideId)) should be(Success(()))

    verify(folderRepository, times(1)).deleteAllUserFolders(any)(any)
    verify(folderRepository, times(1)).deleteAllUserResources(any)(any)
    verify(userRepository, times(1)).deleteUser(any)(any)
  }

  test("That sorting endpoint calls ranking correctly :^)") {
    val feideId = "FEIDE"

    val parent = TestData.emptyDomainFolder.copy(
      id = UUID.randomUUID(),
      feideId = feideId
    )
    val child1 = TestData.emptyDomainFolder.copy(
      id = UUID.randomUUID(),
      feideId = feideId
    )
    val child2 = TestData.emptyDomainFolder.copy(
      id = UUID.randomUUID(),
      feideId = feideId
    )
    val child3 = TestData.emptyDomainFolder.copy(
      id = UUID.randomUUID(),
      feideId = feideId
    )

    val sortRequest = FolderSortRequest(
      sortedIds = List(
        child1.id,
        child3.id,
        child2.id
      )
    )

    when(folderRepository.withTx(any[DBSession => Try[Unit]])).thenAnswer((i: InvocationOnMock) => {
      val func = i.getArgument[DBSession => Try[Unit]](0)
      func(mock[DBSession])
    })
    when(feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    when(userService.getOrCreateMyNDLAUserIfNotExist(any, any, any)(any)).thenReturn(Success(emptyMyNDLAUser))
    when(folderRepository.setFolderRank(any, any, any)(any)).thenReturn(Success(()))
    when(folderRepository.setResourceConnectionRank(any, any, any)(any)).thenReturn(Success(()))
    when(folderRepository.folderWithFeideId(eqTo(parent.id), any)(any)).thenReturn(Success(parent))
    when(folderRepository.folderWithFeideId(eqTo(child1.id), any)(any)).thenReturn(Success(child1))
    when(folderRepository.folderWithFeideId(eqTo(child2.id), any)(any)).thenReturn(Success(child2))
    when(folderRepository.folderWithFeideId(eqTo(child3.id), any)(any)).thenReturn(Success(child3))
    when(folderRepository.getConnections(eqTo(parent.id))(any)).thenReturn(Success(List()))
    when(folderRepository.foldersWithFeideAndParentID(eqTo(Some(parent.id)), any)(any)).thenReturn(
      Success(List(child1, child2, child3))
    )

    service.sortFolder(FolderSorting(parent.id), sortRequest, Some("1234")) should be(Success(()))

    verify(folderRepository, times(1)).setFolderRank(eqTo(child1.id), eqTo(1), any)(any)
    verify(folderRepository, times(1)).setFolderRank(eqTo(child3.id), eqTo(2), any)(any)
    verify(folderRepository, times(1)).setFolderRank(eqTo(child2.id), eqTo(3), any)(any)
  }

  test("that changeStatusToSharedIfParentIsShared actually changes the status if parent is shared") {
    val newFolder =
      NewFolder(
        name = "folder",
        parentId = Some("string"),
        status = Some(FolderStatus.PRIVATE.toString),
        description = None
      )
    val parentFolder = Folder(
      id = UUID.randomUUID(),
      feideId = "feide",
      parentId = None,
      name = "parent",
      status = FolderStatus.SHARED,
      rank = None,
      created = clock.now(),
      updated = clock.now(),
      resources = List(),
      subfolders = List(),
      shared = Some(clock.now()),
      description = None
    )
    val expectedFolder =
      NewFolder(
        name = "folder",
        parentId = Some("string"),
        status = Some(FolderStatus.SHARED.toString),
        description = None
      )

    service.changeStatusToSharedIfParentIsShared(newFolder, Some(parentFolder), isCloning = false) should be(
      expectedFolder
    )
  }

  test("that changeStatusToSharedIfParentIsShared does not alter the status if during cloning or parent is None") {
    val newFolder =
      NewFolder(
        name = "folder",
        parentId = Some("string"),
        status = Some(FolderStatus.PRIVATE.toString),
        description = None
      )
    val parentFolder = Folder(
      id = UUID.randomUUID(),
      feideId = "feide",
      parentId = None,
      name = "parent",
      status = FolderStatus.SHARED,
      rank = None,
      created = clock.now(),
      updated = clock.now(),
      resources = List(),
      subfolders = List(),
      shared = Some(clock.now()),
      description = None
    )
    val expectedFolder =
      NewFolder(
        name = "folder",
        parentId = Some("string"),
        status = Some(FolderStatus.PRIVATE.toString),
        description = None
      )

    val result1 = service.changeStatusToSharedIfParentIsShared(newFolder, Some(parentFolder), isCloning = true)
    val result2 = service.changeStatusToSharedIfParentIsShared(
      newFolder,
      Some(parentFolder.copy(status = FolderStatus.PRIVATE)),
      isCloning = false
    )
    result1 should be(expectedFolder)
    result2 should be(expectedFolder)
  }

  test(
    "that canWriteDuringWriteRestrictionsOrAccessDenied returns Success if user is a Teacher during write restriction"
  ) {
    val myNDLAUser = emptyMyNDLAUser.copy(userRole = UserRole.EMPLOYEE)

    when(userService.getOrCreateMyNDLAUserIfNotExist(any, any, any)(any)).thenReturn(Success(myNDLAUser))
    when(configService.isWriteRestricted).thenReturn(Success(true))

    val result = service.canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied("spiller ing", Some("en rolle"))
    result.isSuccess should be(true)
  }

  test(
    "that canWriteDuringWriteRestrictionsOrAccessDenied returns Failure if user is a Student during write restriction"
  ) {
    val myNDLAUser = emptyMyNDLAUser.copy(userRole = UserRole.STUDENT)

    when(userService.getOrCreateMyNDLAUserIfNotExist(any, any, any)(any)).thenReturn(Success(myNDLAUser))
    when(configService.isMyNDLAWriteRestricted).thenReturn(Success(true))

    val result = service.canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied("spiller ing", Some("en rolle"))
    result should be(Failure(AccessDeniedException("You do not have write access while write restriction is active.")))
  }

  test(
    "that canWriteDuringWriteRestrictionsOrAccessDenied returns Success if user is a Student not during write restriction"
  ) {
    val myNDLAUser = emptyMyNDLAUser.copy(userRole = UserRole.STUDENT)

    when(userService.getOrCreateMyNDLAUserIfNotExist(any, any, any)(any)).thenReturn(Success(myNDLAUser))
    when(configService.isMyNDLAWriteRestricted).thenReturn(Success(false))

    val result = service.canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied("spiller ing", Some("en rolle"))
    result.isSuccess should be(true)
  }

  test("that isOperationAllowedOrAccessDenied denies access if user is student and wants to share a folder") {
    val myNDLAUser = emptyMyNDLAUser.copy(userRole = UserRole.STUDENT)
    when(userService.getOrCreateMyNDLAUserIfNotExist(any, any, any)(any)).thenReturn(Success(myNDLAUser))

    val updatedFolder   = api.UpdatedFolder(name = None, status = Some("shared"), description = None)
    val Failure(result) = service.isOperationAllowedOrAccessDenied("feideid", Some("accesstoken"), updatedFolder)
    result.getMessage should be("You do not have necessary permissions to share folders.")
  }

  test(
    "that isOperationAllowedOrAccessDenied denies access if user is student and wants to update a folder during exam"
  ) {
    val myNDLAUser = emptyMyNDLAUser.copy(userRole = UserRole.STUDENT)
    when(userService.getOrCreateMyNDLAUserIfNotExist(any, any, any)(any)).thenReturn(Success(myNDLAUser))
    when(configService.isMyNDLAWriteRestricted).thenReturn(Success(true))

    val updatedFolder   = api.UpdatedFolder(name = Some("asd"), status = None, description = None)
    val Failure(result) = service.isOperationAllowedOrAccessDenied("feideid", Some("accesstoken"), updatedFolder)
    result.getMessage should be("You do not have write access while write restriction is active.")
  }

  test("that isOperationAllowedOrAccessDenied allows student to update a folder outside of the examination time") {
    val myNDLAUser = emptyMyNDLAUser.copy(userRole = UserRole.STUDENT)
    when(userService.getOrCreateMyNDLAUserIfNotExist(any, any, any)(any)).thenReturn(Success(myNDLAUser))
    when(configService.isMyNDLAWriteRestricted).thenReturn(Success(false))

    val updatedFolder = api.UpdatedFolder(name = Some("asd"), status = None, description = None)
    val result        = service.isOperationAllowedOrAccessDenied("feideid", Some("accesstoken"), updatedFolder)
    result.isSuccess should be(true)
  }

  test(
    "that isOperationAllowedOrAccessDenied allows teacher to cut the cake and eat it too"
  ) {
    val myNDLAUser = emptyMyNDLAUser.copy(userRole = UserRole.EMPLOYEE)
    when(userService.getOrCreateMyNDLAUserIfNotExist(any, any, any)(any)).thenReturn(Success(myNDLAUser))
    when(configService.isMyNDLAWriteRestricted).thenReturn(Success(true))

    val folderWithUpdatedName   = api.UpdatedFolder(name = Some("asd"), status = None, description = None)
    val folderWithUpdatedStatus = api.UpdatedFolder(name = None, status = Some("shared"), description = None)
    val result1 = service.isOperationAllowedOrAccessDenied("feideid", Some("accesstoken"), folderWithUpdatedName)
    val result2 = service.isOperationAllowedOrAccessDenied("feideid", Some("accesstoken"), folderWithUpdatedStatus)
    result1.isSuccess should be(true)
    result2.isSuccess should be(true)
  }

}
