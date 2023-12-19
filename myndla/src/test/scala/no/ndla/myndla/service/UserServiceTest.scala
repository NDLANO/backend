/*
 * Part of NDLA myndla
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndla.service

import no.ndla.common.errors.NotFoundException
import no.ndla.common.model.NDLADate
import no.ndla.myndla.TestData.emptyMyNDLAUser
import no.ndla.myndla.TestEnvironment
import no.ndla.myndla.model.domain.{MyNDLAGroup, MyNDLAUser, MyNDLAUserDocument, UserRole}
import no.ndla.myndla.model.api
import no.ndla.network.clients.{FeideExtendedUserInfo, FeideGroup, Membership}
import no.ndla.scalatestsuite.UnitTestSuite

import scala.util.{Failure, Success}

class UserServiceTest extends UnitTestSuite with TestEnvironment {

  val service: UserService = spy(new UserService)

  override def beforeEach(): Unit = {
    super.beforeEach()
    resetMocks()
  }

  test("That updateUserData updates user if user exist") {
    val feideId = "feide"
    val userBefore = MyNDLAUser(
      id = 42,
      feideId = feideId,
      favoriteSubjects = Seq("h", "b"),
      userRole = UserRole.STUDENT,
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
      shareName = false,
      arenaGroups = List.empty
    )
    val updatedUserData =
      api.UpdatedMyNDLAUser(favoriteSubjects = Some(Seq("r", "e")), arenaEnabled = None, shareName = Some(true), arenaGroups = None)
    val userAfterMerge = MyNDLAUser(
      id = 42,
      feideId = feideId,
      favoriteSubjects = Seq("r", "e"),
      userRole = UserRole.STUDENT,
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
    val expected = api.MyNDLAUser(
      id = 42,
      feideId = "feide",
      username = "example@email.com",
      email = "example@email.com",
      displayName = "Feide",
      favoriteSubjects = Seq("r", "e"),
      role = "student",
      organization = "oslo",
      groups = Seq(api.MyNDLAGroup(id = "id", displayName = "oslo", isPrimarySchool = false, parentId = None)),
      arenaEnabled = false,
      shareName = true
    )

    doReturn(Success(()))
      .when(folderWriteService)
      .canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied("feide", Some("feide"))
    when(feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    when(userService.getOrCreateMyNDLAUserIfNotExist(any, any)(any)).thenReturn(Success(emptyMyNDLAUser))
    when(configService.getMyNDLAEnabledOrgs).thenReturn(Success(List.empty))
    when(userRepository.userWithFeideId(eqTo(feideId))(any)).thenReturn(Success(Some(userBefore)))
    when(userRepository.updateUser(eqTo(feideId), any)(any)).thenReturn(Success(userAfterMerge))

    service.updateMyNDLAUserData(updatedUserData, Some(feideId)) should be(Success(expected))

    verify(userRepository, times(1)).userWithFeideId(any)(any)
    verify(userRepository, times(1)).updateUser(any, any)(any)
  }

  test("That updateUserData fails if user does not exist") {
    val feideId = "feide"
    val updatedUserData =
      api.UpdatedMyNDLAUser(favoriteSubjects = Some(Seq("r", "e")), arenaEnabled = None, shareName = None, arenaGroups = None)

    doReturn(Success(()))
      .when(folderWriteService)
      .canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied("feide", Some("feide"))
    when(feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    when(userService.getOrCreateMyNDLAUserIfNotExist(any, any)(any)).thenReturn(Success(emptyMyNDLAUser))
    when(userRepository.userWithFeideId(eqTo(feideId))(any)).thenReturn(Success(None))

    service.updateMyNDLAUserData(updatedUserData, Some(feideId)) should be(
      Failure(NotFoundException(s"User with feide_id $feideId was not found"))
    )

    verify(userRepository, times(1)).userWithFeideId(any)(any)
    verify(userRepository, times(0)).updateUser(any, any)(any)
  }

  test("That getMyNDLAUserData creates new UserData if no user exist") {
    when(clock.now()).thenReturn(NDLADate.now())

    val feideId = "feide"
    val feideGroups =
      Seq(
        FeideGroup(
          id = "id",
          `type` = FeideGroup.FC_ORG,
          displayName = "oslo",
          membership = Membership(primarySchool = Some(true)),
          parent = None
        )
      )
    val domainUserData = MyNDLAUser(
      id = 42,
      feideId = feideId,
      favoriteSubjects = Seq("r", "e"),
      userRole = UserRole.STUDENT,
      lastUpdated = clock.now(),
      organization = "oslo",
      groups = Seq(
        MyNDLAGroup(
          id = "id",
          displayName = "oslo",
          isPrimarySchool = true,
          parentId = None
        )
      ),
      username = "example@email.com",
      email = "example@email.com",
      arenaEnabled = false,
      displayName = "Feide",
      shareName = false,
      arenaGroups = List.empty
    )
    val apiUserData = api.MyNDLAUser(
      id = 42,
      feideId = "feide",
      username = "example@email.com",
      email = "example@email.com",
      displayName = "Feide",
      favoriteSubjects = Seq("r", "e"),
      role = "student",
      organization = "oslo",
      groups = Seq(api.MyNDLAGroup(id = "id", displayName = "oslo", isPrimarySchool = true, parentId = None)),
      arenaEnabled = false,
      shareName = false
    )
    val feideUserInfo = FeideExtendedUserInfo(
      displayName = "David",
      eduPersonAffiliation = Seq("student"),
      eduPersonPrincipalName = "example@email.com",
      mail = Seq("example@email.com")
    )

    when(configService.getMyNDLAEnabledOrgs).thenReturn(Success(List.empty))
    when(feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    when(feideApiClient.getFeideAccessTokenOrFail(any)).thenReturn(Success(feideId))
    when(feideApiClient.getFeideExtendedUser(any)).thenReturn(Success(feideUserInfo))
    when(feideApiClient.getFeideGroups(Some(feideId))).thenReturn(Success(feideGroups))
    when(feideApiClient.getOrganization(any)).thenReturn(Success("oslo"))
    when(userRepository.userWithFeideId(any)(any)).thenReturn(Success(None))
    when(userRepository.insertUser(any, any[MyNDLAUserDocument])(any))
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
    val domainUserData = MyNDLAUser(
      id = 42,
      feideId = feideId,
      favoriteSubjects = Seq("r", "e"),
      userRole = UserRole.STUDENT,
      lastUpdated = clock.now().plusDays(1),
      organization = "oslo",
      groups = Seq(
        MyNDLAGroup(
          id = "id",
          displayName = "oslo",
          isPrimarySchool = true,
          parentId = None
        )
      ),
      username = "example@email.com",
      email = "example@email.com",
      arenaEnabled = false,
      displayName = "Feide",
      shareName = false,
      arenaGroups = List.empty
    )
    val apiUserData = api.MyNDLAUser(
      id = 42,
      feideId = "feide",
      username = "example@email.com",
      email = "example@email.com",
      displayName = "Feide",
      favoriteSubjects = Seq("r", "e"),
      role = "student",
      organization = "oslo",
      groups = Seq(api.MyNDLAGroup(id = "id", displayName = "oslo", isPrimarySchool = true, parentId = None)),
      arenaEnabled = false,
      shareName = false
    )

    when(configService.getMyNDLAEnabledOrgs).thenReturn(Success(List.empty))
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
    val feideGroups =
      Seq(
        FeideGroup(
          id = "id",
          `type` = FeideGroup.FC_ORG,
          displayName = "oslo",
          membership = Membership(primarySchool = Some(true)),
          parent = None
        )
      )
    val domainUserData = MyNDLAUser(
      id = 42,
      feideId = feideId,
      favoriteSubjects = Seq("r", "e"),
      userRole = UserRole.STUDENT,
      lastUpdated = clock.now().minusDays(1),
      organization = "oslo",
      groups = Seq(
        MyNDLAGroup(
          id = "id",
          displayName = "oslo",
          isPrimarySchool = true,
          parentId = None
        )
      ),
      username = "example@email.com",
      email = "example@email.com",
      arenaEnabled = false,
      displayName = "Feide",
      shareName = false,
      arenaGroups = List.empty
    )
    val updatedFeideUser = FeideExtendedUserInfo(
      displayName = "name",
      eduPersonAffiliation = Seq.empty,
      eduPersonPrincipalName = "example@email.com",
      mail = Seq("example@email.com")
    )
    val apiUserData = api.MyNDLAUser(
      id = 42,
      feideId = "feide",
      username = "example@email.com",
      email = "example@email.com",
      displayName = "Feide",
      favoriteSubjects = Seq("r", "e"),
      role = "student",
      organization = "oslo",
      groups = Seq(api.MyNDLAGroup(id = "id", displayName = "oslo", isPrimarySchool = true, parentId = None)),
      arenaEnabled = false,
      shareName = false
    )

    when(configService.getMyNDLAEnabledOrgs).thenReturn(Success(List.empty))
    when(feideApiClient.getFeideID(Some(feideId))).thenReturn(Success(feideId))
    when(feideApiClient.getFeideExtendedUser(Some(feideId))).thenReturn(Success(updatedFeideUser))
    when(feideApiClient.getFeideGroups(Some(feideId))).thenReturn(Success(feideGroups))
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

}
