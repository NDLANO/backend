/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla.service

import no.ndla.common.model.NDLADate
import no.ndla.myndla.{TestData, TestEnvironment}
import no.ndla.myndla.model.api.{NewFolder, UpdatedFolder}
import no.ndla.myndla.model.api
import no.ndla.myndla.model.domain.{
  Folder,
  FolderStatus,
  MyNDLAGroup,
  MyNDLAUser,
  NewFolderData,
  Resource,
  ResourceDocument,
  UserRole
}
import no.ndla.scalatestsuite.UnitTestSuite

import java.util.UUID
import scala.util.Success

class FolderConverterServiceTest extends UnitTestSuite with TestEnvironment {

  val service = new FolderConverterService

  test("toNewFolderData transforms correctly") {
    val shared = NDLADate.now()
    when(clock.now()).thenReturn(shared)

    val folderUUID = UUID.randomUUID()
    val newFolder1 = NewFolder(
      name = "kenkaku",
      parentId = Some(folderUUID.toString),
      status = Some("private"),
      description = None
    )
    val newFolder2 = NewFolder(
      name = "kenkaku",
      parentId = Some(folderUUID.toString),
      status = Some("shared"),
      description = Some("descc")
    )
    val newFolder3 =
      NewFolder(
        name = "kenkaku",
        parentId = Some(folderUUID.toString),
        status = Some("ikkeesksisterendestatus"),
        description = Some("")
      )

    val expected1 = NewFolderData(
      parentId = Some(folderUUID),
      name = "kenkaku",
      status = FolderStatus.PRIVATE,
      rank = None,
      description = None
    )

    service.toNewFolderData(newFolder1, Some(folderUUID), None).get should be(expected1)
    service.toNewFolderData(newFolder2, Some(folderUUID), None).get should be(
      expected1.copy(status = FolderStatus.SHARED, description = Some("descc"))
    )
    service.toNewFolderData(newFolder3, Some(folderUUID), None).get should be(
      expected1.copy(status = FolderStatus.PRIVATE, description = Some(""))
    )
  }

  test("toApiFolder transforms correctly when data isn't corrupted") {
    val created = NDLADate.now()
    when(clock.now()).thenReturn(created)
    val mainFolderUUID = UUID.randomUUID()
    val subFolder1UUID = UUID.randomUUID()
    val subFolder2UUID = UUID.randomUUID()
    val subFolder3UUID = UUID.randomUUID()
    val resourceUUID   = UUID.randomUUID()

    val resource =
      Resource(
        id = resourceUUID,
        feideId = "w",
        resourceType = "concept",
        path = "/subject/1/topic/1/resource/4",
        created = created,
        tags = List("a", "b", "c"),
        resourceId = "1",
        connection = None
      )
    val folderData1 = Folder(
      id = subFolder1UUID,
      feideId = "u",
      parentId = Some(subFolder3UUID),
      name = "folderData1",
      status = FolderStatus.PRIVATE,
      resources = List(resource),
      subfolders = List.empty,
      rank = None,
      created = created,
      updated = created,
      shared = None,
      description = Some("folderData1")
    )
    val folderData2 = Folder(
      id = subFolder2UUID,
      feideId = "w",
      parentId = Some(mainFolderUUID),
      name = "folderData2",
      status = FolderStatus.SHARED,
      subfolders = List.empty,
      resources = List.empty,
      rank = None,
      created = created,
      updated = created,
      shared = None,
      description = Some("folderData2")
    )
    val folderData3 = Folder(
      id = subFolder3UUID,
      feideId = "u",
      parentId = Some(mainFolderUUID),
      name = "folderData3",
      status = FolderStatus.PRIVATE,
      subfolders = List(folderData1),
      resources = List.empty,
      rank = None,
      created = created,
      updated = created,
      shared = None,
      description = Some("folderData3")
    )
    val mainFolder = Folder(
      id = mainFolderUUID,
      feideId = "u",
      parentId = None,
      name = "mainFolder",
      status = FolderStatus.SHARED,
      subfolders = List(folderData2, folderData3),
      resources = List(resource),
      rank = None,
      created = created,
      updated = created,
      shared = None,
      description = Some("mainFolder")
    )
    val apiResource = api.Resource(
      id = resourceUUID.toString,
      resourceType = "concept",
      tags = List("a", "b", "c"),
      created = created,
      path = "/subject/1/topic/1/resource/4",
      resourceId = "1",
      rank = None
    )
    val apiData1 = api.Folder(
      id = subFolder1UUID.toString,
      name = "folderData1",
      status = "private",
      resources = List(apiResource),
      subfolders = List(),
      breadcrumbs = List(
        api.Breadcrumb(id = mainFolderUUID.toString, name = "mainFolder"),
        api.Breadcrumb(id = subFolder3UUID.toString, name = "folderData3"),
        api.Breadcrumb(id = subFolder1UUID.toString, name = "folderData1")
      ),
      parentId = Some(subFolder3UUID.toString),
      rank = None,
      created = created,
      updated = created,
      shared = None,
      description = Some("folderData1"),
      owner = None
    )
    val apiData2 = api.Folder(
      id = subFolder2UUID.toString,
      name = "folderData2",
      status = "shared",
      resources = List.empty,
      subfolders = List.empty,
      breadcrumbs = List(
        api.Breadcrumb(id = mainFolderUUID.toString, name = "mainFolder"),
        api.Breadcrumb(id = subFolder2UUID.toString, name = "folderData2")
      ),
      parentId = Some(mainFolderUUID.toString),
      rank = None,
      created = created,
      updated = created,
      shared = None,
      description = Some("folderData2"),
      owner = None
    )
    val apiData3 = api.Folder(
      id = subFolder3UUID.toString,
      name = "folderData3",
      status = "private",
      subfolders = List(apiData1),
      resources = List(),
      breadcrumbs = List(
        api.Breadcrumb(id = mainFolderUUID.toString, name = "mainFolder"),
        api.Breadcrumb(id = subFolder3UUID.toString, name = "folderData3")
      ),
      parentId = Some(mainFolderUUID.toString),
      rank = None,
      created = created,
      updated = created,
      shared = None,
      description = Some("folderData3"),
      owner = None
    )
    val expected = api.Folder(
      id = mainFolderUUID.toString,
      name = "mainFolder",
      status = "shared",
      subfolders = List(apiData2, apiData3),
      resources = List(apiResource),
      breadcrumbs = List(
        api.Breadcrumb(id = mainFolderUUID.toString, name = "mainFolder")
      ),
      parentId = None,
      rank = None,
      created = created,
      updated = created,
      shared = None,
      description = Some("mainFolder"),
      owner = None
    )

    val Success(result) =
      service.toApiFolder(mainFolder, List(api.Breadcrumb(id = mainFolderUUID.toString, name = "mainFolder")), None)
    result should be(expected)
  }

  test("updateFolder updates folder correctly") {
    val shared = NDLADate.now()
    when(clock.now()).thenReturn(shared)

    val folderUUID = UUID.randomUUID()
    val parentUUID = UUID.randomUUID()

    val existing = Folder(
      id = folderUUID,
      feideId = "u",
      parentId = Some(parentUUID),
      name = "folderData1",
      status = FolderStatus.PRIVATE,
      subfolders = List.empty,
      resources = List.empty,
      rank = None,
      created = clock.now(),
      updated = clock.now(),
      shared = None,
      description = Some("hei")
    )
    val updatedWithData =
      UpdatedFolder(name = Some("newNamae"), status = Some("shared"), description = Some("halla"))
    val updatedWithoutData = UpdatedFolder(name = None, status = None, description = None)
    val updatedWithGarbageData =
      UpdatedFolder(
        name = Some("huehueuheasdasd+++"),
        status = Some("det å joike er noe kult"),
        description = Some("jog ska visa deg garbage jog")
      )

    val expected1 =
      existing.copy(name = "newNamae", status = FolderStatus.SHARED, shared = Some(shared), description = Some("halla"))
    val expected2 = existing.copy(name = "folderData1", status = FolderStatus.PRIVATE)
    val expected3 = existing.copy(
      name = "huehueuheasdasd+++",
      status = FolderStatus.PRIVATE,
      description = Some("jog ska visa deg garbage jog")
    )

    val result1 = service.mergeFolder(existing, updatedWithData)
    val result2 = service.mergeFolder(existing, updatedWithoutData)
    val result3 = service.mergeFolder(existing, updatedWithGarbageData)

    result1 should be(expected1)
    result2 should be(expected2)
    result3 should be(expected3)
  }

  test("that mergeFolder works correctly for shared field and folder status update") {
    val sharedBefore = NDLADate.now().minusDays(1)
    val sharedNow    = NDLADate.now()
    when(clock.now()).thenReturn(sharedNow)

    val existingBase = Folder(
      id = UUID.randomUUID(),
      feideId = "u",
      parentId = Some(UUID.randomUUID()),
      name = "folderData1",
      status = FolderStatus.SHARED,
      subfolders = List.empty,
      resources = List.empty,
      rank = None,
      created = clock.now(),
      updated = clock.now(),
      shared = Some(sharedBefore),
      description = None
    )
    val existingShared  = existingBase.copy(status = FolderStatus.SHARED, shared = Some(sharedBefore))
    val existingPrivate = existingBase.copy(status = FolderStatus.PRIVATE, shared = None)
    val updatedShared   = UpdatedFolder(name = None, status = Some("shared"), description = None)
    val updatedPrivate  = UpdatedFolder(name = None, status = Some("private"), description = None)
    val expected1       = existingBase.copy(status = FolderStatus.SHARED, shared = Some(sharedBefore))
    val expected2       = existingBase.copy(status = FolderStatus.PRIVATE, shared = None)
    val expected3       = existingBase.copy(status = FolderStatus.SHARED, shared = Some(sharedNow))
    val expected4       = existingBase.copy(status = FolderStatus.PRIVATE, shared = None)

    val result1 = service.mergeFolder(existingShared, updatedShared)
    val result2 = service.mergeFolder(existingShared, updatedPrivate)
    val result3 = service.mergeFolder(existingPrivate, updatedShared)
    val result4 = service.mergeFolder(existingPrivate, updatedPrivate)
    result1 should be(expected1)
    result2 should be(expected2)
    result3 should be(expected3)
    result4 should be(expected4)
  }

  test("that toApiResource converts correctly") {
    val created = NDLADate.now()
    when(clock.now()).thenReturn(created)
    val folderUUID = UUID.randomUUID()

    val existing =
      Resource(
        id = folderUUID,
        feideId = "feideid",
        resourceType = "article",
        path = "/subject/1/topic/1/resource/4",
        created = created,
        tags = List("a", "b", "c"),
        resourceId = "1",
        connection = None
      )
    val expected =
      api.Resource(
        id = folderUUID.toString,
        resourceType = "article",
        path = "/subject/1/topic/1/resource/4",
        created = created,
        tags = List("a", "b", "c"),
        resourceId = "1",
        rank = None
      )

    service.toApiResource(existing) should be(Success(expected))
  }

  test("that newResource toDomainResource converts correctly") {
    val created = NDLADate.now()
    when(clock.now()).thenReturn(created)
    val newResource1 =
      api.NewResource(
        resourceType = "audio",
        path = "/subject/1/topic/1/resource/4",
        tags = Some(List("a", "b")),
        resourceId = "1"
      )
    val newResource2 =
      api.NewResource(resourceType = "audio", path = "/subject/1/topic/1/resource/4", tags = None, resourceId = "2")
    val expected1 = ResourceDocument(tags = List("a", "b"), resourceId = "1")
    val expected2 = expected1.copy(tags = List.empty, resourceId = "2")

    service.toDomainResource(newResource1) should be(expected1)
    service.toDomainResource(newResource2) should be(expected2)
  }

  test("That domainToApimodel transforms Folder from domain to api model correctly") {
    val folder1UUID = UUID.randomUUID()
    val folder2UUID = UUID.randomUUID()
    val folder3UUID = UUID.randomUUID()

    val folderDomainList = List(
      TestData.emptyDomainFolder.copy(id = folder1UUID),
      TestData.emptyDomainFolder.copy(id = folder2UUID),
      TestData.emptyDomainFolder.copy(id = folder3UUID)
    )

    val result = service.domainToApiModel(folderDomainList, f => service.toApiFolder(f, List.empty, None))
    result.get.length should be(3)
    result should be(
      Success(
        List(
          TestData.emptyApiFolder.copy(id = folder1UUID.toString, status = "private"),
          TestData.emptyApiFolder.copy(id = folder2UUID.toString, status = "private"),
          TestData.emptyApiFolder.copy(id = folder3UUID.toString, status = "private")
        )
      )
    )
  }

  test("That toApiUserData works correctly") {
    val domainUserData =
      MyNDLAUser(
        id = 42,
        feideId = "feide",
        favoriteSubjects = Seq("a", "b"),
        userRole = UserRole.STUDENT,
        lastUpdated = clock.now(),
        organization = "oslo",
        groups = Seq(
          MyNDLAGroup(
            id = "id",
            displayName = "oslo",
            isPrimarySchool = true,
            parentId = None
          )
        ),
        username = "example@email.com",
        email = "example@email.com",
        arenaEnabled = false,
        displayName = "Feide",
        shareName = false,
        arenaGroups = List.empty
      )
    val expectedUserData =
      api.MyNDLAUser(
        id = 42,
        feideId = "feide",
        username = "example@email.com",
        email = "example@email.com",
        displayName = "Feide",
        favoriteSubjects = Seq("a", "b"),
        role = "student",
        organization = "oslo",
        groups = Seq(api.MyNDLAGroup(id = "id", displayName = "oslo", isPrimarySchool = true, parentId = None)),
        arenaEnabled = false,
        shareName = false,
        arenaGroups = List.empty
      )

    service.toApiUserData(domainUserData, List.empty, List.empty) should be(expectedUserData)
  }

  test("That mergeUserData works correctly") {
    val domainUserData = MyNDLAUser(
      id = 42,
      feideId = "feide",
      favoriteSubjects = Seq("a", "b"),
      userRole = UserRole.STUDENT,
      lastUpdated = clock.now(),
      organization = "oslo",
      groups = Seq(
        MyNDLAGroup(
          id = "id",
          displayName = "oslo",
          isPrimarySchool = false,
          parentId = None
        )
      ),
      username = "example@email.com",
      email = "example@email.com",
      arenaEnabled = false,
      displayName = "Feide",
      shareName = false,
      arenaGroups = List.empty
    )
    val updatedUserData1 =
      api.UpdatedMyNDLAUser(favoriteSubjects = None, arenaEnabled = None, shareName = None, arenaGroups = None)
    val updatedUserData2 =
      api.UpdatedMyNDLAUser(
        favoriteSubjects = Some(Seq.empty),
        arenaEnabled = None,
        shareName = None,
        arenaGroups = None
      )
    val updatedUserData3 =
      api.UpdatedMyNDLAUser(
        favoriteSubjects = Some(Seq("x", "y", "z")),
        arenaEnabled = None,
        shareName = None,
        arenaGroups = None
      )

    val expectedUserData1 = MyNDLAUser(
      id = 42,
      feideId = "feide",
      favoriteSubjects = Seq("a", "b"),
      userRole = UserRole.STUDENT,
      lastUpdated = clock.now(),
      organization = "oslo",
      groups = Seq(
        MyNDLAGroup(
          id = "id",
          displayName = "oslo",
          isPrimarySchool = false,
          parentId = None
        )
      ),
      username = "example@email.com",
      email = "example@email.com",
      arenaEnabled = false,
      displayName = "Feide",
      shareName = false,
      arenaGroups = List.empty
    )
    val expectedUserData2 = MyNDLAUser(
      id = 42,
      feideId = "feide",
      favoriteSubjects = Seq.empty,
      userRole = UserRole.STUDENT,
      lastUpdated = clock.now(),
      organization = "oslo",
      groups = Seq(
        MyNDLAGroup(
          id = "id",
          displayName = "oslo",
          isPrimarySchool = false,
          parentId = None
        )
      ),
      username = "example@email.com",
      email = "example@email.com",
      arenaEnabled = false,
      displayName = "Feide",
      shareName = false,
      arenaGroups = List.empty
    )
    val expectedUserData3 = MyNDLAUser(
      id = 42,
      feideId = "feide",
      favoriteSubjects = Seq("x", "y", "z"),
      userRole = UserRole.STUDENT,
      lastUpdated = clock.now(),
      organization = "oslo",
      groups = Seq(
        MyNDLAGroup(
          id = "id",
          displayName = "oslo",
          isPrimarySchool = false,
          parentId = None
        )
      ),
      username = "example@email.com",
      email = "example@email.com",
      arenaEnabled = false,
      displayName = "Feide",
      shareName = false,
      arenaGroups = List.empty
    )

    service.mergeUserData(domainUserData, updatedUserData1, None, None) should be(expectedUserData1)
    service.mergeUserData(domainUserData, updatedUserData2, None, None) should be(expectedUserData2)
    service.mergeUserData(domainUserData, updatedUserData3, None, None) should be(expectedUserData3)
  }

}
