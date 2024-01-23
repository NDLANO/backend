/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.controller

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import no.ndla.common.errors.AccessDeniedException
import no.ndla.myndla.model.domain.{ArenaGroup, MyNDLAUser, UserRole}
import no.ndla.myndlaapi.model.arena.api.PaginatedTopics
import no.ndla.myndlaapi.{Eff, TestData, TestEnvironment}
import no.ndla.network.tapir.Service
import no.ndla.scalatestsuite.UnitTestSuite
import sttp.client3.quick._

import scala.util.{Failure, Success}

class ArenaControllerTest extends UnitTestSuite with TestEnvironment {
  val serverPort: Int = findFreePort

  val controller                            = new ArenaController()
  override val services: List[Service[Eff]] = List(controller)

  override def beforeAll(): Unit = {
    IO { Routes.startJdkServer(this.getClass.getName, serverPort) {} }.unsafeRunAndForget()
    Thread.sleep(1000)
  }

  override def beforeEach(): Unit = {
    resetMocks()
    when(clock.now()).thenReturn(TestData.today)
  }

  val feideToken = "aec48787-36b7-4d04-8c11-40e374256f1e"
  val feideId    = "someid"

  val testUser = MyNDLAUser(
    id = 1,
    feideId = feideId,
    favoriteSubjects = Seq.empty,
    userRole = UserRole.EMPLOYEE,
    lastUpdated = TestData.today,
    organization = "yap",
    groups = Seq.empty,
    username = "username",
    displayName = "displayName",
    email = "some@example.com",
    arenaEnabled = true,
    arenaGroups = List.empty,
    shareName = false
  )

  test("That feide token parsing works if token present") {
    when(userService.getArenaEnabledUser(eqTo(Some(feideToken)))).thenReturn(Success(testUser))
    when(arenaReadService.getRecentTopics(any, any, any, any)(any)).thenReturn(
      Success(
        PaginatedTopics(
          items = List.empty,
          page = 1,
          pageSize = 10,
          totalCount = 0
        )
      )
    )
    val request = quickRequest
      .get(uri"http://localhost:$serverPort/myndla-api/v1/arena/topics/recent")
      .header("FeideAuthorization", s"Bearer $feideToken")
    val response = simpleHttpClient.send(request)

    verify(arenaReadService, times(1)).getRecentTopics(any, any, any, any)(any)
    response.code.code should be(200)
  }

  test("That feide token parsing returns forbidden if token present, but no user") {
    when(userService.getArenaEnabledUser(eqTo(Some(feideToken)))).thenReturn(Failure(AccessDeniedException.forbidden))
    when(arenaReadService.getRecentTopics(any, any, any, any)(any)).thenReturn(
      Success(
        PaginatedTopics(
          items = List.empty,
          page = 1,
          pageSize = 10,
          totalCount = 0
        )
      )
    )
    val request = quickRequest
      .get(uri"http://localhost:$serverPort/myndla-api/v1/arena/topics/recent")
      .header("FeideAuthorization", s"Bearer $feideToken")
    val response = simpleHttpClient.send(request)

    verify(arenaReadService, times(0)).getRecentTopics(any, any, any, any)(any)
    response.code.code should be(403)
  }

  test("That feide token parsing returns unauthorized if no token present") {
    when(userService.getArenaEnabledUser(eqTo(None))).thenReturn(Failure(AccessDeniedException.unauthorized))
    when(arenaReadService.getRecentTopics(any, any, any, any)(any)).thenReturn(
      Success(
        PaginatedTopics(
          items = List.empty,
          page = 1,
          pageSize = 10,
          totalCount = 0
        )
      )
    )
    val request  = quickRequest.get(uri"http://localhost:$serverPort/myndla-api/v1/arena/topics/recent")
    val response = simpleHttpClient.send(request)

    verify(arenaReadService, times(0)).getRecentTopics(any, any, any, any)(any)
    response.code.code should be(401)
  }

  test("That admin feide token parsing returns 403 if no admin") {
    when(userService.getArenaEnabledUser(eqTo(Some(feideToken))))
      .thenReturn(Success(testUser.copy(arenaGroups = List.empty)))
    when(arenaReadService.deleteCategory(eqTo(1L), eqTo(testUser))(any)).thenReturn(Success(()))

    val request = quickRequest
      .delete(uri"http://localhost:$serverPort/myndla-api/v1/arena/categories/1")
      .header("FeideAuthorization", s"Bearer $feideToken")

    val response = simpleHttpClient.send(request)

    verify(arenaReadService, times(0)).deleteCategory(any, eqTo(testUser))(any)
    response.code.code should be(403)
  }

  test("That admin feide token parsing returns 200 if admin") {
    val adminUser = testUser.copy(arenaGroups = List(ArenaGroup.ADMIN))
    when(userService.getArenaEnabledUser(eqTo(Some(feideToken))))
      .thenReturn(Success(adminUser))

    when(arenaReadService.deleteCategory(eqTo(1L), eqTo(adminUser))(any)).thenReturn(Success(()))

    val request = quickRequest
      .delete(uri"http://localhost:$serverPort/myndla-api/v1/arena/categories/1")
      .header("FeideAuthorization", s"Bearer $feideToken")

    val response = simpleHttpClient.send(request)

    verify(arenaReadService, times(1)).deleteCategory(any, eqTo(adminUser))(any)
    response.code.code should be(200)
  }

}
