/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.service

import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.{Delete, Missing, UpdateWith}
import no.ndla.common.model.api.myndla.{MyNDLAGroupDTO, MyNDLAUserDTO, UpdatedMyNDLAUserDTO}
import no.ndla.common.model.domain.ResourceType
import no.ndla.common.model.domain.myndla.{FolderStatus, MyNDLAGroup, MyNDLAUser, UserRole}
import no.ndla.myndlaapi.model.api.{FolderDTO, NewFolderDTO, UpdatedFolderDTO}
import no.ndla.myndlaapi.model.{api, domain}
import no.ndla.myndlaapi.model.domain.{NewFolderData, Resource, ResourceDocument}
import no.ndla.myndlaapi.{TestData, TestEnvironment}
import no.ndla.scalatestsuite.UnitTestSuite
import org.mockito.Mockito.when

import java.util.UUID
import scala.util.Success

class FolderConverterServiceTest extends UnitTestSuite with TestEnvironment {

  val service = new FolderConverterService

  test("toNewFolderData transforms correctly") {
    val shared = NDLADate.now()
    when(clock.now()).thenReturn(shared)

    val folderUUID = UUID.randomUUID()
    val newFolder1 =
      NewFolderDTO(name = "kenkaku", parentId = Some(folderUUID.toString), status = Some("private"), description = None)
    val newFolder2 = NewFolderDTO(
      name = "kenkaku",
      parentId = Some(folderUUID.toString),
      status = Some("shared"),
      description = Some("descc"),
    )
    val newFolder3 = NewFolderDTO(
      name = "kenkaku",
      parentId = Some(folderUUID.toString),
      status = Some("ikkeesksisterendestatus"),
      description = Some(""),
    )

    val expected1 = NewFolderData(
      parentId = Some(folderUUID),
      name = "kenkaku",
      status = FolderStatus.PRIVATE,
      rank = 1,
      description = None,
    )

    service.toNewFolderData(newFolder1, Some(folderUUID), 1).get should be(expected1)
    service.toNewFolderData(newFolder2, Some(folderUUID), 1).get should be(
      expected1.copy(status = FolderStatus.SHARED, description = Some("descc"))
    )
    service.toNewFolderData(newFolder3, Some(folderUUID), 1).get should be(
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

    val resource = Resource(
      id = resourceUUID,
      feideId = "w",
      resourceType = ResourceType.Concept,
      path = "/subject/1/topic/1/resource/4",
      created = created,
      tags = List("a", "b", "c"),
      resourceId = "1",
      connection = None,
    )
    val folderData1 = domain.Folder(
      id = subFolder1UUID,
      feideId = "u",
      parentId = Some(subFolder3UUID),
      name = "folderData1",
      status = FolderStatus.PRIVATE,
      resources = List(resource),
      subfolders = List.empty,
      rank = 1,
      created = created,
      updated = created,
      shared = None,
      description = Some("folderData1"),
      user = None,
    )
    val folderData2 = domain.Folder(
      id = subFolder2UUID,
      feideId = "w",
      parentId = Some(mainFolderUUID),
      name = "folderData2",
      status = FolderStatus.SHARED,
      subfolders = List.empty,
      resources = List.empty,
      rank = 1,
      created = created,
      updated = created,
      shared = None,
      description = Some("folderData2"),
      user = None,
    )
    val folderData3 = domain.Folder(
      id = subFolder3UUID,
      feideId = "u",
      parentId = Some(mainFolderUUID),
      name = "folderData3",
      status = FolderStatus.PRIVATE,
      subfolders = List(folderData1),
      resources = List.empty,
      rank = 1,
      created = created,
      updated = created,
      shared = None,
      description = Some("folderData3"),
      user = None,
    )
    val mainFolder = domain.Folder(
      id = mainFolderUUID,
      feideId = "u",
      parentId = None,
      name = "mainFolder",
      status = FolderStatus.SHARED,
      subfolders = List(folderData2, folderData3),
      resources = List(resource),
      rank = 1,
      created = created,
      updated = created,
      shared = None,
      description = Some("mainFolder"),
      user = None,
    )
    val apiResource = api.ResourceDTO(
      id = resourceUUID,
      resourceType = ResourceType.Concept,
      tags = List("a", "b", "c"),
      created = created,
      path = "/subject/1/topic/1/resource/4",
      resourceId = "1",
      rank = None,
    )
    val apiData1 = FolderDTO(
      id = subFolder1UUID,
      name = "folderData1",
      status = "private",
      resources = List(apiResource),
      subfolders = List(),
      breadcrumbs = List(
        api.BreadcrumbDTO(id = mainFolderUUID, name = "mainFolder"),
        api.BreadcrumbDTO(id = subFolder3UUID, name = "folderData3"),
        api.BreadcrumbDTO(id = subFolder1UUID, name = "folderData1"),
      ),
      parentId = Some(subFolder3UUID),
      rank = 1,
      created = created,
      updated = created,
      shared = None,
      description = Some("folderData1"),
      owner = None,
    )
    val apiData2 = api.FolderDTO(
      id = subFolder2UUID,
      name = "folderData2",
      status = "shared",
      resources = List.empty,
      subfolders = List.empty,
      breadcrumbs = List(
        api.BreadcrumbDTO(id = mainFolderUUID, name = "mainFolder"),
        api.BreadcrumbDTO(id = subFolder2UUID, name = "folderData2"),
      ),
      parentId = Some(mainFolderUUID),
      rank = 1,
      created = created,
      updated = created,
      shared = None,
      description = Some("folderData2"),
      owner = None,
    )
    val apiData3 = api.FolderDTO(
      id = subFolder3UUID,
      name = "folderData3",
      status = "private",
      subfolders = List(apiData1),
      resources = List(),
      breadcrumbs = List(
        api.BreadcrumbDTO(id = mainFolderUUID, name = "mainFolder"),
        api.BreadcrumbDTO(id = subFolder3UUID, name = "folderData3"),
      ),
      parentId = Some(mainFolderUUID),
      rank = 1,
      created = created,
      updated = created,
      shared = None,
      description = Some("folderData3"),
      owner = None,
    )
    val expected = api.FolderDTO(
      id = mainFolderUUID,
      name = "mainFolder",
      status = "shared",
      subfolders = List(apiData2, apiData3),
      resources = List(apiResource),
      breadcrumbs = List(api.BreadcrumbDTO(id = mainFolderUUID, name = "mainFolder")),
      parentId = None,
      rank = 1,
      created = created,
      updated = created,
      shared = None,
      description = Some("mainFolder"),
      owner = None,
    )

    val Success(result) = service.toApiFolder(
      mainFolder,
      List(api.BreadcrumbDTO(id = mainFolderUUID, name = "mainFolder")),
      None,
      true,
    ): @unchecked
    result should be(expected)
  }

  test("updateFolder updates folder correctly") {
    val shared = NDLADate.now()
    when(clock.now()).thenReturn(shared)

    val folderUUID = UUID.randomUUID()
    val parentUUID = UUID.randomUUID()

    val existing = domain.Folder(
      id = folderUUID,
      feideId = "u",
      parentId = Some(parentUUID),
      name = "folderData1",
      status = FolderStatus.PRIVATE,
      subfolders = List.empty,
      resources = List.empty,
      rank = 1,
      created = clock.now(),
      updated = clock.now(),
      shared = None,
      description = Some("hei"),
      user = None,
    )
    val updatedWithData = UpdatedFolderDTO(
      parentId = Missing,
      name = Some("newNamae"),
      status = Some("shared"),
      description = Some("halla"),
    )
    val updatedWithoutData     = UpdatedFolderDTO(parentId = Missing, name = None, status = None, description = None)
    val updatedWithGarbageData = UpdatedFolderDTO(
      parentId = Missing,
      name = Some("huehueuheasdasd+++"),
      status = Some("det å joike er noe kult"),
      description = Some("jog ska visa deg garbage jog"),
    )
    val newParentUUID        = UUID.randomUUID()
    val updatedWithNewParent = UpdatedFolderDTO(
      parentId = UpdateWith[String](newParentUUID.toString),
      name = None,
      status = None,
      description = None,
    )
    val updatedWithNoParent = UpdatedFolderDTO(parentId = Delete, name = None, status = None, description = None)

    val expected1 =
      existing.copy(name = "newNamae", status = FolderStatus.SHARED, shared = Some(shared), description = Some("halla"))
    val expected2 = existing.copy(name = "folderData1", status = FolderStatus.PRIVATE)
    val expected3 = existing.copy(
      name = "huehueuheasdasd+++",
      status = FolderStatus.PRIVATE,
      description = Some("jog ska visa deg garbage jog"),
    )
    val expected4 = existing.copy(parentId = Some(newParentUUID))
    val expected5 = existing.copy(parentId = None)

    val result1 = service.mergeFolder(existing, updatedWithData)
    val result2 = service.mergeFolder(existing, updatedWithoutData)
    val result3 = service.mergeFolder(existing, updatedWithGarbageData)
    val result4 = service.mergeFolder(existing, updatedWithNewParent)
    val result5 = service.mergeFolder(existing, updatedWithNoParent)

    result1 should be(Success(expected1))
    result2 should be(Success(expected2))
    result3 should be(Success(expected3))
    result4 should be(Success(expected4))
    result5 should be(Success(expected5))
  }

  test("that mergeFolder works correctly for shared field and folder status update") {
    val sharedBefore = NDLADate.now().minusDays(1)
    val sharedNow    = NDLADate.now()
    when(clock.now()).thenReturn(sharedNow)

    val existingBase = domain.Folder(
      id = UUID.randomUUID(),
      feideId = "u",
      parentId = Some(UUID.randomUUID()),
      name = "folderData1",
      status = FolderStatus.SHARED,
      subfolders = List.empty,
      resources = List.empty,
      rank = 1,
      created = clock.now(),
      updated = clock.now(),
      shared = Some(sharedBefore),
      description = None,
      user = None,
    )
    val existingShared  = existingBase.copy(status = FolderStatus.SHARED, shared = Some(sharedBefore))
    val existingPrivate = existingBase.copy(status = FolderStatus.PRIVATE, shared = None)
    val updatedShared   = UpdatedFolderDTO(parentId = Missing, name = None, status = Some("shared"), description = None)
    val updatedPrivate  = UpdatedFolderDTO(parentId = Missing, name = None, status = Some("private"), description = None)
    val expected1       = existingBase.copy(status = FolderStatus.SHARED, shared = Some(sharedBefore))
    val expected2       = existingBase.copy(status = FolderStatus.PRIVATE, shared = None)
    val expected3       = existingBase.copy(status = FolderStatus.SHARED, shared = Some(sharedNow))
    val expected4       = existingBase.copy(status = FolderStatus.PRIVATE, shared = None)

    val result1 = service.mergeFolder(existingShared, updatedShared)
    val result2 = service.mergeFolder(existingShared, updatedPrivate)
    val result3 = service.mergeFolder(existingPrivate, updatedShared)
    val result4 = service.mergeFolder(existingPrivate, updatedPrivate)
    result1 should be(Success(expected1))
    result2 should be(Success(expected2))
    result3 should be(Success(expected3))
    result4 should be(Success(expected4))
  }

  test("that toApiResource converts correctly") {
    val created = NDLADate.now()
    when(clock.now()).thenReturn(created)
    val folderUUID = UUID.randomUUID()

    val existing = Resource(
      id = folderUUID,
      feideId = "feideid",
      resourceType = ResourceType.Article,
      path = "/subject/1/topic/1/resource/4",
      created = created,
      tags = List("a", "b", "c"),
      resourceId = "1",
      connection = None,
    )
    val expected = api.ResourceDTO(
      id = folderUUID,
      resourceType = ResourceType.Article,
      path = "/subject/1/topic/1/resource/4",
      created = created,
      tags = List("a", "b", "c"),
      resourceId = "1",
      rank = None,
    )

    service.toApiResource(existing, isOwner = true) should be(Success(expected))
  }

  test("that newResource toDomainResource converts correctly") {
    val created = NDLADate.now()
    when(clock.now()).thenReturn(created)
    val newResource1 = api.NewResourceDTO(
      resourceType = ResourceType.Audio,
      path = "/subject/1/topic/1/resource/4",
      tags = Some(List("a", "b")),
      resourceId = "1",
    )
    val newResource2 = api.NewResourceDTO(
      resourceType = ResourceType.Audio,
      path = "/subject/1/topic/1/resource/4",
      tags = None,
      resourceId = "2",
    )
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
      TestData.emptyDomainFolder.copy(id = folder3UUID),
    )

    val result =
      service.domainToApiModel(folderDomainList, f => service.toApiFolder(f, List.empty, None, isOwner = true))
    result.get.length should be(3)
    result should be(
      Success(
        List(
          TestData.emptyApiFolder.copy(id = folder1UUID, status = "private"),
          TestData.emptyApiFolder.copy(id = folder2UUID, status = "private"),
          TestData.emptyApiFolder.copy(id = folder3UUID, status = "private"),
        )
      )
    )
  }

  test("That toApiUserData works correctly") {
    val domainUserData = MyNDLAUser(
      id = 42,
      feideId = "feide",
      favoriteSubjects = Seq("a", "b"),
      userRole = UserRole.STUDENT,
      lastUpdated = clock.now(),
      organization = "oslo",
      groups = Seq(MyNDLAGroup(id = "id", displayName = "oslo", isPrimarySchool = true, parentId = None)),
      username = "example@email.com",
      displayName = "Feide",
      email = "example@email.com",
      arenaEnabled = false,
    )
    val expectedUserData = MyNDLAUserDTO(
      id = 42,
      feideId = "feide",
      username = "example@email.com",
      email = "example@email.com",
      displayName = "Feide",
      favoriteSubjects = Seq("a", "b"),
      role = UserRole.STUDENT,
      organization = "oslo",
      groups = Seq(MyNDLAGroupDTO(id = "id", displayName = "oslo", isPrimarySchool = true, parentId = None)),
      arenaEnabled = false,
    )

    service.toApiUserData(domainUserData) should be(expectedUserData)
  }

  test("That mergeUserData works correctly") {
    val domainUserData = MyNDLAUser(
      id = 42,
      feideId = "feide",
      favoriteSubjects = Seq("a", "b"),
      userRole = UserRole.STUDENT,
      lastUpdated = clock.now(),
      organization = "oslo",
      groups = Seq(MyNDLAGroup(id = "id", displayName = "oslo", isPrimarySchool = false, parentId = None)),
      username = "example@email.com",
      displayName = "Feide",
      email = "example@email.com",
      arenaEnabled = false,
    )
    val updatedUserData1 = UpdatedMyNDLAUserDTO(favoriteSubjects = None, arenaEnabled = None)
    val updatedUserData2 = UpdatedMyNDLAUserDTO(favoriteSubjects = Some(Seq.empty), arenaEnabled = None)
    val updatedUserData3 = UpdatedMyNDLAUserDTO(favoriteSubjects = Some(Seq("x", "y", "z")), arenaEnabled = None)

    val expectedUserData1 = MyNDLAUser(
      id = 42,
      feideId = "feide",
      favoriteSubjects = Seq("a", "b"),
      userRole = UserRole.STUDENT,
      lastUpdated = clock.now(),
      organization = "oslo",
      groups = Seq(MyNDLAGroup(id = "id", displayName = "oslo", isPrimarySchool = false, parentId = None)),
      username = "example@email.com",
      displayName = "Feide",
      email = "example@email.com",
      arenaEnabled = false,
    )
    val expectedUserData2 = MyNDLAUser(
      id = 42,
      feideId = "feide",
      favoriteSubjects = Seq.empty,
      userRole = UserRole.STUDENT,
      lastUpdated = clock.now(),
      organization = "oslo",
      groups = Seq(MyNDLAGroup(id = "id", displayName = "oslo", isPrimarySchool = false, parentId = None)),
      username = "example@email.com",
      displayName = "Feide",
      email = "example@email.com",
      arenaEnabled = false,
    )
    val expectedUserData3 = MyNDLAUser(
      id = 42,
      feideId = "feide",
      favoriteSubjects = Seq("x", "y", "z"),
      userRole = UserRole.STUDENT,
      lastUpdated = clock.now(),
      organization = "oslo",
      groups = Seq(MyNDLAGroup(id = "id", displayName = "oslo", isPrimarySchool = false, parentId = None)),
      username = "example@email.com",
      displayName = "Feide",
      email = "example@email.com",
      arenaEnabled = false,
    )

    service.mergeUserData(domainUserData, updatedUserData1, None).get should be(expectedUserData1)
    service.mergeUserData(domainUserData, updatedUserData2, None).get should be(expectedUserData2)
    service.mergeUserData(domainUserData, updatedUserData3, None).get should be(expectedUserData3)
  }

}
