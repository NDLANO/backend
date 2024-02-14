/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.controller

import no.ndla.myndla.model.api.Folder
import no.ndla.myndla.model.domain.{MyNDLAUser, UserRole}
import no.ndla.myndlaapi.{Eff, TestData, TestEnvironment}
import no.ndla.network.tapir.Service
import no.ndla.scalatestsuite.UnitTestSuite
import sttp.client3.quick._

import java.util.UUID
import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class FolderControllerTest extends UnitTestSuite with TestEnvironment {
  val serverPort: Int = findFreePort

  val controller                            = new FolderController()
  override val services: List[Service[Eff]] = List(controller)

  override def beforeAll(): Unit = {
    implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1))
    Future { Routes.startJdkServer(this.getClass.getName, serverPort) {} }
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

  test("That resources fetching works and doesnt interfere with folder/:id") {
    when(folderReadService.getAllResources(any, any)).thenReturn(Success(List.empty))
    val request = quickRequest
      .get(uri"http://localhost:$serverPort/myndla-api/v1/folders/resources")
      .header("FeideAuthorization", s"Bearer $feideToken")
    val response = simpleHttpClient.send(request)

    verify(folderReadService, times(1)).getAllResources(any, any)
    verify(folderReadService, times(0)).getSingleFolder(any, any, any, any)
    response.code.code should be(200)
  }

  test("That fetching single folder works with id") {
    val someId = UUID.randomUUID()
    when(folderReadService.getSingleFolder(eqTo(someId), any, any, any)).thenReturn(
      Success(
        Folder(
          id = someId.toString,
          name = "folderName",
          status = "private",
          parentId = None,
          breadcrumbs = List.empty,
          subfolders = List.empty,
          resources = List.empty,
          rank = Some(1),
          created = TestData.today,
          updated = TestData.today,
          shared = None,
          description = None,
          owner = None
        )
      )
    )
    val request = quickRequest
      .get(uri"http://localhost:$serverPort/myndla-api/v1/folders/${someId.toString}")
      .header("FeideAuthorization", s"Bearer $feideToken")
    val response = simpleHttpClient.send(request)

    verify(folderReadService, times(0)).getAllResources(any, any)
    verify(folderReadService, times(1)).getSingleFolder(eqTo(someId), any, any, any)
    response.code.code should be(200)
  }

  test("That no endpoints are shadowed") {
    import sttp.tapir.testing.EndpointVerifier
    val errors = EndpointVerifier(controller.endpoints.map(_.endpoint))
    if (errors.nonEmpty) {
      val errString = errors.map(e => e.toString).mkString("\n\t- ", "\n\t- ", "")
      fail(s"Got errors when verifying ${controller.serviceName} controller:$errString")
    }
  }

}
