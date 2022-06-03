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

import java.util.Date
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
    new Date(),
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
    new Date(),
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

  test("That getFolder returns folder, subFolders and resources") {
    val created = clock.now()
    val mainFolder = domain.Folder(
      id = Some(1),
      feideId = "FEIDE",
      parentId = None,
      name = "mainFolder",
      status = FolderStatus.PRIVATE,
      isFavorite = false,
      data = List.empty
    )

    val subFolder1 = domain.Folder(
      id = Some(2),
      feideId = "",
      parentId = Some(1),
      name = "subFolder1",
      status = FolderStatus.PUBLIC,
      isFavorite = false,
      data = List.empty
    )

    val subFolder2 = domain.Folder(
      id = Some(3),
      feideId = "",
      parentId = Some(1),
      name = "subFolder2",
      status = FolderStatus.PRIVATE,
      isFavorite = false,
      data = List.empty
    )

    val resource1 = domain.Resource(
      id = Some(13),
      feideId = "",
      resourceType = "article",
      path = "/subject/1/topic/1/resource/4",
      created = created,
      tags = List.empty
    )

    val expected = api.Folder(
      id = 1,
      name = "mainFolder",
      status = "private",
      isFavorite = false,
      data = List(
        api.Resource(
          id = 13,
          resourceType = "article",
          tags = List.empty,
          path = "/subject/1/topic/1/resource/4",
          created = created
        ),
        api.Folder(id = 2, name = "subFolder1", status = "public", data = List.empty, isFavorite = false),
        api.Folder(id = 3, name = "subFolder2", status = "private", data = List.empty, isFavorite = false)
      )
    )

    when(feideApiClient.getUserFeideID(any)).thenReturn(Success("FEIDE"))
    when(folderRepository.folderWithId(1)).thenReturn(Success(mainFolder))
    when(folderRepository.foldersWithParentID(Some(1))).thenReturn(Success(List(subFolder1, subFolder2)))
    when(folderRepository.foldersWithParentID(Some(2))).thenReturn(Success(List.empty))
    when(folderRepository.foldersWithParentID(Some(3))).thenReturn(Success(List.empty))
    when(folderRepository.getFolderResources(1)).thenReturn(Success(List(resource1)))
    when(folderRepository.getFolderResources(2)).thenReturn(Success(List.empty))
    when(folderRepository.getFolderResources(3)).thenReturn(Success(List.empty))

    val Success(result) = service.getFolder(1, true)
    result should be(expected)
    verify(folderRepository, times(3)).foldersWithParentID(any)
    verify(folderRepository, times(3)).getFolderResources(any)(any[DBSession])
  }

  test("That getFolder returns folder and its data when FEIDE ID does not match but the Folder is Public") {
    val created = clock.now()
    val mainFolder = domain.Folder(
      id = Some(1),
      feideId = "FEIDE",
      parentId = None,
      name = "mainFolder",
      status = FolderStatus.PUBLIC,
      isFavorite = false,
      data = List.empty
    )

    val subFolder1 = domain.Folder(
      id = Some(2),
      feideId = "",
      parentId = Some(1),
      name = "subFolder1",
      status = FolderStatus.PUBLIC,
      isFavorite = false,
      data = List.empty
    )

    val subFolder2 = domain.Folder(
      id = Some(3),
      feideId = "",
      parentId = Some(1),
      name = "subFolder2",
      status = FolderStatus.PRIVATE,
      isFavorite = false,
      data = List.empty
    )

    val resource1 = domain.Resource(
      id = Some(13),
      feideId = "",
      resourceType = "article",
      path = "/subject/1/topic/1/resource/4",
      created = created,
      tags = List.empty
    )

    val expected = api.Folder(
      id = 1,
      name = "mainFolder",
      status = "public",
      isFavorite = false,
      data = List(
        api.Resource(
          id = 13,
          resourceType = "article",
          tags = List.empty,
          path = "/subject/1/topic/1/resource/4",
          created = created
        ),
        api.Folder(id = 2, name = "subFolder1", status = "public", data = List.empty, isFavorite = false),
        api.Folder(id = 3, name = "subFolder2", status = "private", data = List.empty, isFavorite = false)
      )
    )

    when(feideApiClient.getUserFeideID(any)).thenReturn(Success("wrong"))
    when(folderRepository.folderWithId(1)).thenReturn(Success(mainFolder))
    when(folderRepository.foldersWithParentID(Some(1))).thenReturn(Success(List(subFolder1, subFolder2)))
    when(folderRepository.foldersWithParentID(Some(2))).thenReturn(Success(List.empty))
    when(folderRepository.foldersWithParentID(Some(3))).thenReturn(Success(List.empty))
    when(folderRepository.getFolderResources(1)).thenReturn(Success(List(resource1)))
    when(folderRepository.getFolderResources(2)).thenReturn(Success(List.empty))
    when(folderRepository.getFolderResources(3)).thenReturn(Success(List.empty))

    val Success(result) = service.getFolder(1, true)
    result should be(expected)
    verify(folderRepository, times(3)).foldersWithParentID(any)
    verify(folderRepository, times(3)).getFolderResources(any)(any[DBSession])
  }

  test("That setting includeResources to false in getFolders returns only folder and subFolders") {
    val mainFolder = domain.Folder(
      id = Some(1),
      feideId = "FEIDE",
      parentId = None,
      name = "mainFolder",
      status = FolderStatus.PRIVATE,
      isFavorite = false,
      data = List.empty
    )

    val subFolder1 = domain.Folder(
      id = Some(2),
      feideId = "",
      parentId = Some(1),
      name = "subFolder1",
      status = FolderStatus.PUBLIC,
      isFavorite = false,
      data = List.empty
    )

    val subFolder2 = domain.Folder(
      id = Some(3),
      feideId = "",
      parentId = Some(1),
      name = "subFolder2",
      status = FolderStatus.PRIVATE,
      isFavorite = false,
      data = List.empty
    )

    val expected = api.Folder(
      id = 1,
      name = "mainFolder",
      status = "private",
      isFavorite = false,
      data = List(
        api.Folder(id = 2, name = "subFolder1", status = "public", data = List.empty, isFavorite = false),
        api.Folder(id = 3, name = "subFolder2", status = "private", data = List.empty, isFavorite = false)
      )
    )

    when(feideApiClient.getUserFeideID(any)).thenReturn(Success("FEIDE"))
    when(folderRepository.folderWithId(1)).thenReturn(Success(mainFolder))
    when(folderRepository.foldersWithParentID(Some(1))).thenReturn(Success(List(subFolder1, subFolder2)))
    when(folderRepository.foldersWithParentID(Some(2))).thenReturn(Success(List.empty))
    when(folderRepository.foldersWithParentID(Some(3))).thenReturn(Success(List.empty))

    val Success(result) = service.getFolder(1, false)
    result should be(expected)
    verify(folderRepository, times(3)).foldersWithParentID(any)
    verify(folderRepository, times(0)).getFolderResources(any)(any[DBSession])
  }

  test("That user with no access doesn't get the treat") {
    when(feideApiClient.getUserFeideID(any)).thenReturn(Success("not daijoubu"))
    when(folderRepository.folderWithId(1)).thenReturn(Success(emptyDomainFolder))

    val result = service.getFolder(1, false)
    result.isFailure should be(true)
    verify(folderRepository, times(0)).foldersWithParentID(any)
    verify(folderRepository, times(0)).getFolderResources(any)(any[DBSession])
  }

  test("That getFolders returns favorite folder if it exist") {
    val feideId              = "yee boiii"
    val folderWithId         = emptyDomainFolder.copy(id = Some(1))
    val favoriteDomainFolder = folderWithId.copy(name = "favorite", isFavorite = true)
    val favoriteApiFolder    = emptyApiFolder.copy(id = 1, name = "favorite", status = "private", isFavorite = true)

    when(feideApiClient.getUserFeideID(Some("token"))).thenReturn(Success(feideId))
    when(folderRepository.foldersWithFeideAndParentID(None, feideId))
      .thenReturn(Success(List(folderWithId, folderWithId, favoriteDomainFolder)))

    val result = service.getFolders(false, false, Some("token"))
    result.isSuccess should be(true)
    result.get.length should be(3)
    result.get.find(e => e.name.equals("favorite")).get should be(favoriteApiFolder)

    verify(folderRepository, times(1)).foldersWithFeideAndParentID(None, feideId)
    verify(folderRepository, times(0)).insertFolder(any)(any[DBSession])
  }

  test("That getFolders creates favorite folder if favorite does not exist ") {
    val feideId              = "yee boiii"
    val folderWithId         = emptyDomainFolder.copy(id = Some(1))
    val favoriteDomainFolder = folderWithId.copy(name = "favorite", isFavorite = true)
    val favoriteApiFolder    = emptyApiFolder.copy(id = 1, name = "favorite", status = "private", isFavorite = true)

    when(feideApiClient.getUserFeideID(Some("token"))).thenReturn(Success(feideId))
    when(folderRepository.insertFolder(any)(any[DBSession])).thenReturn(Success(favoriteDomainFolder))
    when(folderRepository.foldersWithFeideAndParentID(None, feideId))
      .thenReturn(Success(List(folderWithId, folderWithId)))

    val result = service.getFolders(false, false, Some("token"))
    result.isSuccess should be(true)
    result.get.length should be(3)
    result.get.find(e => e.name.equals("favorite")).get should be(favoriteApiFolder)

    verify(folderRepository, times(1)).foldersWithFeideAndParentID(None, feideId)
    verify(folderRepository, times(1)).insertFolder(any)(any[DBSession])
  }

  test("That getFolders includes resources for the top folders when includeResources flag is set to true") {
    val feideId              = "yee boiii"
    val folderWithId         = emptyDomainFolder.copy(id = Some(1))
    val favoriteDomainFolder = folderWithId.copy(name = "favorite", isFavorite = true)
    val domainResource       = emptyDomainResource.copy(id = Some(1))
    val favoriteApiFolder    = emptyApiFolder.copy(id = 1, name = "favorite", status = "private", isFavorite = true)
    val apiResource          = api.Resource(id = 1, resourceType = "", path = "", created = today, tags = List.empty)
    val folderResourcesResponse1 = Success(List(domainResource, domainResource))
    val folderResourcesResponse2 = Success(List(domainResource))
    val folderResourcesResponse3 = Success(List.empty)

    when(feideApiClient.getUserFeideID(Some("token"))).thenReturn(Success(feideId))
    when(folderRepository.foldersWithFeideAndParentID(None, feideId))
      .thenReturn(Success(List(folderWithId, folderWithId)))
    when(folderRepository.insertFolder(any)(any[DBSession])).thenReturn(Success(favoriteDomainFolder))
    when(folderRepository.getFolderResources(any)(any[DBSession]))
      .thenReturn(folderResourcesResponse1, folderResourcesResponse2, folderResourcesResponse3)

    val result = service.getFolders(false, true, Some("token"))
    result.isSuccess should be(true)
    result.get.length should be(3)
    result.get.find(e => e.name.equals("favorite")).get should be(
      favoriteApiFolder.copy(data = List(apiResource, apiResource))
    )
    result.get.exists(f => f.data.length.equals(2))
    result.get.exists(f => f.data.length.equals(1))
    result.get.exists(f => f.data.isEmpty)

    verify(folderRepository, times(1)).foldersWithFeideAndParentID(None, feideId)
    verify(folderRepository, times(1)).insertFolder(any)(any[DBSession])
    verify(folderRepository, times(3)).getFolderResources(any)(any[DBSession])
  }

  test("That getFolders includes resources and subfolders when both flag are set to true") {
    val feideId              = "yee boiii"
    val folderId1            = emptyDomainFolder.copy(id = Some(1))
    val folderId2            = emptyDomainFolder.copy(id = Some(2))
    val folderId3            = emptyDomainFolder.copy(id = Some(3))
    val folderId4            = emptyDomainFolder.copy(id = Some(4))
    val favoriteDomainFolder = folderId1.copy(id = Some(5), name = "favorite", isFavorite = true)
    val domainResource       = emptyDomainResource.copy(id = Some(1))
    val favoriteApiFolder    = emptyApiFolder.copy(id = 5, name = "favorite", status = "private", isFavorite = true)
    val apiResource          = api.Resource(id = 1, resourceType = "", path = "", created = today, tags = List.empty)
    val folderResourcesResponse1 = Success(List(domainResource, domainResource))
    val folderResourcesResponse2 = Success(List(domainResource))
    val folderResourcesResponse3 = Success(List.empty)

    when(feideApiClient.getUserFeideID(Some("token"))).thenReturn(Success(feideId))
    when(folderRepository.foldersWithFeideAndParentID(None, feideId))
      .thenReturn(Success(List(folderId1, folderId2)))
    when(folderRepository.foldersWithParentID(Some(1))).thenReturn(Success(List(folderId3)))
    when(folderRepository.foldersWithParentID(Some(2))).thenReturn(Success(List(folderId4)))
    when(folderRepository.foldersWithParentID(Some(3))).thenReturn(Success(List.empty))
    when(folderRepository.foldersWithParentID(Some(4))).thenReturn(Success(List.empty))
    when(folderRepository.foldersWithParentID(Some(5))).thenReturn(Success(List.empty))
    when(folderRepository.insertFolder(any)(any[DBSession])).thenReturn(Success(favoriteDomainFolder))
    when(folderRepository.getFolderResources(eqTo(1))(any[DBSession])).thenReturn(folderResourcesResponse1)
    when(folderRepository.getFolderResources(eqTo(2))(any[DBSession])).thenReturn(folderResourcesResponse2)
    when(folderRepository.getFolderResources(eqTo(3))(any[DBSession])).thenReturn(folderResourcesResponse3)
    when(folderRepository.getFolderResources(eqTo(4))(any[DBSession])).thenReturn(folderResourcesResponse3)
    when(folderRepository.getFolderResources(eqTo(5))(any[DBSession])).thenReturn(folderResourcesResponse1)

    val result = service.getFolders(true, true, Some("token"))
    result.isSuccess should be(true)
    result.get.length should be(3)
    result.get.find(e => e.name.equals("favorite")).get should be(
      favoriteApiFolder.copy(data = List(apiResource, apiResource))
    )

    verify(folderRepository, times(1)).foldersWithFeideAndParentID(eqTo(None), eqTo(feideId))
    verify(folderRepository, times(5)).foldersWithParentID(any)
    verify(folderRepository, times(1)).foldersWithParentID(eqTo(Some(1)))
    verify(folderRepository, times(1)).foldersWithParentID(eqTo(Some(2)))
    verify(folderRepository, times(1)).foldersWithParentID(eqTo(Some(3)))
    verify(folderRepository, times(1)).foldersWithParentID(eqTo(Some(4)))
    verify(folderRepository, times(1)).foldersWithParentID(eqTo(Some(5)))
    verify(folderRepository, times(5)).getFolderResources(any)(any[DBSession])
    verify(folderRepository, times(1)).getFolderResources(eqTo(1))(any[DBSession])
    verify(folderRepository, times(1)).getFolderResources(eqTo(2))(any[DBSession])
    verify(folderRepository, times(1)).getFolderResources(eqTo(3))(any[DBSession])
    verify(folderRepository, times(1)).getFolderResources(eqTo(4))(any[DBSession])
    verify(folderRepository, times(1)).getFolderResources(eqTo(5))(any[DBSession])
    verify(folderRepository, times(1)).insertFolder(any)(any[DBSession])
  }
}
