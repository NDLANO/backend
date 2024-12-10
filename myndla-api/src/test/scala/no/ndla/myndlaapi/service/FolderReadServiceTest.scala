/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.service

import no.ndla.common.errors.{AccessDeniedException, NotFoundException}
import no.ndla.common.model.domain.ResourceType
import no.ndla.common.model.domain.myndla.{FolderStatus, MyNDLAGroup, MyNDLAUser, UserRole}
import no.ndla.myndlaapi.TestData.{emptyApiFolder, emptyDomainFolder, emptyDomainResource, emptyMyNDLAUser}
import no.ndla.myndlaapi.model.api
import no.ndla.myndlaapi.{TestData, TestEnvironment}
import no.ndla.myndlaapi.model.domain
import no.ndla.myndlaapi.model.api.{FolderDTO, OwnerDTO, ResourceStatsDTO}
import no.ndla.myndlaapi.model.domain.Resource
import no.ndla.scalatestsuite.UnitTestSuite
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.mockito.invocation.InvocationOnMock
import scalikejdbc.DBSession

import java.util.UUID
import scala.util.{Failure, Success, Try}

class FolderReadServiceTest extends UnitTestSuite with TestEnvironment {

  val service                                                 = new FolderReadService
  override val folderConverterService: FolderConverterService = org.mockito.Mockito.spy(new FolderConverterService)

  override def beforeEach(): Unit = {
    super.beforeEach()
    resetMocks()
    when(clock.now()).thenReturn(TestData.today)
    when(folderRepository.getSession(any)).thenReturn(mock[DBSession])
    when(folderRepository.rollbackOnFailure(any)).thenAnswer((i: InvocationOnMock) => {
      val func = i.getArgument[DBSession => Try[Nothing]](0)
      func(mock[DBSession])
    })
  }

  test("That getSingleFolder returns folder and its data when user is the owner") {
    val created        = clock.now()
    val feideId        = "FEIDE"
    val mainFolderUUID = UUID.randomUUID()
    val subFolder1UUID = UUID.randomUUID()
    val subFolder2UUID = UUID.randomUUID()
    val resource1UUID  = UUID.randomUUID()

    val mainFolder = domain.Folder(
      id = mainFolderUUID,
      feideId = feideId,
      parentId = None,
      name = "mainFolder",
      status = FolderStatus.PRIVATE,
      subfolders = List.empty,
      resources = List.empty,
      rank = 1,
      created = created,
      updated = created,
      shared = None,
      description = None,
      user = None
    )

    val subFolder1 = domain.Folder(
      id = subFolder1UUID,
      feideId = "",
      parentId = Some(mainFolderUUID),
      name = "subFolder1",
      status = FolderStatus.PRIVATE,
      subfolders = List.empty,
      resources = List.empty,
      rank = 1,
      created = created,
      updated = created,
      shared = None,
      description = None,
      user = None
    )

    val subFolder2 = domain.Folder(
      id = subFolder2UUID,
      feideId = "",
      parentId = Some(mainFolderUUID),
      name = "subFolder2",
      status = FolderStatus.PRIVATE,
      subfolders = List.empty,
      resources = List.empty,
      rank = 1,
      created = created,
      updated = created,
      shared = None,
      description = None,
      user = None
    )

    val resource1 = Resource(
      id = resource1UUID,
      feideId = "",
      resourceType = ResourceType.Article,
      path = "/subject/1/topic/1/resource/4",
      created = created,
      tags = List.empty,
      resourceId = "1",
      connection = None
    )

    val expected = FolderDTO(
      id = mainFolderUUID.toString,
      name = "mainFolder",
      status = "private",
      breadcrumbs = List(api.BreadcrumbDTO(id = mainFolderUUID.toString, name = "mainFolder")),
      parentId = None,
      resources = List(
        api.ResourceDTO(
          id = resource1UUID.toString,
          resourceType = ResourceType.Article,
          tags = List.empty,
          path = "/subject/1/topic/1/resource/4",
          created = created,
          resourceId = "1",
          rank = None
        )
      ),
      subfolders = List(
        api.FolderDTO(
          id = subFolder1UUID.toString,
          name = "subFolder1",
          status = "private",
          subfolders = List.empty,
          resources = List.empty,
          breadcrumbs = List(
            api.BreadcrumbDTO(id = mainFolderUUID.toString, name = "mainFolder"),
            api.BreadcrumbDTO(id = subFolder1UUID.toString, name = "subFolder1")
          ),
          parentId = Some(mainFolderUUID.toString),
          rank = 1,
          created = created,
          updated = created,
          shared = None,
          description = None,
          owner = None
        ),
        api.FolderDTO(
          id = subFolder2UUID.toString,
          name = "subFolder2",
          status = "private",
          resources = List.empty,
          subfolders = List.empty,
          breadcrumbs = List(
            api.BreadcrumbDTO(id = mainFolderUUID.toString, name = "mainFolder"),
            api.BreadcrumbDTO(id = subFolder2UUID.toString, name = "subFolder2")
          ),
          parentId = Some(mainFolderUUID.toString),
          rank = 1,
          created = created,
          updated = created,
          shared = None,
          description = None,
          owner = None
        )
      ),
      rank = 1,
      created = created,
      updated = created,
      shared = None,
      description = None,
      owner = None
    )

    val whgaterh = mainFolder.copy(
      subfolders = List(
        subFolder1,
        subFolder2
      ),
      resources = List(
        resource1
      )
    )

    when(feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    when(folderRepository.folderWithId(eqTo(mainFolderUUID))(any)).thenReturn(Success(mainFolder))
    when(folderRepository.foldersWithParentID(eqTo(Some(mainFolderUUID)))(any))
      .thenReturn(Success(List(subFolder1, subFolder2)))
    when(folderRepository.foldersWithParentID(eqTo(Some(subFolder1UUID)))(any)).thenReturn(Success(List.empty))
    when(folderRepository.foldersWithParentID(eqTo(Some(subFolder2UUID)))(any)).thenReturn(Success(List.empty))
    when(folderRepository.getFolderResources(eqTo(mainFolderUUID))(any)).thenReturn(Success(List(resource1)))
    when(folderRepository.getFolderResources(eqTo(subFolder1UUID))(any)).thenReturn(Success(List.empty))
    when(folderRepository.getFolderResources(eqTo(subFolder2UUID))(any)).thenReturn(Success(List.empty))
    when(folderRepository.getFolderAndChildrenSubfoldersWithResources(any)(any)).thenReturn(Success(Some(whgaterh)))
    when(userRepository.userWithFeideId(any)(any[DBSession])).thenReturn(Success(None))

    val result = service.getSingleFolder(
      id = mainFolderUUID,
      includeSubfolders = true,
      includeResources = true,
      feideAccessToken = None
    )
    result should be(Success(expected))
  }

  test("That getSingleFolder fails if user does not own the folder") {
    val mainFolderUUID = UUID.randomUUID()

    when(feideApiClient.getFeideID(any)).thenReturn(Success("not daijoubu"))
    when(folderRepository.folderWithId(eqTo(mainFolderUUID))(any)).thenReturn(Success(emptyDomainFolder))
    when(folderRepository.getFolderAndChildrenSubfolders(any)(any)).thenReturn(Success(Some(emptyDomainFolder)))

    val result = service.getSingleFolder(mainFolderUUID, includeSubfolders = true, includeResources = false, None)
    result should be(Failure(AccessDeniedException("You do not have access to this entity.")))
    verify(folderRepository, times(0)).foldersWithParentID(any)(any)
    verify(folderRepository, times(0)).getFolderResources(any)(any)
  }

  test("That getFolders creates favorite folder if user has no folders") {
    val feideId              = "yee boiii"
    val favoriteUUID         = UUID.randomUUID()
    val favoriteDomainFolder = emptyDomainFolder.copy(id = favoriteUUID, name = "favorite")
    val favoriteApiFolder =
      emptyApiFolder.copy(
        id = favoriteUUID.toString,
        name = "favorite",
        status = "private",
        breadcrumbs = List(api.BreadcrumbDTO(id = favoriteUUID.toString, name = "favorite"))
      )

    when(feideApiClient.getFeideID(Some("token"))).thenReturn(Success(feideId))
    when(folderRepository.insertFolder(any, any)(any)).thenReturn(Success(favoriteDomainFolder))
    when(folderRepository.foldersWithFeideAndParentID(eqTo(None), eqTo(feideId))(any)).thenReturn(Success(List.empty))
    when(folderRepository.folderWithId(eqTo(favoriteUUID))(any)).thenReturn(Success(favoriteDomainFolder))
    when(folderRepository.getSavedSharedFolders(any)(any[DBSession])).thenReturn(Success(List.empty))
    when(userRepository.userWithFeideId(any)(any[DBSession])).thenReturn(Success(None))

    val result = service.getFolders(includeSubfolders = false, includeResources = false, Some("token")).get.folders
    result.length should be(1)
    result.find(_.name == "favorite").get should be(favoriteApiFolder)

    verify(folderRepository, times(1)).foldersWithFeideAndParentID(eqTo(None), eqTo(feideId))(any)
    verify(folderRepository, times(1)).insertFolder(any, any)(any)
  }

  test("that getFolders include sharefolder that is saved by the user") {
    val feideId              = "yee boiii"
    val favoriteUUID         = UUID.randomUUID()
    val favoriteDomainFolder = emptyDomainFolder.copy(id = favoriteUUID, name = "favorite")
    val favoriteApiFolder =
      emptyApiFolder.copy(
        id = favoriteUUID.toString,
        name = "favorite",
        status = "private",
        breadcrumbs = List(api.BreadcrumbDTO(id = favoriteUUID.toString, name = "favorite"))
      )

    val user               = emptyMyNDLAUser.copy(id = 1996, shareName = true, displayName = "hallois")
    val folderId           = UUID.randomUUID()
    val sharedFolderDomain = emptyDomainFolder.copy(id = folderId, name = "SharedFolder", status = FolderStatus.SHARED)
    val savedFolderDomain =
      emptyDomainFolder.copy(id = folderId, name = "SharedFolder", status = FolderStatus.SHARED, user = Some(user))
    val sharedFolderApi = emptyApiFolder.copy(
      id = folderId.toString,
      name = "SharedFolder",
      status = "shared",
      breadcrumbs = List(api.BreadcrumbDTO(id = folderId.toString, name = "SharedFolder")),
      owner = Some(OwnerDTO(name = user.displayName))
    )

    when(feideApiClient.getFeideID(Some("token"))).thenReturn(Success(feideId))
    when(folderRepository.insertFolder(any, any)(any)).thenReturn(Success(favoriteDomainFolder))
    when(folderRepository.foldersWithFeideAndParentID(eqTo(None), eqTo(feideId))(any)).thenReturn(Success(List.empty))
    when(folderRepository.folderWithId(eqTo(favoriteUUID))(any)).thenReturn(Success(favoriteDomainFolder))
    when(folderRepository.getSavedSharedFolders(any)(any[DBSession])).thenReturn(Success(List(sharedFolderDomain)))
    when(folderRepository.getFolderAndChildrenSubfoldersWithResources(any, any, any)(any[DBSession]))
      .thenReturn(Success(Option(sharedFolderDomain)))
    when(folderRepository.getSharedFolderAndChildrenSubfoldersWithResources(any)(any[DBSession]))
      .thenReturn(Success(Option(savedFolderDomain)))
    when(userRepository.userWithFeideId(any)(any[DBSession])).thenReturn(Success(None))

    val result = service.getFolders(includeSubfolders = false, includeResources = false, Some("token")).get
    result.folders.length should be(1)
    result.folders.find(_.name == "favorite").get should be(favoriteApiFolder)

    result.sharedFolders.length should be(1)
    result.sharedFolders.find(_.name == "SharedFolder").get should be(sharedFolderApi)

    verify(folderRepository, times(1)).foldersWithFeideAndParentID(eqTo(None), eqTo(feideId))(any)
    verify(folderRepository, times(1)).insertFolder(any, any)(any)
    verify(folderRepository, times(1)).getSavedSharedFolders(any)(any)
  }

  test("That getFolders includes resources for the top folders when includeResources flag is set to true") {
    val created = clock.now()
    when(clock.now()).thenReturn(created)

    val feideId        = "yee boiii"
    val resourceId     = UUID.randomUUID()
    val folderId       = UUID.randomUUID()
    val folderWithId   = emptyDomainFolder.copy(id = folderId)
    val domainResource = emptyDomainResource.copy(id = resourceId, created = created)

    val folderResourcesResponse1 = Success(List(domainResource, domainResource))
    val folderResourcesResponse2 = Success(List(domainResource))
    val folderResourcesResponse3 = Success(List.empty)

    when(feideApiClient.getFeideID(Some("token"))).thenReturn(Success(feideId))
    when(folderRepository.foldersWithFeideAndParentID(eqTo(None), eqTo(feideId))(any))
      .thenReturn(Success(List(folderWithId, folderWithId)))
    when(folderRepository.folderWithId(eqTo(folderWithId.id))(any)).thenReturn(Success(folderWithId))
    when(folderRepository.getFolderResources(any)(any))
      .thenReturn(folderResourcesResponse1, folderResourcesResponse2, folderResourcesResponse3)
    when(folderRepository.getSavedSharedFolders(any)(any)).thenReturn(Success(List.empty))
    when(userRepository.userWithFeideId(any)(any[DBSession])).thenReturn(Success(None))

    val result = service.getFolders(includeSubfolders = false, includeResources = true, Some("token")).get.folders
    result.length should be(2)

    verify(folderRepository, times(1)).foldersWithFeideAndParentID(eqTo(None), eqTo(feideId))(any)
    verify(folderRepository, times(0)).insertFolder(any, any)(any)
    verify(folderRepository, times(2)).getFolderResources(any)(any)
  }

  test("That getSharedFolder returns a folder if the status is shared") {
    val folderUUID   = UUID.randomUUID()
    val folderWithId = emptyDomainFolder.copy(id = folderUUID, status = FolderStatus.SHARED)
    val apiFolder =
      emptyApiFolder.copy(
        id = folderUUID.toString,
        name = "",
        status = "shared",
        breadcrumbs = List(api.BreadcrumbDTO(id = folderUUID.toString, name = ""))
      )

    when(
      folderRepository.getFolderAndChildrenSubfoldersWithResources(eqTo(folderUUID), eqTo(FolderStatus.SHARED), any)(
        any
      )
    )
      .thenReturn(Success(Some(folderWithId)))
    when(userRepository.userWithFeideId(any)(any[DBSession])).thenReturn(Success(None))

    service.getSharedFolder(folderUUID, None) should be(Success(apiFolder))
  }

  test("That getSharedFolder returns a folder with owner info if the owner wants to") {
    val feideId = "feide"
    val domainUserData = MyNDLAUser(
      id = 42,
      feideId = feideId,
      favoriteSubjects = Seq.empty,
      userRole = UserRole.EMPLOYEE,
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
      shareName = true,
      arenaGroups = List.empty
    )

    val folderUUID   = UUID.randomUUID()
    val folderWithId = emptyDomainFolder.copy(id = folderUUID, status = FolderStatus.SHARED)
    val apiFolder =
      emptyApiFolder.copy(
        id = folderUUID.toString,
        name = "",
        status = "shared",
        breadcrumbs = List(api.BreadcrumbDTO(id = folderUUID.toString, name = "")),
        owner = Some(OwnerDTO("Feide"))
      )

    when(
      folderRepository.getFolderAndChildrenSubfoldersWithResources(eqTo(folderUUID), eqTo(FolderStatus.SHARED), any)(
        any
      )
    )
      .thenReturn(Success(Some(folderWithId)))
    when(userRepository.userWithFeideId(any)(any[DBSession])).thenReturn(Success(Some(domainUserData)))

    service.getSharedFolder(folderUUID, None) should be(Success(apiFolder))
  }

  test("That getSharedFolder returns a Failure Not Found if the status is not shared") {
    val folderUUID   = UUID.randomUUID()
    val folderWithId = emptyDomainFolder.copy(id = folderUUID, status = FolderStatus.PRIVATE)

    when(
      folderRepository.getFolderAndChildrenSubfoldersWithResources(eqTo(folderUUID), eqTo(FolderStatus.SHARED), any)(
        any
      )
    )
      .thenReturn(Success(Some(folderWithId)))

    val Failure(result: NotFoundException) = service.getSharedFolder(folderUUID, None)
    result.message should be("Folder does not exist")
  }

  test("That getting stats fetches stats for my ndla usage") {
    when(userRepository.numberOfUsers()(any)).thenReturn(Some(5))
    when(folderRepository.numberOfFolders()(any)).thenReturn(Some(10))
    when(folderRepository.numberOfResources()(any)).thenReturn(Some(20))
    when(folderRepository.numberOfTags()(any)).thenReturn(Some(10))
    when(userRepository.numberOfFavouritedSubjects()(any)).thenReturn(Some(15))
    when(folderRepository.numberOfSharedFolders()(any)).thenReturn(Some(5))
    when(folderRepository.numberOfResourcesGrouped()(any))
      .thenReturn(List((1, "article"), (2, "learningpath"), (3, "video")))

    service.getStats.get should be(
      api.StatsDTO(
        5,
        10,
        20,
        10,
        15,
        5,
        List(ResourceStatsDTO("article", 1), ResourceStatsDTO("learningpath", 2), ResourceStatsDTO("video", 3)),
        Map("article" -> 1, "learningpath" -> 2, "video" -> 3)
      )
    )
  }

  test("That getSharedFolder returns an unshared folder if requested by the owner") {
    val feideId      = "feide"
    val folderUUID   = UUID.randomUUID()
    val folderWithId = emptyDomainFolder.copy(id = folderUUID, status = FolderStatus.PRIVATE, feideId = feideId)
    val apiFolder =
      emptyApiFolder.copy(
        id = folderUUID.toString,
        name = "",
        status = "private",
        breadcrumbs = List(api.BreadcrumbDTO(id = folderUUID.toString, name = ""))
      )

    when(feideApiClient.getFeideID(Some(feideId))).thenReturn(Success(feideId))
    when(
      folderRepository.getFolderAndChildrenSubfoldersWithResources(
        eqTo(folderUUID),
        eqTo(FolderStatus.SHARED),
        eqTo(Some(feideId))
      )(any)
    )
      .thenReturn(Success(Some(folderWithId)))
    when(userRepository.userWithFeideId(any)(any[DBSession])).thenReturn(Success(None))

    service.getSharedFolder(folderUUID, Some(feideId)) should be(Success(apiFolder))
  }

  test("That getSharedFolder returns folder with tags only if owner") {
    reset(feideApiClient)
    when(clock.now()).thenReturn(TestData.today)
    val ownerId      = "ownerId"
    val otherId      = "someOtherId"
    val folderUUID   = UUID.randomUUID()
    val resourceUUID = UUID.randomUUID()
    val resource = emptyDomainResource.copy(
      id = resourceUUID,
      feideId = ownerId,
      tags = List("a", "b"),
      resourceType = ResourceType.Article,
      path = "/path",
      created = TestData.today
    )
    val folderWithId = emptyDomainFolder.copy(
      id = folderUUID,
      status = FolderStatus.SHARED,
      feideId = ownerId,
      resources = List(resource)
    )

    val apiResource = api.ResourceDTO(
      id = resourceUUID.toString,
      resourceType = ResourceType.Article,
      path = "/path",
      created = TestData.today,
      tags = List("a", "b"),
      resourceId = "1",
      rank = None
    )
    val apiFolder = emptyApiFolder.copy(
      id = folderUUID.toString,
      name = "",
      status = "shared",
      breadcrumbs = List(api.BreadcrumbDTO(id = folderUUID.toString, name = "")),
      resources = List(apiResource)
    )

    when(feideApiClient.getFeideID(Some(ownerId))).thenReturn(Success(ownerId))
    when(feideApiClient.getFeideID(Some(otherId))).thenReturn(Success(otherId))
    when(
      folderRepository.getFolderAndChildrenSubfoldersWithResources(
        eqTo(folderUUID),
        eqTo(FolderStatus.SHARED),
        eqTo(Some(ownerId))
      )(any)
    )
      .thenReturn(Success(Some(folderWithId)))
    when(
      folderRepository.getFolderAndChildrenSubfoldersWithResources(
        eqTo(folderUUID),
        eqTo(FolderStatus.SHARED),
        eqTo(Some(otherId))
      )(any)
    )
      .thenReturn(Success(Some(folderWithId)))

    when(userRepository.userWithFeideId(eqTo(ownerId))(any[DBSession])).thenReturn(
      Success(
        Some(
          MyNDLAUser(
            id = 1,
            feideId = ownerId,
            favoriteSubjects = Seq.empty,
            userRole = UserRole.EMPLOYEE,
            lastUpdated = TestData.today,
            organization = "lal",
            groups = Seq.empty,
            username = "username",
            displayName = "User Name",
            email = "user_name@example.com",
            arenaEnabled = true,
            arenaGroups = List.empty,
            shareName = false
          )
        )
      )
    )

    when(userRepository.userWithFeideId(eqTo(otherId))(any[DBSession])).thenReturn(
      Success(
        Some(
          MyNDLAUser(
            id = 2,
            feideId = otherId,
            favoriteSubjects = Seq.empty,
            userRole = UserRole.EMPLOYEE,
            lastUpdated = TestData.today,
            organization = "lal",
            groups = Seq.empty,
            username = "username",
            displayName = "User Name",
            email = "user_name@example.com",
            arenaEnabled = true,
            arenaGroups = List.empty,
            shareName = false
          )
        )
      )
    )

    service.getSharedFolder(folderUUID, Some(ownerId)) should be(Success(apiFolder))

    service.getSharedFolder(folderUUID, Some(otherId)) should be(
      Success(apiFolder.copy(resources = List(apiResource.copy(tags = List.empty))))
    )
  }

}
