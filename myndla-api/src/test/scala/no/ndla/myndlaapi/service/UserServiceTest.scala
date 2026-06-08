/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.service

import no.ndla.common.aws.NdlaEmailClient
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.myndla.{MyNDLAGroupDTO, MyNDLAUserDTO, UpdatedMyNDLAUserDTO}
import no.ndla.common.model.domain.myndla.{MyNDLAGroup, MyNDLAUser, MyNDLAUserDocument, UserRole}
import no.ndla.myndlaapi.TestData.emptyMyNDLAUser
import no.ndla.myndlaapi.{TestData, TestEnvironment}
import no.ndla.myndlaapi.model.api.InactiveUserResultDTO
import no.ndla.myndlaapi.model.domain.InactiveUserCleanupResult
import no.ndla.network.clients.{FeideExtendedUserInfo, FeideGroup, Membership}
import no.ndla.network.model.{FeideIdToken, FeideUserWrapper}
import no.ndla.scalatestsuite.UnitTestSuite
import no.ndla.tapirtesting.FeideAuthTestData
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*

import scala.util.{Failure, Success, Try}

class UserServiceTest extends UnitTestSuite with TestEnvironment {
  override implicit lazy val folderConverterService: FolderConverterService = spy(new FolderConverterService)
  implicit lazy val emailClient: NdlaEmailClient                            = mock[NdlaEmailClient]
  val service: UserService                                                  = spy(new UserService)

  override def resetMocks(): Unit = {
    super.resetMocks()
    reset(service)
    reset(emailClient)
    reset(folderConverterService)
  }

  private def userWithLastSeen(id: Long, feideId: String, lastSeen: NDLADate): MyNDLAUser =
    emptyMyNDLAUser.copy(id = id, feideId = feideId, lastSeen = lastSeen)

  private val feideAccessToken = "foo-bar-baz"

  private def feideWrapper(feideId: String): FeideUserWrapper = {
    val userWrapper = FeideAuthTestData.FrankForeleser
    userWrapper.copy(user = userWrapper.user.copy(feideId = feideId), idToken = userWrapper.idToken.copy(sub = feideId))
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    resetMocks()
    when(userRepository.updateLastSeen(any, any[NDLADate])(using any)).thenReturn(Success(TestData.today))
  }

  test("That updateUserData updates user if user exist") {
    val now        = clock.now()
    val feideId    = "feide"
    val userBefore = MyNDLAUser(
      id = 42,
      feideId = feideId,
      favoriteSubjects = Seq("h", "b"),
      userRole = UserRole.STUDENT,
      lastUpdated = now,
      organization = "oslo",
      groups = Seq(MyNDLAGroup(id = "id", displayName = "oslo", isPrimarySchool = false, parentId = None)),
      username = "example@email.com",
      displayName = "Feide",
      email = "example@email.com",
      arenaEnabled = false,
      lastSeen = now,
    )
    val updatedUserData = UpdatedMyNDLAUserDTO(favoriteSubjects = Some(Seq("r", "e")), arenaEnabled = None)
    val userAfterMerge  = MyNDLAUser(
      id = 42,
      feideId = feideId,
      favoriteSubjects = Seq("r", "e"),
      userRole = UserRole.STUDENT,
      lastUpdated = clock.now(),
      organization = "oslo",
      groups = Seq(MyNDLAGroup(id = "id", displayName = "oslo", isPrimarySchool = false, parentId = None)),
      username = "example@email.com",
      displayName = "Feide",
      email = "example@email.com",
      arenaEnabled = false,
      lastSeen = now,
    )
    val expected = MyNDLAUserDTO(
      id = 42,
      feideId = "feide",
      username = "example@email.com",
      email = "example@email.com",
      displayName = "Feide",
      favoriteSubjects = Seq("r", "e"),
      role = UserRole.STUDENT,
      organization = "oslo",
      groups = Seq(MyNDLAGroupDTO(id = "id", displayName = "oslo", isPrimarySchool = false, parentId = None)),
      arenaEnabled = false,
    )

    val feide = FeideUserWrapper(userBefore, mock[FeideIdToken], feideAccessToken)
    when(folderWriteService.canWriteOrAccessDenied(any[FeideUserWrapper])).thenReturn(Success(userBefore))
    when(userRepository.userWithFeideId(eqTo(feideId))(using any)).thenReturn(Success(Some(userBefore)))
    when(userRepository.updateUser(eqTo(feideId), any)(using any)).thenReturn(Success(userAfterMerge))

    service.updateMyNDLAUserData(updatedUserData, feide) should be(Success(expected))

    verify(userRepository, times(1)).updateUser(any, any)(using any)
  }

  test("That updateUserData fails if user does not exist") {
    val feideId         = "feide"
    val updatedUserData = UpdatedMyNDLAUserDTO(favoriteSubjects = Some(Seq("r", "e")), arenaEnabled = None)
    val feide           = feideWrapper(feideId)

    when(folderWriteService.canWriteOrAccessDenied(any[FeideUserWrapper])).thenReturn(
      Success(emptyMyNDLAUser.copy(feideId = feideId))
    )
    val expectedException = RuntimeException()
    when(userRepository.updateUser(eqTo(feideId), any)(using any)).thenReturn(Failure(expectedException))

    service.updateMyNDLAUserData(updatedUserData, feide) should be(Failure(expectedException))
  }

  test("That getOrCreateMyNdlaUser creates new UserData if no user exist") {
    val now = TestData.today
    when(clock.now()).thenReturn(now)
    when(userRepository.reserveFeideIdIfNotExists(any)(using any)).thenReturn(Success(false))

    val feideId     = "feide"
    val feideGroups = Seq(
      FeideGroup(
        id = "id",
        `type` = FeideGroup.FC_ORG,
        displayName = "oslo",
        membership = Membership(primarySchool = Some(true)),
        parent = None,
      )
    )
    val domainUserData = MyNDLAUser(
      id = 42,
      feideId = feideId,
      favoriteSubjects = Seq("r", "e"),
      userRole = UserRole.STUDENT,
      lastUpdated = now,
      organization = "oslo",
      groups = Seq(MyNDLAGroup(id = "id", displayName = "oslo", isPrimarySchool = true, parentId = None)),
      username = "example@email.com",
      displayName = "Feide",
      email = "example@email.com",
      arenaEnabled = false,
      lastSeen = now,
    )
    val feideUserInfo = FeideExtendedUserInfo(
      displayName = "David",
      eduPersonAffiliation = Seq("student"),
      None,
      eduPersonPrincipalName = "example@email.com",
      mail = Some(Seq("example@email.com")),
    )

    when(feideApiClient.getFeideExtendedUser(eqTo(feideId), eqTo(feideAccessToken))).thenReturn(Success(feideUserInfo))
    when(feideApiClient.getFeideGroupsAndOrganization(eqTo(feideId), eqTo(feideAccessToken))).thenReturn(
      Success((feideGroups, "oslo"))
    )
    when(userRepository.userWithFeideId(any)(using any)).thenReturn(Success(None))
    when(userRepository.insertUser(any, any[MyNDLAUserDocument])(using any)).thenReturn(Success(domainUserData))

    service.getOrCreateMyNdlaUser(feideId, feideAccessToken)(using DBUtil.autoSession).get should be(domainUserData)

    verify(feideApiClient, times(1)).getFeideExtendedUser(any, any)
    verify(feideApiClient, times(1)).getFeideGroupsAndOrganization(any, any)
    verify(userRepository, times(1)).reserveFeideIdIfNotExists(any)(using any)
    verify(userRepository, times(1)).insertUser(any, any)(using any)
    verify(userRepository, times(0)).updateUser(any, any)(using any)
  }

  test("That getOrCreateMyNdlaUser returns already created user if it exists and was updated lately") {
    val now = TestData.today
    when(clock.now()).thenReturn(now)
    when(userRepository.reserveFeideIdIfNotExists(any)(using any)).thenReturn(Success(true))

    val feideId        = "feide"
    val domainUserData = MyNDLAUser(
      id = 42,
      feideId = feideId,
      favoriteSubjects = Seq("r", "e"),
      userRole = UserRole.STUDENT,
      lastUpdated = now.plusDays(1),
      organization = "oslo",
      groups = Seq(MyNDLAGroup(id = "id", displayName = "oslo", isPrimarySchool = true, parentId = None)),
      username = "example@email.com",
      displayName = "Feide",
      email = "example@email.com",
      arenaEnabled = false,
      lastSeen = now,
    )

    when(userRepository.userWithFeideId(eqTo(feideId))(using any)).thenReturn(Success(Some(domainUserData)))

    service.getOrCreateMyNdlaUser(feideId, feideAccessToken)(using DBUtil.autoSession).get should be(domainUserData)

    verify(feideApiClient, times(0)).getFeideExtendedUser(any, any)
    verify(feideApiClient, times(0)).getFeideGroupsAndOrganization(any, any)
    verify(userRepository, times(1)).userWithFeideId(any)(using any)
    verify(userRepository, times(0)).insertUser(any, any)(using any)
    verify(userRepository, times(0)).updateUser(any, any)(using any)
  }

  test("That getOrCreateMyNdlaUser returns already created user if it exists but needs update") {
    val now = TestData.today
    when(clock.now()).thenReturn(now)
    when(userRepository.reserveFeideIdIfNotExists(any)(using any)).thenReturn(Success(true))

    val feideId     = "feide"
    val feideGroups = Seq(
      FeideGroup(
        id = "id",
        `type` = FeideGroup.FC_ORG,
        displayName = "oslo",
        membership = Membership(primarySchool = Some(true)),
        parent = None,
      )
    )
    val domainUserData = MyNDLAUser(
      id = 42,
      feideId = feideId,
      favoriteSubjects = Seq("r", "e"),
      userRole = UserRole.STUDENT,
      lastUpdated = now.minusDays(1),
      organization = "oslo",
      groups = Seq(MyNDLAGroup(id = "id", displayName = "oslo", isPrimarySchool = true, parentId = None)),
      username = "example@email.com",
      displayName = "Feide",
      email = "example@email.com",
      arenaEnabled = false,
      lastSeen = now,
    )
    val updatedFeideUser = FeideExtendedUserInfo(
      displayName = "name",
      eduPersonAffiliation = Seq.empty,
      None,
      eduPersonPrincipalName = "example@email.com",
      mail = Some(Seq("example@email.com")),
    )

    when(feideApiClient.getFeideExtendedUser(eqTo(feideId), eqTo(feideAccessToken))).thenReturn(
      Success(updatedFeideUser)
    )
    when(feideApiClient.getFeideGroupsAndOrganization(eqTo(feideId), eqTo(feideAccessToken))).thenReturn(
      Success((feideGroups, "oslo"))
    )
    when(userRepository.userWithFeideId(eqTo(feideId))(using any)).thenReturn(Success(Some(domainUserData)))
    when(userRepository.updateUser(any, any)(using any)).thenReturn(Success(domainUserData))

    service.getOrCreateMyNdlaUser(feideId, feideAccessToken)(using DBUtil.autoSession).get should be(domainUserData)

    verify(feideApiClient, times(1)).getFeideExtendedUser(any, any)
    verify(feideApiClient, times(1)).getFeideGroupsAndOrganization(any, any)
    verify(userRepository, times(1)).userWithFeideId(any)(using any)
    verify(userRepository, times(0)).insertUser(any, any)(using any)
    verify(userRepository, times(1)).updateUser(any, any)(using any)
  }

  test("That cleanupInactiveUsers emails all candidates when no previous cleanup exists") {
    val now = NDLADate.of(2025, 1, 1, 0, 0, 0)
    when(clock.now()).thenReturn(now)

    val usersToDelete   = List(userWithLastSeen(id = 1, feideId = "delete-1", lastSeen = now.minusDays(220)))
    val emailCandidates = List(
      userWithLastSeen(id = 2, feideId = "email-1", lastSeen = now.minusDays(181)),
      userWithLastSeen(id = 3, feideId = "email-2", lastSeen = now.minusDays(200)),
    )

    when(userRepository.getLastCleanup(using any)).thenReturn(Success(None))
    when(userRepository.getUserNotSeenSince(any[NDLADate])(using any)).thenReturn(
      Success(usersToDelete ++ emailCandidates)
    )
    when(userRepository.deleteUser(any)(using any)).thenReturn(Success("deleted"))
    when(userRepository.insertCleanupResult(eqTo(usersToDelete.size), eqTo(emailCandidates.size), eqTo(now))(using any))
      .thenReturn(Success(InactiveUserCleanupResult(1, usersToDelete.size, emailCandidates.size, now)))

    service.cleanupInactiveUsers() should be(Success(InactiveUserResultDTO(usersToDelete.size, emailCandidates.size)))

    verify(service, times(emailCandidates.size)).sendInactivityEmail(any)
    verify(userRepository, times(usersToDelete.size)).deleteUser(any)(using any)
    verify(userRepository, times(1)).insertCleanupResult(
      eqTo(usersToDelete.size),
      eqTo(emailCandidates.size),
      eqTo(now),
    )(using any)
  }

  test("That cleanupInactiveUsers does not email users that will be deleted in the same run") {
    val now = NDLADate.of(2025, 1, 1, 0, 0, 0)
    when(clock.now()).thenReturn(now)

    val deleteUser      = userWithLastSeen(id = 1, feideId = "delete-1", lastSeen = now.minusDays(220))
    val usersToDelete   = List(deleteUser)
    val shouldEmail     = userWithLastSeen(id = 2, feideId = "email-1", lastSeen = now.minusDays(181))
    val emailCandidates = List(shouldEmail)
    val expectedEmailed = 1

    when(userRepository.getLastCleanup(using any)).thenReturn(Success(None))
    when(userRepository.getUserNotSeenSince(any[NDLADate])(using any)).thenReturn(
      Success(usersToDelete ++ emailCandidates)
    )
    when(userRepository.deleteUser(any)(using any)).thenReturn(Success("deleted"))
    when(userRepository.insertCleanupResult(eqTo(usersToDelete.size), eqTo(expectedEmailed), eqTo(now))(using any))
      .thenReturn(Success(InactiveUserCleanupResult(1, usersToDelete.size, expectedEmailed, now)))

    val result = service.cleanupInactiveUsers()
    result should be(Success(InactiveUserResultDTO(usersToDelete.size, expectedEmailed)))

    verify(service, times(1)).sendInactivityEmail(eqTo(shouldEmail))
    verify(service, times(0)).sendInactivityEmail(eqTo(deleteUser))
    verify(userRepository, times(usersToDelete.size)).deleteUser(any)(using any)
    verify(userRepository, times(1)).insertCleanupResult(eqTo(usersToDelete.size), eqTo(expectedEmailed), eqTo(now))(
      using any
    )
  }

  test("That cleanupInactiveUsers only emails users who became inactive since the last successful run") {
    val now             = NDLADate.of(2025, 1, 1, 0, 0, 0)
    val lastCleanupDate = now.minusDays(5)
    when(clock.now()).thenReturn(now)

    val usersToDelete   = List.empty[MyNDLAUser]
    val shouldEmail     = userWithLastSeen(id = 10, feideId = "email-new", lastSeen = now.minusDays(181))
    val shouldSkip      = userWithLastSeen(id = 11, feideId = "email-old", lastSeen = now.minusDays(190))
    val emailCandidates = List(shouldEmail, shouldSkip)

    when(userRepository.getLastCleanup(using any)).thenReturn(
      Success(Some(InactiveUserCleanupResult(1, 0, 0, lastCleanupDate)))
    )
    when(userRepository.getUserNotSeenSince(any[NDLADate])(using any)).thenReturn(
      Success(usersToDelete ++ emailCandidates)
    )
    when(userRepository.insertCleanupResult(eqTo(0), eqTo(1), eqTo(now))(using any)).thenReturn(
      Success(InactiveUserCleanupResult(2, 0, 1, now))
    )

    service.cleanupInactiveUsers() should be(Success(InactiveUserResultDTO(0, 1)))

    verify(service, times(1)).sendInactivityEmail(eqTo(shouldEmail))
    verify(service, times(0)).sendInactivityEmail(eqTo(shouldSkip))
    verify(userRepository, times(0)).deleteUser(any)(using any)
    verify(userRepository, times(1)).insertCleanupResult(eqTo(0), eqTo(1), eqTo(now))(using any)
  }

}
