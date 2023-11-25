/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import no.ndla.common.errors.AccessDeniedException
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.learningpath.LearningpathCopyright
import no.ndla.common.model.domain.{Author, Title}
import no.ndla.learningpathapi.TestData._
import no.ndla.learningpathapi.model.api.{MyNDLAGroup, Owner, Stats}
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.model.{api, domain}
import no.ndla.learningpathapi.{UnitSuite, UnitTestEnvironment}
import no.ndla.network.clients.{FeideExtendedUserInfo, FeideGroup, Membership}
import no.ndla.network.tapir.auth.TokenUser
import scalikejdbc.DBSession

import java.util.UUID
import scala.collection.immutable.Seq
import scala.util.{Failure, Success}

class ReadServiceTest extends UnitSuite with UnitTestEnvironment {

  var service: ReadService = _

  val PUBLISHED_ID = 1L
  val PRIVATE_ID   = 2L

  val PUBLISHED_OWNER: TokenUser       = TokenUser("published_owner", Set.empty, None)
  val PRIVATE_OWNER: TokenUser         = TokenUser("private_owner", Set.empty, None)
  val cruz: Author                     = Author("author", "Lyin' Ted")
  val license                          = "publicdomain"
  val copyright: LearningpathCopyright = LearningpathCopyright(license, List(cruz))

  val PUBLISHED_LEARNINGPATH: LearningPath = LearningPath(
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
    NDLADate.now(),
    List(),
    PUBLISHED_OWNER.id,
    copyright
  )

  val PRIVATE_LEARNINGPATH: LearningPath = LearningPath(
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
    NDLADate.now(),
    List(),
    PRIVATE_OWNER.id,
    copyright
  )

  val STEP1: LearningStep = LearningStep(
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

  val STEP2: LearningStep = LearningStep(
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

  val STEP3: LearningStep = LearningStep(
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

  override def beforeEach(): Unit = {
    service = new ReadService
    resetMocks()
    when(folderRepository.getSession(any)).thenReturn(mock[DBSession])
  }

  test("That withIdV2 returns None when id does not exist") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(None)
    val Failure(ex) = service.withIdV2(PUBLISHED_ID, "nb", fallback = false, TokenUser.PublicUser)
    ex.isInstanceOf[NotFoundException]
  }

  test("That withIdV2 returns a learningPath when the status is PUBLISHED") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    val learningPath = service.withIdV2(PUBLISHED_ID, "nb", fallback = false, TokenUser.PublicUser)
    assert(learningPath.isSuccess)
    assert(learningPath.get.id == PUBLISHED_ID)
    assert(learningPath.get.status == "PUBLISHED")
  }

  test("That withId returns a learningPath when the status is PUBLISHED and user is not the owner") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    val learningPath = service.withIdV2(PUBLISHED_ID, "nb", fallback = false, PRIVATE_OWNER)
    assert(learningPath.isSuccess)
    assert(learningPath.get.id == PUBLISHED_ID)
    assert(learningPath.get.status == "PUBLISHED")
  }

  test("That withId throws an AccessDeniedException when the status is PRIVATE and no user") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    val Failure(ex) = service.withIdV2(PRIVATE_ID, "nb", fallback = false, TokenUser.PublicUser)
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("That withId throws an AccessDeniedException when the status is PRIVATE and user is not the owner") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    val Failure(ex) = service.withIdV2(PRIVATE_ID, "nb", fallback = false, PUBLISHED_OWNER)
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("That withId returns a learningPath when the status is PRIVATE and user is the owner") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    val learningPath = service.withIdV2(PRIVATE_ID, "nb", fallback = false, PRIVATE_OWNER)
    assert(learningPath.isSuccess)
    assert(learningPath.get.id == PRIVATE_ID)
    assert(learningPath.get.status == "PRIVATE")
  }

  test("That statusFor returns None when id does not exist") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(None)
    val Failure(ex) = service.statusFor(PUBLISHED_ID, TokenUser.PublicUser)
    ex.isInstanceOf[NotFoundException]
  }

  test("That statusFor returns a LearningPathStatus when the status is PUBLISHED") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    assertResult("PUBLISHED") {
      service.statusFor(PUBLISHED_ID, TokenUser.PublicUser).map(_.status).get
    }
  }

  test("That statusFor throws an AccessDeniedException when the status is PRIVATE and no user") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))

    val Failure(ex) = service.statusFor(2, TokenUser.PublicUser)
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
    val Failure(ex) = service.learningstepsForWithStatusV2(
      PUBLISHED_ID,
      StepStatus.ACTIVE,
      "nb",
      fallback = false,
      TokenUser.PublicUser
    )
    ex.isInstanceOf[NotFoundException]
  }

  test("That learningstepsFor returns None the learningPath does not have any learningsteps") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepsFor(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(List())
    val Failure(ex) = service.learningstepsForWithStatusV2(
      PUBLISHED_ID,
      StepStatus.ACTIVE,
      "nb",
      fallback = false,
      TokenUser.PublicUser
    )
    ex.isInstanceOf[NotFoundException]
  }

  test("That learningstepsFor returns only active steps when specifying status active") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepsFor(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(List(STEP1, STEP2.copy(status = StepStatus.DELETED), STEP3))
    val learningSteps = service.learningstepsForWithStatusV2(
      PUBLISHED_ID,
      StepStatus.ACTIVE,
      "nb",
      fallback = false,
      TokenUser.PublicUser
    )
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
    val learningSteps = service.learningstepsForWithStatusV2(
      PUBLISHED_ID,
      StepStatus.DELETED,
      "nb",
      fallback = false,
      TokenUser.PublicUser
    )
    learningSteps.isSuccess should be(true)
    learningSteps.get.learningsteps.size should be(1)
    learningSteps.get.learningsteps.head.id should equal(STEP2.id.get)
  }

  test("That learningstepsFor throws an AccessDeniedException when the status is PRIVATE and no user") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    val Failure(ex) =
      service.learningstepsForWithStatusV2(PRIVATE_ID, StepStatus.ACTIVE, "nb", fallback = false, TokenUser.PublicUser)
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("That learningstepsFor throws an AccessDeniedException when the status is PRIVATE and user is not the owner") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    val Failure(ex) =
      service.learningstepsForWithStatusV2(PRIVATE_ID, StepStatus.ACTIVE, "nb", fallback = false, PUBLISHED_OWNER)
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
        .learningstepsForWithStatusV2(PRIVATE_ID, StepStatus.ACTIVE, "nb", fallback = false, PRIVATE_OWNER)
        .get
        .learningsteps
        .length
    }
  }

  test("That learningstepV2For returns None when the learningPath does not exist") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(None)
    val Failure(ex) =
      service.learningstepV2For(PUBLISHED_ID, STEP1.id.get, "nb", fallback = false, TokenUser.PublicUser)
    ex.isInstanceOf[NotFoundException]
  }

  test("That learningstepV2For returns None when the learningStep does not exist") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PUBLISHED_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(None)
    val Failure(ex) =
      service.learningstepV2For(PUBLISHED_ID, STEP1.id.get, "nb", fallback = false, TokenUser.PublicUser)
    ex.isInstanceOf[NotFoundException]
  }

  test("That learningstepV2For returns the LearningStep when it exists") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PUBLISHED_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(Some(STEP1))
    assertResult(STEP1.id.get) {
      service.learningstepV2For(PUBLISHED_ID, STEP1.id.get, "nb", fallback = false, TokenUser.PublicUser).get.id
    }
  }

  test("That learningstepV2For returns the LearningStep when it exists and status is PRIVATE and user is the owner") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(Some(STEP1))
    assertResult(STEP1.id.get) {
      service
        .learningstepV2For(PRIVATE_ID, STEP1.id.get, "nb", fallback = false, PRIVATE_OWNER)
        .get
        .id
    }
  }

  test("That learningstepV2For throws an AccessDeniedException when the status is PRIVATE and no user") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    val Failure(ex) = service.learningstepV2For(PRIVATE_ID, STEP1.id.get, "nb", fallback = false, TokenUser.PublicUser)
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("That learningstepFor throws an AccessDeniedException when the status is PRIVATE and user is not the owner") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    val Failure(ex) = service.learningstepV2For(PRIVATE_ID, STEP1.id.get, "nb", fallback = false, PUBLISHED_OWNER)
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
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
      rank = None,
      created = created,
      updated = created,
      shared = None,
      description = None
    )

    val subFolder1 = domain.Folder(
      id = subFolder1UUID,
      feideId = "",
      parentId = Some(mainFolderUUID),
      name = "subFolder1",
      status = FolderStatus.PRIVATE,
      subfolders = List.empty,
      resources = List.empty,
      rank = None,
      created = created,
      updated = created,
      shared = None,
      description = None
    )

    val subFolder2 = domain.Folder(
      id = subFolder2UUID,
      feideId = "",
      parentId = Some(mainFolderUUID),
      name = "subFolder2",
      status = FolderStatus.PRIVATE,
      subfolders = List.empty,
      resources = List.empty,
      rank = None,
      created = created,
      updated = created,
      shared = None,
      description = None
    )

    val resource1 = domain.Resource(
      id = resource1UUID,
      feideId = "",
      resourceType = "article",
      path = "/subject/1/topic/1/resource/4",
      created = created,
      tags = List.empty,
      resourceId = "1",
      connection = None
    )

    val expected = api.Folder(
      id = mainFolderUUID.toString,
      name = "mainFolder",
      status = "private",
      breadcrumbs = List(api.Breadcrumb(id = mainFolderUUID.toString, name = "mainFolder")),
      parentId = None,
      resources = List(
        api.Resource(
          id = resource1UUID.toString,
          resourceType = "article",
          tags = List.empty,
          path = "/subject/1/topic/1/resource/4",
          created = created,
          resourceId = "1",
          rank = None
        )
      ),
      subfolders = List(
        api.Folder(
          id = subFolder1UUID.toString,
          name = "subFolder1",
          status = "private",
          subfolders = List.empty,
          resources = List.empty,
          breadcrumbs = List(
            api.Breadcrumb(id = mainFolderUUID.toString, name = "mainFolder"),
            api.Breadcrumb(id = subFolder1UUID.toString, name = "subFolder1")
          ),
          parentId = Some(mainFolderUUID.toString),
          rank = None,
          created = created,
          updated = created,
          shared = None,
          description = None,
          owner = None
        ),
        api.Folder(
          id = subFolder2UUID.toString,
          name = "subFolder2",
          status = "private",
          resources = List.empty,
          subfolders = List.empty,
          breadcrumbs = List(
            api.Breadcrumb(id = mainFolderUUID.toString, name = "mainFolder"),
            api.Breadcrumb(id = subFolder2UUID.toString, name = "subFolder2")
          ),
          parentId = Some(mainFolderUUID.toString),
          rank = None,
          created = created,
          updated = created,
          shared = None,
          description = None,
          owner = None
        )
      ),
      rank = None,
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

    val result = service.getSingleFolder(mainFolderUUID, includeSubfolders = true, includeResources = true, None)
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
        breadcrumbs = List(api.Breadcrumb(id = favoriteUUID.toString, name = "favorite"))
      )

    when(feideApiClient.getFeideID(Some("token"))).thenReturn(Success(feideId))
    when(folderRepository.insertFolder(any, any)(any)).thenReturn(Success(favoriteDomainFolder))
    when(folderRepository.foldersWithFeideAndParentID(eqTo(None), eqTo(feideId))(any)).thenReturn(Success(List.empty))
    when(folderRepository.folderWithId(eqTo(favoriteUUID))(any)).thenReturn(Success(favoriteDomainFolder))
    when(userRepository.userWithFeideId(any)(any[DBSession])).thenReturn(Success(None))

    val result = service.getFolders(includeSubfolders = false, includeResources = false, Some("token"))
    result.get.length should be(1)
    result.get.find(_.name == "favorite").get should be(favoriteApiFolder)

    verify(folderRepository, times(1)).foldersWithFeideAndParentID(eqTo(None), eqTo(feideId))(any)
    verify(folderRepository, times(1)).insertFolder(any, any)(any)
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
    when(userRepository.userWithFeideId(any)(any[DBSession])).thenReturn(Success(None))

    val result = service.getFolders(includeSubfolders = false, includeResources = true, Some("token"))
    result.get.length should be(2)

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
        breadcrumbs = List(api.Breadcrumb(id = folderUUID.toString, name = ""))
      )

    when(folderRepository.getFolderAndChildrenSubfoldersWithResources(eqTo(folderUUID), eqTo(FolderStatus.SHARED))(any))
      .thenReturn(Success(Some(folderWithId)))
    when(userRepository.userWithFeideId(any)(any[DBSession])).thenReturn(Success(None))

    service.getSharedFolder(folderUUID) should be(Success(apiFolder))
  }

  test("That getSharedFolder returns a folder with owner info if the owner wants to") {
    val feideId = "feide"
    val domainUserData = domain.MyNDLAUser(
      id = 42,
      feideId = feideId,
      favoriteSubjects = Seq.empty,
      userRole = UserRole.TEACHER,
      lastUpdated = clock.now(),
      organization = "oslo",
      groups = Seq(
        FeideGroup(
          id = "id",
          `type` = FeideGroup.FC_ORG,
          displayName = "oslo",
          membership = Membership(primarySchool = None),
          parent = None
        )
      ),
      email = "example@email.com",
      arenaEnabled = false,
      displayName = "Feide",
      shareName = true
    )

    val folderUUID   = UUID.randomUUID()
    val folderWithId = emptyDomainFolder.copy(id = folderUUID, status = FolderStatus.SHARED)
    val apiFolder =
      emptyApiFolder.copy(
        id = folderUUID.toString,
        name = "",
        status = "shared",
        breadcrumbs = List(api.Breadcrumb(id = folderUUID.toString, name = "")),
        owner = Some(Owner("Feide"))
      )

    when(folderRepository.getFolderAndChildrenSubfoldersWithResources(eqTo(folderUUID), eqTo(FolderStatus.SHARED))(any))
      .thenReturn(Success(Some(folderWithId)))
    when(userRepository.userWithFeideId(any)(any[DBSession])).thenReturn(Success(Some(domainUserData)))

    service.getSharedFolder(folderUUID) should be(Success(apiFolder))
  }

  test("That getSharedFolder returns a Failure Not Found if the status is not shared") {
    val folderUUID   = UUID.randomUUID()
    val folderWithId = emptyDomainFolder.copy(id = folderUUID, status = FolderStatus.PRIVATE)

    when(folderRepository.getFolderAndChildrenSubfoldersWithResources(eqTo(folderUUID), eqTo(FolderStatus.SHARED))(any))
      .thenReturn(Success(Some(folderWithId)))

    val Failure(result: NotFoundException) = service.getSharedFolder(folderUUID)
    result.message should be("Folder does not exist")
  }

  test("That getMyNDLAUserData creates new UserData if no user exist") {
    when(clock.now()).thenReturn(NDLADate.now())

    val feideId = "feide"
    val groups =
      Seq(
        FeideGroup(
          id = "id",
          `type` = FeideGroup.FC_ORG,
          displayName = "oslo",
          membership = Membership(primarySchool = Some(true)),
          parent = None
        )
      )
    val domainUserData = domain.MyNDLAUser(
      id = 42,
      feideId = feideId,
      favoriteSubjects = Seq("r", "e"),
      userRole = UserRole.STUDENT,
      lastUpdated = clock.now(),
      organization = "oslo",
      groups = groups,
      email = "example@email.com",
      arenaEnabled = false,
      displayName = "Feide",
      shareName = false
    )
    val apiUserData = api.MyNDLAUser(
      id = 42,
      username = "example@email.com",
      displayName = "Feide",
      favoriteSubjects = Seq("r", "e"),
      role = "student",
      organization = "oslo",
      groups = Seq(MyNDLAGroup(id = "id", displayName = "oslo", isPrimarySchool = true, parentId = None)),
      arenaEnabled = false,
      shareName = false
    )
    val feideUserInfo = FeideExtendedUserInfo(
      displayName = "David",
      eduPersonAffiliation = Seq("student"),
      eduPersonPrincipalName = "example@email.com"
    )

    when(readService.getMyNDLAEnabledOrgs).thenReturn(Success(List.empty))
    when(feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    when(feideApiClient.getFeideAccessTokenOrFail(any)).thenReturn(Success(feideId))
    when(feideApiClient.getFeideExtendedUser(any)).thenReturn(Success(feideUserInfo))
    when(feideApiClient.getFeideGroups(Some(feideId))).thenReturn(Success(groups))
    when(feideApiClient.getOrganization(any)).thenReturn(Success("oslo"))
    when(userRepository.userWithFeideId(any)(any)).thenReturn(Success(None))
    when(userRepository.insertUser(any, any[domain.MyNDLAUserDocument])(any))
      .thenReturn(Success(domainUserData))

    service.getMyNDLAUserData(Some(feideId)).get should be(apiUserData)

    verify(feideApiClient, times(1)).getFeideExtendedUser(any)
    verify(feideApiClient, times(1)).getFeideGroups(any)
    verify(feideApiClient, times(1)).getOrganization(any)
    verify(userRepository, times(1)).userWithFeideId(any)(any)
    verify(userRepository, times(1)).insertUser(any, any)(any)
    verify(userRepository, times(0)).updateUser(any, any)(any)
  }

  test("That getMyNDLAUserData returns already created user if it exists and was updated lately") {
    when(clock.now()).thenReturn(NDLADate.now())

    val feideId = "feide"
    val domainUserData = domain.MyNDLAUser(
      id = 42,
      feideId = feideId,
      favoriteSubjects = Seq("r", "e"),
      userRole = UserRole.STUDENT,
      lastUpdated = clock.now().plusDays(1),
      organization = "oslo",
      groups = Seq(
        FeideGroup(
          id = "id",
          `type` = FeideGroup.FC_ORG,
          displayName = "oslo",
          membership = Membership(primarySchool = Some(true)),
          parent = None
        )
      ),
      email = "example@email.com",
      arenaEnabled = false,
      displayName = "Feide",
      shareName = false
    )
    val apiUserData = api.MyNDLAUser(
      id = 42,
      username = "example@email.com",
      displayName = "Feide",
      favoriteSubjects = Seq("r", "e"),
      role = "student",
      organization = "oslo",
      groups = Seq(MyNDLAGroup(id = "id", displayName = "oslo", isPrimarySchool = true, parentId = None)),
      arenaEnabled = false,
      shareName = false
    )

    when(readService.getMyNDLAEnabledOrgs).thenReturn(Success(List.empty))
    when(feideApiClient.getFeideID(Some(feideId))).thenReturn(Success(feideId))
    when(userRepository.userWithFeideId(eqTo(feideId))(any)).thenReturn(Success(Some(domainUserData)))

    service.getMyNDLAUserData(Some(feideId)).get should be(apiUserData)

    verify(feideApiClient, times(0)).getFeideExtendedUser(any)
    verify(feideApiClient, times(0)).getFeideGroups(any)
    verify(userRepository, times(1)).userWithFeideId(any)(any)
    verify(userRepository, times(0)).insertUser(any, any)(any)
    verify(userRepository, times(0)).updateUser(any, any)(any)
  }

  test("That getMyNDLAUserData returns already created user if it exists but needs update") {
    when(clock.now()).thenReturn(NDLADate.now())

    val feideId = "feide"
    val groups =
      Seq(
        FeideGroup(
          id = "id",
          `type` = FeideGroup.FC_ORG,
          displayName = "oslo",
          membership = Membership(primarySchool = Some(true)),
          parent = None
        )
      )
    val domainUserData = domain.MyNDLAUser(
      id = 42,
      feideId = feideId,
      favoriteSubjects = Seq("r", "e"),
      userRole = UserRole.STUDENT,
      lastUpdated = clock.now().minusDays(1),
      organization = "oslo",
      groups = groups,
      email = "example@email.com",
      arenaEnabled = false,
      displayName = "Feide",
      shareName = false
    )
    val updatedFeideUser = FeideExtendedUserInfo(
      displayName = "name",
      eduPersonAffiliation = Seq.empty,
      eduPersonPrincipalName = "example@email.com"
    )
    val apiUserData = api.MyNDLAUser(
      id = 42,
      username = "example@email.com",
      displayName = "Feide",
      favoriteSubjects = Seq("r", "e"),
      role = "student",
      organization = "oslo",
      groups = Seq(MyNDLAGroup(id = "id", displayName = "oslo", isPrimarySchool = true, parentId = None)),
      arenaEnabled = false,
      shareName = false
    )

    when(readService.getMyNDLAEnabledOrgs).thenReturn(Success(List.empty))
    when(feideApiClient.getFeideID(Some(feideId))).thenReturn(Success(feideId))
    when(feideApiClient.getFeideExtendedUser(Some(feideId))).thenReturn(Success(updatedFeideUser))
    when(feideApiClient.getFeideGroups(Some(feideId))).thenReturn(Success(groups))
    when(feideApiClient.getOrganization(Some(feideId))).thenReturn(Success("oslo"))
    when(userRepository.userWithFeideId(eqTo(feideId))(any)).thenReturn(Success(Some(domainUserData)))
    when(userRepository.updateUser(any, any)(any)).thenReturn(Success(domainUserData))

    service.getMyNDLAUserData(Some(feideId)).get should be(apiUserData)

    verify(feideApiClient, times(1)).getFeideExtendedUser(any)
    verify(feideApiClient, times(1)).getFeideGroups(any)
    verify(feideApiClient, times(1)).getOrganization(any)
    verify(userRepository, times(1)).userWithFeideId(any)(any)
    verify(userRepository, times(0)).insertUser(any, any)(any)
    verify(userRepository, times(1)).updateUser(any, any)(any)
  }

  test("That getting stats fetches stats for my ndla usage") {
    when(userRepository.numberOfUsers()(any)).thenReturn(Some(5))
    when(folderRepository.numberOfFolders()(any)).thenReturn(Some(10))
    when(folderRepository.numberOfResources()(any)).thenReturn(Some(20))
    when(folderRepository.numberOfTags()(any)).thenReturn(Some(10))
    when(userRepository.numberOfFavouritedSubjects()(any)).thenReturn(Some(15))
    when(folderRepository.numberOfSharedFolders()(any)).thenReturn(Some(5))
    when(folderRepository.numberOfResourcesGrouped()(any)).thenReturn(List.empty)

    service.getStats.get should be(Stats(5, 10, 20, 10, 15, 5, List.empty))
  }
}
