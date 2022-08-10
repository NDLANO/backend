/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import no.ndla.learningpathapi.TestData._
import no.ndla.learningpathapi.model.api
import no.ndla.learningpathapi.model.domain
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.{UnitSuite, UnitTestEnvironment}
import scalikejdbc.DBSession

import java.time.LocalDateTime
import java.util.UUID
import scala.util.{Failure, Success}

class ReadServiceTest extends UnitSuite with UnitTestEnvironment {

  var service: ReadService = _

  val PUBLISHED_ID = 1
  val PRIVATE_ID   = 2

  val PUBLISHED_OWNER = UserInfo("published_owner", Set.empty)
  val PRIVATE_OWNER   = UserInfo("private_owner", Set.empty)
  val cruz            = Author("author", "Lyin' Ted")
  val license         = "publicdomain"
  val copyright       = Copyright(license, List(cruz))

  val PUBLISHED_LEARNINGPATH = LearningPath(
    Some(PUBLISHED_ID),
    Some(1),
    None,
    None,
    List(Title("Tittel", "nb")),
    List(),
    None,
    Some(1),
    LearningPathStatus.PUBLISHED,
    LearningPathVerificationStatus.EXTERNAL,
    LocalDateTime.now(),
    List(),
    PUBLISHED_OWNER.userId,
    copyright
  )

  val PRIVATE_LEARNINGPATH = LearningPath(
    Some(PRIVATE_ID),
    Some(1),
    None,
    None,
    List(Title("Tittel", "nb")),
    List(),
    None,
    Some(1),
    LearningPathStatus.PRIVATE,
    LearningPathVerificationStatus.EXTERNAL,
    LocalDateTime.now(),
    List(),
    PRIVATE_OWNER.userId,
    copyright
  )

  val STEP1 = LearningStep(
    Some(1),
    Some(1),
    None,
    None,
    1,
    List(Title("Tittel", "nb")),
    List(),
    List(),
    StepType.TEXT,
    None,
    showTitle = true,
    StepStatus.ACTIVE
  )

  val STEP2 = LearningStep(
    Some(2),
    Some(1),
    None,
    None,
    2,
    List(Title("Tittel", "nb")),
    List(),
    List(),
    StepType.TEXT,
    None,
    showTitle = false,
    StepStatus.ACTIVE
  )

  val STEP3 = LearningStep(
    Some(3),
    Some(1),
    None,
    None,
    3,
    List(Title("Tittel", "nb")),
    List(),
    List(),
    StepType.TEXT,
    None,
    showTitle = false,
    StepStatus.ACTIVE
  )

  override def beforeEach() = {
    service = new ReadService
    resetMocks()
    when(folderRepository.getSession(any)).thenReturn(mock[DBSession])
  }

  test("That withIdV2 returns None when id does not exist") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(None)
    val Failure(ex) = service.withIdV2(PUBLISHED_ID, "nb", false)
    ex.isInstanceOf[NotFoundException]
  }

  test("That withIdV2 returns a learningPath when the status is PUBLISHED") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    val learningPath = service.withIdV2(PUBLISHED_ID, "nb", false)
    assert(learningPath.isSuccess)
    assert(learningPath.get.id == PUBLISHED_ID)
    assert(learningPath.get.status == "PUBLISHED")
  }

  test("That withId returns a learningPath when the status is PUBLISHED and user is not the owner") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    val learningPath = service.withIdV2(PUBLISHED_ID, "nb", false, PRIVATE_OWNER)
    assert(learningPath.isSuccess)
    assert(learningPath.get.id == PUBLISHED_ID)
    assert(learningPath.get.status == "PUBLISHED")
  }

  test("That withId throws an AccessDeniedException when the status is PRIVATE and no user") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    val Failure(ex) = service.withIdV2(PRIVATE_ID, "nb", false)
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("That withId throws an AccessDeniedException when the status is PRIVATE and user is not the owner") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    val Failure(ex) = service.withIdV2(PRIVATE_ID, "nb", false, PUBLISHED_OWNER)
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("That withId returns a learningPath when the status is PRIVATE and user is the owner") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    val learningPath = service.withIdV2(PRIVATE_ID, "nb", false, PRIVATE_OWNER)
    assert(learningPath.isSuccess)
    assert(learningPath.get.id == PRIVATE_ID)
    assert(learningPath.get.status == "PRIVATE")
  }

  test("That statusFor returns None when id does not exist") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(None)
    val Failure(ex) = service.statusFor(PUBLISHED_ID)
    ex.isInstanceOf[NotFoundException]
  }

  test("That statusFor returns a LearningPathStatus when the status is PUBLISHED") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    assertResult("PUBLISHED") {
      service.statusFor(PUBLISHED_ID).map(_.status).get
    }
  }

  test("That statusFor throws an AccessDeniedException when the status is PRIVATE and no user") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))

    val Failure(ex) = service.statusFor(2)
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("That statusFor throws an AccessDeniedException when the status is PRIVATE and user is not the owner") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    val Failure(ex) = service.statusFor(2, PUBLISHED_OWNER)
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("That statusFor returns a LearningPathStatus when the status is PRIVATE and the user is the owner") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("PRIVATE") {
      service.statusFor(PRIVATE_ID, PRIVATE_OWNER).map(_.status).get
    }
  }

  test("That learningstepsFor returns None when the learningPath does not exist") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(None)
    val Failure(ex) = service.learningstepsForWithStatusV2(PUBLISHED_ID, StepStatus.ACTIVE, "nb", false)
    ex.isInstanceOf[NotFoundException]
  }

  test("That learningstepsFor returns None the learningPath does not have any learningsteps") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepsFor(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(List())
    val Failure(ex) = service.learningstepsForWithStatusV2(PUBLISHED_ID, StepStatus.ACTIVE, "nb", false)
    ex.isInstanceOf[NotFoundException]
  }

  test("That learningstepsFor returns only active steps when specifying status active") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepsFor(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(List(STEP1, STEP2.copy(status = StepStatus.DELETED), STEP3))
    val learningSteps = service.learningstepsForWithStatusV2(PUBLISHED_ID, StepStatus.ACTIVE, "nb", false)
    learningSteps.isSuccess should be(true)
    learningSteps.get.learningsteps.size should be(2)
    learningSteps.get.learningsteps.head.id should equal(STEP1.id.get)
    learningSteps.get.learningsteps.last.id should equal(STEP3.id.get)
  }

  test("That learningstepsFor returns only deleted steps when specifying status deleted") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepsFor(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(List(STEP1, STEP2.copy(status = StepStatus.DELETED), STEP3))
    val learningSteps = service.learningstepsForWithStatusV2(PUBLISHED_ID, StepStatus.DELETED, "nb", false)
    learningSteps.isSuccess should be(true)
    learningSteps.get.learningsteps.size should be(1)
    learningSteps.get.learningsteps.head.id should equal(STEP2.id.get)
  }

  test("That learningstepsFor throws an AccessDeniedException when the status is PRIVATE and no user") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    val Failure(ex) = service.learningstepsForWithStatusV2(PRIVATE_ID, StepStatus.ACTIVE, "nb", false)
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("That learningstepsFor throws an AccessDeniedException when the status is PRIVATE and user is not the owner") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    val Failure(ex) = service.learningstepsForWithStatusV2(PRIVATE_ID, StepStatus.ACTIVE, "nb", false, PUBLISHED_OWNER)
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test(
    "That learningstepsFor returns all learningsteps for a learningpath when the status is PRIVATE and the user is the owner"
  ) {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepsFor(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(List(STEP1, STEP2))
    assertResult(2) {
      service
        .learningstepsForWithStatusV2(PRIVATE_ID, StepStatus.ACTIVE, "nb", false, PRIVATE_OWNER)
        .get
        .learningsteps
        .length
    }
  }

  test("That learningstepV2For returns None when the learningPath does not exist") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(None)
    val Failure(ex) = service.learningstepV2For(PUBLISHED_ID, STEP1.id.get, "nb", false)
    ex.isInstanceOf[NotFoundException]
  }

  test("That learningstepV2For returns None when the learningStep does not exist") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PUBLISHED_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(None)
    val Failure(ex) = service.learningstepV2For(PUBLISHED_ID, STEP1.id.get, "nb", false)
    ex.isInstanceOf[NotFoundException]
  }

  test("That learningstepV2For returns the LearningStep when it exists") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PUBLISHED_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(Some(STEP1))
    assertResult(STEP1.id.get) {
      service.learningstepV2For(PUBLISHED_ID, STEP1.id.get, "nb", false).get.id
    }
  }

  test("That learningstepV2For returns the LearningStep when it exists and status is PRIVATE and user is the owner") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(Some(STEP1))
    assertResult(STEP1.id.get) {
      service
        .learningstepV2For(PRIVATE_ID, STEP1.id.get, "nb", false, PRIVATE_OWNER)
        .get
        .id
    }
  }

  test("That learningstepV2For throws an AccessDeniedException when the status is PRIVATE and no user") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    val Failure(ex) = service.learningstepV2For(PRIVATE_ID, STEP1.id.get, "nb", false)
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("That learningstepFor throws an AccessDeniedException when the status is PRIVATE and user is not the owner") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    val Failure(ex) = service.learningstepV2For(PRIVATE_ID, STEP1.id.get, "nb", false, PUBLISHED_OWNER)
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("That getFolder returns folder and its data when FEIDE ID does not match but the Folder is Public") {
    val created        = clock.now()
    val mainFolderUUID = UUID.randomUUID()
    val subFolder1UUID = UUID.randomUUID()
    val subFolder2UUID = UUID.randomUUID()
    val resource1UUID  = UUID.randomUUID()

    val mainFolder = domain.Folder(
      id = mainFolderUUID,
      feideId = "FEIDE",
      parentId = None,
      name = "mainFolder",
      status = FolderStatus.PUBLIC,
      isFavorite = false,
      subfolders = List.empty,
      resources = List.empty
    )

    val subFolder1 = domain.Folder(
      id = subFolder1UUID,
      feideId = "",
      parentId = Some(mainFolderUUID),
      name = "subFolder1",
      status = FolderStatus.PUBLIC,
      isFavorite = false,
      subfolders = List.empty,
      resources = List.empty
    )

    val subFolder2 = domain.Folder(
      id = subFolder2UUID,
      feideId = "",
      parentId = Some(mainFolderUUID),
      name = "subFolder2",
      status = FolderStatus.PRIVATE,
      isFavorite = false,
      subfolders = List.empty,
      resources = List.empty
    )

    val resource1 = domain.Resource(
      id = resource1UUID,
      feideId = "",
      resourceType = "article",
      path = "/subject/1/topic/1/resource/4",
      created = created,
      tags = List.empty,
      resourceId = 1
    )

    val expected = api.Folder(
      id = mainFolderUUID.toString,
      name = "mainFolder",
      status = "public",
      isFavorite = false,
      breadcrumbs = List(api.Breadcrumb(id = mainFolderUUID.toString, name = "mainFolder")),
      parentId = None,
      resources = List(
        api.Resource(
          id = resource1UUID.toString,
          resourceType = "article",
          tags = List.empty,
          path = "/subject/1/topic/1/resource/4",
          created = created,
          resourceId = 1
        )
      ),
      subfolders = List(
        api.Folder(
          id = subFolder1UUID.toString,
          name = "subFolder1",
          status = "public",
          subfolders = List.empty,
          resources = List.empty,
          isFavorite = false,
          breadcrumbs = List(
            api.Breadcrumb(id = mainFolderUUID.toString, name = "mainFolder"),
            api.Breadcrumb(id = subFolder1UUID.toString, name = "subFolder1")
          ),
          parentId = Some(mainFolderUUID.toString)
        ),
        api.Folder(
          id = subFolder2UUID.toString,
          name = "subFolder2",
          status = "private",
          resources = List.empty,
          subfolders = List.empty,
          isFavorite = false,
          breadcrumbs = List(
            api.Breadcrumb(id = mainFolderUUID.toString, name = "mainFolder"),
            api.Breadcrumb(id = subFolder2UUID.toString, name = "subFolder2")
          ),
          parentId = Some(mainFolderUUID.toString)
        )
      )
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

    when(feideApiClient.getUserFeideID(any)).thenReturn(Success("wrong"))
    when(folderRepository.folderWithId(eqTo(mainFolderUUID))(any)).thenReturn(Success(mainFolder))
    when(folderRepository.foldersWithParentID(eqTo(Some(mainFolderUUID)))(any))
      .thenReturn(Success(List(subFolder1, subFolder2)))
    when(folderRepository.foldersWithParentID(eqTo(Some(subFolder1UUID)))(any)).thenReturn(Success(List.empty))
    when(folderRepository.foldersWithParentID(eqTo(Some(subFolder2UUID)))(any)).thenReturn(Success(List.empty))
    when(folderRepository.getFolderResources(eqTo(mainFolderUUID))(any)).thenReturn(Success(List(resource1)))
    when(folderRepository.getFolderResources(eqTo(subFolder1UUID))(any)).thenReturn(Success(List.empty))
    when(folderRepository.getFolderResources(eqTo(subFolder2UUID))(any)).thenReturn(Success(List.empty))
    when(folderRepository.getFolderAndChildrenSubfoldersWithResources(any)(any)).thenReturn(Success(Some(whgaterh)))

    val result = service.getSingleFolder(mainFolderUUID, true, true, None)
    result should be(Success(expected))
  }

  test("That user with no access doesn't get the treat") {
    val mainFolderUUID = UUID.randomUUID()

    when(feideApiClient.getUserFeideID(any)).thenReturn(Success("not daijoubu"))
    when(folderRepository.folderWithId(eqTo(mainFolderUUID))(any)).thenReturn(Success(emptyDomainFolder))
    when(folderRepository.getFolderAndChildrenSubfolders(any)(any)).thenReturn(Success(Some(emptyDomainFolder)))

    val result = service.getSingleFolder(mainFolderUUID, true, false, None)
    result should be(Failure(AccessDeniedException("You do not have access to this entity.")))
    verify(folderRepository, times(0)).foldersWithParentID(any)(any)
    verify(folderRepository, times(0)).getFolderResources(any)(any)
  }

  test("That getFolders creates favorite folder if favorite does not exist ") {
    val feideId              = "yee boiii"
    val folderUUID           = UUID.randomUUID()
    val favoriteUUID         = UUID.randomUUID()
    val folderWithId         = emptyDomainFolder.copy(id = folderUUID)
    val favoriteDomainFolder = folderWithId.copy(id = favoriteUUID, name = "favorite", isFavorite = true)
    val favoriteApiFolder =
      emptyApiFolder.copy(
        id = favoriteUUID.toString,
        name = "favorite",
        status = "private",
        isFavorite = true,
        breadcrumbs = List(api.Breadcrumb(id = favoriteUUID.toString, name = "favorite"))
      )

    when(feideApiClient.getUserFeideID(Some("token"))).thenReturn(Success(feideId))
    when(folderRepository.insertFolder(any, any, any)(any)).thenReturn(Success(favoriteDomainFolder))
    when(folderRepository.foldersWithFeideAndParentID(eqTo(None), eqTo(feideId))(any))
      .thenReturn(Success(List(folderWithId, folderWithId)))
    when(folderRepository.folderWithId(eqTo(folderUUID))(any)).thenReturn(Success(folderWithId))
    when(folderRepository.folderWithId(eqTo(favoriteUUID))(any)).thenReturn(Success(favoriteDomainFolder))

    val result = service.getFolders(false, false, Some("token"))
    result.get.length should be(3)
    result.get.find(_.name == "favorite").get should be(favoriteApiFolder)

    verify(folderRepository, times(1)).foldersWithFeideAndParentID(eqTo(None), eqTo(feideId))(any)
    verify(folderRepository, times(1)).insertFolder(any, any, any)(any)
  }

  test("That getFolders includes resources for the top folders when includeResources flag is set to true") {
    val created = clock.now()
    when(clock.now()).thenReturn(created)

    val feideId              = "yee boiii"
    val favoriteId           = UUID.randomUUID()
    val resourceId           = UUID.randomUUID()
    val folderId             = UUID.randomUUID()
    val folderWithId         = emptyDomainFolder.copy(id = folderId)
    val favoriteDomainFolder = folderWithId.copy(id = favoriteId, name = "favorite", isFavorite = true)
    val domainResource       = emptyDomainResource.copy(id = resourceId, created = created)
    val favoriteApiFolder =
      emptyApiFolder.copy(
        id = favoriteId.toString,
        name = "favorite",
        status = "private",
        isFavorite = true,
        breadcrumbs = List(api.Breadcrumb(id = favoriteId.toString, name = "favorite"))
      )
    val apiResource =
      api.Resource(
        id = resourceId.toString,
        resourceType = "",
        path = "",
        created = created,
        tags = List.empty,
        resourceId = 1
      )
    val folderResourcesResponse1 = Success(List(domainResource, domainResource))
    val folderResourcesResponse2 = Success(List(domainResource))
    val folderResourcesResponse3 = Success(List.empty)

    when(feideApiClient.getUserFeideID(Some("token"))).thenReturn(Success(feideId))
    when(folderRepository.foldersWithFeideAndParentID(eqTo(None), eqTo(feideId))(any))
      .thenReturn(Success(List(folderWithId, folderWithId)))

    when(folderRepository.folderWithId(eqTo(folderWithId.id))(any)).thenReturn(Success(folderWithId))
    when(folderRepository.folderWithId(eqTo(favoriteId))(any)).thenReturn(Success(favoriteDomainFolder))

    when(folderRepository.insertFolder(any, any, any)(any)).thenReturn(Success(favoriteDomainFolder))
    when(folderRepository.getFolderResources(any)(any))
      .thenReturn(folderResourcesResponse1, folderResourcesResponse2, folderResourcesResponse3)

    val result = service.getFolders(includeSubfolders = false, includeResources = true, Some("token"))
    result.get.length should be(3)
    result.get.find(e => e.name == "favorite").get should be(
      favoriteApiFolder.copy(resources = List(apiResource, apiResource))
    )

    verify(folderRepository, times(1)).foldersWithFeideAndParentID(eqTo(None), eqTo(feideId))(any)
    verify(folderRepository, times(1)).insertFolder(any, any, any)(any)
    verify(folderRepository, times(3)).getFolderResources(any)(any)
  }

  test("That getSharedFolder returns a folder if the status is shared or public") {
    val folderUUID    = UUID.randomUUID()
    val folderWithId  = emptyDomainFolder.copy(id = folderUUID, status = FolderStatus.SHARED)
    val folderWithId2 = folderWithId.copy(status = FolderStatus.PUBLIC)
    val apiFolder =
      emptyApiFolder.copy(
        id = folderUUID.toString,
        name = "",
        status = "shared",
        isFavorite = false,
        breadcrumbs = List(api.Breadcrumb(id = folderUUID.toString, name = ""))
      )
    val apiFolder2 = apiFolder.copy(status = "public")

    when(folderRepository.getFolderAndChildrenSubfoldersWithResources(eqTo(folderUUID))(any))
      .thenReturn(Success(Some(folderWithId)), Success(Some(folderWithId2)))

    service.getSharedFolder(folderUUID) should be(Success(apiFolder))
    service.getSharedFolder(folderUUID) should be(Success(apiFolder2))
  }

  test("That getSharedFolder returns a Failure Not Found if the status is not shared or public") {
    val folderUUID   = UUID.randomUUID()
    val folderWithId = emptyDomainFolder.copy(id = folderUUID, status = FolderStatus.PRIVATE)

    when(folderRepository.getFolderAndChildrenSubfoldersWithResources(eqTo(folderUUID))(any))
      .thenReturn(Success(Some(folderWithId)))

    val Failure(result: NotFoundException) = service.getSharedFolder(folderUUID)
    result.message should be("Folder does not exist")
  }
}
