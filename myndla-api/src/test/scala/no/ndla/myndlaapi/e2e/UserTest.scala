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
import no.ndla.myndlaapi.model.api.FolderDTO
import no.ndla.myndlaapi.repository.{FolderRepository, UserRepository}
import no.ndla.myndlaapi.service.UserService
import no.ndla.myndlaapi.{ComponentRegistry, MainClass, MyNdlaApiProperties, TestEnvironment, UnitSuite}
import no.ndla.network.clients.{FeideApiClient, FeideExtendedUserInfo}
import no.ndla.scalatestsuite.{DatabaseIntegrationSuite, RedisIntegrationSuite}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.mockito.quality.Strictness
import org.testcontainers.postgresql.PostgreSQLContainer
import scalikejdbc.DBSession
import sttp.client3.quick.*

import java.util.UUID
import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.Success

class UserTest extends DatabaseIntegrationSuite with RedisIntegrationSuite with UnitSuite with TestEnvironment {

  val myndlaApiPort: Int                    = findFreePort
  val pgc: PostgreSQLContainer              = postgresContainer.get
  val redisPort: Int                        = redisContainer.get.port
  val myndlaproperties: MyNdlaApiProperties = new MyNdlaApiProperties {
    override def ApplicationPort: Int = myndlaApiPort

    override val MetaServer: Prop[String]   = propFromTestValue("META_SERVER", pgc.getHost)
    override val MetaResource: Prop[String] = propFromTestValue("META_RESOURCE", pgc.getDatabaseName)
    override val MetaUserName: Prop[String] = propFromTestValue("META_USER_NAME", pgc.getUsername)
    override val MetaPassword: Prop[String] = propFromTestValue("META_PASSWORD", pgc.getPassword)
    override val MetaPort: Prop[Int]        = propFromTestValue("META_PORT", pgc.getMappedPort(5432))
    override val MetaSchema: Prop[String]   = propFromTestValue("META_SCHEMA", schemaName)

    override def RedisHost: String = "localhost"
    override def RedisPort: Int    = redisPort
  }

  val feideId = "feide"

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

      when(clock.now()).thenReturn(NDLADate.of(2017, 1, 1, 1, 59))
      when(feideApiClient.getFeideID(any)).thenReturn(Success("feideid"))
      when(feideApiClient.getFeideAccessTokenOrFail(any)).thenReturn(Success("notimportante"))
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
  }

  override def afterAll(): Unit = {
    super.afterAll()
  }

  def createFolder(feideId: String, name: String, parentId: Option[String]): api.FolderDTO = {
    import io.circe.generic.auto.*
    val newFolderData = api.NewFolderDTO(
      name = name,
      status = Some(FolderStatus.SHARED.toString),
      parentId = parentId,
      description = None,
    )
    val body = CirceUtil.toJsonString(newFolderData)

    val newFolder = simpleHttpClient.send(
      quickRequest
        .post(uri"$myndlaApiFolderUrl/")
        .header("FeideAuthorization", s"Bearer $feideId")
        .contentType("application/json")
        .body(body)
    )
    if (!newFolder.isSuccess)
      fail(s"Failed to create folder $name failed with code ${newFolder.code} and body:\n${newFolder.body}")

    CirceUtil.unsafeParseAs[api.FolderDTO](newFolder.body)
  }

  def getFolders(feideId: String, includeSubfolders: Boolean): api.UserFolderDTO = {
    import io.circe.generic.auto.*
    var uri = uri"$myndlaApiFolderUrl/"
    if (includeSubfolders) uri = uri.addParam("include-subfolders", "true")
    val folders = simpleHttpClient.send(quickRequest.get(uri).header("FeideAuthorization", s"Bearer $feideId"))
    if (!folders.isSuccess)
      fail(s"Fetching all folders for $feideId failed with code ${folders.code} and body:\n${folders.body}")

    CirceUtil.unsafeParseAs[api.UserFolderDTO](folders.body)
  }

  def addRootResource(feideId: String, resource: api.NewResourceDTO): api.ResourceDTO = {
    import io.circe.generic.auto.*
    val body = CirceUtil.toJsonString(resource)

    val newResource = simpleHttpClient.send(
      quickRequest
        .post(uri"$myndlaApiFolderUrl/resources/root")
        .header("FeideAuthorization", s"Bearer $feideId")
        .contentType("application/json")
        .body(body)
    )
    if (!newResource.isSuccess)
      fail(s"Failed to create root resource, failed with code ${newResource.code} and body:\n${newResource.body}")

    CirceUtil.unsafeParseAs[api.ResourceDTO](newResource.body)

  }

  def addResourceToFolder(feideId: String, folderId: UUID, resource: api.NewResourceDTO): api.ResourceDTO = {
    import io.circe.generic.auto.*
    val body = CirceUtil.toJsonString(resource)

    val newResource = simpleHttpClient.send(
      quickRequest
        .post(uri"$myndlaApiFolderUrl/$folderId/resources")
        .header("FeideAuthorization", s"Bearer $feideId")
        .contentType("application/json")
        .body(body)
    )
    if (!newResource.isSuccess)
      fail(s"Failed to create resource, failed with code ${newResource.code} and body:\n${newResource.body}")

    CirceUtil.unsafeParseAs[api.ResourceDTO](newResource.body)
  }

  def getFolderResources(feideId: String, folderId: UUID): api.FolderDTO = {
    val resources = simpleHttpClient.send(
      quickRequest
        .get(uri"$myndlaApiFolderUrl/$folderId?include-resources=true")
        .header("FeideAuthorization", s"Bearer $feideId")
    )
    if (!resources.isSuccess)
      fail(s"Fetching all resources for $feideId failed with code ${resources.code} and body:\n${resources.body}")

    CirceUtil.unsafeParseAs[api.FolderDTO](resources.body)
  }

  def getRootResources(feideId: String): List[api.ResourceDTO] = {
    val resources = simpleHttpClient.send(
      quickRequest.get(uri"$myndlaApiFolderUrl/resources/root").header("FeideAuthorization", s"Bearer $feideId")
    )
    if (!resources.isSuccess)
      fail(s"Fetching root resources for $feideId failed with code ${resources.code} and body:\n${resources.body}")

    CirceUtil.unsafeParseAs[List[api.ResourceDTO]](resources.body)
  }

  def deleteUser(feideId: String): Unit = {
    val response = simpleHttpClient.send(
      quickRequest.delete(uri"$myndlaApiUserUrl/delete-personal-data").header("FeideAuthorization", s"Bearer $feideId")
    )

    if (!response.isSuccess) fail(s"Deleting $feideId failed with code ${response.code} and body:\n${response.body}")
  }

  test("Creating and deleting user also deletes folders and resources") {
    val f1 = createFolder(feideId, "folder1", None)
    addResourceToFolder(feideId, f1.id, api.NewResourceDTO(ResourceType.Article, "/article/1", None, "1"))
    addResourceToFolder(feideId, f1.id, api.NewResourceDTO(ResourceType.Article, "/article/2", None, "2"))
    val f2 = createFolder(feideId, "folder2", None)

    addRootResource(feideId, api.NewResourceDTO(ResourceType.Topic, "/topic/3", None, "3"))

    val foldersForU1 = getFolders(feideId, false)
    foldersForU1.sharedFolders.length should be(0)
    foldersForU1.folders.length should be(2)
    foldersForU1.folders.head.id should be(f1.id)
    foldersForU1.folders.head.rank should be(1)
    foldersForU1.folders(1).id should be(f2.id)
    foldersForU1.folders(1).rank should be(2)
    val resourcesForF1 = getFolderResources(feideId, f1.id)
    resourcesForF1.resources.length should be(2)
    val rootResourcesForU1 = getRootResources(feideId)
    rootResourcesForU1.length should be(1)

    deleteUser(feideId)

    val foldersForU1again = getFolders(feideId, false)
    foldersForU1again.sharedFolders.length should be(0)
    foldersForU1again.folders.length should be(0)
    val rootResourcesForU1again = getRootResources(feideId)
    rootResourcesForU1again.length should be(0)

  }
}
