/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.controller

import no.ndla.common.model.domain.myndla.{MyNDLAUser, UserRole}
import no.ndla.myndlaapi.model.api.FolderDTO
import no.ndla.myndlaapi.{TestData, TestEnvironment}
import no.ndla.scalatestsuite.UnitTestSuite
import no.ndla.tapirtesting.TapirControllerTest
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import sttp.client3.quick.*

import java.util.UUID
import scala.util.Success

class FolderControllerTest extends UnitTestSuite with TestEnvironment with TapirControllerTest {
  val controller: FolderController = new FolderController()

  override def beforeEach(): Unit = {
    resetMocks()
    when(clock.now()).thenReturn(TestData.today)
  }

  val feideToken = "aec48787-36b7-4d04-8c11-40e374256f1e"
  val feideId    = "someid"

  val testUser: MyNDLAUser = MyNDLAUser(
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
    arenaAccepted = true,
    shareNameAccepted = false
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
        FolderDTO(
          id = someId,
          name = "folderName",
          status = "private",
          parentId = None,
          breadcrumbs = List.empty,
          subfolders = List.empty,
          resources = List.empty,
          rank = 1,
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
}
