/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import no.ndla.common.model.{NDLADate, api => commonApi, domain => common}
import no.ndla.common.errors.{AccessDeniedException, ValidationException}
import no.ndla.common.model.domain.{Author, Title}
import no.ndla.common.model.domain.learningpath.LearningpathCopyright
import no.ndla.learningpathapi.TestData._
import no.ndla.learningpathapi._
import no.ndla.learningpathapi.model._
import no.ndla.learningpathapi.model.api.config.ConfigMetaValue
import no.ndla.learningpathapi.model.api.{
  FolderSortRequest,
  NewCopyLearningPathV2,
  NewLearningPathV2,
  NewLearningStepV2,
  NewResource,
  UpdatedFolder,
  UpdatedLearningPathV2,
  UpdatedLearningStepV2
}
import no.ndla.learningpathapi.model.domain.FolderSortObject.FolderSorting
import no.ndla.learningpathapi.model.domain._
import no.ndla.learningpathapi.model.domain.config.{ConfigKey, ConfigMeta}
import no.ndla.network.tapir.auth.Permission.{LEARNINGPATH_API_ADMIN, LEARNINGPATH_API_PUBLISH}
import no.ndla.network.tapir.auth.TokenUser
import org.mockito.invocation.InvocationOnMock
import scalikejdbc.DBSession

import java.util.UUID
import scala.util.{Failure, Success, Try}

class UpdateServiceTest extends UnitSuite with UnitTestEnvironment {
  var service: UpdateService = _

  val PUBLISHED_ID: Long = 1
  val PRIVATE_ID: Long   = 2

  val PUBLISHED_OWNER: TokenUser = TokenUser("eier1", Set.empty, None)
  val PRIVATE_OWNER: TokenUser   = TokenUser("eier2", Set.empty, None)

  val STEP1 = domain.LearningStep(
    Some(1),
    Some(1),
    None,
    None,
    0,
    List(common.Title("Tittel", "nb")),
    List(),
    List(),
    StepType.TEXT,
    None,
    showTitle = true,
    status = StepStatus.ACTIVE
  )

  val STEP2 = domain.LearningStep(
    Some(2),
    Some(1),
    None,
    None,
    1,
    List(common.Title("Tittel", "nb")),
    List(),
    List(),
    StepType.TEXT,
    None,
    showTitle = false,
    status = StepStatus.ACTIVE
  )

  val STEP3 = domain.LearningStep(
    Some(3),
    Some(1),
    None,
    None,
    2,
    List(common.Title("Tittel", "nb")),
    List(),
    List(),
    StepType.TEXT,
    None,
    showTitle = true,
    status = StepStatus.ACTIVE
  )

  val STEP4 = domain.LearningStep(
    Some(4),
    Some(1),
    None,
    None,
    3,
    List(common.Title("Tittel", "nb")),
    List(),
    List(),
    StepType.TEXT,
    None,
    showTitle = false,
    status = StepStatus.ACTIVE
  )

  val STEP5 = domain.LearningStep(
    Some(5),
    Some(1),
    None,
    None,
    4,
    List(common.Title("Tittel", "nb")),
    List(),
    List(),
    StepType.TEXT,
    None,
    showTitle = true,
    status = StepStatus.ACTIVE
  )

  val STEP6 = domain.LearningStep(
    Some(6),
    Some(1),
    None,
    None,
    5,
    List(common.Title("Tittel", "nb")),
    List(),
    List(),
    StepType.TEXT,
    None,
    showTitle = false,
    status = StepStatus.ACTIVE
  )

  val NEW_STEPV2 =
    NewLearningStepV2("Tittel", Some("Beskrivelse"), "nb", Some(api.EmbedUrlV2("", "oembed")), true, "TEXT", None)

  val UPDATED_STEPV2 =
    UpdatedLearningStepV2(1, Option("Tittel"), "nb", Some("Beskrivelse"), None, Some(false), None, None)

  val rubio     = Author("author", "Little Marco")
  val license   = "publicdomain"
  val copyright = LearningpathCopyright(license, List(rubio))
  val apiRubio  = commonApi.Author("author", "Little Marco")
  val apiLicense =
    commonApi.License("publicdomain", Some("Public Domain"), Some("https://creativecommons.org/about/pdm"))
  val apiCopyright = api.Copyright(apiLicense, List(apiRubio))

  val PUBLISHED_LEARNINGPATH = domain.LearningPath(
    Some(PUBLISHED_ID),
    Some(1),
    Some("1"),
    None,
    List(Title("Tittel", "nb")),
    List(Description("Beskrivelse", "nb")),
    None,
    Some(1),
    domain.LearningPathStatus.PUBLISHED,
    LearningPathVerificationStatus.EXTERNAL,
    NDLADate.now(),
    List(),
    PUBLISHED_OWNER.id,
    copyright,
    Some(STEP1 :: STEP2 :: STEP3 :: STEP4 :: STEP5 :: STEP6 :: Nil)
  )

  val PUBLISHED_LEARNINGPATH_NO_STEPS = domain.LearningPath(
    Some(PUBLISHED_ID),
    Some(1),
    Some("1"),
    None,
    List(Title("Tittel", "nb")),
    List(Description("Beskrivelse", "nb")),
    None,
    Some(1),
    domain.LearningPathStatus.PUBLISHED,
    LearningPathVerificationStatus.EXTERNAL,
    NDLADate.now(),
    List(),
    PUBLISHED_OWNER.id,
    copyright,
    None
  )

  val PRIVATE_LEARNINGPATH = domain.LearningPath(
    Some(PRIVATE_ID),
    Some(1),
    None,
    None,
    List(Title("Tittel", "nb")),
    List(Description("Beskrivelse", "nb")),
    None,
    Some(1),
    domain.LearningPathStatus.PRIVATE,
    LearningPathVerificationStatus.EXTERNAL,
    NDLADate.now(),
    List(),
    PRIVATE_OWNER.id,
    copyright,
    Some(STEP1 :: STEP2 :: STEP3 :: STEP4 :: STEP5 :: STEP6 :: Nil)
  )

  val PRIVATE_LEARNINGPATH_NO_STEPS = domain.LearningPath(
    Some(PRIVATE_ID),
    Some(1),
    None,
    None,
    List(Title("Tittel", "nb")),
    List(Description("Beskrivelse", "nb")),
    None,
    Some(1),
    domain.LearningPathStatus.PRIVATE,
    LearningPathVerificationStatus.EXTERNAL,
    NDLADate.now(),
    List(),
    PRIVATE_OWNER.id,
    copyright,
    None
  )

  val DELETED_LEARNINGPATH = domain.LearningPath(
    Some(PRIVATE_ID),
    Some(1),
    None,
    None,
    List(Title("Tittel", "nb")),
    List(Description("Beskrivelse", "nb")),
    None,
    Some(1),
    domain.LearningPathStatus.DELETED,
    LearningPathVerificationStatus.EXTERNAL,
    NDLADate.now(),
    List(),
    PRIVATE_OWNER.id,
    copyright,
    Some(STEP1 :: STEP2 :: STEP3 :: STEP4 :: STEP5 :: STEP6 :: Nil)
  )
  val NEW_PRIVATE_LEARNINGPATHV2 = NewLearningPathV2("Tittel", "Beskrivelse", None, Some(1), List(), "nb", apiCopyright)
  val NEW_COPIED_LEARNINGPATHV2  = NewCopyLearningPathV2("Tittel", Some("Beskrivelse"), "nb", None, Some(1), None, None)

  val UPDATED_PRIVATE_LEARNINGPATHV2 =
    UpdatedLearningPathV2(1, None, "nb", None, None, Some(1), None, Some(apiCopyright), None)

  val UPDATED_PUBLISHED_LEARNINGPATHV2 =
    UpdatedLearningPathV2(1, None, "nb", None, None, Some(1), None, Some(apiCopyright), None)

  override def beforeEach(): Unit = {
    service = new UpdateService
    resetMocks()
    when(folderRepository.getSession(any)).thenReturn(mock[DBSession])
    when(readService.canWriteNow(any[TokenUser])).thenReturn(true)
    when(searchIndexService.deleteDocument(any[domain.LearningPath], any)).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument[domain.LearningPath](0))
    )
    when(searchIndexService.indexDocument(any[domain.LearningPath])).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument[domain.LearningPath](0))
    )
    when(taxonomyApiClient.updateTaxonomyForLearningPath(any[domain.LearningPath], any[Boolean], any))
      .thenAnswer((i: InvocationOnMock) => Success(i.getArgument[domain.LearningPath](0)))
    when(learningStepValidator.validate(any[LearningStep], any[Boolean])).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument[LearningStep](0))
    )
  }

  test("That addLearningPathV2 inserts the given LearningPathV2") {
    when(learningPathRepository.insert(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PRIVATE_LEARNINGPATH)

    val saved =
      service.addLearningPathV2(NEW_PRIVATE_LEARNINGPATHV2, PRIVATE_OWNER)
    assert(saved.get.id == PRIVATE_LEARNINGPATH.id.get)

    verify(learningPathRepository, times(1)).insert(any[domain.LearningPath])(any)
    verify(searchIndexService, never).indexDocument(any[domain.LearningPath])
  }

  test("That updateLearningPathV2 returns Failure when the given ID does not exist") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession])).thenReturn(None)
    val Failure(ex) = service.updateLearningPathV2(PRIVATE_ID, UPDATED_PRIVATE_LEARNINGPATHV2, PRIVATE_OWNER)
    ex should be(NotFoundException("Could not find learningpath with id '2'."))
  }

  test("That updateLearningPathV2 updates the learningpath when the given user is the owner if the status is PRIVATE") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession])).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenReturn(PRIVATE_LEARNINGPATH)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    assertResult(PRIVATE_LEARNINGPATH.id.get) {
      service
        .updateLearningPathV2(PRIVATE_ID, UPDATED_PRIVATE_LEARNINGPATHV2, PRIVATE_OWNER)
        .get
        .id
    }
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])(any)
    verify(searchIndexService, times(1)).indexDocument(any[domain.LearningPath])

  }

  test(
    "That updateLearningPathV2 updates the learningpath when the given user is the owner if the status is UNLISTED"
  ) {
    val unlistedLp = PRIVATE_LEARNINGPATH.copy(status = LearningPathStatus.UNLISTED)
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession])).thenReturn(Some(unlistedLp))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenReturn(unlistedLp)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    assertResult(PRIVATE_LEARNINGPATH.id.get) {
      service
        .updateLearningPathV2(PRIVATE_ID, UPDATED_PRIVATE_LEARNINGPATHV2, PRIVATE_OWNER)
        .get
        .id
    }
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])(any)
    verify(searchIndexService, times(1)).indexDocument(any[domain.LearningPath])
  }

  test(
    "That updateLearningPathV2 updates the learningpath when the given user is a publisher if the status is PUBLISHED"
  ) {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenReturn(PUBLISHED_LEARNINGPATH)

    assertResult(PUBLISHED_LEARNINGPATH.id.get) {
      service
        .updateLearningPathV2(PUBLISHED_ID, UPDATED_PUBLISHED_LEARNINGPATHV2, PUBLISHED_OWNER)
        .get
        .id
    }
  }

  test("That updateLearningPathV2 returns Failure if user is not the owner") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession])).thenReturn(Some(PRIVATE_LEARNINGPATH))

    val Failure(ex) =
      service.updateLearningPathV2(
        PRIVATE_ID,
        UPDATED_PRIVATE_LEARNINGPATHV2,
        TokenUser("not_the_owner", Set.empty, None)
      )
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("That updateLearningPathV2 sets status to UNLISTED if owner is not publisher and status is PUBLISHED") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PUBLISHED_LEARNINGPATH.copy(status = LearningPathStatus.UNLISTED))
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val result = service.updateLearningPathV2(PUBLISHED_ID, UPDATED_PUBLISHED_LEARNINGPATHV2, PUBLISHED_OWNER).get
    result.id should be(PUBLISHED_LEARNINGPATH.id.get)
    result.status should be(LearningPathStatus.UNLISTED.toString)
  }

  test("That updateLearningPathV2 status PRIVATE remains PRIVATE if not publisher") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession])).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenReturn(PRIVATE_LEARNINGPATH)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val result = service.updateLearningPathV2(PRIVATE_ID, UPDATED_PRIVATE_LEARNINGPATHV2, PRIVATE_OWNER).get
    result.id should be(PRIVATE_LEARNINGPATH.id.get)
    result.status should be(LearningPathStatus.PRIVATE.toString)
  }

  test("That updateLearningPathStatusV2 returns None when the given ID does not exist") {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PRIVATE_ID))(any[DBSession])).thenReturn(None)
    val Failure(ex) = service.updateLearningPathStatusV2(PRIVATE_ID, LearningPathStatus.PUBLISHED, PRIVATE_OWNER, "nb")
    ex should be(NotFoundException(s"Could not find learningpath with id '$PRIVATE_ID'."))
  }

  test("That updateLearningPathStatusV2 updates the status when the given user is admin and the status is PUBLISHED") {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PUBLISHED_LEARNINGPATH.copy(status = domain.LearningPathStatus.PRIVATE))
    when(learningPathRepository.learningPathsWithIsBasedOn(PUBLISHED_ID)).thenReturn(List())

    assertResult("PRIVATE") {
      service
        .updateLearningPathStatusV2(
          PUBLISHED_ID,
          LearningPathStatus.PRIVATE,
          PRIVATE_OWNER.copy(permissions = Set(LEARNINGPATH_API_ADMIN)),
          "nb"
        )
        .get
        .status
    }
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])(any)
    verify(searchIndexService, times(1)).indexDocument(any[domain.LearningPath])
  }

  test(
    "That updateLearningPathStatusV2 updates the status when the given user is not the owner, but is admin and the status is PUBLISHED"
  ) {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PUBLISHED_LEARNINGPATH.copy(status = domain.LearningPathStatus.PRIVATE))
    when(learningPathRepository.learningPathsWithIsBasedOn(PUBLISHED_ID))
      .thenReturn(List())

    assertResult("PRIVATE") {
      service
        .updateLearningPathStatusV2(
          PUBLISHED_ID,
          LearningPathStatus.PRIVATE,
          TokenUser("not_the_owner", Set(LEARNINGPATH_API_ADMIN), None),
          "nb"
        )
        .get
        .status
    }
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])(any)
    verify(searchIndexService, times(1)).indexDocument(any[domain.LearningPath])
    verify(searchIndexService, times(0)).deleteDocument(any[domain.LearningPath], any)
  }

  test(
    "That updateLearningPathStatusV2 updates the status when the given user is the owner and the status is PRIVATE"
  ) {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PRIVATE_LEARNINGPATH.copy(status = domain.LearningPathStatus.DELETED))
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    assertResult("DELETED") {
      service
        .updateLearningPathStatusV2(PRIVATE_ID, LearningPathStatus.DELETED, PRIVATE_OWNER, "nb")
        .get
        .status
    }
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])(any)
  }

  test("That updateLearningPathStatusV2 updates the status when the given user is owner and the status is DELETED") {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(DELETED_LEARNINGPATH))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(DELETED_LEARNINGPATH.copy(status = domain.LearningPathStatus.UNLISTED))
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    assertResult("UNLISTED") {
      service
        .updateLearningPathStatusV2(PRIVATE_ID, LearningPathStatus.UNLISTED, PRIVATE_OWNER, "nb")
        .get
        .status
    }
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])(any)
    verify(searchIndexService, times(1)).indexDocument(any[domain.LearningPath])
  }

  test(
    "That updateLearningPathStatusV2 updates the status when the given user is publisher and the status is DELETED"
  ) {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(DELETED_LEARNINGPATH))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(DELETED_LEARNINGPATH.copy(status = domain.LearningPathStatus.PUBLISHED))

    assertResult("PUBLISHED") {
      service
        .updateLearningPathStatusV2(
          PRIVATE_ID,
          LearningPathStatus.PUBLISHED,
          PRIVATE_OWNER.copy(permissions = Set(LEARNINGPATH_API_ADMIN)),
          "nb"
        )
        .get
        .status
    }
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])(any)
    verify(searchIndexService, times(1)).indexDocument(any[domain.LearningPath])
  }

  test("That updateLearningPathStatusV2 updates isBasedOn when a PUBLISHED path is DELETED") {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PUBLISHED_LEARNINGPATH.copy(status = domain.LearningPathStatus.DELETED))
    when(learningPathRepository.learningPathsWithIsBasedOn(PUBLISHED_ID))
      .thenReturn(
        List(
          DELETED_LEARNINGPATH.copy(id = Some(9), isBasedOn = Some(PUBLISHED_ID)),
          DELETED_LEARNINGPATH.copy(id = Some(8), isBasedOn = Some(PUBLISHED_ID))
        )
      )

    assertResult("DELETED") {
      service
        .updateLearningPathStatusV2(
          PUBLISHED_ID,
          LearningPathStatus.DELETED,
          PUBLISHED_OWNER.copy(permissions = Set(LEARNINGPATH_API_ADMIN)),
          "nb"
        )
        .get
        .status
    }

    verify(learningPathRepository, times(3)).update(any[domain.LearningPath])(any)
    verify(learningPathRepository, times(1)).learningPathsWithIsBasedOn(any[Long])
    verify(searchIndexService, times(1))
      .indexDocument(any[domain.LearningPath])
  }

  test("That updateLearningPathStatusV2 throws an AccessDeniedException when non-admin tries to publish") {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    val Failure(ex) = service.updateLearningPathStatusV2(PRIVATE_ID, LearningPathStatus.PUBLISHED, PRIVATE_OWNER, "nb")
    ex should be(AccessDeniedException("You need to be a publisher to publish learningpaths."))
  }

  test("That updateLearningPathStatusV2 allows owner to edit PUBLISHED to PRIVATE") {
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PUBLISHED_LEARNINGPATH.copy(status = LearningPathStatus.PRIVATE))

    assertResult("PRIVATE") {
      service
        .updateLearningPathStatusV2(PUBLISHED_ID, LearningPathStatus.PRIVATE, PUBLISHED_OWNER, "nb")
        .get
        .status
    }
    verify(learningPathRepository, times(1)).learningPathsWithIsBasedOn(any[Long])
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])(any)
    verify(searchIndexService, times(1)).indexDocument(any[domain.LearningPath])
  }

  test("That updateLearningPathStatusV2 allows owner to edit PUBLISHED to UNLISTED") {
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PUBLISHED_LEARNINGPATH.copy(status = LearningPathStatus.UNLISTED))

    assertResult("UNLISTED") {
      service
        .updateLearningPathStatusV2(PUBLISHED_ID, LearningPathStatus.UNLISTED, PUBLISHED_OWNER, "nb")
        .get
        .status
    }
    verify(learningPathRepository, times(1)).learningPathsWithIsBasedOn(any[Long])
    verify(learningPathRepository, times(1)).update(any[domain.LearningPath])(any)
    verify(searchIndexService, times(1)).indexDocument(any[domain.LearningPath])
  }

  test("That updateLearningPathStatusV2 throws an AccessDeniedException when non-owner tries to change status") {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    val Failure(ex) = service.updateLearningPathStatusV2(PUBLISHED_ID, LearningPathStatus.PRIVATE, PRIVATE_OWNER, "nb")
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("That updateLearningPathStatusV2 ignores message if not admin") {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.update(any[LearningPath])(any[DBSession])).thenAnswer((i: InvocationOnMock) =>
      i.getArgument[LearningPath](0)
    )
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)
    when(clock.now()).thenReturn(NDLADate.fromUnixTime(0))

    val expected = PUBLISHED_LEARNINGPATH.copy(
      message = None,
      status = LearningPathStatus.PRIVATE,
      lastUpdated = clock.now()
    )

    service.updateLearningPathStatusV2(
      PUBLISHED_ID,
      LearningPathStatus.PRIVATE,
      PUBLISHED_OWNER,
      "nb",
      Some("new message")
    )
    verify(learningPathRepository, times(1)).update(expected)
  }

  test("That updateLearningPathStatusV2 adds message if admin") {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.update(any[LearningPath])(any[DBSession])).thenAnswer((i: InvocationOnMock) =>
      i.getArgument[LearningPath](0)
    )
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)
    when(clock.now()).thenReturn(NDLADate.MIN)

    service.updateLearningPathStatusV2(
      PUBLISHED_ID,
      LearningPathStatus.PRIVATE,
      PRIVATE_OWNER.copy(permissions = Set(LEARNINGPATH_API_ADMIN)),
      "nb",
      Some("new message")
    )
    verify(learningPathRepository, times(1)).update(
      PUBLISHED_LEARNINGPATH.copy(
        message = Some(Message("new message", PRIVATE_OWNER.id, clock.now())),
        status = LearningPathStatus.PRIVATE,
        lastUpdated = clock.now()
      )
    )
  }

  test("That addLearningStepV2 returns None when the given learningpath does not exist") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession])).thenReturn(None)
    val Failure(ex) = service.addLearningStepV2(PRIVATE_ID, NEW_STEPV2, PRIVATE_OWNER)
    ex.isInstanceOf[NotFoundException] should be(true)
    verify(learningPathRepository, never).insertLearningStep(any[domain.LearningStep])(any)
    verify(learningPathRepository, never).update(any[domain.LearningPath])(any)
  }

  test(
    "That addLearningStepV2 inserts the learningstepV2 and update lastUpdated on the learningpath when the given user is the owner and status is PRIVATE"
  ) {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.insertLearningStep(any[LearningStep])(any[DBSession])).thenReturn(STEP1)
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PRIVATE_LEARNINGPATH)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    assertResult(STEP1.id.get) {
      service.addLearningStepV2(PRIVATE_ID, NEW_STEPV2, PRIVATE_OWNER).get.id
    }
    verify(learningPathRepository, times(1))
      .insertLearningStep(any[domain.LearningStep])(any[DBSession])
    verify(learningPathRepository, times(1))
      .update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, times(1)).indexDocument(any[domain.LearningPath])
    verify(searchIndexService, times(0)).deleteDocument(any[domain.LearningPath], any)
  }

  test(
    "That addLearningStep inserts the learningstep and update lastUpdated on the learningpath when the given user is the owner and status is PUBLISHED"
  ) {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.insertLearningStep(any[LearningStep])(any[DBSession])).thenReturn(STEP2)
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenAnswer((i: InvocationOnMock) => i.getArgument[domain.LearningPath](0))
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)
    assertResult(STEP2.id.get) {
      service
        .addLearningStepV2(PUBLISHED_ID, NEW_STEPV2, PUBLISHED_OWNER)
        .get
        .id
    }
    verify(learningPathRepository, times(1))
      .insertLearningStep(any[domain.LearningStep])(any[DBSession])
    verify(learningPathRepository, times(1))
      .update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, times(1)).indexDocument(any[domain.LearningPath])
    verify(searchIndexService, times(0)).deleteDocument(any[domain.LearningPath], any)
  }

  test("That addLearningStepV2 throws an AccessDeniedException when the given user is NOT the owner") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    val Failure(ex) = service.addLearningStepV2(PUBLISHED_ID, NEW_STEPV2, PRIVATE_OWNER)
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("That updateLearningStepV2 returns None when the learningpathV2 does not exist") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(None)

    val Failure(ex) = service.updateLearningStepV2(PUBLISHED_ID, STEP1.id.get, UPDATED_STEPV2, PUBLISHED_OWNER)
    ex.isInstanceOf[NotFoundException] should be(true)

    verify(learningPathRepository, never).updateLearningStep(any[domain.LearningStep])(any)
    verify(learningPathRepository, never).update(any[domain.LearningPath])(any)
  }

  test("That updateLearningStepV2 returns None when the learningstep does not exist") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PUBLISHED_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(None)
    val Failure(ex) = service.updateLearningStepV2(PUBLISHED_ID, STEP1.id.get, UPDATED_STEPV2, PUBLISHED_OWNER)
    ex.isInstanceOf[NotFoundException] should be(true)
    verify(learningPathRepository, never).updateLearningStep(any[domain.LearningStep])(any[DBSession])
    verify(learningPathRepository, never).update(any[domain.LearningPath])(any[DBSession])
  }

  test(
    "That updateLearningStep updates the learningstep and update lastUpdated on the learningpath when the given user is ADMIN and status is PUBLISHED"
  ) {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PUBLISHED_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(Some(STEP1))
    when(learningPathRepository.updateLearningStep(any[LearningStep])(any[DBSession])).thenReturn(STEP1)
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PUBLISHED_LEARNINGPATH)

    assertResult(STEP1.id.get) {
      service
        .updateLearningStepV2(
          PUBLISHED_ID,
          STEP1.id.get,
          UPDATED_STEPV2,
          PUBLISHED_OWNER.copy(permissions = Set(LEARNINGPATH_API_ADMIN))
        )
        .get
        .id
    }
    verify(learningPathRepository, times(1))
      .updateLearningStep(any[domain.LearningStep])(any[DBSession])
    verify(learningPathRepository, times(1))
      .update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, times(1)).indexDocument(any[domain.LearningPath])
  }

  test(
    "That updateLearningStepV2 updates the learningstep and update lastUpdated on the learningpath when the given user is the owner and status is PRIVATE"
  ) {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(Some(STEP1))
    when(learningPathRepository.updateLearningStep(any[LearningStep])(any[DBSession])).thenReturn(STEP1)
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PRIVATE_LEARNINGPATH)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    assertResult(STEP1.id.get) {
      service
        .updateLearningStepV2(PRIVATE_ID, STEP1.id.get, UPDATED_STEPV2, PRIVATE_OWNER)
        .get
        .id
    }
    verify(learningPathRepository, times(1))
      .updateLearningStep(any[domain.LearningStep])(any[DBSession])
    verify(learningPathRepository, times(1))
      .update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, times(1)).indexDocument(any[domain.LearningPath])
  }

  test("That updateLearningStepV2 throws an AccessDeniedException when the given user is NOT the owner") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession])).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(PRIVATE_ID, STEP1.id.get)).thenReturn(Some(STEP1))
    val Failure(ex) = service.updateLearningStepV2(PRIVATE_ID, STEP1.id.get, UPDATED_STEPV2, PUBLISHED_OWNER)
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("That updateLearningStepStatusV2 returns None when the given learningpath does not exist") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(None)

    val Failure(ex) =
      service.updateLearningStepStatusV2(PUBLISHED_ID, STEP1.id.get, StepStatus.DELETED, PUBLISHED_OWNER)
    ex.isInstanceOf[NotFoundException] should be(true)
  }

  test("That updateLearningStepStatusV2 returns None when the given learningstep does not exist") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PUBLISHED_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(None)

    val Failure(ex) =
      service.updateLearningStepStatusV2(PUBLISHED_ID, STEP1.id.get, StepStatus.DELETED, PUBLISHED_OWNER)
    ex.isInstanceOf[NotFoundException] should be(true)
  }

  test(
    "That updateLearningStepStatusV2 marks the learningstep as DELETED when the given user is the owner and the status is PRIVATE"
  ) {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(Some(STEP1))
    when(learningPathRepository.learningStepsFor(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(List())
    when(learningPathRepository.updateLearningStep(eqTo(STEP1.copy(status = StepStatus.DELETED)))(any[DBSession]))
      .thenReturn(STEP1.copy(status = StepStatus.DELETED))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PRIVATE_LEARNINGPATH)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val updatedStep =
      service.updateLearningStepStatusV2(PRIVATE_ID, STEP1.id.get, StepStatus.DELETED, PRIVATE_OWNER)
    updatedStep.isSuccess should be(true)
    updatedStep.get.status should equal(StepStatus.DELETED.entryName)

    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP1.copy(status = StepStatus.DELETED)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, times(1)).indexDocument(any[domain.LearningPath])
  }

  test(
    "That updateLearningStepStatusV2 marks the learningstep as DELETED when the given user is the owner and the status is PUBLISHED"
  ) {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PUBLISHED_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(Some(STEP1))
    when(learningPathRepository.learningStepsFor(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(List())
    when(learningPathRepository.updateLearningStep(eqTo(STEP1.copy(status = StepStatus.DELETED)))(any[DBSession]))
      .thenReturn(STEP1.copy(status = StepStatus.DELETED))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenAnswer((i: InvocationOnMock) =>
      i.getArgument[domain.LearningPath](0)
    )
    val updatedDate = NDLADate.fromUnixTime(0)
    when(clock.now()).thenReturn(updatedDate)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val updatedStep =
      service.updateLearningStepStatusV2(PUBLISHED_ID, STEP1.id.get, StepStatus.DELETED, PUBLISHED_OWNER)
    updatedStep.isSuccess should be(true)
    updatedStep.get.status should equal(StepStatus.DELETED.entryName)

    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP1.copy(status = StepStatus.DELETED)))(any[DBSession])
    verify(learningPathRepository, times(1)).update(
      eqTo(
        PUBLISHED_LEARNINGPATH.copy(
          learningsteps = PUBLISHED_LEARNINGPATH.learningsteps,
          status = LearningPathStatus.UNLISTED,
          lastUpdated = updatedDate
        )
      )
    )(any[DBSession])
    verify(searchIndexService, times(1)).indexDocument(any[domain.LearningPath])
    verify(searchIndexService, times(0)).deleteDocument(any[domain.LearningPath], any)
  }

  test("That marking the first learningStep as deleted changes the seqNo for all other learningsteps") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(Some(STEP1))
    when(learningPathRepository.learningStepsFor(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(List(STEP1, STEP2, STEP3))
    when(learningPathRepository.updateLearningStep(eqTo(STEP1.copy(status = StepStatus.DELETED)))(any[DBSession]))
      .thenReturn(STEP1.copy(status = StepStatus.DELETED))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PRIVATE_LEARNINGPATH)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val updatedStep =
      service.updateLearningStepStatusV2(PRIVATE_ID, STEP1.id.get, StepStatus.DELETED, PRIVATE_OWNER)
    updatedStep.isSuccess should be(true)
    updatedStep.get.status should equal(StepStatus.DELETED.entryName)

    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP1.copy(status = StepStatus.DELETED)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP2.copy(seqNo = STEP2.seqNo - 1)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP3.copy(seqNo = STEP3.seqNo - 1)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, times(1)).indexDocument(any[domain.LearningPath])
  }

  test("That marking the first learningStep as active changes the seqNo for all other learningsteps") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(
      learningPathRepository
        .learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(any[DBSession])
    )
      .thenReturn(Some(STEP1.copy(status = StepStatus.DELETED)))
    when(learningPathRepository.learningStepsFor(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(List(STEP1, STEP2, STEP3))
    when(learningPathRepository.updateLearningStep(eqTo(STEP1.copy(status = StepStatus.ACTIVE)))(any[DBSession]))
      .thenReturn(STEP1.copy(status = StepStatus.ACTIVE))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PRIVATE_LEARNINGPATH)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val updatedStep =
      service.updateLearningStepStatusV2(PRIVATE_ID, STEP1.id.get, StepStatus.ACTIVE, PRIVATE_OWNER)
    updatedStep.isSuccess should be(true)
    updatedStep.get.status should equal(StepStatus.ACTIVE.entryName)

    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP1.copy(status = StepStatus.ACTIVE)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP2.copy(seqNo = STEP2.seqNo + 1)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP3.copy(seqNo = STEP3.seqNo + 1)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, times(1)).indexDocument(any[domain.LearningPath])
  }

  test("That marking the last learningStep as deleted does not affect any of the other learningsteps") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP3.id.get))(any[DBSession]))
      .thenReturn(Some(STEP3))
    when(learningPathRepository.learningStepsFor(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(List(STEP1, STEP2, STEP3))
    when(learningPathRepository.updateLearningStep(eqTo(STEP3.copy(status = StepStatus.DELETED)))(any[DBSession]))
      .thenReturn(STEP3.copy(status = StepStatus.DELETED))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PRIVATE_LEARNINGPATH)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val updatedStep =
      service.updateLearningStepStatusV2(PRIVATE_ID, STEP3.id.get, StepStatus.DELETED, PRIVATE_OWNER)
    updatedStep.isSuccess should be(true)
    updatedStep.get.status should equal(StepStatus.DELETED.entryName)

    verify(learningPathRepository, times(1))
      .updateLearningStep(any[LearningStep])(any[DBSession])
    verify(learningPathRepository, times(1))
      .update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, times(1)).indexDocument(any[domain.LearningPath])
  }

  test("That marking the last learningStep as active does not affect any of the other learningsteps") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(
      learningPathRepository
        .learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP3.id.get))(any[DBSession])
    )
      .thenReturn(Some(STEP3.copy(status = StepStatus.DELETED)))
    when(learningPathRepository.learningStepsFor(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(List(STEP1, STEP2, STEP3))
    when(learningPathRepository.updateLearningStep(eqTo(STEP3.copy(status = StepStatus.ACTIVE)))(any[DBSession]))
      .thenReturn(STEP3.copy(status = StepStatus.ACTIVE))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PRIVATE_LEARNINGPATH)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val updatedStep =
      service.updateLearningStepStatusV2(PRIVATE_ID, STEP3.id.get, StepStatus.ACTIVE, PRIVATE_OWNER)
    updatedStep.isSuccess should be(true)
    updatedStep.get.status should equal(StepStatus.ACTIVE.entryName)

    verify(learningPathRepository, times(1))
      .updateLearningStep(any[LearningStep])(any[DBSession])
    verify(learningPathRepository, times(1))
      .update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, times(1)).indexDocument(any[domain.LearningPath])
  }

  test("That marking the middle learningStep as deleted only affects subsequent learningsteps") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP2.id.get))(any[DBSession]))
      .thenReturn(Some(STEP2))
    when(learningPathRepository.learningStepsFor(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(List(STEP1, STEP2, STEP3))
    when(learningPathRepository.updateLearningStep(eqTo(STEP2.copy(status = StepStatus.DELETED)))(any[DBSession]))
      .thenReturn(STEP2.copy(status = StepStatus.DELETED))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PRIVATE_LEARNINGPATH)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val updatedStep =
      service.updateLearningStepStatusV2(PRIVATE_ID, STEP2.id.get, StepStatus.DELETED, PRIVATE_OWNER)
    updatedStep.isSuccess should be(true)
    updatedStep.get.status should equal(StepStatus.DELETED.entryName)

    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP2.copy(status = StepStatus.DELETED)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP3.copy(seqNo = STEP3.seqNo - 1)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, times(1)).indexDocument(any[domain.LearningPath])
  }

  test("That marking the middle learningStep as active only affects subsequent learningsteps") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(
      learningPathRepository
        .learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP2.id.get))(any[DBSession])
    )
      .thenReturn(Some(STEP2.copy(status = StepStatus.DELETED)))
    when(learningPathRepository.learningStepsFor(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(List(STEP1, STEP2, STEP3))
    when(learningPathRepository.updateLearningStep(eqTo(STEP2.copy(status = StepStatus.ACTIVE)))(any[DBSession]))
      .thenReturn(STEP2.copy(status = StepStatus.ACTIVE))
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PRIVATE_LEARNINGPATH)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val updatedStep =
      service.updateLearningStepStatusV2(PRIVATE_ID, STEP2.id.get, StepStatus.ACTIVE, PRIVATE_OWNER)
    updatedStep.isSuccess should be(true)
    updatedStep.get.status should equal(StepStatus.ACTIVE.entryName)

    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP2.copy(status = StepStatus.ACTIVE)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP3.copy(seqNo = STEP3.seqNo + 1)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .update(any[domain.LearningPath])(any[DBSession])
    verify(searchIndexService, times(1)).indexDocument(any[domain.LearningPath])
  }

  test("That deleteLearningStep throws an AccessDeniedException when the given user is NOT the owner") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(PRIVATE_ID, STEP1.id.get))
      .thenReturn(Some(STEP1))
    val Failure(ex) = service.updateLearningStepStatusV2(PRIVATE_ID, STEP1.id.get, StepStatus.DELETED, PUBLISHED_OWNER)
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("That updateSeqNo throws ValidationException when seqNo out of range") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(Some(STEP1))

    val exception = intercept[ValidationException] {
      service.updateSeqNo(PRIVATE_ID, STEP1.id.get, 100, PRIVATE_OWNER)
    }

    exception.errors.length should be(1)
    exception.errors.head.field should equal("seqNo")
    exception.errors.head.message should equal("seqNo must be between 0 and 5")
  }

  test("That updateSeqNo from 0 to last updates all learningsteps in between") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(Some(STEP1))

    val updatedStep =
      service.updateSeqNo(PRIVATE_ID, STEP1.id.get, STEP6.seqNo, PRIVATE_OWNER)
    updatedStep.get.seqNo should equal(STEP6.seqNo)

    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP2.copy(seqNo = STEP2.seqNo - 1)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP3.copy(seqNo = STEP3.seqNo - 1)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP4.copy(seqNo = STEP4.seqNo - 1)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP5.copy(seqNo = STEP5.seqNo - 1)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP6.copy(seqNo = STEP6.seqNo - 1)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP1.copy(seqNo = STEP6.seqNo)))(any[DBSession])
  }

  test("That updateSeqNo from last to 0 updates all learningsteps in between") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP6.id.get))(any[DBSession]))
      .thenReturn(Some(STEP6))

    val updatedStep =
      service.updateSeqNo(PRIVATE_ID, STEP6.id.get, STEP1.seqNo, PRIVATE_OWNER)
    updatedStep.get.seqNo should equal(STEP1.seqNo)

    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP6.copy(seqNo = STEP1.seqNo)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP1.copy(seqNo = STEP1.seqNo + 1)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP2.copy(seqNo = STEP2.seqNo + 1)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP3.copy(seqNo = STEP3.seqNo + 1)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP4.copy(seqNo = STEP4.seqNo + 1)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP5.copy(seqNo = STEP5.seqNo + 1)))(any[DBSession])
  }

  test("That updateSeqNo between two middle steps only updates the two middle steps") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP2.id.get))(any[DBSession]))
      .thenReturn(Some(STEP2))

    val updatedStep =
      service.updateSeqNo(PRIVATE_ID, STEP2.id.get, STEP3.seqNo, PRIVATE_OWNER)
    updatedStep.get.seqNo should equal(STEP3.seqNo)

    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP2.copy(seqNo = STEP3.seqNo)))(any[DBSession])
    verify(learningPathRepository, times(1))
      .updateLearningStep(eqTo(STEP3.copy(seqNo = STEP2.seqNo)))(any[DBSession])
  }

  test("That updateSeqNo also update seqNo for all affected steps") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(Some(STEP1))

    val updatedStep =
      service.updateSeqNo(PRIVATE_ID, STEP1.id.get, 1, PRIVATE_OWNER)
    updatedStep.get.seqNo should equal(1)

    verify(learningPathRepository, times(2))
      .updateLearningStep(any[LearningStep])(any[DBSession])
  }

  test("new fromExisting2 should allow laugage fields set to unknown") {
    val learningpathWithUnknownLang = PUBLISHED_LEARNINGPATH.copy(title = Seq(Title("what språk is this", "unknown")))

    when(learningPathRepository.withId(eqTo(learningpathWithUnknownLang.id.get))(any[DBSession]))
      .thenReturn(Some(learningpathWithUnknownLang))
    when(learningPathRepository.insert(any[LearningPath])(any[DBSession]))
      .thenReturn(learningpathWithUnknownLang)

    val newCopy =
      NewCopyLearningPathV2("hehe", None, "nb", None, None, None, None)
    service
      .newFromExistingV2(learningpathWithUnknownLang.id.get, newCopy, TokenUser("me", Set.empty, None))
      .isSuccess should be(true)
  }

  test("That newFromExistingV2 throws exception when user is not owner of the path and the path is private") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))

    val Failure(ex) = service.newFromExistingV2(PRIVATE_ID, NEW_COPIED_LEARNINGPATHV2, PUBLISHED_OWNER)
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("Owner updates step of published should update status to UNLISTED") {
    val newDate          = NDLADate.fromUnixTime(648000000)
    val stepWithBadTitle = STEP1.copy(title = Seq(common.Title("Dårlig tittel", "nb")))

    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PUBLISHED_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(Some(STEP1))
    when(learningPathRepository.learningStepsFor(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(List())
    when(learningPathRepository.updateLearningStep(eqTo(stepWithBadTitle))(any[DBSession])).thenReturn(stepWithBadTitle)
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenAnswer((i: InvocationOnMock) =>
      i.getArgument[domain.LearningPath](0)
    )
    when(clock.now()).thenReturn(newDate)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val updatedLs = UpdatedLearningStepV2(1, Some("Dårlig tittel"), "nb", None, None, None, None, None)
    service.updateLearningStepV2(PUBLISHED_ID, STEP1.id.get, updatedLs, PUBLISHED_OWNER)
    val updatedPath = PUBLISHED_LEARNINGPATH.copy(
      status = LearningPathStatus.UNLISTED,
      lastUpdated = newDate,
      learningsteps = Some(PUBLISHED_LEARNINGPATH.learningsteps.get.tail ++ List(stepWithBadTitle))
    )

    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(stepWithBadTitle))(any[DBSession])
    verify(learningPathRepository, times(1)).update(eqTo(updatedPath))(any[DBSession])
    verify(searchIndexService, times(1)).indexDocument(updatedPath)
    verify(searchIndexService, times(0)).deleteDocument(eqTo(updatedPath), any)
  }

  test("owner updates published path should update status to unlisted") {
    val newDate = NDLADate.fromUnixTime(648000000)
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PUBLISHED_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(Some(STEP1))
    when(learningPathRepository.learningStepsFor(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(List())
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenAnswer((i: InvocationOnMock) =>
      i.getArgument[LearningPath](0)
    )
    when(clock.now()).thenReturn(newDate)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val lpToUpdate = UpdatedLearningPathV2(1, Some("YapThisUpdated"), "nb", None, None, None, None, None, None)
    service.updateLearningPathV2(PUBLISHED_ID, lpToUpdate, PUBLISHED_OWNER)

    val expectedUpdatedPath = PUBLISHED_LEARNINGPATH.copy(
      title = List(Title("YapThisUpdated", "nb")),
      status = LearningPathStatus.UNLISTED,
      lastUpdated = newDate
    )

    verify(learningPathRepository, times(1)).update(eqTo(expectedUpdatedPath))(any[DBSession])
    verify(searchIndexService, times(1)).indexDocument(expectedUpdatedPath)
    verify(searchIndexService, times(0)).deleteDocument(any[domain.LearningPath], any)
  }

  test("owner updates step private should not update status") {
    val newDate          = NDLADate.fromUnixTime(648000000)
    val stepWithBadTitle = STEP1.copy(title = Seq(common.Title("Dårlig tittel", "nb")))

    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession])).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(Some(STEP1))
    when(learningPathRepository.learningStepsFor(eqTo(PRIVATE_ID))(any[DBSession])).thenReturn(List())
    when(learningPathRepository.updateLearningStep(eqTo(stepWithBadTitle))(any[DBSession])).thenReturn(stepWithBadTitle)
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenAnswer((i: InvocationOnMock) =>
      i.getArgument[LearningPath](0)
    )
    when(clock.now()).thenReturn(newDate)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val updatedLs = UpdatedLearningStepV2(1, Some("Dårlig tittel"), "nb", None, None, None, None, None)
    service.updateLearningStepV2(PRIVATE_ID, STEP1.id.get, updatedLs, PRIVATE_OWNER)
    val updatedPath = PRIVATE_LEARNINGPATH.copy(
      lastUpdated = newDate,
      learningsteps = Some(PRIVATE_LEARNINGPATH.learningsteps.get.tail ++ List(stepWithBadTitle))
    )

    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(stepWithBadTitle))(any[DBSession])
    verify(learningPathRepository, times(1)).update(eqTo(updatedPath))(any[DBSession])
    verify(searchIndexService, times(1)).indexDocument(updatedPath)
    verify(searchIndexService, times(0)).deleteDocument(eqTo(updatedPath), any)
  }

  test("admin updates step should not update status") {
    val newDate          = NDLADate.fromUnixTime(648000000)
    val stepWithBadTitle = STEP1.copy(title = Seq(common.Title("Dårlig tittel", "nb")))

    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PUBLISHED_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(Some(STEP1))
    when(learningPathRepository.learningStepsFor(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(List())
    when(learningPathRepository.updateLearningStep(eqTo(stepWithBadTitle))(any[DBSession])).thenReturn(stepWithBadTitle)
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenAnswer((i: InvocationOnMock) =>
      i.getArgument[LearningPath](0)
    )
    when(clock.now()).thenReturn(newDate)

    val updatedLs = UpdatedLearningStepV2(1, Some("Dårlig tittel"), "nb", None, None, None, None, None)
    service.updateLearningStepV2(
      PUBLISHED_ID,
      STEP1.id.get,
      updatedLs,
      PUBLISHED_OWNER.copy(permissions = Set(LEARNINGPATH_API_ADMIN))
    )
    val updatedPath = PUBLISHED_LEARNINGPATH.copy(
      lastUpdated = newDate,
      learningsteps = Some(PUBLISHED_LEARNINGPATH.learningsteps.get.tail ++ List(stepWithBadTitle))
    )

    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(stepWithBadTitle))(any[DBSession])
    verify(learningPathRepository, times(1)).update(eqTo(updatedPath))(any[DBSession])
    verify(searchIndexService, times(1)).indexDocument(updatedPath)
  }

  test("That newFromExistingV2 returns None when given id does not exist") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(None)
    val Failure(ex) = service.newFromExistingV2(PUBLISHED_ID, NEW_COPIED_LEARNINGPATHV2, PUBLISHED_OWNER)
    ex.isInstanceOf[NotFoundException] should be(true)
  }

  test("That basic-information unique per learningpath is reset in newFromExistingV2") {
    val now = NDLADate.now()
    when(clock.now()).thenReturn(now)

    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH_NO_STEPS))
    when(learningPathRepository.insert(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PUBLISHED_LEARNINGPATH_NO_STEPS)

    service.newFromExistingV2(PUBLISHED_ID, NEW_COPIED_LEARNINGPATHV2, PRIVATE_OWNER)

    val expectedNewLearningPath = PUBLISHED_LEARNINGPATH_NO_STEPS.copy(
      id = None,
      revision = None,
      externalId = None,
      isBasedOn = Some(PUBLISHED_ID),
      status = domain.LearningPathStatus.PRIVATE,
      verificationStatus = LearningPathVerificationStatus.EXTERNAL,
      owner = PRIVATE_OWNER.id,
      lastUpdated = now
    )

    verify(learningPathRepository, times(1))
      .insert(eqTo(expectedNewLearningPath))(any)
  }

  test("That isBasedOn is not sat if the existing learningpath is PRIVATE") {
    val now = NDLADate.now()
    when(clock.now()).thenReturn(now)
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH_NO_STEPS))
    when(learningPathRepository.insert(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PRIVATE_LEARNINGPATH_NO_STEPS)

    service.newFromExistingV2(PRIVATE_ID, NEW_COPIED_LEARNINGPATHV2, PRIVATE_OWNER)

    val expectedNewLearningPath = PRIVATE_LEARNINGPATH_NO_STEPS.copy(
      id = None,
      revision = None,
      externalId = None,
      isBasedOn = None,
      status = domain.LearningPathStatus.PRIVATE,
      verificationStatus = LearningPathVerificationStatus.EXTERNAL,
      owner = PRIVATE_OWNER.id,
      lastUpdated = now
    )

    verify(learningPathRepository, times(1))
      .insert(eqTo(expectedNewLearningPath))(any)
  }

  test("That isBasedOn is sat if the existing learningpath is PUBLISHED") {
    val now = NDLADate.now()
    when(clock.now()).thenReturn(now)
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH_NO_STEPS))
    when(learningPathRepository.insert(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PUBLISHED_LEARNINGPATH_NO_STEPS)

    service.newFromExistingV2(PUBLISHED_ID, NEW_COPIED_LEARNINGPATHV2, PRIVATE_OWNER)

    val expectedNewLearningPath = PUBLISHED_LEARNINGPATH_NO_STEPS.copy(
      id = None,
      revision = None,
      externalId = None,
      isBasedOn = Some(PUBLISHED_ID),
      status = domain.LearningPathStatus.PRIVATE,
      verificationStatus = LearningPathVerificationStatus.EXTERNAL,
      owner = PRIVATE_OWNER.id,
      lastUpdated = now
    )

    verify(learningPathRepository, times(1))
      .insert(eqTo(expectedNewLearningPath))(any)
  }

  test("That all editable fields are overridden if specified in input in newFromExisting") {
    val now = NDLADate.now()
    when(clock.now()).thenReturn(now)

    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH_NO_STEPS))
    when(learningPathRepository.insert(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PUBLISHED_LEARNINGPATH_NO_STEPS)

    val titlesToOverride       = "Overridden title"
    val descriptionsToOverride = Some("Overridden description")
    val tagsToOverride         = Some(Seq("Overridden tag"))
    val coverPhotoId           = "9876"
    val coverPhotoToOverride   = Some(s"http://api.ndla.no/images/$coverPhotoId")
    val durationOverride       = Some(100)

    service.newFromExistingV2(
      PUBLISHED_ID,
      NEW_COPIED_LEARNINGPATHV2.copy(
        title = titlesToOverride,
        description = descriptionsToOverride,
        tags = tagsToOverride,
        coverPhotoMetaUrl = coverPhotoToOverride,
        duration = durationOverride
      ),
      PRIVATE_OWNER
    )

    val expectedNewLearningPath = PUBLISHED_LEARNINGPATH_NO_STEPS.copy(
      id = None,
      revision = None,
      externalId = None,
      isBasedOn = Some(PUBLISHED_ID),
      status = domain.LearningPathStatus.PRIVATE,
      verificationStatus = LearningPathVerificationStatus.EXTERNAL,
      owner = PRIVATE_OWNER.id,
      lastUpdated = now,
      title = Seq(converterService.asTitle(api.Title(titlesToOverride, "nb"))),
      description = descriptionsToOverride
        .map(desc => converterService.asDescription(api.Description(desc, "nb")))
        .toSeq,
      tags = tagsToOverride
        .map(tagSeq => converterService.asLearningPathTags(api.LearningPathTags(tagSeq, "nb")))
        .toSeq,
      coverPhotoId = Some(coverPhotoId),
      duration = durationOverride
    )

    verify(learningPathRepository, times(1))
      .insert(eqTo(expectedNewLearningPath))(any)
  }

  test("That learningsteps are copied but with basic information reset in newFromExistingV2") {
    val now = NDLADate.now()
    when(clock.now()).thenReturn(now)

    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.insert(any[domain.LearningPath])(any[DBSession]))
      .thenReturn(PUBLISHED_LEARNINGPATH)

    service.newFromExistingV2(PUBLISHED_ID, NEW_COPIED_LEARNINGPATHV2, PRIVATE_OWNER)

    val expectedNewLearningPath = PUBLISHED_LEARNINGPATH.copy(
      id = None,
      revision = None,
      externalId = None,
      isBasedOn = Some(PUBLISHED_ID),
      status = domain.LearningPathStatus.PRIVATE,
      verificationStatus = LearningPathVerificationStatus.EXTERNAL,
      owner = PRIVATE_OWNER.id,
      lastUpdated = now,
      learningsteps = PUBLISHED_LEARNINGPATH.learningsteps.map(
        _.map(_.copy(id = None, revision = None, externalId = None, learningPathId = None))
      )
    )

    verify(learningPathRepository, times(1))
      .insert(eqTo(expectedNewLearningPath))(any)

  }

  test("That delete message field deletes admin message") {
    val newDate = NDLADate.now()
    val originalLearningPath =
      PUBLISHED_LEARNINGPATH.copy(message = Some(Message("You need to fix some stuffs", "kari", clock.now())))
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(Some(originalLearningPath))
    when(learningPathRepository.learningStepWithId(eqTo(PUBLISHED_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(Some(STEP1))
    when(learningPathRepository.learningStepsFor(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(List())
    when(learningPathRepository.update(any[domain.LearningPath])(any[DBSession])).thenAnswer((i: InvocationOnMock) =>
      i.getArgument[LearningPath](0)
    )
    when(clock.now()).thenReturn(newDate)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val lpToUpdate = UpdatedLearningPathV2(1, None, "nb", None, None, None, None, None, Some(true))
    service.updateLearningPathV2(PUBLISHED_ID, lpToUpdate, PUBLISHED_OWNER)

    val expectedUpdatedPath = PUBLISHED_LEARNINGPATH.copy(
      status = LearningPathStatus.UNLISTED,
      lastUpdated = newDate,
      message = None
    )

    verify(learningPathRepository, times(1)).update(eqTo(expectedUpdatedPath))(any[DBSession])
  }

  test("That writeOrAccessDenied denies writes while write restriction is enabled.") {
    val readMock = mock[ReadService]
    when(readService.canWriteNow(any[TokenUser])).thenReturn(false)

    service.writeDuringWriteRestrictionOrAccessDenied(TokenUser("SomeDude", scopes = Set(), None)) {
      Success(readMock.tags)
    }
    verify(readMock, times(0)).tags
  }

  test("That updating config returns failure for non-admin users") {
    when(configRepository.updateConfigParam(any[ConfigMeta])(any[DBSession]))
      .thenReturn(Success(TestData.testConfigMeta))
    val Failure(ex) = service.updateConfig(
      ConfigKey.LearningpathWriteRestricted,
      ConfigMetaValue(true),
      TokenUser("Kari", Set(LEARNINGPATH_API_PUBLISH), None)
    )
    ex.isInstanceOf[AccessDeniedException] should be(true)
  }

  test("That updating config returns success if all is good") {
    when(configRepository.updateConfigParam(any[ConfigMeta])(any[DBSession]))
      .thenReturn(Success(TestData.testConfigMeta))
    val Success(_) = service.updateConfig(
      ConfigKey.LearningpathWriteRestricted,
      ConfigMetaValue(true),
      TokenUser("Kari", Set(LEARNINGPATH_API_ADMIN), None)
    )
  }

  test("That validation fails if IsWriteRestricted is not a boolean") {
    when(configRepository.updateConfigParam(any[ConfigMeta])(any[DBSession]))
      .thenReturn(Success(TestData.testConfigMeta))
    val Failure(ex) = service.updateConfig(
      ConfigKey.LearningpathWriteRestricted,
      ConfigMetaValue(List("123")),
      TokenUser("Kari", Set(LEARNINGPATH_API_ADMIN), None)
    )

    ex.isInstanceOf[ValidationException] should be(true)
  }

  test("That validation succeeds if IsWriteRestricted is a boolean") {
    when(configRepository.updateConfigParam(any[ConfigMeta])(any[DBSession]))
      .thenReturn(Success(TestData.testConfigMeta))
    val res = service.updateConfig(
      ConfigKey.LearningpathWriteRestricted,
      ConfigMetaValue(true),
      TokenUser("Kari", Set(LEARNINGPATH_API_ADMIN), None)
    )
    res.isSuccess should be(true)
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
    when(readService.getOrCreateMyNDLAUserIfNotExist(any, any)(any)).thenReturn(Success(emptyMyNDLAUser))
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
    when(readService.getOrCreateMyNDLAUserIfNotExist(any, any)(any)).thenReturn(Success(emptyMyNDLAUser))
    when(folderRepository.folderResourceConnectionCount(any)(any[DBSession])).thenReturn(Success(1))
    when(folderRepository.folderWithId(eqTo(mainFolderId))(any)).thenReturn(Success(folder))
    when(readService.getSingleFolderWithContent(eqTo(folder.id), any, eqTo(true))(any))
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
    verify(readService, times(1)).getSingleFolderWithContent(eqTo(folder.id), any, eqTo(true))(any)
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
    when(readService.getOrCreateMyNDLAUserIfNotExist(any, any)(any)).thenReturn(Success(emptyMyNDLAUser))
    when(folderRepository.folderResourceConnectionCount(eqTo(resourceId))(any)).thenReturn(Success(5))
    when(folderRepository.folderWithId(eqTo(mainFolderId))(any)).thenReturn(Success(folder))
    when(readService.getSingleFolderWithContent(eqTo(folder.id), any, eqTo(true))(any))
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
    verify(readService, times(1)).getSingleFolderWithContent(eqTo(folder.id), any, eqTo(true))(any)
  }

  test("that deleteConnection only deletes connection when there are several references to a resource") {
    val folderId       = UUID.randomUUID()
    val resourceId     = UUID.randomUUID()
    val correctFeideId = "FEIDE"
    val folder         = emptyDomainFolder.copy(id = folderId, feideId = "FEIDE")
    val resource       = emptyDomainResource.copy(id = resourceId, feideId = "FEIDE")
    val folderResource = FolderResource(folderId = folder.id, resourceId = resource.id, rank = 1)

    when(feideApiClient.getFeideID(any)).thenReturn(Success(correctFeideId))
    when(readService.getOrCreateMyNDLAUserIfNotExist(any, any)(any)).thenReturn(Success(emptyMyNDLAUser))
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
    when(readService.getOrCreateMyNDLAUserIfNotExist(any, any)(any)).thenReturn(Success(emptyMyNDLAUser))
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
    when(readService.getOrCreateMyNDLAUserIfNotExist(any, any)(any)).thenReturn(Success(emptyMyNDLAUser))
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
    when(readService.getOrCreateMyNDLAUserIfNotExist(any, any)(any)).thenReturn(Success(emptyMyNDLAUser))
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
    val newResource  = api.NewResource(resourceType = "", path = resourcePath, tags = None, resourceId = "1")
    val resource =
      domain.Resource(
        id = resourceId,
        feideId = feideId,
        path = resourcePath,
        resourceType = "",
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

    verify(folderRepository, times(1)).resourceWithPathAndTypeAndFeideId(eqTo(resourcePath), eqTo(""), eqTo(feideId))(
      any
    )
    verify(converterService, times(1)).toDomainResource(eqTo(newResource))
    verify(folderRepository, times(1)).insertResource(eqTo(feideId), eqTo(resourcePath), eqTo(""), any, any)(any)
    verify(folderRepository, times(1)).createFolderResourceConnection(eqTo(folderId), eqTo(resourceId), any)(any)
    verify(converterService, times(0)).mergeResource(any, any[NewResource])
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
    val newResource  = api.NewResource(resourceType = "", path = resourcePath, tags = None, resourceId = "1")
    val resource =
      domain.Resource(
        id = resourceId,
        feideId = feideId,
        path = resourcePath,
        resourceType = "",
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

    verify(folderRepository, times(1)).resourceWithPathAndTypeAndFeideId(eqTo(resourcePath), eqTo(""), eqTo(feideId))(
      any
    )
    verify(converterService, times(0)).toDomainResource(eqTo(newResource))
    verify(folderRepository, times(0)).insertResource(any, any, any, any, any)(any)
    verify(converterService, times(1)).mergeResource(eqTo(resource), eqTo(newResource))
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
    when(readService.getOrCreateMyNDLAUserIfNotExist(any, any)(any)).thenReturn(Success(emptyMyNDLAUser))
    when(folderRepository.folderWithId(eqTo(folder1Id))(any[DBSession])).thenReturn(Success(folder1))
    when(readService.getSingleFolderWithContent(eqTo(folder1Id), eqTo(true), eqTo(true))(any[DBSession]))
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

    verify(readService, times(1)).getSingleFolderWithContent(eqTo(folder1Id), eqTo(true), eqTo(true))(any)
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
    val newFolder = api.NewFolder(name = "asd", parentId = Some(parentId.toString), status = None, description = None)

    when(feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    when(readService.getOrCreateMyNDLAUserIfNotExist(any, any)(any)).thenReturn(Success(emptyMyNDLAUser))
    when(converterService.toUUIDValidated(eqTo(Some(parentId.toString)), eqTo("parentId")))
      .thenReturn(Success(parentId))
    when(folderRepository.folderWithFeideId(eqTo(parentId), eqTo(feideId))(any[DBSession]))
      .thenReturn(Success(emptyDomainFolder))
    when(folderRepository.foldersWithFeideAndParentID(eqTo(Some(parentId)), eqTo(feideId))(any))
      .thenReturn(Success(List.empty))
    when(folderRepository.getFoldersDepth(eqTo(parentId))(any[DBSession])).thenReturn(Success(props.MaxFolderDepth))
    when(folderRepository.getConnections(any)(any)).thenReturn(Success(List.empty))

    val Failure(result: ValidationException) = service.newFolder(newFolder, Some(feideId))
    result.errors.head.message should be(
      s"Folder can not be created, max folder depth limit of ${props.MaxFolderDepth} reached."
    )

    verify(folderRepository, times(0)).insertFolder(any, any)(any)
  }

  test("that folder is created if depth count is below the limit") {
    val created   = clock.now()
    val feideId   = "FEIDE"
    val folderId  = UUID.randomUUID()
    val parentId  = UUID.randomUUID()
    val newFolder = api.NewFolder(name = "asd", parentId = Some(parentId.toString), status = None, description = None)
    val domainFolder = domain.Folder(
      id = folderId,
      feideId = feideId,
      parentId = Some(parentId),
      name = "asd",
      status = domain.FolderStatus.PRIVATE,
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
    val belowLimit = props.MaxFolderDepth - 2

    when(feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    when(readService.getOrCreateMyNDLAUserIfNotExist(any, any)(any)).thenReturn(Success(emptyMyNDLAUser))
    when(converterService.toUUIDValidated(eqTo(Some(parentId.toString)), eqTo("parentId")))
      .thenReturn(Success(parentId))
    when(folderRepository.folderWithFeideId(eqTo(parentId), eqTo(feideId))(any[DBSession]))
      .thenReturn(Success(emptyDomainFolder))
    when(folderRepository.getFoldersDepth(eqTo(parentId))(any[DBSession])).thenReturn(Success(belowLimit))
    when(folderRepository.insertFolder(any, any)(any[DBSession])).thenReturn(Success(domainFolder))
    when(folderRepository.getConnections(any)(any)).thenReturn(Success(List.empty))
    when(folderRepository.getConnections(any)(any)).thenReturn(Success(List.empty))
    when(readService.getBreadcrumbs(any)(any)).thenReturn(Success(List.empty))
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
    val newFolder = api.NewFolder(name = "asd", parentId = Some(parentId.toString), status = None, description = None)
    val domainFolder = domain.Folder(
      id = folderId,
      feideId = feideId,
      parentId = Some(parentId),
      name = "asd",
      status = domain.FolderStatus.PRIVATE,
      subfolders = List.empty,
      resources = List.empty,
      rank = None,
      created = created,
      updated = created,
      shared = None,
      description = None
    )
    val siblingFolder = domain.Folder(
      id = UUID.randomUUID(),
      feideId = feideId,
      parentId = Some(parentId),
      name = "aSd",
      status = domain.FolderStatus.PRIVATE,
      subfolders = List.empty,
      resources = List.empty,
      rank = None,
      created = created,
      updated = created,
      shared = None,
      description = None
    )
    val belowLimit = props.MaxFolderDepth - 2

    when(feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    when(readService.getOrCreateMyNDLAUserIfNotExist(any, any)(any)).thenReturn(Success(emptyMyNDLAUser))
    when(converterService.toUUIDValidated(eqTo(Some(parentId.toString)), eqTo("parentId")))
      .thenReturn(Success(parentId))
    when(folderRepository.folderWithFeideId(eqTo(parentId), eqTo(feideId))(any[DBSession]))
      .thenReturn(Success(emptyDomainFolder))
    when(folderRepository.getFoldersDepth(eqTo(parentId))(any[DBSession])).thenReturn(Success(belowLimit))
    when(folderRepository.insertFolder(any, any)(any[DBSession])).thenReturn(Success(domainFolder))
    when(folderRepository.getConnections(any)(any)).thenReturn(Success(List.empty))
    when(readService.getBreadcrumbs(any)(any)).thenReturn(Success(List.empty))
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

    val existingFolder = domain.Folder(
      id = folderId,
      feideId = feideId,
      parentId = Some(parentId),
      name = "noe unikt",
      status = domain.FolderStatus.PRIVATE,
      subfolders = List.empty,
      resources = List.empty,
      rank = None,
      created = created,
      updated = created,
      shared = None,
      description = None
    )
    val siblingFolder = domain.Folder(
      id = UUID.randomUUID(),
      feideId = feideId,
      parentId = Some(parentId),
      name = "aSd",
      status = domain.FolderStatus.PRIVATE,
      subfolders = List.empty,
      resources = List.empty,
      rank = None,
      created = created,
      updated = created,
      shared = None,
      description = None
    )
    val belowLimit = props.MaxFolderDepth - 2

    when(feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    when(readService.getOrCreateMyNDLAUserIfNotExist(any, any)(any)).thenReturn(Success(emptyMyNDLAUser))
    when(converterService.toUUIDValidated(eqTo(Some(parentId.toString)), eqTo("parentId")))
      .thenReturn(Success(parentId))
    when(folderRepository.folderWithFeideId(eqTo(parentId), eqTo(feideId))(any[DBSession]))
      .thenReturn(Success(emptyDomainFolder))
    when(folderRepository.getFoldersDepth(eqTo(parentId))(any[DBSession])).thenReturn(Success(belowLimit))
    when(readService.getBreadcrumbs(any)(any)).thenReturn(Success(List.empty))
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

    val existingFolder = domain.Folder(
      id = folderId,
      feideId = feideId,
      parentId = Some(parentId),
      name = "noe unikt",
      status = domain.FolderStatus.PRIVATE,
      subfolders = List.empty,
      resources = List.empty,
      rank = None,
      created = created,
      updated = created,
      shared = None,
      description = None
    )
    val mergedFolder = existingFolder.copy(status = FolderStatus.SHARED)
    val siblingFolder = domain.Folder(
      id = UUID.randomUUID(),
      feideId = feideId,
      parentId = Some(parentId),
      name = "aSd",
      status = domain.FolderStatus.PRIVATE,
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
    val belowLimit = props.MaxFolderDepth - 2

    when(feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    when(readService.getOrCreateMyNDLAUserIfNotExist(any, any)(any)).thenReturn(Success(emptyMyNDLAUser))
    when(converterService.toUUIDValidated(eqTo(Some(parentId.toString)), eqTo("parentId")))
      .thenReturn(Success(parentId))
    when(folderRepository.folderWithFeideId(eqTo(parentId), eqTo(feideId))(any[DBSession]))
      .thenReturn(Success(emptyDomainFolder))
    when(folderRepository.getFoldersDepth(eqTo(parentId))(any[DBSession])).thenReturn(Success(belowLimit))
    when(readService.getBreadcrumbs(any)(any)).thenReturn(Success(List.empty))
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

  test("That updateUserData updates user if user exist") {
    val feideId = "feide"
    val userBefore = domain.MyNDLAUser(
      id = 42,
      feideId = feideId,
      favoriteSubjects = Seq("h", "b"),
      userRole = UserRole.STUDENT,
      lastUpdated = clock.now(),
      organization = "oslo",
      email = "example@email.com",
      arenaEnabled = false,
      displayName = "Feide",
      shareName = false
    )
    val updatedUserData =
      api.UpdatedMyNDLAUser(favoriteSubjects = Some(Seq("r", "e")), arenaEnabled = None, shareName = None)
    val userAfterMerge = domain.MyNDLAUser(
      id = 42,
      feideId = feideId,
      favoriteSubjects = Seq("r", "e"),
      userRole = UserRole.STUDENT,
      lastUpdated = clock.now(),
      organization = "oslo",
      email = "example@email.com",
      arenaEnabled = false,
      displayName = "Feide",
      shareName = false
    )
    val expected = api.MyNDLAUser(
      id = 42,
      favoriteSubjects = Seq("r", "e"),
      role = "student",
      organization = "oslo",
      arenaEnabled = false,
      shareName = false
    )

    when(feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    when(readService.getOrCreateMyNDLAUserIfNotExist(any, any)(any)).thenReturn(Success(emptyMyNDLAUser))
    when(readService.getMyNDLAEnabledOrgs).thenReturn(Success(List.empty))
    when(userRepository.userWithFeideId(eqTo(feideId))(any)).thenReturn(Success(Some(userBefore)))
    when(userRepository.updateUser(eqTo(feideId), any)(any)).thenReturn(Success(userAfterMerge))

    service.updateMyNDLAUserData(updatedUserData, Some(feideId)) should be(Success(expected))

    verify(userRepository, times(1)).userWithFeideId(any)(any)
    verify(userRepository, times(1)).updateUser(any, any)(any)
  }

  test("That updateUserData fails if user does not exist") {
    val feideId = "feide"
    val updatedUserData =
      api.UpdatedMyNDLAUser(favoriteSubjects = Some(Seq("r", "e")), arenaEnabled = None, shareName = None)

    when(feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    when(readService.getOrCreateMyNDLAUserIfNotExist(any, any)(any)).thenReturn(Success(emptyMyNDLAUser))
    when(userRepository.userWithFeideId(eqTo(feideId))(any)).thenReturn(Success(None))

    service.updateMyNDLAUserData(updatedUserData, Some(feideId)) should be(
      Failure(NotFoundException(s"User with feide_id $feideId was not found"))
    )

    verify(userRepository, times(1)).userWithFeideId(any)(any)
    verify(userRepository, times(0)).updateUser(any, any)(any)
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
    when(readService.getOrCreateMyNDLAUserIfNotExist(any, any)(any)).thenReturn(Success(emptyMyNDLAUser))
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

  test(
    "that canWriteDuringWriteRestrictionsOrAccessDenied returns Success if user is a Teacher during write restriction"
  ) {
    val myNDLAUser = emptyMyNDLAUser.copy(userRole = UserRole.TEACHER)

    when(readService.getOrCreateMyNDLAUserIfNotExist(any, any)(any)).thenReturn(Success(myNDLAUser))
    when(readService.isWriteRestricted).thenReturn(true)

    val result = service.canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied("spiller ing", Some("en rolle"))
    result.isSuccess should be(true)
  }

  test(
    "that canWriteDuringWriteRestrictionsOrAccessDenied returns Failure if user is a Student during write restriction"
  ) {
    val myNDLAUser = emptyMyNDLAUser.copy(userRole = UserRole.STUDENT)

    when(readService.getOrCreateMyNDLAUserIfNotExist(any, any)(any)).thenReturn(Success(myNDLAUser))
    when(readService.isMyNDLAWriteRestricted).thenReturn(true)

    val result = service.canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied("spiller ing", Some("en rolle"))
    result should be(Failure(AccessDeniedException("You do not have write access while write restriction is active.")))
  }

  test(
    "that canWriteDuringWriteRestrictionsOrAccessDenied returns Success if user is a Student not during write restriction"
  ) {
    val myNDLAUser = emptyMyNDLAUser.copy(userRole = UserRole.STUDENT)

    when(readService.getOrCreateMyNDLAUserIfNotExist(any, any)(any)).thenReturn(Success(myNDLAUser))
    when(readService.isMyNDLAWriteRestricted).thenReturn(false)

    val result = service.canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied("spiller ing", Some("en rolle"))
    result.isSuccess should be(true)
  }

  test("that isOperationAllowedOrAccessDenied denies access if user is student and wants to share a folder") {
    val myNDLAUser = emptyMyNDLAUser.copy(userRole = UserRole.STUDENT)
    when(readService.getOrCreateMyNDLAUserIfNotExist(any, any)(any)).thenReturn(Success(myNDLAUser))

    val updatedFolder   = UpdatedFolder(name = None, status = Some("shared"), description = None)
    val Failure(result) = service.isOperationAllowedOrAccessDenied("feideid", Some("accesstoken"), updatedFolder)
    result.getMessage should be("You do not have necessary permissions to share folders.")
  }

  test(
    "that isOperationAllowedOrAccessDenied denies access if user is student and wants to update a folder during exam"
  ) {
    val myNDLAUser = emptyMyNDLAUser.copy(userRole = UserRole.STUDENT)
    when(readService.getOrCreateMyNDLAUserIfNotExist(any, any)(any)).thenReturn(Success(myNDLAUser))
    when(readService.isMyNDLAWriteRestricted).thenReturn(true)

    val updatedFolder   = UpdatedFolder(name = Some("asd"), status = None, description = None)
    val Failure(result) = service.isOperationAllowedOrAccessDenied("feideid", Some("accesstoken"), updatedFolder)
    result.getMessage should be("You do not have write access while write restriction is active.")
  }

  test("that isOperationAllowedOrAccessDenied allows student to update a folder outside of the examination time") {
    val myNDLAUser = emptyMyNDLAUser.copy(userRole = UserRole.STUDENT)
    when(readService.getOrCreateMyNDLAUserIfNotExist(any, any)(any)).thenReturn(Success(myNDLAUser))
    when(readService.isMyNDLAWriteRestricted).thenReturn(false)

    val updatedFolder = UpdatedFolder(name = Some("asd"), status = None, description = None)
    val result        = service.isOperationAllowedOrAccessDenied("feideid", Some("accesstoken"), updatedFolder)
    result.isSuccess should be(true)
  }

  test(
    "that isOperationAllowedOrAccessDenied allows teacher to cut the cake and eat it too"
  ) {
    val myNDLAUser = emptyMyNDLAUser.copy(userRole = UserRole.TEACHER)
    when(readService.getOrCreateMyNDLAUserIfNotExist(any, any)(any)).thenReturn(Success(myNDLAUser))
    when(readService.isMyNDLAWriteRestricted).thenReturn(true)

    val folderWithUpdatedName   = UpdatedFolder(name = Some("asd"), status = None, description = None)
    val folderWithUpdatedStatus = UpdatedFolder(name = None, status = Some("shared"), description = None)
    val result1 = service.isOperationAllowedOrAccessDenied("feideid", Some("accesstoken"), folderWithUpdatedName)
    val result2 = service.isOperationAllowedOrAccessDenied("feideid", Some("accesstoken"), folderWithUpdatedStatus)
    result1.isSuccess should be(true)
    result2.isSuccess should be(true)
  }

  test("that changeStatusToSharedIfParentIsShared actually changes the status if parent is shared") {
    val newFolder =
      api.NewFolder(
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
      api.NewFolder(
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
      api.NewFolder(
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
      api.NewFolder(
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
}
