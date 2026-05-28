/*
 * Part of NDLA myndla-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.e2e

import no.ndla.common.configuration.Prop
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.ResourceType
import no.ndla.common.model.domain.myndla.FolderStatus
import no.ndla.common.{CirceUtil, Clock}
import no.ndla.myndlaapi.integration.SearchApiClient
import no.ndla.myndlaapi.integration.nodebb.NodeBBClient
import no.ndla.myndlaapi.model.api
import no.ndla.myndlaapi.model.api.{FeideSessionDTO, FolderDTO}
import no.ndla.myndlaapi.repository.{FolderRepository, UserRepository}
import no.ndla.myndlaapi.service.UserService
import no.ndla.myndlaapi.{ComponentRegistry, MainClass, MyNdlaApiProperties, TestEnvironment, UnitSuite}
import no.ndla.network.clients.{FeideApiClient, FeideExtendedUserInfo}
import no.ndla.network.tapir.auth.FeideAuth
import no.ndla.scalatestsuite.{DatabaseIntegrationSuite, RedisIntegrationSuite}
import no.ndla.tapirtesting.{FeideAuthTest, FeideAuthTestData}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.mockito.quality.Strictness
import scalikejdbc.DBSession
import sttp.client4.quick.*

import java.util.UUID
import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.Success

class UserTest extends DatabaseIntegrationSuite with RedisIntegrationSuite with UnitSuite with TestEnvironment {

  val myndlaApiPort: Int                    = findFreePort
  val pgc: PgConnectionInfo                 = pgConnectionInfo.get
  val testRedisPort: Int                    = redisPort.get
  val myndlaproperties: MyNdlaApiProperties = new MyNdlaApiProperties {
    override def ApplicationPort: Int = myndlaApiPort

    override val MetaServer: Prop[String]   = propFromTestValue("META_SERVER", pgc.host)
    override val MetaResource: Prop[String] = propFromTestValue("META_RESOURCE", pgc.databaseName)
    override val MetaUserName: Prop[String] = propFromTestValue("META_USER_NAME", pgc.username)
    override val MetaPassword: Prop[String] = propFromTestValue("META_PASSWORD", pgc.password)
    override val MetaPort: Prop[Int]        = propFromTestValue("META_PORT", pgc.port)
    override val MetaSchema: Prop[String]   = propFromTestValue("META_SCHEMA", schemaName)

    override def RedisHost: String = "localhost"
    override def RedisPort: Int    = testRedisPort
  }

  val feideId: String      = FeideAuthTestData.FrankForeleser.idToken.sub
  val feideIdToken: String = FeideAuthTestData.FrankForeleser.idToken.originalToken

  val myndlaApi: MainClass = new MainClass(myndlaproperties) {
    override val componentRegistry: ComponentRegistry = new ComponentRegistry(myndlaproperties) {
      override implicit lazy val feideApiClient: FeideApiClient =
        mock[FeideApiClient](withSettings.strictness(Strictness.LENIENT))
      override implicit lazy val clock: Clock                       = mock[Clock](withSettings.strictness(Strictness.LENIENT))
      override implicit lazy val folderRepository: FolderRepository = spy(new FolderRepository)
      override implicit lazy val userRepository: UserRepository     = spy(new UserRepository)
      override implicit lazy val userService: UserService           = spy(new UserService)
      override implicit lazy val searchApiClient: SearchApiClient   = mock[SearchApiClient]
      override implicit lazy val nodebb: NodeBBClient               = mock[NodeBBClient]
      override implicit lazy val feideAuth: FeideAuth               = FeideAuthTest()

      when(clock.now()).thenReturn(NDLADate.of(2017, 1, 1, 1, 59))
      when(feideApiClient.getFeideGroups(any)).thenReturn(Success(Seq.empty))
      when(feideApiClient.getFeideExtendedUser(any)).thenReturn(
        Success(
          FeideExtendedUserInfo("", Seq("employee"), Some("employee"), "email@ndla.no", Some(Seq("email@ndla.no")))
        )
      )
      when(feideApiClient.getOrganization(any)).thenReturn(Success("zxc"))
      when(nodebb.getUserId(any)).thenReturn(Success(Some(1L)))
      when(nodebb.deleteUser(any, any)).thenReturn(Success(()))
    }
  }

  val testClock: Clock = myndlaApi.componentRegistry.clock

  val myndlaApiBaseUrl: String   = s"http://localhost:$myndlaApiPort"
  val myndlaApiFolderUrl: String = s"$myndlaApiBaseUrl/myndla-api/v1/folders"
  val myndlaApiUserUrl: String   = s"$myndlaApiBaseUrl/myndla-api/v1/users"

  override def beforeAll(): Unit = {
    super.beforeAll()
    implicit val ec: ExecutionContextExecutorService =
      ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
    Future {
      myndlaApi.run(Array.empty)
    }: Unit
    blockUntilHealthy(s"$myndlaApiBaseUrl/health/readiness")
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(myndlaApi.componentRegistry.folderRepository)
    reset(myndlaApi.componentRegistry.userRepository)

    implicit val session: DBSession = myndlaApi.componentRegistry.dbUtil.autoSession
    myndlaApi.componentRegistry.userRepository.deleteAllUsers

    val feideSessionDto  = FeideSessionDTO(FeideAuthTestData.FrankForeleser.accessToken)
    val feideSessionBody = CirceUtil.toJsonString(feideSessionDto)
    quickRequest
      .put(uri"$myndlaApiUserUrl/session")
      .header("FeideAuthorization", s"Bearer $feideIdToken")
      .contentType("application/json")
      .body(feideSessionBody)
      .send()
  }

  override def afterAll(): Unit = {
    super.afterAll()
  }

  def createFolder(name: String, parentId: Option[String]): api.FolderDTO = {
    import io.circe.generic.auto.*
    val newFolderData = api.NewFolderDTO(
      name = name,
      status = Some(FolderStatus.SHARED.toString),
      parentId = parentId,
      description = None,
    )
    val body = CirceUtil.toJsonString(newFolderData)

    val newFolder = quickRequest
      .post(uri"$myndlaApiFolderUrl/")
      .header("FeideAuthorization", s"Bearer $feideIdToken")
      .contentType("application/json")
      .body(body)
      .send()
    if (!newFolder.isSuccess)
      fail(s"Failed to create folder $name failed with code ${newFolder.code} and body:\n${newFolder.body}")

    CirceUtil.unsafeParseAs[api.FolderDTO](newFolder.body)
  }

  def getFolders(includeSubfolders: Boolean): api.UserFolderDTO = {
    import io.circe.generic.auto.*
    var uri = uri"$myndlaApiFolderUrl/"
    if (includeSubfolders) uri = uri.addParam("include-subfolders", "true")
    val folders = quickRequest.get(uri).header("FeideAuthorization", s"Bearer $feideIdToken").send()
    if (!folders.isSuccess)
      fail(s"Fetching all folders for $feideId failed with code ${folders.code} and body:\n${folders.body}")

    CirceUtil.unsafeParseAs[api.UserFolderDTO](folders.body)
  }

  def addRootResource(resource: api.NewResourceDTO): api.ResourceDTO = {
    import io.circe.generic.auto.*
    val body = CirceUtil.toJsonString(resource)

    val newResource = quickRequest
      .post(uri"$myndlaApiFolderUrl/resources/root")
      .header("FeideAuthorization", s"Bearer $feideIdToken")
      .contentType("application/json")
      .body(body)
      .send()
    if (!newResource.isSuccess)
      fail(s"Failed to create root resource, failed with code ${newResource.code} and body:\n${newResource.body}")

    CirceUtil.unsafeParseAs[api.ResourceDTO](newResource.body)

  }

  def addResourceToFolder(folderId: UUID, resource: api.NewResourceDTO): api.ResourceDTO = {
    import io.circe.generic.auto.*
    val body = CirceUtil.toJsonString(resource)

    val newResource = quickRequest
      .post(uri"$myndlaApiFolderUrl/$folderId/resources")
      .header("FeideAuthorization", s"Bearer $feideIdToken")
      .contentType("application/json")
      .body(body)
      .send()
    if (!newResource.isSuccess)
      fail(s"Failed to create resource, failed with code ${newResource.code} and body:\n${newResource.body}")

    CirceUtil.unsafeParseAs[api.ResourceDTO](newResource.body)
  }

  def getFolderResources(folderId: UUID): api.FolderDTO = {
    val resources = quickRequest
      .get(uri"$myndlaApiFolderUrl/$folderId?include-resources=true")
      .header("FeideAuthorization", s"Bearer $feideIdToken")
      .send()
    if (!resources.isSuccess)
      fail(s"Fetching all resources for $feideId failed with code ${resources.code} and body:\n${resources.body}")

    CirceUtil.unsafeParseAs[api.FolderDTO](resources.body)
  }

  def getRootResources: List[api.ResourceDTO] = {
    val resources = quickRequest
      .get(uri"$myndlaApiFolderUrl/resources/root")
      .header("FeideAuthorization", s"Bearer $feideIdToken")
      .send()
    if (!resources.isSuccess)
      fail(s"Fetching root resources for $feideId failed with code ${resources.code} and body:\n${resources.body}")

    CirceUtil.unsafeParseAs[List[api.ResourceDTO]](resources.body)
  }

  def deleteUser(): Unit = {
    val response = quickRequest
      .delete(uri"$myndlaApiUserUrl/delete-personal-data")
      .header("FeideAuthorization", s"Bearer $feideIdToken")
      .send()

    if (!response.isSuccess) fail(s"Deleting $feideId failed with code ${response.code} and body:\n${response.body}")
  }

  test("Creating and deleting user also deletes folders and resources") {
    val f1 = createFolder("folder1", None)
    addResourceToFolder(f1.id, api.NewResourceDTO(ResourceType.Article, "/article/1", None, "1"))
    addResourceToFolder(f1.id, api.NewResourceDTO(ResourceType.Article, "/article/2", None, "2"))
    val f2 = createFolder("folder2", None)

    addRootResource(api.NewResourceDTO(ResourceType.Topic, "/topic/3", None, "3"))

    val foldersForU1 = getFolders(false)
    foldersForU1.sharedFolders.length should be(0)
    foldersForU1.folders.length should be(2)
    foldersForU1.folders.head.id should be(f1.id)
    foldersForU1.folders.head.rank should be(1)
    foldersForU1.folders(1).id should be(f2.id)
    foldersForU1.folders(1).rank should be(2)
    val resourcesForF1 = getFolderResources(f1.id)
    resourcesForF1.resources.length should be(2)
    val rootResourcesForU1 = getRootResources
    rootResourcesForU1.length should be(1)

    deleteUser()

    val foldersForU1again = getFolders(false)
    foldersForU1again.sharedFolders.length should be(0)
    foldersForU1again.folders.length should be(0)
    val rootResourcesForU1again = getRootResources
    rootResourcesForU1again.length should be(0)

  }
}
