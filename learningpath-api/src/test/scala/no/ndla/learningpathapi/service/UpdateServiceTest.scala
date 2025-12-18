/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.service

import no.ndla.common.errors.{
  AccessDeniedException,
  NotFoundException,
  OperationNotAllowedException,
  ValidationException,
}
import no.ndla.common.model.api.myndla.MyNDLAUserDTO
import no.ndla.common.model.domain.learningpath.*
import no.ndla.common.model.domain.myndla.UserRole.EMPLOYEE
import no.ndla.common.model.domain.{Author, ContributorType, Title, learningpath}
import no.ndla.common.model.{NDLADate, api as commonApi, domain as common}
import no.ndla.learningpathapi.*
import no.ndla.learningpathapi.model.*
import no.ndla.learningpathapi.model.api.*
import no.ndla.mapping.License
import no.ndla.network.model.CombinedUserWithMyNDLAUser
import no.ndla.network.tapir.auth.Permission.{LEARNINGPATH_API_ADMIN, LEARNINGPATH_API_PUBLISH, LEARNINGPATH_API_WRITE}
import no.ndla.network.tapir.auth.TokenUser
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{doAnswer, never, times, verify, when}
import org.mockito.invocation.InvocationOnMock
import scalikejdbc.DBSession

import scala.util.{Failure, Success, Try}
import org.mockito.ArgumentCaptor

class UpdateServiceTest extends UnitSuite with UnitTestEnvironment {
  override implicit lazy val converterService: ConverterService = new ConverterService
  var service: UpdateService                                    = scala.compiletime.uninitialized

  val PUBLISHED_ID: Long = 1L
  val PRIVATE_ID: Long   = 2L
  val now: NDLADate      = NDLADate.now()

  val PUBLISHED_OWNER: TokenUser = TokenUser("eier1", Set(LEARNINGPATH_API_WRITE, LEARNINGPATH_API_PUBLISH), None)
  val PRIVATE_OWNER: TokenUser   = TokenUser("eier2", Set(LEARNINGPATH_API_WRITE), None)

  val MYNDLA_USER = CombinedUserWithMyNDLAUser(
    None,
    MyNDLAUserDTO(
      id = 1L,
      feideId = "eier3",
      username = "eier3",
      email = "",
      displayName = "eier3",
      favoriteSubjects = Seq.empty,
      role = EMPLOYEE,
      organization = "",
      groups = Seq.empty,
      arenaEnabled = false,
    ),
  )

  val today: NDLADate = NDLADate.now()

  val STEP1: LearningStep = LearningStep(
    id = Some(1),
    revision = Some(1),
    externalId = None,
    learningPathId = None,
    seqNo = 0,
    title = List(Title("Tittel", "nb")),
    introduction = List(),
    description = List(),
    embedUrl = List(),
    articleId = None,
    `type` = StepType.TEXT,
    copyright = None,
    created = today,
    lastUpdated = today,
    owner = PRIVATE_OWNER.id,
    showTitle = true,
    status = StepStatus.ACTIVE,
  )
  val STEP2: LearningStep = LearningStep(
    id = Some(2),
    revision = Some(1),
    externalId = None,
    learningPathId = None,
    seqNo = 1,
    title = List(Title("Tittel", "nb")),
    introduction = List(),
    description = List(),
    embedUrl = List(),
    articleId = None,
    `type` = StepType.TEXT,
    copyright = None,
    created = today,
    lastUpdated = today,
    owner = PUBLISHED_OWNER.id,
    showTitle = true,
    status = StepStatus.ACTIVE,
  )
  val STEP3: LearningStep = LearningStep(
    id = Some(3),
    revision = Some(1),
    externalId = None,
    learningPathId = None,
    seqNo = 2,
    title = List(Title("Tittel", "nb")),
    introduction = List(),
    description = List(),
    embedUrl = List(),
    articleId = None,
    `type` = StepType.TEXT,
    copyright = None,
    created = today,
    lastUpdated = today,
    owner = "me",
    showTitle = true,
    status = StepStatus.ACTIVE,
  )
  val STEP4: LearningStep = LearningStep(
    id = Some(4),
    revision = Some(1),
    externalId = None,
    learningPathId = None,
    seqNo = 3,
    title = List(Title("Tittel", "nb")),
    introduction = List(),
    description = List(),
    embedUrl = List(),
    articleId = None,
    `type` = StepType.TEXT,
    copyright = None,
    created = today,
    lastUpdated = today,
    owner = "me",
    showTitle = true,
    status = StepStatus.ACTIVE,
  )
  val STEP5: LearningStep = LearningStep(
    id = Some(5),
    revision = Some(1),
    externalId = None,
    learningPathId = None,
    seqNo = 4,
    title = List(Title("Tittel", "nb")),
    introduction = List(),
    description = List(),
    embedUrl = List(),
    articleId = None,
    `type` = StepType.TEXT,
    copyright = None,
    created = today,
    lastUpdated = today,
    owner = "me",
    showTitle = true,
    status = StepStatus.ACTIVE,
  )
  val STEP6: LearningStep = LearningStep(
    id = Some(6),
    revision = Some(1),
    externalId = None,
    learningPathId = None,
    seqNo = 5,
    title = List(Title("Tittel", "nb")),
    introduction = List(),
    description = List(),
    embedUrl = List(),
    articleId = None,
    `type` = StepType.TEXT,
    copyright = None,
    created = today,
    lastUpdated = today,
    owner = "me",
    showTitle = true,
    status = StepStatus.ACTIVE,
  )

  val NEW_STEPV2: NewLearningStepV2DTO = NewLearningStepV2DTO(
    "Tittel",
    Some("Beskrivelse"),
    None,
    "nb",
    None,
    Some(api.EmbedUrlV2DTO("", "oembed")),
    true,
    "TEXT",
    None,
    None,
  )

  val UPDATED_STEPV2: UpdatedLearningStepV2DTO = UpdatedLearningStepV2DTO(
    1,
    commonApi.UpdateWith("Tittel"),
    commonApi.Missing,
    "nb",
    commonApi.UpdateWith("Beskrivelse"),
    commonApi.Missing,
    commonApi.Missing,
    Some(false),
    None,
    None,
    commonApi.Missing,
  )

  val rubio: Author                    = Author(ContributorType.Writer, "Little Marco")
  val license: String                  = License.PublicDomain.toString
  val copyright: LearningpathCopyright = LearningpathCopyright(license, List(rubio))
  val apiRubio: commonApi.AuthorDTO    = commonApi.AuthorDTO(ContributorType.Writer, "Little Marco")
  val apiLicense: commonApi.LicenseDTO = commonApi.LicenseDTO(
    License.PublicDomain.toString,
    Some("Public Domain"),
    Some("https://creativecommons.org/about/pdm"),
  )
  val apiCopyright: CopyrightDTO = api.CopyrightDTO(apiLicense, List(apiRubio))

  val PUBLISHED_LEARNINGPATH: LearningPath = LearningPath(
    id = Some(PUBLISHED_ID),
    revision = Some(1),
    externalId = Some("1"),
    isBasedOn = None,
    title = List(Title("Tittel", "nb")),
    description = List(Description("Beskrivelse", "nb")),
    introduction = List(Introduction("<section><p>Introduksjon</p></section>", "nb")),
    coverPhotoId = Some("1234"),
    duration = Some(1),
    status = LearningPathStatus.PUBLISHED,
    verificationStatus = LearningPathVerificationStatus.EXTERNAL,
    created = today,
    lastUpdated = today,
    tags = List(),
    owner = PUBLISHED_OWNER.id,
    copyright = copyright,
    isMyNDLAOwner = false,
    learningsteps = Some(STEP1 :: STEP2 :: STEP3 :: STEP4 :: STEP5 :: STEP6 :: Nil),
    responsible = None,
    comments = Seq.empty,
    priority = common.Priority.Unspecified,
    revisionMeta = common.RevisionMeta.default,
    grepCodes = Seq.empty,
  )

  val PUBLISHED_LEARNINGPATH_NO_STEPS: LearningPath = LearningPath(
    id = Some(PUBLISHED_ID),
    revision = Some(1),
    externalId = Some("1"),
    isBasedOn = None,
    title = List(Title("Tittel", "nb")),
    description = List(Description("Beskrivelse", "nb")),
    introduction = List(Introduction("<section><p>Introduksjon</p></section>", "nb")),
    coverPhotoId = None,
    duration = Some(1),
    status = learningpath.LearningPathStatus.PUBLISHED,
    verificationStatus = LearningPathVerificationStatus.EXTERNAL,
    created = today,
    lastUpdated = today,
    tags = List(),
    owner = PUBLISHED_OWNER.id,
    copyright = copyright,
    isMyNDLAOwner = false,
    learningsteps = None,
    responsible = None,
    comments = Seq.empty,
    priority = common.Priority.Unspecified,
    revisionMeta = common.RevisionMeta.default,
    grepCodes = Seq.empty,
  )

  val PRIVATE_LEARNINGPATH: LearningPath = LearningPath(
    id = Some(PRIVATE_ID),
    revision = Some(1),
    externalId = None,
    isBasedOn = None,
    title = List(Title("Tittel", "nb")),
    description = List(Description("Beskrivelse", "nb")),
    introduction = List(Introduction("<section><p>Introduksjon</p></section>", "nb")),
    coverPhotoId = None,
    duration = Some(1),
    status = learningpath.LearningPathStatus.PRIVATE,
    verificationStatus = LearningPathVerificationStatus.EXTERNAL,
    created = today,
    lastUpdated = today,
    tags = List(),
    owner = PRIVATE_OWNER.id,
    copyright = copyright,
    isMyNDLAOwner = false,
    learningsteps = Some(STEP1 :: STEP2 :: STEP3 :: STEP4 :: STEP5 :: STEP6 :: Nil),
    responsible = None,
    comments = Seq.empty,
    priority = common.Priority.Unspecified,
    revisionMeta = common.RevisionMeta.default,
    grepCodes = Seq.empty,
  )

  val PRIVATE_LEARNINGPATH_NO_STEPS: LearningPath = LearningPath(
    id = Some(PRIVATE_ID),
    revision = Some(1),
    externalId = None,
    isBasedOn = None,
    title = List(Title("Tittel", "nb")),
    description = List(Description("Beskrivelse", "nb")),
    introduction = List(Introduction("<section><p>Introduksjon</p></section>", "nb")),
    coverPhotoId = None,
    duration = Some(1),
    status = learningpath.LearningPathStatus.PRIVATE,
    verificationStatus = LearningPathVerificationStatus.EXTERNAL,
    created = today,
    lastUpdated = today,
    tags = List(),
    owner = PRIVATE_OWNER.id,
    copyright = copyright,
    isMyNDLAOwner = false,
    learningsteps = None,
    responsible = None,
    comments = Seq.empty,
    priority = common.Priority.Unspecified,
    revisionMeta = common.RevisionMeta.default,
    grepCodes = Seq.empty,
  )

  val DELETED_LEARNINGPATH: LearningPath = LearningPath(
    id = Some(PRIVATE_ID),
    revision = Some(1),
    externalId = None,
    isBasedOn = None,
    title = List(Title("Tittel", "nb")),
    description = List(Description("Beskrivelse", "nb")),
    introduction = List(Introduction("<section><p>Introduksjon</p></section>", "nb")),
    coverPhotoId = None,
    duration = Some(1),
    status = learningpath.LearningPathStatus.DELETED,
    verificationStatus = LearningPathVerificationStatus.EXTERNAL,
    created = today,
    lastUpdated = today,
    tags = List(),
    owner = PRIVATE_OWNER.id,
    copyright = copyright,
    isMyNDLAOwner = false,
    learningsteps = Some(STEP1 :: STEP2 :: STEP3 :: STEP4 :: STEP5 :: STEP6 :: Nil),
    responsible = None,
    comments = Seq.empty,
    priority = common.Priority.Unspecified,
    revisionMeta = common.RevisionMeta.default,
    grepCodes = Seq.empty,
  )
  val NEW_PRIVATE_LEARNINGPATHV2: NewLearningPathV2DTO = NewLearningPathV2DTO(
    "Tittel",
    Some("Beskrivelse"),
    None,
    Some(1),
    None,
    "nb",
    Some(apiCopyright),
    None,
    None,
    None,
    None,
    None,
    None,
  )
  val NEW_COPIED_LEARNINGPATHV2: NewCopyLearningPathV2DTO = NewCopyLearningPathV2DTO(
    "Tittel",
    Some("<section><p>Introduksjon</p></section>"),
    Some("Beskrivelse"),
    "nb",
    None,
    Some(1),
    None,
    None,
  )

  val UPDATED_PRIVATE_LEARNINGPATHV2: UpdatedLearningPathV2DTO = UpdatedLearningPathV2DTO(
    1,
    None,
    "nb",
    None,
    commonApi.Missing,
    Some(1),
    None,
    Some(apiCopyright),
    None,
    commonApi.Missing,
    None,
    None,
    None,
    commonApi.Missing,
    None,
  )

  val UPDATED_PUBLISHED_LEARNINGPATHV2: UpdatedLearningPathV2DTO = UpdatedLearningPathV2DTO(
    1,
    None,
    "nb",
    None,
    commonApi.Missing,
    Some(1),
    None,
    Some(apiCopyright),
    None,
    commonApi.Missing,
    None,
    None,
    None,
    commonApi.Missing,
    None,
  )

  override def beforeEach(): Unit = {
    service = new UpdateService
    resetMocks()
    when(searchIndexService.deleteDocument(any[LearningPath], any)).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument[LearningPath](0))
    )
    when(searchIndexService.indexDocument(any[LearningPath])).thenAnswer((i: InvocationOnMock) =>
      Success(i.getArgument[LearningPath](0))
    )
    when(taxonomyApiClient.updateTaxonomyForLearningPath(any[LearningPath], any[Boolean], any)).thenAnswer(
      (i: InvocationOnMock) => Success(i.getArgument[LearningPath](0))
    )
    when(learningPathValidator.validate(any[LearningPath], any[Boolean])).thenAnswer(i => Success(i.getArgument(0)))
    when(learningPathValidator.validate(any[UpdatedLearningPathV2DTO], any[LearningPath])).thenAnswer(i =>
      Success(i.getArgument(0))
    )
    when(learningStepValidator.validate(any[LearningStep], any[LearningPath], any[Boolean])).thenAnswer(
      (i: InvocationOnMock) => Success(i.getArgument[LearningStep](0))
    )
    doAnswer((i: InvocationOnMock) => {
      val x = i.getArgument[DBSession => Try[?]](0)
      x(mock[DBSession])
    }).when(learningPathRepository).inTransaction(any())(using any())
  }

  test("That addLearningPathV2 inserts the given LearningPathV2") {
    when(learningPathRepository.insert(any[LearningPath])(using any[DBSession])).thenReturn(PRIVATE_LEARNINGPATH)

    val saved = service.addLearningPathV2(NEW_PRIVATE_LEARNINGPATHV2, PRIVATE_OWNER.toCombined)
    assert(saved.get.id == PRIVATE_LEARNINGPATH.id.get)

    verify(learningPathRepository, times(1)).insert(any[LearningPath])(using any)
    verify(searchIndexService, never).indexDocument(any[LearningPath])
  }

  test("That updateLearningPathV2 returns Failure when the given ID does not exist") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(None)
    val Failure(ex) =
      service.updateLearningPathV2(PRIVATE_ID, UPDATED_PRIVATE_LEARNINGPATHV2, PRIVATE_OWNER.toCombined): @unchecked
    ex should be(NotFoundException("Could not find learningpath with id '2'."))
  }

  test("That updateLearningPathV2 updates the learningpath when the given user is the owner if the status is PRIVATE") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.update(any[LearningPath])(using any[DBSession])).thenReturn(PRIVATE_LEARNINGPATH)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    assertResult(PRIVATE_LEARNINGPATH.id.get) {
      service.updateLearningPathV2(PRIVATE_ID, UPDATED_PRIVATE_LEARNINGPATHV2, PRIVATE_OWNER.toCombined).get.id
    }
    verify(learningPathRepository, times(1)).update(any[LearningPath])(using any)
    verify(searchIndexService, times(1)).indexDocument(any[LearningPath])

  }

  test(
    "That updateLearningPathV2 updates the learningpath when the given user is the owner if the status is UNLISTED"
  ) {
    val unlistedLp = PRIVATE_LEARNINGPATH.copy(status = LearningPathStatus.UNLISTED)
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(Some(unlistedLp))
    when(learningPathRepository.update(any[LearningPath])(using any[DBSession])).thenReturn(unlistedLp)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    assertResult(PRIVATE_LEARNINGPATH.id.get) {
      service.updateLearningPathV2(PRIVATE_ID, UPDATED_PRIVATE_LEARNINGPATHV2, PRIVATE_OWNER.toCombined).get.id
    }
    verify(learningPathRepository, times(1)).update(any[LearningPath])(using any)
    verify(searchIndexService, times(1)).indexDocument(any[LearningPath])
  }

  test(
    "That updateLearningPathV2 updates the learningpath when the given user is a publisher if the status is PUBLISHED"
  ) {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(using any[DBSession])).thenReturn(
      Some(PUBLISHED_LEARNINGPATH)
    )
    when(learningPathRepository.update(any[LearningPath])(using any[DBSession])).thenReturn(PUBLISHED_LEARNINGPATH)

    assertResult(PUBLISHED_LEARNINGPATH.id.get) {
      service.updateLearningPathV2(PUBLISHED_ID, UPDATED_PUBLISHED_LEARNINGPATHV2, PUBLISHED_OWNER.toCombined).get.id
    }
  }

  test("That updateLearningPathV2 returns Failure if user is not the owner") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(Some(PRIVATE_LEARNINGPATH))

    val Failure(ex) = service.updateLearningPathV2(
      PRIVATE_ID,
      UPDATED_PRIVATE_LEARNINGPATHV2,
      TokenUser("not_the_owner", Set.empty, None).toCombined,
    ): @unchecked
    ex should be(AccessDeniedException("You do not have permission to perform this action."))
  }

  test("That updateLearningPathV2 sets status to UNLISTED if owner is not publisher and status is PUBLISHED") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(using any[DBSession])).thenReturn(
      Some(PUBLISHED_LEARNINGPATH)
    )
    when(learningPathRepository.update(any[LearningPath])(using any[DBSession])).thenReturn(
      PUBLISHED_LEARNINGPATH.copy(status = LearningPathStatus.UNLISTED)
    )
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val result = service
      .updateLearningPathV2(PUBLISHED_ID, UPDATED_PUBLISHED_LEARNINGPATHV2, PUBLISHED_OWNER.toCombined)
      .get
    result.id should be(PUBLISHED_LEARNINGPATH.id.get)
    result.status should be(LearningPathStatus.UNLISTED.toString)
  }

  test("That updateLearningPathV2 status PRIVATE remains PRIVATE if not publisher") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.update(any[LearningPath])(using any[DBSession])).thenReturn(PRIVATE_LEARNINGPATH)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val result = service.updateLearningPathV2(PRIVATE_ID, UPDATED_PRIVATE_LEARNINGPATHV2, PRIVATE_OWNER.toCombined).get
    result.id should be(PRIVATE_LEARNINGPATH.id.get)
    result.status should be(LearningPathStatus.PRIVATE.toString)
  }

  test("That updateLearningPathStatusV2 returns None when the given ID does not exist") {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(None)
    val Failure(ex) = service.updateLearningPathStatusV2(
      PRIVATE_ID,
      LearningPathStatus.PUBLISHED,
      PRIVATE_OWNER.toCombined,
      "nb",
    ): @unchecked
    ex should be(NotFoundException(s"Could not find learningpath with id '$PRIVATE_ID'."))
  }

  test("That updateLearningPathStatusV2 updates the status when the given user is admin and the status is PUBLISHED") {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PUBLISHED_ID))(using any[DBSession])).thenReturn(
      Some(PUBLISHED_LEARNINGPATH)
    )
    when(learningPathRepository.update(any[LearningPath])(using any[DBSession])).thenReturn(
      PUBLISHED_LEARNINGPATH.copy(status = learningpath.LearningPathStatus.PRIVATE)
    )
    when(learningPathRepository.learningPathsWithIsBasedOn(PUBLISHED_ID)).thenReturn(List())

    assertResult("PRIVATE") {
      service
        .updateLearningPathStatusV2(
          PUBLISHED_ID,
          LearningPathStatus.PRIVATE,
          PRIVATE_OWNER.copy(permissions = Set(LEARNINGPATH_API_ADMIN)).toCombined,
          "nb",
        )
        .get
        .status
    }
    verify(learningPathRepository, times(1)).update(any[LearningPath])(using any)
    verify(searchIndexService, times(1)).indexDocument(any[LearningPath])
  }

  test("That updateLearningPathStatusV2 updates madeAvailable when going to UNLISTED") {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PUBLISHED_ID))(using any[DBSession])).thenReturn(
      Some(PUBLISHED_LEARNINGPATH)
    )
    when(learningPathRepository.update(any[LearningPath])(using any[DBSession])).thenReturn(
      PUBLISHED_LEARNINGPATH.copy(status = learningpath.LearningPathStatus.PRIVATE)
    )
    when(learningPathRepository.learningPathsWithIsBasedOn(PUBLISHED_ID)).thenReturn(List())
    val nowDate = NDLADate.fromUnixTime(1337)
    when(clock.now()).thenReturn(nowDate)
    val user = PRIVATE_OWNER.copy(permissions = Set(LEARNINGPATH_API_ADMIN)).toCombined

    service.updateLearningPathStatusV2(PUBLISHED_ID, LearningPathStatus.UNLISTED, user, "nb").failIfFailure

    val expectedLearningPath = PUBLISHED_LEARNINGPATH.copy(
      status = LearningPathStatus.UNLISTED,
      lastUpdated = nowDate,
      madeAvailable = Some(nowDate),
    )
    verify(learningPathRepository, times(1)).update(eqTo(expectedLearningPath))(using any)
  }

  test(
    "That updateLearningPathStatusV2 updates the status when the given user is not the owner, but is admin and the status is PUBLISHED"
  ) {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PUBLISHED_ID))(using any[DBSession])).thenReturn(
      Some(PUBLISHED_LEARNINGPATH)
    )
    when(learningPathRepository.update(any[LearningPath])(using any[DBSession])).thenReturn(
      PUBLISHED_LEARNINGPATH.copy(status = learningpath.LearningPathStatus.PRIVATE)
    )
    when(learningPathRepository.learningPathsWithIsBasedOn(PUBLISHED_ID)).thenReturn(List())

    assertResult("PRIVATE") {
      service
        .updateLearningPathStatusV2(
          PUBLISHED_ID,
          LearningPathStatus.PRIVATE,
          TokenUser("not_the_owner", Set(LEARNINGPATH_API_ADMIN), None).toCombined,
          "nb",
        )
        .get
        .status
    }
    verify(learningPathRepository, times(1)).update(any[LearningPath])(using any)
    verify(searchIndexService, times(1)).indexDocument(any[LearningPath])
    verify(searchIndexService, times(0)).deleteDocument(any[LearningPath], any)
  }

  test(
    "That updateLearningPathStatusV2 updates the status when the given user is the owner and the status is PRIVATE"
  ) {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(
      Some(PRIVATE_LEARNINGPATH)
    )
    when(learningPathRepository.update(any[LearningPath])(using any[DBSession])).thenReturn(
      PRIVATE_LEARNINGPATH.copy(status = learningpath.LearningPathStatus.DELETED)
    )
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    assertResult("DELETED") {
      service
        .updateLearningPathStatusV2(PRIVATE_ID, LearningPathStatus.DELETED, PRIVATE_OWNER.toCombined, "nb")
        .get
        .status
    }
    verify(learningPathRepository, times(1)).update(any[LearningPath])(using any)
  }

  test("That updateLearningPathStatusV2 updates the status when the given user is owner and the status is DELETED") {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(
      Some(DELETED_LEARNINGPATH)
    )
    when(learningPathRepository.update(any[LearningPath])(using any[DBSession])).thenReturn(
      DELETED_LEARNINGPATH.copy(status = learningpath.LearningPathStatus.UNLISTED)
    )
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    assertResult("UNLISTED") {
      service
        .updateLearningPathStatusV2(PRIVATE_ID, LearningPathStatus.UNLISTED, PRIVATE_OWNER.toCombined, "nb")
        .get
        .status
    }
    verify(learningPathRepository, times(1)).update(any[LearningPath])(using any)
    verify(searchIndexService, times(1)).indexDocument(any[LearningPath])
  }

  test(
    "That updateLearningPathStatusV2 updates the status when the given user is publisher and the status is DELETED"
  ) {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(
      Some(DELETED_LEARNINGPATH)
    )
    when(learningPathRepository.update(any[LearningPath])(using any[DBSession])).thenReturn(
      DELETED_LEARNINGPATH.copy(status = learningpath.LearningPathStatus.PUBLISHED)
    )

    assertResult("PUBLISHED") {
      service
        .updateLearningPathStatusV2(
          PRIVATE_ID,
          LearningPathStatus.PUBLISHED,
          PRIVATE_OWNER.copy(permissions = Set(LEARNINGPATH_API_ADMIN)).toCombined,
          "nb",
        )
        .get
        .status
    }
    verify(learningPathRepository, times(1)).update(any[LearningPath])(using any)
    verify(searchIndexService, times(1)).indexDocument(any[LearningPath])
  }

  test("That updateLearningPathStatusV2 updates isBasedOn when a PUBLISHED path is DELETED") {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PUBLISHED_ID))(using any[DBSession])).thenReturn(
      Some(PUBLISHED_LEARNINGPATH)
    )
    when(learningPathRepository.update(any[LearningPath])(using any[DBSession])).thenReturn(
      PUBLISHED_LEARNINGPATH.copy(status = learningpath.LearningPathStatus.DELETED)
    )
    when(learningPathRepository.learningPathsWithIsBasedOn(PUBLISHED_ID)).thenReturn(
      List(
        DELETED_LEARNINGPATH.copy(id = Some(9), isBasedOn = Some(PUBLISHED_ID)),
        DELETED_LEARNINGPATH.copy(id = Some(8), isBasedOn = Some(PUBLISHED_ID)),
      )
    )

    assertResult("DELETED") {
      service
        .updateLearningPathStatusV2(
          PUBLISHED_ID,
          LearningPathStatus.DELETED,
          PUBLISHED_OWNER.copy(permissions = Set(LEARNINGPATH_API_ADMIN)).toCombined,
          "nb",
        )
        .get
        .status
    }

    verify(learningPathRepository, times(3)).update(any[LearningPath])(using any)
    verify(learningPathRepository, times(1)).learningPathsWithIsBasedOn(any[Long])
    verify(searchIndexService, times(1)).indexDocument(any[LearningPath])
  }

  test("That updateLearningPathStatusV2 throws an AccessDeniedException when non-admin tries to publish") {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(
      Some(PRIVATE_LEARNINGPATH)
    )
    val Failure(ex) = service.updateLearningPathStatusV2(
      PRIVATE_ID,
      LearningPathStatus.PUBLISHED,
      PRIVATE_OWNER.toCombined,
      "nb",
    ): @unchecked
    ex should be(AccessDeniedException("You need to be a publisher to publish learningpaths."))
  }

  test("That updateLearningPathStatusV2 allows owner to edit PUBLISHED to PRIVATE") {
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PUBLISHED_ID))(using any[DBSession])).thenReturn(
      Some(PUBLISHED_LEARNINGPATH)
    )
    when(learningPathRepository.update(any[LearningPath])(using any[DBSession])).thenReturn(
      PUBLISHED_LEARNINGPATH.copy(status = LearningPathStatus.PRIVATE)
    )

    assertResult("PRIVATE") {
      service
        .updateLearningPathStatusV2(PUBLISHED_ID, LearningPathStatus.PRIVATE, PUBLISHED_OWNER.toCombined, "nb")
        .get
        .status
    }
    verify(learningPathRepository, times(1)).update(any[LearningPath])(using any)
    verify(searchIndexService, times(1)).indexDocument(any[LearningPath])
  }

  test("That updateLearningPathStatusV2 allows owner to edit PUBLISHED to UNLISTED") {
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PUBLISHED_ID))(using any[DBSession])).thenReturn(
      Some(PUBLISHED_LEARNINGPATH)
    )
    when(learningPathRepository.update(any[LearningPath])(using any[DBSession])).thenReturn(
      PUBLISHED_LEARNINGPATH.copy(status = LearningPathStatus.UNLISTED)
    )

    assertResult("UNLISTED") {
      service
        .updateLearningPathStatusV2(PUBLISHED_ID, LearningPathStatus.UNLISTED, PUBLISHED_OWNER.toCombined, "nb")
        .get
        .status
    }
    verify(learningPathRepository, times(1)).update(any[LearningPath])(using any)
    verify(searchIndexService, times(1)).indexDocument(any[LearningPath])
  }

  test("That updateLearningPathStatusV2 throws an AccessDeniedException when non-owner tries to change status") {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PUBLISHED_ID))(using any[DBSession])).thenReturn(
      Some(PUBLISHED_LEARNINGPATH)
    )
    val Failure(ex) = service.updateLearningPathStatusV2(
      PUBLISHED_ID,
      LearningPathStatus.PRIVATE,
      PRIVATE_OWNER.toCombined,
      "nb",
    ): @unchecked
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("That updateLearningPathStatusV2 ignores message if not admin") {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PUBLISHED_ID))(using any[DBSession])).thenReturn(
      Some(PUBLISHED_LEARNINGPATH)
    )
    when(learningPathRepository.update(any[LearningPath])(using any[DBSession])).thenAnswer((i: InvocationOnMock) =>
      i.getArgument[LearningPath](0)
    )
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)
    when(clock.now()).thenReturn(NDLADate.fromUnixTime(0))

    val expected =
      PUBLISHED_LEARNINGPATH.copy(message = None, status = LearningPathStatus.PRIVATE, lastUpdated = clock.now())

    service.updateLearningPathStatusV2(
      PUBLISHED_ID,
      LearningPathStatus.PRIVATE,
      PUBLISHED_OWNER.toCombined,
      "nb",
      Some("new message"),
    )
    verify(learningPathRepository, times(1)).update(expected)
  }

  test("That updateLearningPathStatusV2 adds message if admin") {
    when(learningPathRepository.withIdIncludingDeleted(eqTo(PUBLISHED_ID))(using any[DBSession])).thenReturn(
      Some(PUBLISHED_LEARNINGPATH)
    )
    when(learningPathRepository.update(any[LearningPath])(using any[DBSession])).thenAnswer((i: InvocationOnMock) =>
      i.getArgument[LearningPath](0)
    )
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)
    when(clock.now()).thenReturn(NDLADate.MIN)

    service.updateLearningPathStatusV2(
      PUBLISHED_ID,
      LearningPathStatus.PRIVATE,
      PRIVATE_OWNER.copy(permissions = Set(LEARNINGPATH_API_ADMIN)).toCombined,
      "nb",
      Some("new message"),
    )
    verify(learningPathRepository, times(1)).update(
      PUBLISHED_LEARNINGPATH.copy(
        message = Some(Message("new message", PRIVATE_OWNER.id, clock.now())),
        status = LearningPathStatus.PRIVATE,
        lastUpdated = clock.now(),
      )
    )
  }

  test("That addLearningStepV2 returns None when the given learningpath does not exist") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(None)
    val Failure(ex) = service.addLearningStepV2(PRIVATE_ID, NEW_STEPV2, PRIVATE_OWNER.toCombined): @unchecked
    ex.isInstanceOf[NotFoundException] should be(true)
    verify(learningPathRepository, never).insertLearningStep(any[LearningStep])(using any)
    verify(learningPathRepository, never).update(any[LearningPath])(using any)
  }

  test(
    "That addLearningStepV2 inserts the learningstepV2 and update lastUpdated on the learningpath when the given user is the owner and status is PRIVATE"
  ) {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.insertLearningStep(any[LearningStep])(using any[DBSession])).thenReturn(STEP1)
    when(learningPathRepository.update(any[LearningPath])(using any[DBSession])).thenReturn(PRIVATE_LEARNINGPATH)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    assertResult(STEP1.id.get) {
      service.addLearningStepV2(PRIVATE_ID, NEW_STEPV2, PRIVATE_OWNER.toCombined).get.id
    }
    verify(learningPathRepository, times(1)).insertLearningStep(any[LearningStep])(using any[DBSession])
    verify(learningPathRepository, times(1)).update(any[LearningPath])(using any[DBSession])
    verify(searchIndexService, times(1)).indexDocument(any[LearningPath])
    verify(searchIndexService, times(0)).deleteDocument(any[LearningPath], any)
  }

  test(
    "That addLearningStep inserts the learningstep and update lastUpdated on the learningpath when the given user is the owner and status is PUBLISHED"
  ) {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(using any[DBSession])).thenReturn(
      Some(PUBLISHED_LEARNINGPATH)
    )
    when(learningPathRepository.insertLearningStep(any[LearningStep])(using any[DBSession])).thenReturn(STEP2)
    when(learningPathRepository.update(any[LearningPath])(using any[DBSession])).thenAnswer((i: InvocationOnMock) =>
      i.getArgument[LearningPath](0)
    )
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)
    assertResult(STEP2.id.get) {
      service.addLearningStepV2(PUBLISHED_ID, NEW_STEPV2, PUBLISHED_OWNER.toCombined).get.id
    }
    verify(learningPathRepository, times(1)).insertLearningStep(any[LearningStep])(using any[DBSession])
    verify(learningPathRepository, times(1)).update(any[LearningPath])(using any[DBSession])
    verify(searchIndexService, times(1)).indexDocument(any[LearningPath])
    verify(searchIndexService, times(0)).deleteDocument(any[LearningPath], any)
  }

  test("That addLearningStepV2 throws an AccessDeniedException when the given user is NOT the owner") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(using any[DBSession])).thenReturn(
      Some(PUBLISHED_LEARNINGPATH)
    )
    val Failure(ex) = service.addLearningStepV2(PUBLISHED_ID, NEW_STEPV2, PRIVATE_OWNER.toCombined): @unchecked
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("That updateLearningStepV2 returns None when the learningpathV2 does not exist") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(using any[DBSession])).thenReturn(None)

    val Failure(ex) =
      service.updateLearningStepV2(PUBLISHED_ID, STEP1.id.get, UPDATED_STEPV2, PUBLISHED_OWNER.toCombined): @unchecked
    ex.isInstanceOf[NotFoundException] should be(true)

    verify(learningPathRepository, never).updateLearningStep(any[LearningStep])(using any)
    verify(learningPathRepository, never).update(any[LearningPath])(using any)
  }

  test("That updateLearningStepV2 returns None when the learningstep does not exist") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(using any[DBSession])).thenReturn(
      Some(PUBLISHED_LEARNINGPATH)
    )
    when(learningPathRepository.learningStepWithId(eqTo(PUBLISHED_ID), eqTo(STEP1.id.get))(using any[DBSession]))
      .thenReturn(None)
    val Failure(ex) =
      service.updateLearningStepV2(PUBLISHED_ID, STEP1.id.get, UPDATED_STEPV2, PUBLISHED_OWNER.toCombined): @unchecked
    ex.isInstanceOf[NotFoundException] should be(true)
    verify(learningPathRepository, never).updateLearningStep(any[LearningStep])(using any[DBSession])
    verify(learningPathRepository, never).update(any[LearningPath])(using any[DBSession])
  }

  test(
    "That updateLearningStep updates the learningstep and update lastUpdated on the learningpath when the given user is ADMIN and status is PUBLISHED"
  ) {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(using any[DBSession])).thenReturn(
      Some(PUBLISHED_LEARNINGPATH)
    )
    when(learningPathRepository.learningStepWithId(eqTo(PUBLISHED_ID), eqTo(STEP1.id.get))(using any[DBSession]))
      .thenReturn(Some(STEP1))
    when(learningPathRepository.updateLearningStep(any[LearningStep])(using any[DBSession])).thenReturn(STEP1)
    when(learningPathRepository.update(any[LearningPath])(using any[DBSession])).thenReturn(PUBLISHED_LEARNINGPATH)

    assertResult(STEP1.id.get) {
      service
        .updateLearningStepV2(
          PUBLISHED_ID,
          STEP1.id.get,
          UPDATED_STEPV2,
          PUBLISHED_OWNER.copy(permissions = Set(LEARNINGPATH_API_ADMIN)).toCombined,
        )
        .get
        .id
    }
    verify(learningPathRepository, times(1)).updateLearningStep(any[LearningStep])(using any[DBSession])
    verify(learningPathRepository, times(1)).update(any[LearningPath])(using any[DBSession])
    verify(searchIndexService, times(1)).indexDocument(any[LearningPath])
  }

  test(
    "That updateLearningStepV2 updates the learningstep and update lastUpdated on the learningpath when the given user is the owner and status is PRIVATE"
  ) {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(using any[DBSession]))
      .thenReturn(Some(STEP1))
    when(learningPathRepository.updateLearningStep(any[LearningStep])(using any[DBSession])).thenReturn(STEP1)
    when(learningPathRepository.update(any[LearningPath])(using any[DBSession])).thenReturn(PRIVATE_LEARNINGPATH)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    assertResult(STEP1.id.get) {
      service.updateLearningStepV2(PRIVATE_ID, STEP1.id.get, UPDATED_STEPV2, PRIVATE_OWNER.toCombined).get.id
    }
    verify(learningPathRepository, times(1)).updateLearningStep(any[LearningStep])(using any[DBSession])
    verify(learningPathRepository, times(1)).update(any[LearningPath])(using any[DBSession])
    verify(searchIndexService, times(1)).indexDocument(any[LearningPath])
  }

  test("That updateLearningStepV2 throws an AccessDeniedException when the given user is NOT the owner") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(PRIVATE_ID, STEP1.id.get)).thenReturn(Some(STEP1))
    val Failure(ex) = service.updateLearningStepV2(PRIVATE_ID, STEP1.id.get, UPDATED_STEPV2, MYNDLA_USER): @unchecked
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test(
    "That updateLearningStepV2 throws an AccessDeniedException when the given user owns the learningpath but NOT the learningstep"
  ) {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(Some(PUBLISHED_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(PRIVATE_ID, STEP1.id.get)).thenReturn(Some(STEP1))
    val Failure(ex) = service.updateLearningStepV2(PRIVATE_ID, STEP1.id.get, UPDATED_STEPV2, MYNDLA_USER): @unchecked
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("That updateLearningStepStatusV2 returns None when the given learningpath does not exist") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(using any[DBSession])).thenReturn(None)

    val Failure(ex) = service.updateLearningStepStatusV2(
      PUBLISHED_ID,
      STEP1.id.get,
      StepStatus.DELETED,
      PUBLISHED_OWNER.toCombined,
    ): @unchecked
    ex.isInstanceOf[NotFoundException] should be(true)
  }

  test("That updateLearningStepStatusV2 returns None when the given learningstep does not exist") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(using any[DBSession])).thenReturn(
      Some(PUBLISHED_LEARNINGPATH)
    )
    when(learningPathRepository.learningStepsFor(eqTo(PUBLISHED_ID))(using any[DBSession])).thenReturn(Seq())

    val Failure(ex) = service.updateLearningStepStatusV2(
      PUBLISHED_ID,
      STEP1.id.get,
      StepStatus.DELETED,
      PUBLISHED_OWNER.toCombined,
    ): @unchecked
    ex.isInstanceOf[NotFoundException] should be(true)
  }

  test(
    "That updateLearningStepStatusV2 marks the learningstep as DELETED when the given user is the owner and the status is PRIVATE"
  ) {
    val nowDate = NDLADate.fromUnixTime(1337)
    when(clock.now()).thenReturn(nowDate)

    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(using any[DBSession]))
      .thenReturn(Some(STEP1))
    when(learningPathRepository.learningStepsFor(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(List(STEP1))
    when(
      learningPathRepository.updateLearningStep(eqTo(STEP1.copy(status = StepStatus.DELETED, lastUpdated = nowDate)))(
        using any[DBSession]
      )
    ).thenReturn(STEP1.copy(status = StepStatus.DELETED, lastUpdated = nowDate))
    when(learningPathRepository.update(any[LearningPath])(using any[DBSession])).thenReturn(PRIVATE_LEARNINGPATH)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val updatedStep =
      service.updateLearningStepStatusV2(PRIVATE_ID, STEP1.id.get, StepStatus.DELETED, PRIVATE_OWNER.toCombined)
    updatedStep.isSuccess should be(true)
    updatedStep.get.status should equal(StepStatus.DELETED.entryName)

    verify(learningPathRepository, times(1)).updateLearningStep(
      eqTo(STEP1.copy(status = StepStatus.DELETED, lastUpdated = nowDate))
    )(using any[DBSession])
    verify(learningPathRepository, times(1)).update(any[LearningPath])(using any[DBSession])
    verify(searchIndexService, times(1)).indexDocument(any[LearningPath])
  }

  test(
    "That updateLearningStepStatusV2 marks the learningstep as DELETED when the given user is the owner and the status is PUBLISHED"
  ) {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(using any[DBSession])).thenReturn(
      Some(PUBLISHED_LEARNINGPATH)
    )
    when(learningPathRepository.learningStepWithId(eqTo(PUBLISHED_ID), eqTo(STEP2.id.get))(using any[DBSession]))
      .thenReturn(Some(STEP2))
    when(learningPathRepository.learningStepsFor(eqTo(PUBLISHED_ID))(using any[DBSession])).thenReturn(
      PUBLISHED_LEARNINGPATH.learningsteps.get
    )
    when(learningPathRepository.updateLearningStep(any)(using any)).thenAnswer((i: InvocationOnMock) =>
      i.getArgument[LearningStep](0)
    )
    when(learningPathRepository.update(any[LearningPath])(using any[DBSession])).thenAnswer((i: InvocationOnMock) =>
      i.getArgument[LearningPath](0)
    )
    val updatedDate = NDLADate.fromUnixTime(0)
    when(clock.now()).thenReturn(updatedDate)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val updatedStep =
      service.updateLearningStepStatusV2(PUBLISHED_ID, STEP2.id.get, StepStatus.DELETED, PUBLISHED_OWNER.toCombined)
    updatedStep.isSuccess should be(true)
    updatedStep.get.status should equal(StepStatus.DELETED.entryName)

    val expectedUpdatePath = PUBLISHED_LEARNINGPATH.copy(learningsteps = None, lastUpdated = updatedDate)

    verify(learningPathRepository, times(1)).updateLearningStep(
      eqTo(STEP2.copy(status = StepStatus.DELETED, lastUpdated = updatedDate))
    )(using any[DBSession])
    verify(learningPathRepository, times(1)).update(eqTo(expectedUpdatePath))(using any[DBSession])
    verify(searchIndexService, times(1)).indexDocument(any[LearningPath])
    verify(searchIndexService, times(0)).deleteDocument(any[LearningPath], any)
  }

  test("That marking the first learningStep as deleted changes the seqNo for all other learningsteps") {
    val nowDate = NDLADate.fromUnixTime(1337)
    when(clock.now()).thenReturn(nowDate)

    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(using any[DBSession]))
      .thenReturn(Some(STEP1))
    when(learningPathRepository.learningStepsFor(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(
      List(STEP1, STEP2, STEP3)
    )
    when(learningPathRepository.updateLearningStep(any)(using any[DBSession])).thenAnswer((i: InvocationOnMock) =>
      i.getArgument[LearningStep](0)
    )
    when(learningPathRepository.update(any[LearningPath])(using any[DBSession])).thenReturn(PRIVATE_LEARNINGPATH)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val updatedStep =
      service.updateLearningStepStatusV2(PRIVATE_ID, STEP1.id.get, StepStatus.DELETED, PRIVATE_OWNER.toCombined)
    updatedStep.isSuccess should be(true)
    updatedStep.get.status should equal(StepStatus.DELETED.entryName)

    verify(learningPathRepository, times(1)).updateLearningStep(
      eqTo(STEP1.copy(status = StepStatus.DELETED, lastUpdated = nowDate))
    )(using any[DBSession])
    verify(learningPathRepository, times(1)).updateLearningStep(
      eqTo(STEP2.copy(seqNo = STEP2.seqNo - 1, lastUpdated = nowDate))
    )(using any[DBSession])
    verify(learningPathRepository, times(1)).updateLearningStep(
      eqTo(STEP3.copy(seqNo = STEP3.seqNo - 1, lastUpdated = nowDate))
    )(using any[DBSession])
    verify(learningPathRepository, times(1)).update(any[LearningPath])(using any[DBSession])
    verify(searchIndexService, times(1)).indexDocument(any[LearningPath])
  }

  test("That marking the first learningStep as active changes the seqNo for all other learningsteps") {
    val nowDate = NDLADate.fromUnixTime(1337)
    when(clock.now()).thenReturn(nowDate)

    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(using any[DBSession]))
      .thenReturn(Some(STEP1.copy(status = StepStatus.DELETED)))
    when(learningPathRepository.learningStepsFor(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(
      List(STEP1, STEP2, STEP3)
    )
    when(learningPathRepository.updateLearningStep(any)(using any[DBSession])).thenAnswer((i: InvocationOnMock) =>
      i.getArgument[LearningStep](0)
    )
    when(learningPathRepository.update(any[LearningPath])(using any[DBSession])).thenReturn(PRIVATE_LEARNINGPATH)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val updatedStep =
      service.updateLearningStepStatusV2(PRIVATE_ID, STEP1.id.get, StepStatus.ACTIVE, PRIVATE_OWNER.toCombined)
    updatedStep.isSuccess should be(true)
    updatedStep.get.status should equal(StepStatus.ACTIVE.entryName)

    verify(learningPathRepository, times(1)).updateLearningStep(
      eqTo(STEP1.copy(status = StepStatus.ACTIVE, seqNo = 0, lastUpdated = nowDate))
    )(using any[DBSession])
    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(STEP2.copy(seqNo = 1, lastUpdated = nowDate)))(
      using any[DBSession]
    )
    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(STEP3.copy(seqNo = 2, lastUpdated = nowDate)))(
      using any[DBSession]
    )
    verify(learningPathRepository, times(1)).update(any[LearningPath])(using any[DBSession])
    verify(searchIndexService, times(1)).indexDocument(any[LearningPath])
  }

  test("That deleteLearningStep throws an AccessDeniedException when the given user is NOT the owner") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(PRIVATE_ID, STEP1.id.get)).thenReturn(Some(STEP1))
    val Failure(ex) = service.updateLearningStepStatusV2(
      PRIVATE_ID,
      STEP1.id.get,
      StepStatus.DELETED,
      PUBLISHED_OWNER.toCombined,
    ): @unchecked
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("That updateSeqNo throws ValidationException when seqNo out of range") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(using any[DBSession]))
      .thenReturn(Some(STEP1))

    intercept[ValidationException] {
      val Failure(exception: ValidationException) =
        service.updateSeqNo(PRIVATE_ID, STEP1.id.get, 100, PRIVATE_OWNER.toCombined): @unchecked
      exception.errors.length should be(1)
      exception.errors.head.field should equal("seqNo")
      exception.errors.head.message should equal("seqNo must be between 0 and 5")
    }
  }

  test("That updateSeqNo from 0 to last updates all learningsteps in between") {
    val nowDate = NDLADate.fromUnixTime(1337)
    when(clock.now()).thenReturn(nowDate)

    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(using any[DBSession]))
      .thenReturn(Some(STEP1.copy(lastUpdated = nowDate)))

    val updatedStep = service.updateSeqNo(PRIVATE_ID, STEP1.id.get, STEP6.seqNo, PRIVATE_OWNER.toCombined)
    updatedStep.get.seqNo should equal(STEP6.seqNo)

    verify(learningPathRepository, times(1)).updateLearningStep(
      eqTo(STEP2.copy(seqNo = STEP2.seqNo - 1, lastUpdated = nowDate))
    )(using any[DBSession])
    verify(learningPathRepository, times(1)).updateLearningStep(
      eqTo(STEP3.copy(seqNo = STEP3.seqNo - 1, lastUpdated = nowDate))
    )(using any[DBSession])
    verify(learningPathRepository, times(1)).updateLearningStep(
      eqTo(STEP4.copy(seqNo = STEP4.seqNo - 1, lastUpdated = nowDate))
    )(using any[DBSession])
    verify(learningPathRepository, times(1)).updateLearningStep(
      eqTo(STEP5.copy(seqNo = STEP5.seqNo - 1, lastUpdated = nowDate))
    )(using any[DBSession])
    verify(learningPathRepository, times(1)).updateLearningStep(
      eqTo(STEP6.copy(seqNo = STEP6.seqNo - 1, lastUpdated = nowDate))
    )(using any[DBSession])
    verify(learningPathRepository, times(1)).updateLearningStep(
      eqTo(STEP1.copy(seqNo = STEP6.seqNo, lastUpdated = nowDate))
    )(using any[DBSession])
  }

  test("That updateSeqNo from last to 0 updates all learningsteps in between") {
    val nowDate = NDLADate.fromUnixTime(1337)
    when(clock.now()).thenReturn(nowDate)

    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP6.id.get))(using any[DBSession]))
      .thenReturn(Some(STEP6))

    val updatedStep = service.updateSeqNo(PRIVATE_ID, STEP6.id.get, STEP1.seqNo, PRIVATE_OWNER.toCombined)
    updatedStep.get.seqNo should equal(STEP1.seqNo)

    verify(learningPathRepository, times(1)).updateLearningStep(
      eqTo(STEP6.copy(seqNo = STEP1.seqNo, lastUpdated = nowDate))
    )(using any[DBSession])
    verify(learningPathRepository, times(1)).updateLearningStep(
      eqTo(STEP1.copy(seqNo = STEP1.seqNo + 1, lastUpdated = nowDate))
    )(using any[DBSession])
    verify(learningPathRepository, times(1)).updateLearningStep(
      eqTo(STEP2.copy(seqNo = STEP2.seqNo + 1, lastUpdated = nowDate))
    )(using any[DBSession])
    verify(learningPathRepository, times(1)).updateLearningStep(
      eqTo(STEP3.copy(seqNo = STEP3.seqNo + 1, lastUpdated = nowDate))
    )(using any[DBSession])
    verify(learningPathRepository, times(1)).updateLearningStep(
      eqTo(STEP4.copy(seqNo = STEP4.seqNo + 1, lastUpdated = nowDate))
    )(using any[DBSession])
    verify(learningPathRepository, times(1)).updateLearningStep(
      eqTo(STEP5.copy(seqNo = STEP5.seqNo + 1, lastUpdated = nowDate))
    )(using any[DBSession])
  }

  test("That updateSeqNo between two middle steps only updates the two middle steps") {
    val nowDate = NDLADate.fromUnixTime(1337)
    when(clock.now()).thenReturn(nowDate)

    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP2.id.get))(using any[DBSession]))
      .thenReturn(Some(STEP2))

    val updatedStep = service.updateSeqNo(PRIVATE_ID, STEP2.id.get, STEP3.seqNo, PRIVATE_OWNER.toCombined)
    updatedStep.get.seqNo should equal(STEP3.seqNo)

    verify(learningPathRepository, times(1)).updateLearningStep(
      eqTo(STEP2.copy(seqNo = STEP3.seqNo, lastUpdated = nowDate))
    )(using any[DBSession])
    verify(learningPathRepository, times(1)).updateLearningStep(
      eqTo(STEP3.copy(seqNo = STEP2.seqNo, lastUpdated = nowDate))
    )(using any[DBSession])
  }

  test("That updateSeqNo also update seqNo for all affected steps") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(using any[DBSession]))
      .thenReturn(Some(STEP1))

    val updatedStep = service.updateSeqNo(PRIVATE_ID, STEP1.id.get, 1, PRIVATE_OWNER.toCombined)
    updatedStep.get.seqNo should equal(1)

    verify(learningPathRepository, times(2)).updateLearningStep(any[LearningStep])(using any[DBSession])
  }

  test("new fromExisting2 should allow language fields set to unknown") {
    val learningpathWithUnknownLang = PUBLISHED_LEARNINGPATH.copy(title = Seq(Title("what språk is this", "unknown")))

    when(learningPathRepository.withId(eqTo(learningpathWithUnknownLang.id.get))(using any[DBSession])).thenReturn(
      Some(learningpathWithUnknownLang)
    )
    when(learningPathRepository.insert(any[LearningPath])(using any[DBSession])).thenReturn(learningpathWithUnknownLang)

    val newCopy = NewCopyLearningPathV2DTO("hehe", None, None, "nb", None, None, None, None)
    service
      .newFromExistingV2(
        learningpathWithUnknownLang.id.get,
        newCopy,
        TokenUser("me", Set(LEARNINGPATH_API_WRITE), None).toCombined,
      )
      .isSuccess should be(true)
  }

  test("That newFromExistingV2 throws exception when user is not owner of the path and the path is private") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(Some(PRIVATE_LEARNINGPATH))

    val Failure(ex) =
      service.newFromExistingV2(PRIVATE_ID, NEW_COPIED_LEARNINGPATHV2, PUBLISHED_OWNER.toCombined): @unchecked
    ex should be(AccessDeniedException("You do not have access to the requested resource."))
  }

  test("owner updates step private should not update status") {
    val newDate          = NDLADate.fromUnixTime(648000000)
    val stepWithBadTitle = STEP1.copy(title = Seq(common.Title("Dårlig tittel", "nb")), lastUpdated = newDate)

    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(using any[DBSession]))
      .thenReturn(Some(STEP1))
    when(learningPathRepository.learningStepsFor(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(List())
    when(learningPathRepository.updateLearningStep(eqTo(stepWithBadTitle))(using any[DBSession])).thenReturn(
      stepWithBadTitle
    )
    when(learningPathRepository.update(any[LearningPath])(using any[DBSession])).thenAnswer((i: InvocationOnMock) =>
      i.getArgument[LearningPath](0)
    )
    when(clock.now()).thenReturn(newDate)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val updatedLs = UpdatedLearningStepV2DTO(
      1,
      commonApi.UpdateWith("Dårlig tittel"),
      commonApi.Missing,
      "nb",
      commonApi.Missing,
      commonApi.Missing,
      commonApi.Missing,
      None,
      None,
      None,
      commonApi.Missing,
    )
    service.updateLearningStepV2(PRIVATE_ID, STEP1.id.get, updatedLs, PRIVATE_OWNER.toCombined)
    val updatedPath = PRIVATE_LEARNINGPATH.copy(
      lastUpdated = newDate,
      learningsteps = Some(PRIVATE_LEARNINGPATH.learningsteps.get.tail ++ List(stepWithBadTitle)),
    )

    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(stepWithBadTitle))(using any[DBSession])
    verify(learningPathRepository, times(1)).update(eqTo(updatedPath))(using any[DBSession])
    verify(searchIndexService, times(1)).indexDocument(updatedPath)
    verify(searchIndexService, times(0)).deleteDocument(eqTo(updatedPath), any)
  }

  test("admin updates step should not update status") {
    val newDate          = NDLADate.fromUnixTime(648000000)
    val stepWithBadTitle = STEP1.copy(title = Seq(common.Title("Dårlig tittel", "nb")), lastUpdated = newDate)

    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(using any[DBSession])).thenReturn(
      Some(PUBLISHED_LEARNINGPATH)
    )
    when(learningPathRepository.learningStepWithId(eqTo(PUBLISHED_ID), eqTo(STEP1.id.get))(using any[DBSession]))
      .thenReturn(Some(STEP1))
    when(learningPathRepository.learningStepsFor(eqTo(PUBLISHED_ID))(using any[DBSession])).thenReturn(List())
    when(learningPathRepository.updateLearningStep(eqTo(stepWithBadTitle))(using any[DBSession])).thenReturn(
      stepWithBadTitle
    )
    when(learningPathRepository.update(any[LearningPath])(using any[DBSession])).thenAnswer((i: InvocationOnMock) =>
      i.getArgument[LearningPath](0)
    )
    when(clock.now()).thenReturn(newDate)

    val updatedLs = UpdatedLearningStepV2DTO(
      1,
      commonApi.UpdateWith("Dårlig tittel"),
      commonApi.Missing,
      "nb",
      commonApi.Missing,
      commonApi.Missing,
      commonApi.Missing,
      None,
      None,
      None,
      commonApi.Missing,
    )
    service.updateLearningStepV2(
      PUBLISHED_ID,
      STEP1.id.get,
      updatedLs,
      PUBLISHED_OWNER.copy(permissions = Set(LEARNINGPATH_API_ADMIN)).toCombined,
    )
    val updatedPath = PUBLISHED_LEARNINGPATH.copy(
      lastUpdated = newDate,
      learningsteps = Some(PUBLISHED_LEARNINGPATH.learningsteps.get.tail ++ List(stepWithBadTitle)),
    )

    verify(learningPathRepository, times(1)).updateLearningStep(eqTo(stepWithBadTitle))(using any[DBSession])
    verify(learningPathRepository, times(1)).update(eqTo(updatedPath))(using any[DBSession])
    verify(searchIndexService, times(1)).indexDocument(updatedPath)
  }

  test("That newFromExistingV2 returns None when given id does not exist") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(using any[DBSession])).thenReturn(None)
    val Failure(ex) =
      service.newFromExistingV2(PUBLISHED_ID, NEW_COPIED_LEARNINGPATHV2, PUBLISHED_OWNER.toCombined): @unchecked
    ex.isInstanceOf[NotFoundException] should be(true)
  }

  test("That basic-information unique per learningpath is reset in newFromExistingV2") {
    val now = NDLADate.now()
    when(clock.now()).thenReturn(now)

    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(using any[DBSession])).thenReturn(
      Some(PUBLISHED_LEARNINGPATH_NO_STEPS)
    )
    when(learningPathRepository.insert(any[LearningPath])(using any[DBSession])).thenReturn(
      PUBLISHED_LEARNINGPATH_NO_STEPS
    )

    service.newFromExistingV2(PUBLISHED_ID, NEW_COPIED_LEARNINGPATHV2, PRIVATE_OWNER.toCombined)

    val expectedNewLearningPath = PUBLISHED_LEARNINGPATH_NO_STEPS.copy(
      id = None,
      revision = None,
      externalId = None,
      isBasedOn = Some(PUBLISHED_ID),
      status = learningpath.LearningPathStatus.PRIVATE,
      verificationStatus = LearningPathVerificationStatus.CREATED_BY_NDLA,
      owner = PRIVATE_OWNER.id,
      lastUpdated = now,
    )

    verify(learningPathRepository, times(1)).insert(eqTo(expectedNewLearningPath))(using any)
  }

  test("That isBasedOn is not sat if the existing learningpath is PRIVATE") {
    val now = NDLADate.now()
    when(clock.now()).thenReturn(now)
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(
      Some(PRIVATE_LEARNINGPATH_NO_STEPS)
    )
    when(learningPathRepository.insert(any[LearningPath])(using any[DBSession])).thenReturn(
      PRIVATE_LEARNINGPATH_NO_STEPS
    )

    service.newFromExistingV2(PRIVATE_ID, NEW_COPIED_LEARNINGPATHV2, PRIVATE_OWNER.toCombined)

    val expectedNewLearningPath = PRIVATE_LEARNINGPATH_NO_STEPS.copy(
      id = None,
      revision = None,
      externalId = None,
      isBasedOn = None,
      status = learningpath.LearningPathStatus.PRIVATE,
      verificationStatus = LearningPathVerificationStatus.CREATED_BY_NDLA,
      owner = PRIVATE_OWNER.id,
      lastUpdated = now,
    )

    verify(learningPathRepository, times(1)).insert(eqTo(expectedNewLearningPath))(using any)
  }

  test("That isBasedOn is sat if the existing learningpath is PUBLISHED") {
    val now = NDLADate.now()
    when(clock.now()).thenReturn(now)
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(using any[DBSession])).thenReturn(
      Some(PUBLISHED_LEARNINGPATH_NO_STEPS)
    )
    when(learningPathRepository.insert(any[LearningPath])(using any[DBSession])).thenReturn(
      PUBLISHED_LEARNINGPATH_NO_STEPS
    )

    service.newFromExistingV2(PUBLISHED_ID, NEW_COPIED_LEARNINGPATHV2, MYNDLA_USER)

    val expectedNewLearningPath = PUBLISHED_LEARNINGPATH_NO_STEPS.copy(
      id = None,
      revision = None,
      externalId = None,
      isBasedOn = Some(PUBLISHED_ID),
      status = learningpath.LearningPathStatus.PRIVATE,
      verificationStatus = LearningPathVerificationStatus.EXTERNAL,
      owner = MYNDLA_USER.id,
      isMyNDLAOwner = true,
      lastUpdated = now,
    )

    verify(learningPathRepository, times(1)).insert(eqTo(expectedNewLearningPath))(using any)
  }

  test("That all editable fields are overridden if specified in input in newFromExisting") {
    val now = NDLADate.now()
    when(clock.now()).thenReturn(now)

    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(using any[DBSession])).thenReturn(
      Some(PUBLISHED_LEARNINGPATH_NO_STEPS)
    )
    when(learningPathRepository.insert(any[LearningPath])(using any[DBSession])).thenReturn(
      PUBLISHED_LEARNINGPATH_NO_STEPS
    )

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
        duration = durationOverride,
      ),
      PRIVATE_OWNER.toCombined,
    )

    val expectedNewLearningPath = PUBLISHED_LEARNINGPATH_NO_STEPS.copy(
      id = None,
      revision = None,
      externalId = None,
      isBasedOn = Some(PUBLISHED_ID),
      status = learningpath.LearningPathStatus.PRIVATE,
      verificationStatus = LearningPathVerificationStatus.CREATED_BY_NDLA,
      owner = PRIVATE_OWNER.id,
      lastUpdated = now,
      title = Seq(converterService.asTitle(api.TitleDTO(titlesToOverride, "nb"))),
      description =
        descriptionsToOverride.map(desc => converterService.asDescription(api.DescriptionDTO(desc, "nb"))).toSeq,
      tags =
        tagsToOverride.map(tagSeq => converterService.asLearningPathTags(api.LearningPathTagsDTO(tagSeq, "nb"))).toSeq,
      coverPhotoId = Some(coverPhotoId),
      duration = durationOverride,
    )

    verify(learningPathRepository, times(1)).insert(eqTo(expectedNewLearningPath))(using any)
  }

  test("That learningsteps are copied but with basic information reset in newFromExistingV2") {
    val now = NDLADate.now()
    when(clock.now()).thenReturn(now)

    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(using any[DBSession])).thenReturn(
      Some(PUBLISHED_LEARNINGPATH)
    )
    when(learningPathRepository.insert(any[LearningPath])(using any[DBSession])).thenReturn(PUBLISHED_LEARNINGPATH)

    service.newFromExistingV2(PUBLISHED_ID, NEW_COPIED_LEARNINGPATHV2, PRIVATE_OWNER.toCombined)

    val expectedNewLearningPath = PUBLISHED_LEARNINGPATH.copy(
      id = None,
      revision = None,
      externalId = None,
      isBasedOn = Some(PUBLISHED_ID),
      status = learningpath.LearningPathStatus.PRIVATE,
      verificationStatus = LearningPathVerificationStatus.CREATED_BY_NDLA,
      owner = PRIVATE_OWNER.id,
      lastUpdated = now,
      learningsteps = PUBLISHED_LEARNINGPATH
        .learningsteps
        .map(
          _.map { step =>
            val stepCopyright = step.`type` match {
              case StepType.TEXT if step.copyright.isEmpty => Some(PUBLISHED_LEARNINGPATH.copyright)
              case StepType.TEXT                           => step.copyright
              case _                                       => None

            }
            step.copy(
              id = None,
              revision = None,
              externalId = None,
              learningPathId = None,
              copyright = stepCopyright,
              lastUpdated = now,
            )
          }
        ),
    )

    verify(learningPathRepository, times(1)).insert(eqTo(expectedNewLearningPath))(using any)

  }

  test("That delete message field deletes admin message") {
    val newDate              = clock.now()
    val originalLearningPath =
      PUBLISHED_LEARNINGPATH.copy(message = Some(Message("You need to fix some stuffs", "kari", clock.now())))
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(using any[DBSession])).thenReturn(Some(originalLearningPath))
    when(learningPathRepository.learningStepWithId(eqTo(PUBLISHED_ID), eqTo(STEP1.id.get))(using any[DBSession]))
      .thenReturn(Some(STEP1))
    when(learningPathRepository.learningStepsFor(eqTo(PUBLISHED_ID))(using any[DBSession])).thenReturn(List())
    when(learningPathRepository.update(any[LearningPath])(using any[DBSession])).thenAnswer((i: InvocationOnMock) =>
      i.getArgument[LearningPath](0)
    )
    when(clock.now()).thenReturn(newDate)
    when(learningPathRepository.learningPathsWithIsBasedOn(any[Long])).thenReturn(List.empty)

    val lpToUpdate = UpdatedLearningPathV2DTO(
      1,
      None,
      "nb",
      None,
      commonApi.Missing,
      None,
      None,
      None,
      Some(true),
      commonApi.Missing,
      None,
      None,
      None,
      commonApi.Missing,
      None,
    )
    service.updateLearningPathV2(PUBLISHED_ID, lpToUpdate, PUBLISHED_OWNER.toCombined)

    val expectedUpdatedPath = PUBLISHED_LEARNINGPATH.copy(lastUpdated = newDate, message = None)

    verify(learningPathRepository, times(1)).update(eqTo(expectedUpdatedPath))(using any[DBSession])
  }

  test("That an existing coverphoto can be removed") {
    when(learningPathRepository.withId(eqTo(PUBLISHED_ID))(using any[DBSession])).thenReturn(
      Some(PUBLISHED_LEARNINGPATH)
    )
    when(learningPathRepository.update(any[LearningPath])(using any[DBSession])).thenReturn(
      PUBLISHED_LEARNINGPATH.copy(coverPhotoId = None)
    )

    assertResult(None) {
      service
        .updateLearningPathV2(PUBLISHED_ID, UPDATED_PUBLISHED_LEARNINGPATHV2, PUBLISHED_OWNER.toCombined)
        .get
        .coverPhoto
    }
  }

  test("That delete learning step language should fail when only one language") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(Some(PRIVATE_LEARNINGPATH))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(STEP1.id.get))(using any[DBSession]))
      .thenReturn(Some(STEP1))
    val Failure(result) = service.deleteLearningStepLanguage(
      PRIVATE_LEARNINGPATH.id.get,
      STEP1.id.get,
      "nb",
      PRIVATE_OWNER.toCombined,
    ): @unchecked

    result.getMessage should equal("Cannot delete last title for step with id 1")
  }
  test("That delete learning step removes language from all language fields") {
    val step = STEP1.copy(title = Seq(Title("Tittel", "nb"), Title("Title", "en")))

    val lp = PRIVATE_LEARNINGPATH.copy(learningsteps = Some(Seq(step)))

    val stepCaptor: ArgumentCaptor[LearningStep] = ArgumentCaptor.forClass(classOf[LearningStep])
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(Some(lp))
    when(learningPathRepository.learningStepWithId(eqTo(PRIVATE_ID), eqTo(step.id.get))(using any[DBSession]))
      .thenReturn(Some(step))
    service.deleteLearningStepLanguage(lp.id.get, step.id.get, "en", PRIVATE_OWNER.toCombined)
    verify(learningPathRepository).updateLearningStep(stepCaptor.capture())(using any)
    stepCaptor.getValue.title.length should be(1)
  }

  test("That delete learning path language should fail when only one language") {
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(
      Some(PRIVATE_LEARNINGPATH_NO_STEPS)
    )
    val res = service.deleteLearningPathLanguage(PRIVATE_ID, "nb", PRIVATE_OWNER.toCombined)
    res should be(
      Failure(OperationNotAllowedException(s"Cannot delete last language for learning path with id $PRIVATE_ID"))
    )
  }

  test("That delete learning path language should also delete from all steps") {
    val learningPath = PRIVATE_LEARNINGPATH.copy(
      title = PRIVATE_LEARNINGPATH.title :+ Title("Tittel", "nn"),
      description = PRIVATE_LEARNINGPATH.description :+ Description("Beskrivelse", "nn"),
      learningsteps =
        Some(PRIVATE_LEARNINGPATH.learningsteps.get.map(step => step.copy(title = step.title :+ Title("Tittel", "nn")))),
    )
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(Some(learningPath))
    when(learningPathRepository.updateLearningStep(any)(using any[DBSession])).thenAnswer(_.getArgument(0))
    when(learningPathRepository.update(any)(using any[DBSession])).thenAnswer(_.getArgument(0))

    val res = service.deleteLearningPathLanguage(PRIVATE_ID, "nb", PRIVATE_OWNER.toCombined).failIfFailure
    verify(learningPathRepository, times(PRIVATE_LEARNINGPATH.learningsteps.get.size)).updateLearningStep(any)(using
      any
    )
    res.supportedLanguages should be(Seq("nn"))
  }

  test("That delete learning path language should succeed even if learning step doesn't contain said language") {
    val learningPath = PRIVATE_LEARNINGPATH.copy(
      title = PRIVATE_LEARNINGPATH.title :+ Title("Tittel", "nn"),
      description = PRIVATE_LEARNINGPATH.description :+ Description("Beskrivelse", "nn"),
    )
    when(learningPathRepository.withId(eqTo(PRIVATE_ID))(using any[DBSession])).thenReturn(Some(learningPath))
    when(learningPathRepository.updateLearningStep(any)(using any[DBSession])).thenAnswer(_.getArgument(0))
    when(learningPathRepository.update(any)(using any[DBSession])).thenAnswer(_.getArgument(0))

    val res = service.deleteLearningPathLanguage(PRIVATE_ID, "nn", PRIVATE_OWNER.toCombined).failIfFailure
    verify(learningPathRepository, times(PRIVATE_LEARNINGPATH.learningsteps.get.size)).updateLearningStep(any)(using
      any
    )
    res.supportedLanguages should be(Seq("nb"))

  }
}
