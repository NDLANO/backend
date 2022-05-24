/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import no.ndla.learningpathapi.TestData._
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

  test("That getting a folder returns folder, subFolders and resources") {
    val mainFolder = emptyDomainFolder.copy(
      id = Some(1),
      feideId = Some("FEIDE"),
      name = "mainFolder",
      status = FolderStatus.PRIVATE,
      data = List.empty
    )

    val subFolder1 = emptyDomainFolder.copy(
      id = Some(2),
      parentId = Some(1),
      name = "subFolder1",
      status = FolderStatus.PUBLIC,
      data = List.empty
    )

    val subFolder2 = emptyDomainFolder.copy(
      id = Some(3),
      parentId = Some(1),
      name = "subFolder2",
      status = FolderStatus.PRIVATE,
      data = List.empty
    )

    val resource1 = emptyDomainResource.copy(
      id = Some(13),
      resourceType = "article",
      path = "/subject/1/topic/1/resource/4",
      tags = List.empty
    )

    val expected = emptyApiFolder.copy(
      id = 1,
      name = "mainFolder",
      status = "private",
      data = List(
        Right(
          emptyApiResource.copy(id = 42, resourceType = "article", tags = List.empty)
        ),
        Left(
          emptyApiFolder.copy(id = 2, name = "subFolder1", status = "public", data = List.empty)
        ),
        Left(
          emptyApiFolder.copy(id = 3, name = "subFolder2", status = "private", data = List.empty)
        )
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

    val Success(result) = service.getFolder(1, false)
    result should be(expected)
    verify(folderRepository, times(3)).foldersWithParentID(any)
    verify(folderRepository, times(3)).getFolderResources(any)(any[DBSession])
  }

  test("That getFolder returns folder and its data when FEIDE ID does not match but the Folder is Public") {
    val mainFolder = emptyDomainFolder.copy(
      id = Some(1),
      feideId = Some("FEIDE"),
      name = "mainFolder",
      status = FolderStatus.PUBLIC,
      data = List.empty
    )

    val subFolder1 = emptyDomainFolder.copy(
      id = Some(2),
      parentId = Some(1),
      name = "subFolder1",
      status = FolderStatus.PUBLIC,
      data = List.empty
    )

    val subFolder2 = emptyDomainFolder.copy(
      id = Some(3),
      parentId = Some(1),
      name = "subFolder2",
      status = FolderStatus.PRIVATE,
      data = List.empty
    )

    val resource1 = emptyDomainResource.copy(
      id = Some(13),
      resourceType = "article",
      path = "/subject/1/topic/1/resource/4",
      tags = List.empty
    )

    val expected = emptyApiFolder.copy(
      id = 1,
      name = "mainFolder",
      status = "public",
      data = List(
        Right(
          emptyApiResource.copy(id = 42, resourceType = "article", tags = List.empty)
        ),
        Left(
          emptyApiFolder.copy(id = 2, name = "subFolder1", status = "public", data = List.empty)
        ),
        Left(
          emptyApiFolder.copy(id = 3, name = "subFolder2", status = "private", data = List.empty)
        )
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

    val Success(result) = service.getFolder(1, false)
    result should be(expected)
    verify(folderRepository, times(3)).foldersWithParentID(any)
    verify(folderRepository, times(3)).getFolderResources(any)(any[DBSession])
  }

  test("That setting excludeResources to true returns only folder and subFolders") {
    val mainFolder = emptyDomainFolder.copy(
      id = Some(1),
      feideId = Some("FEIDE"),
      name = "mainFolder",
      status = FolderStatus.PRIVATE,
      data = List.empty
    )

    val subFolder1 = emptyDomainFolder.copy(
      id = Some(2),
      parentId = Some(1),
      name = "subFolder1",
      status = FolderStatus.PUBLIC,
      data = List.empty
    )

    val subFolder2 = emptyDomainFolder.copy(
      id = Some(3),
      parentId = Some(1),
      name = "subFolder2",
      status = FolderStatus.PRIVATE,
      data = List.empty
    )

    val expected = emptyApiFolder.copy(
      id = 1,
      name = "mainFolder",
      status = "private",
      data = List(
        Left(
          emptyApiFolder.copy(id = 2, name = "subFolder1", status = "public", data = List.empty)
        ),
        Left(
          emptyApiFolder.copy(id = 3, name = "subFolder2", status = "private", data = List.empty)
        )
      )
    )

    when(feideApiClient.getUserFeideID(any)).thenReturn(Success("FEIDE"))
    when(folderRepository.folderWithId(1)).thenReturn(Success(mainFolder))
    when(folderRepository.foldersWithParentID(Some(1))).thenReturn(Success(List(subFolder1, subFolder2)))
    when(folderRepository.foldersWithParentID(Some(2))).thenReturn(Success(List.empty))
    when(folderRepository.foldersWithParentID(Some(3))).thenReturn(Success(List.empty))

    val Success(result) = service.getFolder(1, true)
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

  test("That domainToApimodel transforms Folder from domain to api model correctly") {
    val folderDomainList = List(
      emptyDomainFolder.copy(id = Some(1)),
      emptyDomainFolder.copy(id = Some(2)),
      emptyDomainFolder.copy(id = Some(3))
    )

    val result = service.domainToApiModel(folderDomainList, converterService.toApiFolder)
    result.get.length should be(3)
    result should be(
      Success(
        List(
          emptyApiFolder.copy(id = 1, status = "private"),
          emptyApiFolder.copy(id = 2, status = "private"),
          emptyApiFolder.copy(id = 3, status = "private")
        )
      )
    )
  }

}
