/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.service

import no.ndla.common.errors.NotFoundException
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.myndla.{MyNDLAGroupDTO, MyNDLAUserDTO, UpdatedMyNDLAUserDTO}
import no.ndla.common.model.domain.myndla.{MyNDLAGroup, MyNDLAUser, MyNDLAUserDocument, UserRole}
import no.ndla.myndlaapi.TestData.emptyMyNDLAUser
import no.ndla.myndlaapi.TestEnvironment
import no.ndla.network.clients.{FeideExtendedUserInfo, FeideGroup, Membership}
import no.ndla.scalatestsuite.UnitTestSuite
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*

import scala.util.{Failure, Success, Try}

class UserServiceTest extends UnitTestSuite with TestEnvironment {
  override implicit lazy val folderConverterService: FolderConverterService = spy(new FolderConverterService)
  val service: UserService                                                  = spy(new UserService)

  override def beforeEach(): Unit = {
    super.beforeEach()
    resetMocks()
  }

  test("That updateUserData updates user if user exist") {
    val feideId    = "feide"
    val userBefore = MyNDLAUser(
      id = 42,
      feideId = feideId,
      favoriteSubjects = Seq("h", "b"),
      userRole = UserRole.STUDENT,
      lastUpdated = clock.now(),
      organization = "oslo",
      groups = Seq(MyNDLAGroup(id = "id", displayName = "oslo", isPrimarySchool = false, parentId = None)),
      username = "example@email.com",
      displayName = "Feide",
      email = "example@email.com",
      arenaEnabled = false,
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

    doReturn(Success(()))
      .when(folderWriteService)
      .canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied("feide", Some("feide"))
    when(feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    when(userService.getOrCreateMyNDLAUserIfNotExist(any, any)(using any)).thenReturn(Success(emptyMyNDLAUser))
    when(userRepository.userWithFeideId(eqTo(feideId))(using any)).thenReturn(Success(Some(userBefore)))
    when(userRepository.updateUser(eqTo(feideId), any)(using any)).thenReturn(Success(userAfterMerge))

    service.updateMyNDLAUserData(updatedUserData, Some(feideId)) should be(Success(expected))

    verify(userRepository, times(1)).userWithFeideId(any)(using any)
    verify(userRepository, times(1)).updateUser(any, any)(using any)
  }

  test("That updateUserData fails if user does not exist") {
    val feideId         = "feide"
    val updatedUserData = UpdatedMyNDLAUserDTO(favoriteSubjects = Some(Seq("r", "e")), arenaEnabled = None)

    doReturn(Success(()))
      .when(folderWriteService)
      .canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied("feide", Some("feide"))
    when(feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    when(userService.getOrCreateMyNDLAUserIfNotExist(any, any)(using any)).thenReturn(Success(emptyMyNDLAUser))
    when(userRepository.userWithFeideId(eqTo(feideId))(using any)).thenReturn(Success(None))

    service.updateMyNDLAUserData(updatedUserData, Some(feideId)) should be(
      Failure(NotFoundException(s"User with feide_id $feideId was not found"))
    )

    verify(userRepository, times(1)).userWithFeideId(any)(using any)
    verify(userRepository, times(0)).updateUser(any, any)(using any)
  }

  test("That getMyNDLAUserData creates new UserData if no user exist") {
    when(clock.now()).thenReturn(NDLADate.now())
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
      lastUpdated = clock.now(),
      organization = "oslo",
      groups = Seq(MyNDLAGroup(id = "id", displayName = "oslo", isPrimarySchool = true, parentId = None)),
      username = "example@email.com",
      displayName = "Feide",
      email = "example@email.com",
      arenaEnabled = false,
    )
    val apiUserData = MyNDLAUserDTO(
      id = 42,
      feideId = "feide",
      username = "example@email.com",
      email = "example@email.com",
      displayName = "Feide",
      favoriteSubjects = Seq("r", "e"),
      role = UserRole.STUDENT,
      organization = "oslo",
      groups = Seq(MyNDLAGroupDTO(id = "id", displayName = "oslo", isPrimarySchool = true, parentId = None)),
      arenaEnabled = false,
    )
    val feideUserInfo = FeideExtendedUserInfo(
      displayName = "David",
      eduPersonAffiliation = Seq("student"),
      None,
      eduPersonPrincipalName = "example@email.com",
      mail = Some(Seq("example@email.com")),
    )

    when(feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    when(feideApiClient.getFeideAccessTokenOrFail(any)).thenReturn(Success(feideId))
    when(feideApiClient.getFeideExtendedUser(any)).thenReturn(Success(feideUserInfo))
    when(feideApiClient.getFeideGroups(Some(feideId))).thenReturn(Success(feideGroups))
    when(feideApiClient.getOrganization(any)).thenReturn(Success("oslo"))
    when(userRepository.userWithFeideId(any)(using any)).thenReturn(Success(None))
    when(userRepository.insertUser(any, any[MyNDLAUserDocument])(using any)).thenReturn(Success(domainUserData))

    service.getMyNDLAUserData(Some(feideId)).get should be(apiUserData)

    verify(feideApiClient, times(1)).getFeideExtendedUser(any)
    verify(feideApiClient, times(1)).getFeideGroups(any)
    verify(feideApiClient, times(1)).getOrganization(any)
    verify(userRepository, times(1)).reserveFeideIdIfNotExists(any)(using any)
    verify(userRepository, times(1)).insertUser(any, any)(using any)
    verify(userRepository, times(0)).updateUser(any, any)(using any)
  }

  test("That getMyNDLAUserData returns already created user if it exists and was updated lately") {
    when(clock.now()).thenReturn(NDLADate.now())
    when(userRepository.reserveFeideIdIfNotExists(any)(using any)).thenReturn(Success(true))

    val feideId        = "feide"
    val domainUserData = MyNDLAUser(
      id = 42,
      feideId = feideId,
      favoriteSubjects = Seq("r", "e"),
      userRole = UserRole.STUDENT,
      lastUpdated = clock.now().plusDays(1),
      organization = "oslo",
      groups = Seq(MyNDLAGroup(id = "id", displayName = "oslo", isPrimarySchool = true, parentId = None)),
      username = "example@email.com",
      displayName = "Feide",
      email = "example@email.com",
      arenaEnabled = false,
    )
    val apiUserData = MyNDLAUserDTO(
      id = 42,
      feideId = "feide",
      username = "example@email.com",
      email = "example@email.com",
      displayName = "Feide",
      favoriteSubjects = Seq("r", "e"),
      role = UserRole.STUDENT,
      organization = "oslo",
      groups = Seq(MyNDLAGroupDTO(id = "id", displayName = "oslo", isPrimarySchool = true, parentId = None)),
      arenaEnabled = false,
    )

    when(feideApiClient.getFeideID(Some(feideId))).thenReturn(Success(feideId))
    when(userRepository.userWithFeideId(eqTo(feideId))(using any)).thenReturn(Success(Some(domainUserData)))

    service.getMyNDLAUserData(Some(feideId)).get should be(apiUserData)

    verify(feideApiClient, times(0)).getFeideExtendedUser(any)
    verify(feideApiClient, times(0)).getFeideGroups(any)
    verify(userRepository, times(1)).userWithFeideId(any)(using any)
    verify(userRepository, times(0)).insertUser(any, any)(using any)
    verify(userRepository, times(0)).updateUser(any, any)(using any)
  }

  test("That getMyNDLAUserData returns already created user if it exists but needs update") {
    when(clock.now()).thenReturn(NDLADate.now())
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
      lastUpdated = clock.now().minusDays(1),
      organization = "oslo",
      groups = Seq(MyNDLAGroup(id = "id", displayName = "oslo", isPrimarySchool = true, parentId = None)),
      username = "example@email.com",
      displayName = "Feide",
      email = "example@email.com",
      arenaEnabled = false,
    )
    val updatedFeideUser = FeideExtendedUserInfo(
      displayName = "name",
      eduPersonAffiliation = Seq.empty,
      None,
      eduPersonPrincipalName = "example@email.com",
      mail = Some(Seq("example@email.com")),
    )
    val apiUserData = MyNDLAUserDTO(
      id = 42,
      feideId = "feide",
      username = "example@email.com",
      email = "example@email.com",
      displayName = "Feide",
      favoriteSubjects = Seq("r", "e"),
      role = UserRole.STUDENT,
      organization = "oslo",
      groups = Seq(MyNDLAGroupDTO(id = "id", displayName = "oslo", isPrimarySchool = true, parentId = None)),
      arenaEnabled = false,
    )

    when(feideApiClient.getFeideID(Some(feideId))).thenReturn(Success(feideId))
    when(feideApiClient.getFeideExtendedUser(Some(feideId))).thenReturn(Success(updatedFeideUser))
    when(feideApiClient.getFeideGroups(Some(feideId))).thenReturn(Success(feideGroups))
    when(feideApiClient.getOrganization(Some(feideId))).thenReturn(Success("oslo"))
    when(userRepository.userWithFeideId(eqTo(feideId))(using any)).thenReturn(Success(Some(domainUserData)))
    when(userRepository.updateUser(any, any)(using any)).thenReturn(Success(domainUserData))

    service.getMyNDLAUserData(Some(feideId)).get should be(apiUserData)

    verify(feideApiClient, times(1)).getFeideExtendedUser(any)
    verify(feideApiClient, times(1)).getFeideGroups(any)
    verify(feideApiClient, times(1)).getOrganization(any)
    verify(userRepository, times(1)).userWithFeideId(any)(using any)
    verify(userRepository, times(0)).insertUser(any, any)(using any)
    verify(userRepository, times(1)).updateUser(any, any)(using any)
  }

}
