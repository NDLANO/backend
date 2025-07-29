/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import no.ndla.common.errors.{AccessDeniedException, NotFoundException}
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.learningpath.{
  LearningPath,
  LearningPathStatus,
  LearningPathVerificationStatus,
  LearningStep,
  LearningpathCopyright,
  StepStatus,
  StepType
}
import no.ndla.common.model.domain.{Author, ContributorType, Title}
import no.ndla.learningpathapi.{UnitSuite, UnitTestEnvironment}
import no.ndla.mapping.License
import no.ndla.network.tapir.auth.TokenUser
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import scalikejdbc.DBSession

import scala.util.Failure

class ReadServiceTest extends UnitSuite with UnitTestEnvironment {

  var service: ReadService = _

  val PUBLISHED_ID = 1L
  val PRIVATE_ID   = 2L

  val PUBLISHED_OWNER: TokenUser       = TokenUser("published_owner", Set.empty, None)
  val PRIVATE_OWNER: TokenUser         = TokenUser("private_owner", Set.empty, None)
  val cruz: Author                     = Author(ContributorType.Writer, "Lyin' Ted")
  val license: String                  = License.PublicDomain.toString
  val copyright: LearningpathCopyright = LearningpathCopyright(license, List(cruz))

  val PUBLISHED_LEARNINGPATH: LearningPath = LearningPath(
    id = Some(PUBLISHED_ID),
    revision = Some(1),
    externalId = None,
    isBasedOn = None,
    title = List(Title("Tittel", "nb")),
    description = List(),
    coverPhotoId = None,
    duration = Some(1),
    status = LearningPathStatus.PUBLISHED,
    verificationStatus = LearningPathVerificationStatus.EXTERNAL,
    created = NDLADate.now(),
    lastUpdated = NDLADate.now(),
    tags = List(),
    owner = PUBLISHED_OWNER.id,
    copyright = copyright,
    isMyNDLAOwner = false,
    responsible = None
  )

  val PRIVATE_LEARNINGPATH: LearningPath = LearningPath(
    id = Some(PRIVATE_ID),
    revision = Some(1),
    externalId = None,
    isBasedOn = None,
    title = List(Title("Tittel", "nb")),
    description = List(),
    coverPhotoId = None,
    duration = Some(1),
    status = LearningPathStatus.PRIVATE,
    verificationStatus = LearningPathVerificationStatus.EXTERNAL,
    created = NDLADate.now(),
    lastUpdated = NDLADate.now(),
    tags = List(),
    owner = PRIVATE_OWNER.id,
    copyright = copyright,
    isMyNDLAOwner = false,
    responsible = None
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
    List(),
    StepType.TEXT,
    None,
    showTitle = false,
    StepStatus.ACTIVE
  )

  override def beforeEach(): Unit = {
    service = new ReadService
    resetMocks()
  }

  test("That withIdV2 returns None when id does not exist") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(None)
    val Failure(ex) = service.withIdV2(PUBLISHED_ID, "nb", fallback = false, TokenUser.PublicUser.toCombined)
    ex.isInstanceOf[NotFoundException]
  }

  test("That withIdV2 returns a learningPath when the status is PUBLISHED") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    val learningPath = service.withIdV2(PUBLISHED_ID, "nb", fallback = false, TokenUser.PublicUser.toCombined)
    assert(learningPath.isSuccess)
    assert(learningPath.get.id == PUBLISHED_ID)
    assert(learningPath.get.status == "PUBLISHED")
  }

  test("That withId returns a learningPath when the status is PUBLISHED and user is not the owner") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    val learningPath = service.withIdV2(PUBLISHED_ID, "nb", fallback = false, PRIVATE_OWNER.toCombined)
    assert(learningPath.isSuccess)
    assert(learningPath.get.id == PUBLISHED_ID)
    assert(learningPath.get.status == "PUBLISHED")
  }

  test("That withId throws an AccessDeniedException when the status is PRIVATE and no user") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    val Failure(ex) = service.withIdV2(PRIVATE_ID, "nb", fallback = false, TokenUser.PublicUser.toCombined)
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("That withId throws an AccessDeniedException when the status is PRIVATE and user is not the owner") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    val Failure(ex) = service.withIdV2(PRIVATE_ID, "nb", fallback = false, PUBLISHED_OWNER.toCombined)
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("That withId returns a learningPath when the status is PRIVATE and user is the owner") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    val learningPath = service.withIdV2(PRIVATE_ID, "nb", fallback = false, PRIVATE_OWNER.toCombined)
    assert(learningPath.isSuccess)
    assert(learningPath.get.id == PRIVATE_ID)
    assert(learningPath.get.status == "PRIVATE")
  }

  test("That statusFor returns None when id does not exist") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(None)
    val Failure(ex) = service.statusFor(PUBLISHED_ID, TokenUser.PublicUser.toCombined)
    ex.isInstanceOf[NotFoundException]
  }

  test("That statusFor returns a LearningPathStatus when the status is PUBLISHED") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    assertResult("PUBLISHED") {
      service.statusFor(PUBLISHED_ID, TokenUser.PublicUser.toCombined).map(_.status).get
    }
  }

  test("That statusFor throws an AccessDeniedException when the status is PRIVATE and no user") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))

    val Failure(ex) = service.statusFor(2, TokenUser.PublicUser.toCombined)
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("That statusFor throws an AccessDeniedException when the status is PRIVATE and user is not the owner") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    val Failure(ex) = service.statusFor(2, PUBLISHED_OWNER.toCombined)
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("That statusFor returns a LearningPathStatus when the status is PRIVATE and the user is the owner") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    assertResult("PRIVATE") {
      service.statusFor(PRIVATE_ID, PRIVATE_OWNER.toCombined).map(_.status).get
    }
  }

  test("That learningstepsFor returns None when the learningPath does not exist") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(None)
    val Failure(ex) = service.learningstepsForWithStatusV2(
      PUBLISHED_ID,
      StepStatus.ACTIVE,
      "nb",
      fallback = false,
      TokenUser.PublicUser.toCombined
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
      TokenUser.PublicUser.toCombined
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
      TokenUser.PublicUser.toCombined
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
      TokenUser.PublicUser.toCombined
    )
    learningSteps.isSuccess should be(true)
    learningSteps.get.learningsteps.size should be(1)
    learningSteps.get.learningsteps.head.id should equal(STEP2.id.get)
  }

  test("That learningstepsFor throws an AccessDeniedException when the status is PRIVATE and no user") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    val Failure(ex) =
      service.learningstepsForWithStatusV2(
        PRIVATE_ID,
        StepStatus.ACTIVE,
        "nb",
        fallback = false,
        TokenUser.PublicUser.toCombined
      )
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("That learningstepsFor throws an AccessDeniedException when the status is PRIVATE and user is not the owner") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    val Failure(ex) =
      service.learningstepsForWithStatusV2(
        PRIVATE_ID,
        StepStatus.ACTIVE,
        "nb",
        fallback = false,
        PUBLISHED_OWNER.toCombined
      )
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
        .learningstepsForWithStatusV2(PRIVATE_ID, StepStatus.ACTIVE, "nb", fallback = false, PRIVATE_OWNER.toCombined)
        .get
        .learningsteps
        .length
    }
  }

  test("That learningstepV2For returns None when the learningPath does not exist") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession])).thenReturn(None)
    val Failure(ex) =
      service.learningstepV2For(PUBLISHED_ID, STEP1.id.get, "nb", fallback = false, TokenUser.PublicUser.toCombined)
    ex.isInstanceOf[NotFoundException]
  }

  test("That learningstepV2For returns None when the learningStep does not exist") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PUBLISHED_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(None)
    val Failure(ex) =
      service.learningstepV2For(PUBLISHED_ID, STEP1.id.get, "nb", fallback = false, TokenUser.PublicUser.toCombined)
    ex.isInstanceOf[NotFoundException]
  }

  test("That learningstepV2For returns the LearningStep when it exists") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(any[DBSession]))
      .thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PUBLISHED_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(Some(STEP1))
    assertResult(STEP1.id.get) {
      service
        .learningstepV2For(PUBLISHED_ID, STEP1.id.get, "nb", fallback = false, TokenUser.PublicUser.toCombined)
        .get
        .id
    }
  }

  test("That learningstepV2For returns the LearningStep when it exists and status is PRIVATE and user is the owner") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(any[DBSession]))
      .thenReturn(Some(STEP1))
    assertResult(STEP1.id.get) {
      service
        .learningstepV2For(PRIVATE_ID, STEP1.id.get, "nb", fallback = false, PRIVATE_OWNER.toCombined)
        .get
        .id
    }
  }

  test("That learningstepV2For throws an AccessDeniedException when the status is PRIVATE and no user") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    val Failure(ex) =
      service.learningstepV2For(PRIVATE_ID, STEP1.id.get, "nb", fallback = false, TokenUser.PublicUser.toCombined)
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("That learningstepFor throws an AccessDeniedException when the status is PRIVATE and user is not the owner") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(any[DBSession]))
      .thenReturn(Some(PRIVATE_LEARNINGPATH))
    val Failure(ex) =
      service.learningstepV2For(PRIVATE_ID, STEP1.id.get, "nb", fallback = false, PUBLISHED_OWNER.toCombined)
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

}
