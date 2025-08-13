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
import org.mockito.invocation.InvocationOnMock
import scalikejdbc.DBSession

import scala.util.{Failure, Success, Try}

class UserServiceTest extends UnitTestSuite with TestEnvironment {

  val service: UserService                                         = spy(new UserService)
  override lazy val folderConverterService: FolderConverterService = spy(new FolderConverterService)

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
      groups = Seq(
        MyNDLAGroup(
          id = "id",
          displayName = "oslo",
          isPrimarySchool = false,
          parentId = None
        )
      ),
      username = "example@email.com",
      displayName = "Feide",
      email = "example@email.com",
      arenaEnabled = false,
      arenaAccepted = true,
      shareNameAccepted = false
    )
    val updatedUserData =
      UpdatedMyNDLAUserDTO(
        favoriteSubjects = Some(Seq("r", "e")),
        arenaEnabled = None,
        arenaAccepted = None,
        shareNameAccepted = None
      )
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
      displayName = "Feide",
      email = "example@email.com",
      arenaEnabled = false,
      arenaAccepted = true,
      shareNameAccepted = false
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
      arenaAccepted = true,
      shareNameAccepted = false
    )

    doReturn(Success(()))
      .when(folderWriteService)
      .canWriteDuringMyNDLAWriteRestrictionsOrAccessDenied("feide", Some("feide"))
    when(feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    when(userService.getOrCreateMyNDLAUserIfNotExist(any, any)(any)).thenReturn(Success(emptyMyNDLAUser))
    when(userRepository.userWithFeideId(eqTo(feideId))(any)).thenReturn(Success(Some(userBefore)))
    when(userRepository.updateUser(eqTo(feideId), any)(any)).thenReturn(Success(userAfterMerge))

    service.updateMyNDLAUserData(updatedUserData, Some(feideId)) should be(Success(expected))

    verify(userRepository, times(1)).userWithFeideId(any)(any)
    verify(userRepository, times(1)).updateUser(any, any)(any)
  }

  test("That updateUserData fails if user does not exist") {
    val feideId         = "feide"
    val updatedUserData =
      UpdatedMyNDLAUserDTO(
        favoriteSubjects = Some(Seq("r", "e")),
        arenaEnabled = None,
        arenaAccepted = None,
        shareNameAccepted = None
      )

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
    doAnswer((i: InvocationOnMock) => {
      val func = i.getArgument[DBSession => Try[Nothing]](0)
      func(mock[DBSession])
    }).when(DBUtil).rollbackOnFailure(any())
    when(userRepository.reserveFeideIdIfNotExists(any)(any)).thenReturn(Success(false))

    val feideId     = "feide"
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
      displayName = "Feide",
      email = "example@email.com",
      arenaEnabled = false,
      arenaAccepted = true,
      shareNameAccepted = false
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
      arenaAccepted = true,
      shareNameAccepted = false
    )
    val feideUserInfo = FeideExtendedUserInfo(
      displayName = "David",
      eduPersonAffiliation = Seq("student"),
      None,
      eduPersonPrincipalName = "example@email.com",
      mail = Some(Seq("example@email.com"))
    )

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
    verify(userRepository, times(1)).reserveFeideIdIfNotExists(any)(any)
    verify(userRepository, times(1)).insertUser(any, any)(any)
    verify(userRepository, times(0)).updateUser(any, any)(any)
  }

  test("That getMyNDLAUserData returns already created user if it exists and was updated lately") {
    when(clock.now()).thenReturn(NDLADate.now())
    doAnswer((i: InvocationOnMock) => {
      val func = i.getArgument[DBSession => Try[Nothing]](0)
      func(mock[DBSession])
    }).when(DBUtil).rollbackOnFailure(any())
    when(userRepository.reserveFeideIdIfNotExists(any)(any)).thenReturn(Success(true))

    val feideId        = "feide"
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
      displayName = "Feide",
      email = "example@email.com",
      arenaEnabled = false,
      arenaAccepted = true,
      shareNameAccepted = false
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
      arenaAccepted = true,
      shareNameAccepted = false
    )

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
    doAnswer((i: InvocationOnMock) => {
      val func = i.getArgument[DBSession => Try[Nothing]](0)
      func(mock[DBSession])
    }).when(DBUtil).rollbackOnFailure(any())
    when(userRepository.reserveFeideIdIfNotExists(any)(any)).thenReturn(Success(true))

    val feideId     = "feide"
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
      displayName = "Feide",
      email = "example@email.com",
      arenaEnabled = false,
      arenaAccepted = true,
      shareNameAccepted = false
    )
    val updatedFeideUser = FeideExtendedUserInfo(
      displayName = "name",
      eduPersonAffiliation = Seq.empty,
      None,
      eduPersonPrincipalName = "example@email.com",
      mail = Some(Seq("example@email.com"))
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
      arenaAccepted = true,
      shareNameAccepted = false
    )

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
