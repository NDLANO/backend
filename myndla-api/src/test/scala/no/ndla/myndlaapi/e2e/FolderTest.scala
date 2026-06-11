/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.e2e

import no.ndla.common.configuration.Prop
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.UpdateWith
import no.ndla.common.model.api.{NullValue, Value}
import no.ndla.common.model.domain.ResourceType.Article
import no.ndla.common.model.domain.myndla.FolderStatus
import no.ndla.common.{CirceUtil, Clock}
import no.ndla.myndlaapi.model.api
import no.ndla.myndlaapi.model.api.{FeideAccessTokenDTO, FolderDTO, MoveResourceDTO}
import no.ndla.myndlaapi.repository.{FolderRepository, UserRepository}
import no.ndla.myndlaapi.service.UserService
import no.ndla.myndlaapi.{ComponentRegistry, MainClass, MyNdlaApiProperties, TestEnvironment, UnitSuite}
import no.ndla.network.clients.{FeideApiClient, FeideExtendedUserInfo}
import no.ndla.scalatestsuite.{DatabaseIntegrationSuite, RedisIntegrationSuite}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{doNothing, reset, spy, when, withSettings}
import org.mockito.quality.Strictness
import scalikejdbc.DBSession
import sttp.client4.quick.*

import java.util.UUID
import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.Success
import no.ndla.myndlaapi.integration.SearchApiClient
import no.ndla.network.model.FeideUserWrapper
import no.ndla.network.tapir.auth.FeideAuth
import no.ndla.tapirtesting.{FeideAuthTest, FeideAuthTestData}

class FolderTest extends DatabaseIntegrationSuite with RedisIntegrationSuite with UnitSuite with TestEnvironment {

  val myndlaApiPort: Int                    = findFreePort
  val pgc: PgConnectionInfo                 = pgConnectionInfo.get
  val myndlaproperties: MyNdlaApiProperties = new MyNdlaApiProperties {
    override def ApplicationPort: Int = myndlaApiPort

    override val MetaServer: Prop[String]   = propFromTestValue("META_SERVER", pgc.host)
    override val MetaResource: Prop[String] = propFromTestValue("META_RESOURCE", pgc.databaseName)
    override val MetaUserName: Prop[String] = propFromTestValue("META_USER_NAME", pgc.username)
    override val MetaPassword: Prop[String] = propFromTestValue("META_PASSWORD", pgc.password)
    override val MetaPort: Prop[Int]        = propFromTestValue("META_PORT", pgc.port)
    override val MetaSchema: Prop[String]   = propFromTestValue("META_SCHEMA", schemaName)

    override def RedisHost: String = "localhost"

    override def RedisPort: Int = redisPort.get
  }

  val feide1: FeideUserWrapper = FeideAuthTestData.FrankForeleser
  val feide2: FeideUserWrapper = FeideAuthTestData.AnneLaerer

  val myndlaApi: MainClass = new MainClass(myndlaproperties) {
    override val componentRegistry: ComponentRegistry = new ComponentRegistry(myndlaproperties) {
      override implicit lazy val feideApiClient: FeideApiClient =
        mock[FeideApiClient](withSettings.strictness(Strictness.LENIENT))
      override implicit lazy val clock: Clock                       = mock[Clock](withSettings.strictness(Strictness.LENIENT))
      override implicit lazy val folderRepository: FolderRepository = spy(new FolderRepository)
      override implicit lazy val userRepository: UserRepository     = spy(new UserRepository)
      override implicit lazy val userService: UserService           = spy(new UserService)
      override implicit lazy val searchApiClient: SearchApiClient   = mock[SearchApiClient]
      override implicit lazy val feideAuth: FeideAuth               = FeideAuthTest()

      when(clock.now()).thenReturn(NDLADate.of(2017, 1, 1, 1, 59))
      when(feideApiClient.getFeideGroupsAndOrganization(any)).thenReturn(Success((Seq.empty, "zxc")))
      when(feideApiClient.getFeideExtendedUser(any)).thenReturn(
        Success(
          FeideExtendedUserInfo("", Seq("employee"), Some("employee"), "email@ndla.no", Some(Seq("email@ndla.no")))
        )
      )
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

    Seq(feide1, feide2)
      .zipWithIndex
      .foreach { (feide, i) =>
        val body = CirceUtil.toJsonString(FeideAccessTokenDTO(s"access-token-$i"))
        quickRequest
          .put(uri"$myndlaApiUserUrl")
          .header("FeideAuthorization", s"Bearer ${feide.idToken.originalToken}")
          .contentType("application/json")
          .body(body)
          .send()
      }
  }

  override def afterAll(): Unit = {
    super.afterAll()
  }

  // id is autogenerated in database, so we need to replace it to something constant in order to compare objects
  def replaceIdRecursively(folder: FolderDTO, newId: UUID): FolderDTO = {
    val updatedId          = newId
    val updatedParentId    = folder.parentId.map(_ => newId)
    val updatedBreadcrumbs = folder.breadcrumbs.map(_.copy(id = newId))
    val updatedResources   = folder.resources.map(_.copy(id = newId))
    val updatedSubfolders  = folder
      .subfolders
      .map { case child: FolderDTO =>
        replaceIdRecursively(child, newId)
      }

    folder.copy(
      id = updatedId,
      parentId = updatedParentId,
      subfolders = updatedSubfolders,
      resources = updatedResources,
      breadcrumbs = updatedBreadcrumbs,
    )
  }

  def createFolder(feide: FeideUserWrapper, name: String, parentId: Option[String]): api.FolderDTO = {
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
      .header("FeideAuthorization", s"Bearer ${feide.idToken.originalToken}")
      .contentType("application/json")
      .body(body)
      .send()
    if (!newFolder.isSuccess)
      fail(s"Failed to create folder $name failed with code ${newFolder.code} and body:\n${newFolder.body}")

    CirceUtil.unsafeParseAs[api.FolderDTO](newFolder.body)
  }

  def updateFolder(feide: FeideUserWrapper, folderId: UUID, updatedFolder: api.UpdatedFolderDTO): api.FolderDTO = {
    import io.circe.generic.auto.*
    val body = CirceUtil.toJsonString(updatedFolder)

    val updatedFolderResponse = quickRequest
      .patch(uri"$myndlaApiFolderUrl/$folderId")
      .header("FeideAuthorization", s"Bearer ${feide.idToken.originalToken}")
      .contentType("application/json")
      .body(body)
      .send()
    if (!updatedFolderResponse.isSuccess) fail(
      s"Failed to update folder $folderId failed with code ${updatedFolderResponse.code} and body:\n${updatedFolderResponse.body}"
    )

    CirceUtil.unsafeParseAs[api.FolderDTO](updatedFolderResponse.body)
  }

  def getFolders(feide: FeideUserWrapper, includeSubfolders: Boolean): api.UserFolderDTO = {
    import io.circe.generic.auto.*
    var uri = uri"$myndlaApiFolderUrl/"
    if (includeSubfolders) uri = uri.addParam("include-subfolders", "true")
    val folders = quickRequest.get(uri).header("FeideAuthorization", s"Bearer ${feide.idToken.originalToken}").send()
    if (!folders.isSuccess)
      fail(s"Fetching all folders for ${feide.idToken.sub} failed with code ${folders.code} and body:\n${folders.body}")

    CirceUtil.unsafeParseAs[api.UserFolderDTO](folders.body)
  }

  def saveFolder(feide: FeideUserWrapper, folderId: UUID): Unit = {
    val savedResponse = quickRequest
      .post(uri"$myndlaApiFolderUrl/shared/$folderId/save")
      .header("FeideAuthorization", s"Bearer ${feide.idToken.originalToken}")
      .send()

    if (!savedResponse.isSuccess) fail(
      s"Saving folder $folderId for ${feide.idToken.sub} failed with code ${savedResponse.code} and body:\n${savedResponse.body}"
    )
  }

  def sortFolders(feide: FeideUserWrapper, idsInOrder: List[UUID], sortShared: Boolean = false): Unit = {
    import io.circe.generic.auto.*
    val sortData = api.FolderSortRequestDTO(sortedIds = idsInOrder)
    val body     = CirceUtil.toJsonString(sortData)

    val requestUrl =
      if (sortShared) uri"$myndlaApiFolderUrl/sort-saved"
      else uri"$myndlaApiFolderUrl/sort-subfolders"

    val sortedFolders = quickRequest
      .put(requestUrl)
      .header("FeideAuthorization", s"Bearer ${feide.idToken.originalToken}")
      .contentType("application/json")
      .body(body)
      .send()
    if (!sortedFolders.isSuccess) fail(
      s"Sorting folders for ${feide.idToken.sub} failed with code ${sortedFolders.code} and body:\n${sortedFolders.body}"
    )
  }

  def addRootResource(feide: FeideUserWrapper, resource: api.NewResourceDTO): api.ResourceDTO = {
    import io.circe.generic.auto.*
    val body = CirceUtil.toJsonString(resource)

    val newResource = quickRequest
      .post(uri"$myndlaApiFolderUrl/resources/root")
      .header("FeideAuthorization", s"Bearer ${feide.idToken.originalToken}")
      .contentType("application/json")
      .body(body)
      .send()
    if (!newResource.isSuccess)
      fail(s"Failed to create root resource, failed with code ${newResource.code} and body:\n${newResource.body}")

    CirceUtil.unsafeParseAs[api.ResourceDTO](newResource.body)

  }

  def addResourceToFolder(feide: FeideUserWrapper, folderId: UUID, resource: api.NewResourceDTO): api.ResourceDTO = {
    import io.circe.generic.auto.*
    val body = CirceUtil.toJsonString(resource)

    val newResource = quickRequest
      .post(uri"$myndlaApiFolderUrl/$folderId/resources")
      .header("FeideAuthorization", s"Bearer ${feide.idToken.originalToken}")
      .contentType("application/json")
      .body(body)
      .send()
    if (!newResource.isSuccess)
      fail(s"Failed to create resource, failed with code ${newResource.code} and body:\n${newResource.body}")

    CirceUtil.unsafeParseAs[api.ResourceDTO](newResource.body)
  }

  def getFolderResources(feide: FeideUserWrapper, folderId: UUID): api.FolderDTO = {
    val resources = quickRequest
      .get(uri"$myndlaApiFolderUrl/$folderId?include-resources=true")
      .header("FeideAuthorization", s"Bearer ${feide.idToken.originalToken}")
      .send()
    if (!resources.isSuccess) fail(
      s"Fetching all resources for ${feide.idToken.sub} failed with code ${resources.code} and body:\n${resources.body}"
    )

    CirceUtil.unsafeParseAs[api.FolderDTO](resources.body)
  }

  def getRootResources(feide: FeideUserWrapper): List[api.ResourceDTO] = {
    val resources = quickRequest
      .get(uri"$myndlaApiFolderUrl/resources/root")
      .header("FeideAuthorization", s"Bearer ${feide.idToken.originalToken}")
      .send()
    if (!resources.isSuccess) fail(
      s"Fetching root resources for ${feide.idToken.sub} failed with code ${resources.code} and body:\n${resources.body}"
    )

    CirceUtil.unsafeParseAs[List[api.ResourceDTO]](resources.body)
  }

  def sortResourcesInFolder(feide: FeideUserWrapper, folderId: UUID, resourceIdsInOrder: List[UUID]): Unit = {
    import io.circe.generic.auto.*
    val sortData = api.FolderSortRequestDTO(sortedIds = resourceIdsInOrder)
    val body     = CirceUtil.toJsonString(sortData)

    val sortedResources = quickRequest
      .put(uri"$myndlaApiFolderUrl/sort-resources/$folderId")
      .header("FeideAuthorization", s"Bearer ${feide.idToken.originalToken}")
      .contentType("application/json")
      .body(body)
      .send()
    if (!sortedResources.isSuccess) fail(
      s"Sorting resources in folder $folderId for ${feide.idToken.sub} failed with code ${sortedResources.code} and body:\n${sortedResources.body}"
    )
  }

  def sortRootResources(feide: FeideUserWrapper, resourceIdsInOrder: List[UUID]): Unit = {
    import io.circe.generic.auto.*
    val sortData = api.FolderSortRequestDTO(sortedIds = resourceIdsInOrder)
    val body     = CirceUtil.toJsonString(sortData)

    val sortedResources = quickRequest
      .put(uri"$myndlaApiFolderUrl/sort-resources/root")
      .header("FeideAuthorization", s"Bearer ${feide.idToken.originalToken}")
      .contentType("application/json")
      .body(body)
      .send()
    if (!sortedResources.isSuccess) fail(
      s"Sorting root resources for ${feide.idToken.sub} failed with code ${sortedResources.code} and body:\n${sortedResources.body}"
    )
  }

  def moveResource(feide: FeideUserWrapper, dto: MoveResourceDTO, failOnError: Boolean = true): Int = {
    import io.circe.generic.auto.*
    val body     = CirceUtil.toJsonString(dto)
    val response = quickRequest
      .put(uri"$myndlaApiFolderUrl/resources/move")
      .header("FeideAuthorization", s"Bearer ${feide.idToken.originalToken}")
      .contentType("application/json")
      .body(body)
      .send()
    if (failOnError && !response.isSuccess) fail(
      s"Moving resource ${dto.resourceId} from folder ${dto.fromFolderId} to folder ${dto.toFolderId} failed with code ${response.code} and body:\n${response.body}"
    )
    response.code.code
  }

  def deleteResourceFromFolder(feide: FeideUserWrapper, folderId: UUID, resourceId: UUID): Unit = {
    val response = quickRequest
      .delete(uri"$myndlaApiFolderUrl/$folderId/resources/$resourceId")
      .header("FeideAuthorization", s"Bearer ${feide.idToken.originalToken}")
      .send()
    if (!response.isSuccess) fail(
      s"Deleting resource $resourceId from folder $folderId for ${feide.idToken.sub} failed with code ${response.code} and body:\n${response.body}"
    )
  }

  def deleteRootResource(feide: FeideUserWrapper, resourceId: UUID): Unit = {
    val response = quickRequest
      .delete(uri"$myndlaApiFolderUrl/resources/root/$resourceId")
      .header("FeideAuthorization", s"Bearer ${feide.idToken.originalToken}")
      .send()

    if (!response.isSuccess) fail(
      s"Deleting root resource $resourceId for ${feide.idToken.sub} failed with code ${response.code} and body:\n${response.body}"
    )
  }

  test("Inserting and sorting folders") {
    when(feideApiClient.getFeideGroupsAndOrganization(any)).thenReturn(Success((Seq.empty, "zxc")))

    val f1 = createFolder(feide1, "folder1", None)
    val f2 = createFolder(feide1, "folder2", None)
    val f3 = createFolder(feide1, "folder3", None)
    val f4 = createFolder(feide1, "folder4", None)

    val foldersForU1 = getFolders(feide1, false)
    foldersForU1.sharedFolders.length should be(0)
    foldersForU1.folders.length should be(4)
    foldersForU1.folders.head.id should be(f1.id)
    foldersForU1.folders.head.rank should be(1)
    foldersForU1.folders(1).id should be(f2.id)
    foldersForU1.folders(1).rank should be(2)
    foldersForU1.folders(2).id should be(f3.id)
    foldersForU1.folders(2).rank should be(3)
    foldersForU1.folders(3).id should be(f4.id)
    foldersForU1.folders(3).rank should be(4)

    sortFolders(feide1, List(f4.id, f2.id, f3.id, f1.id))

    val foldersForU1Sorted = getFolders(feide1, false)
    foldersForU1Sorted.sharedFolders.length should be(0)
    foldersForU1Sorted.folders.length should be(4)
    foldersForU1Sorted.folders.head.id should be(f4.id)
    foldersForU1Sorted.folders.head.rank should be(1)
    foldersForU1Sorted.folders(1).id should be(f2.id)
    foldersForU1Sorted.folders(1).rank should be(2)
    foldersForU1Sorted.folders(2).id should be(f3.id)
    foldersForU1Sorted.folders(2).rank should be(3)
    foldersForU1Sorted.folders(3).id should be(f1.id)
    foldersForU1Sorted.folders(3).rank should be(4)
  }

  test("Saving and sorting shared folders") {
    when(feideApiClient.getFeideGroupsAndOrganization(any)).thenReturn(Success((Seq.empty, "zxc")))
    when(myndlaApi.componentRegistry.feideApiClient.getFeideExtendedUser(any)).thenReturn(
      Success(FeideExtendedUserInfo("", Seq("employee"), Some("employee"), "email@ndla.no", Some(Seq("email@ndla.no"))))
    )

    val f1 = createFolder(feide1, "folder1", None)
    val f2 = createFolder(feide1, "folder2", None)
    val f3 = createFolder(feide1, "folder3", None)
    val f4 = createFolder(feide1, "folder4", None)

    val foldersForU1 = getFolders(feide1, false)
    foldersForU1.sharedFolders.length should be(0)
    foldersForU1.folders.length should be(4)
    foldersForU1.folders.head.id should be(f1.id)
    foldersForU1.folders.head.rank should be(1)
    foldersForU1.folders(1).id should be(f2.id)
    foldersForU1.folders(1).rank should be(2)
    foldersForU1.folders(2).id should be(f3.id)
    foldersForU1.folders(2).rank should be(3)
    foldersForU1.folders(3).id should be(f4.id)
    foldersForU1.folders(3).rank should be(4)

    sortFolders(feide1, List(f4.id, f2.id, f3.id, f1.id))

    val foldersForU1Sorted = getFolders(feide1, false)
    foldersForU1Sorted.sharedFolders.length should be(0)
    foldersForU1Sorted.folders.length should be(4)
    foldersForU1Sorted.folders.head.id should be(f4.id)
    foldersForU1Sorted.folders.head.rank should be(1)
    foldersForU1Sorted.folders(1).id should be(f2.id)
    foldersForU1Sorted.folders(1).rank should be(2)
    foldersForU1Sorted.folders(2).id should be(f3.id)
    foldersForU1Sorted.folders(2).rank should be(3)
    foldersForU1Sorted.folders(3).id should be(f1.id)
    foldersForU1Sorted.folders(3).rank should be(4)

    val foldersForU2 = getFolders(feide2, false)
    foldersForU2.sharedFolders.length should be(0)

    saveFolder(feide2, f1.id)
    saveFolder(feide2, f3.id)

    val foldersForU2AfterSave = getFolders(feide2, false)
    foldersForU2AfterSave.sharedFolders.length should be(2)
    foldersForU2AfterSave.sharedFolders.head.id should be(f1.id)
    foldersForU2AfterSave.sharedFolders.head.rank should be(1)
    foldersForU2AfterSave.sharedFolders(1).id should be(f3.id)
    foldersForU2AfterSave.sharedFolders(1).rank should be(2)

    sortFolders(feide2, List(f3.id, f1.id), sortShared = true)

    val foldersForU2AfterSort = getFolders(feide2, false)
    foldersForU2AfterSort.sharedFolders.length should be(2)
    foldersForU2AfterSort.sharedFolders.head.id should be(f3.id)
    foldersForU2AfterSort.sharedFolders.head.rank should be(1)
    foldersForU2AfterSort.sharedFolders(1).id should be(f1.id)
    foldersForU2AfterSort.sharedFolders(1).rank should be(2)
  }

  test("Inserting and sorting root resources") {
    when(feideApiClient.getFeideGroupsAndOrganization(any)).thenReturn(Success((Seq.empty, "zxc")))
    doNothing().when(searchApiClient).reindexDraft(any);

    val rootResourcesU = getRootResources(feide1)
    rootResourcesU.length should be(0)

    val res1 = addRootResource(
      feide = feide1,
      resource = api.NewResourceDTO(resourceType = Article, path = "/path/to/1", tags = None, resourceId = "1"),
    )

    val res2 = addRootResource(
      feide = feide1,
      resource = api.NewResourceDTO(resourceType = Article, path = "/path/to/2", tags = None, resourceId = "2"),
    )
    //
    val res3 = addRootResource(
      feide = feide1,
      resource = api.NewResourceDTO(resourceType = Article, path = "/path/to/3", tags = None, resourceId = "3"),
    )

    val res4 = addRootResource(
      feide = feide1,
      resource = api.NewResourceDTO(resourceType = Article, path = "/path/to/4", tags = None, resourceId = "4"),
    )

    val rootResources = getRootResources(feide1)
    rootResources.length should be(4)
    rootResources.head.id should be(res1.id)
    rootResources.map(_.id) should be(List(res1.id, res2.id, res3.id, res4.id))

    deleteRootResource(feide1, res2.id)

    val rootResourcesAfterDelete = getRootResources(feide1)

    rootResourcesAfterDelete.length should be(3)
    rootResourcesAfterDelete.map(_.id) should be(List(res1.id, res3.id, res4.id))
    rootResourcesAfterDelete.flatMap(_.rank) should be(List(1, 2, 3))

    sortRootResources(feide1, List(res4.id, res1.id, res3.id))

    val rootResourcesAfterSort = getRootResources(feide1)
    rootResourcesAfterSort.length should be(3)
    rootResourcesAfterSort.map(_.id) should be(List(res4.id, res1.id, res3.id))
  }

  test("Saving, sorting and deleting resources in a folder") {
    // This test fails if you remove the order by clause in fetching resources in FolderRepository
    when(feideApiClient.getFeideGroupsAndOrganization(any)).thenReturn(Success((Seq.empty, "zxc")))

    val f1 = createFolder(feide1, "folder1", None)

    val foldersForU1 = getFolders(feide1, false)
    foldersForU1.sharedFolders.length should be(0)
    foldersForU1.folders.length should be(1)
    foldersForU1.folders.head.id should be(f1.id)
    foldersForU1.folders.head.rank should be(1)
    foldersForU1.folders.head.resources.length should be(0)

    val res1 = addResourceToFolder(
      feide = feide1,
      folderId = f1.id,
      resource = api.NewResourceDTO(resourceType = Article, path = "/path/to/1", tags = None, resourceId = "1"),
    )
    val res2 = addResourceToFolder(
      feide = feide1,
      folderId = f1.id,
      resource = api.NewResourceDTO(resourceType = Article, path = "/path/to/2", tags = None, resourceId = "2"),
    )
    val res3 = addResourceToFolder(
      feide = feide1,
      folderId = f1.id,
      resource = api.NewResourceDTO(resourceType = Article, path = "/path/to/3", tags = None, resourceId = "3"),
    )
    val res4 = addResourceToFolder(
      feide = feide1,
      folderId = f1.id,
      resource = api.NewResourceDTO(resourceType = Article, path = "/path/to/4", tags = None, resourceId = "4"),
    )
    val res5 = addResourceToFolder(
      feide = feide1,
      folderId = f1.id,
      resource = api.NewResourceDTO(resourceType = Article, path = "/path/to/5", tags = None, resourceId = "5"),
    )

    val folderAfterAdd = getFolderResources(feide1, f1.id)
    folderAfterAdd.resources.length should be(5)
    folderAfterAdd.resources.map(_.id) should be(List(res1.id, res2.id, res3.id, res4.id, res5.id))

    sortResourcesInFolder(feide1, f1.id, List(res3.id, res1.id, res2.id, res5.id, res4.id))
    sortResourcesInFolder(feide1, f1.id, List(res2.id, res4.id, res3.id, res1.id, res5.id))
    sortResourcesInFolder(feide1, f1.id, List(res5.id, res3.id, res2.id, res4.id, res1.id))
    sortResourcesInFolder(feide1, f1.id, List(res3.id, res1.id, res2.id, res5.id, res4.id))

    val folderAfterSort = getFolderResources(feide1, f1.id)
    folderAfterSort.resources.length should be(5)
    folderAfterSort.resources.map(_.id) should be(List(res3.id, res1.id, res2.id, res5.id, res4.id))

    deleteResourceFromFolder(feide1, f1.id, res1.id)

    val folderAfterDelete = getFolderResources(feide1, f1.id)
    folderAfterDelete.resources.length should be(4)
    folderAfterDelete.resources.map(_.id) should be(List(res3.id, res2.id, res5.id, res4.id))

  }

  test("Saving and then moving folder to different parent") {
    when(feideApiClient.getFeideGroupsAndOrganization(any)).thenReturn(Success((Seq.empty, "zxc")))

    /*
        f1
        ├─ f2
        ├─ f3
            └─ f4
     */
    val f1 = createFolder(feide1, "folder1", None)
    val f2 = createFolder(feide1, "folder2", Some(f1.id.toString))
    val f3 = createFolder(feide1, "folder3", Some(f1.id.toString))
    val f4 = createFolder(feide1, "folder4", Some(f3.id.toString))

    // Move f4 to be child of f1
    val updated = updateFolder(
      feide = feide1,
      folderId = f4.id,
      updatedFolder =
        api.UpdatedFolderDTO(parentId = UpdateWith(f1.id.toString), name = None, status = None, description = None),
    )
    updated.parentId should be(Some(f1.id))

    val foldersForU1Sorted = getFolders(feide1, true)
    foldersForU1Sorted.sharedFolders.length should be(0)
    foldersForU1Sorted.folders.length should be(1)
    foldersForU1Sorted.folders.head.id should be(f1.id)
    foldersForU1Sorted.folders.head.rank should be(1)
    foldersForU1Sorted.folders.head.subfolders.length should be(3)
    val subfoldersOfF1 = foldersForU1Sorted.folders.head.subfolders.sortBy(_.rank)
    subfoldersOfF1.head.id should be(f2.id)
    subfoldersOfF1.head.rank should be(1)
    subfoldersOfF1(1).id should be(f4.id) // rank is reset to 0 when moving
    subfoldersOfF1(1).rank should be(2)
    subfoldersOfF1(2).id should be(f3.id)
    subfoldersOfF1(2).rank should be(3)

  }

  test("moving resources to and from different folders work as intended") {
    when(feideApiClient.getFeideGroupsAndOrganization(any)).thenReturn(Success((Seq.empty, "zxc")))

    /*
        f1
        ├─ f2
        ├─ f3
            └─ f4
     */
    val f1 = createFolder(feide1, "folder1", None)
    val f2 = createFolder(feide1, "folder2", Some(f1.id.toString))
    val f3 = createFolder(feide1, "folder3", Some(f1.id.toString))
    val f4 = createFolder(feide1, "folder4", Some(f3.id.toString))

    val folderResourceDto =
      api.NewResourceDTO(resourceType = Article, path = "path/to/1", tags = None, resourceId = "1")

    val folderResourceDto2 =
      api.NewResourceDTO(resourceType = Article, path = "path/to/2", tags = None, resourceId = "2")

    val rr1 = addRootResource(feide = feide1, resource = folderResourceDto)
    val rr2 = addRootResource(feide = feide1, resource = folderResourceDto2)

    val fr1 = addResourceToFolder(feide = feide1, folderId = f1.id, resource = folderResourceDto)

    val fr2 = addResourceToFolder(feide = feide1, folderId = f4.id, resource = folderResourceDto)

    doNothing().when(searchApiClient).reindexDraft(any)

    // Initial state: root has rr1 (rank 1), rr2 (rank 2)
    // fr1.id == rr1.id == fr2.id (same underlying resource, path "path/to/1")
    val rootResourcesInitial = getRootResources(feide1)
    rootResourcesInitial.length should be(2)
    rootResourcesInitial.map(_.id) should be(List(rr1.id, rr2.id))

    val f1InitialResources = getFolderResources(feide1, f1.id)
    f1InitialResources.resources.length should be(1)
    f1InitialResources.resources.head.id should be(fr1.id)

    val f4InitialResources = getFolderResources(feide1, f4.id)
    f4InitialResources.resources.length should be(1)
    f4InitialResources.resources.head.id should be(fr2.id)

    // Add a second resource to f2 so we can verify rank is computed correctly when moving to a non-empty folder
    val folderResourceDto3 =
      api.NewResourceDTO(resourceType = Article, path = "path/to/3", tags = None, resourceId = "3")
    val fr3 = addResourceToFolder(feide = feide1, folderId = f2.id, resource = folderResourceDto3)

    // Move fr1 from f1 to f2 (folder-to-folder, target folder is non-empty)
    moveResource(
      feide1,
      MoveResourceDTO(fromFolderId = Value(f1.id), toFolderId = Value(f2.id), resourceId = fr1.id),
    ) should be(204)

    val f1AfterFolderMove = getFolderResources(feide1, f1.id)
    f1AfterFolderMove.resources.length should be(0)

    val f2AfterFolderMove = getFolderResources(feide1, f2.id)
    f2AfterFolderMove.resources.length should be(2)
    f2AfterFolderMove.resources.map(_.id) should be(List(fr3.id, fr1.id))
    f2AfterFolderMove.resources.flatMap(_.rank) should be(List(1, 2))

    // Root should be unaffected by the folder-to-folder move
    val rootAfterFolderMove = getRootResources(feide1)
    rootAfterFolderMove.length should be(2)
    rootAfterFolderMove.map(_.id) should be(List(rr1.id, rr2.id))

    // Move rr1 from root to f3 (root-to-folder)
    moveResource(
      feide1,
      MoveResourceDTO(fromFolderId = NullValue, toFolderId = Value(f3.id), resourceId = rr1.id),
    ) should be(204)

    val rootAfterRootToFolderMove = getRootResources(feide1)
    rootAfterRootToFolderMove.length should be(1)
    rootAfterRootToFolderMove.map(_.id) should be(List(rr2.id))
    rootAfterRootToFolderMove.flatMap(_.rank) should be(List(1))

    val f3AfterMove = getFolderResources(feide1, f3.id)
    f3AfterMove.resources.length should be(1)
    f3AfterMove.resources.head.id should be(rr1.id)
    f3AfterMove.resources.head.rank should be(Some(1))

    // Move fr2 from f4 to root (folder-to-root)
    moveResource(
      feide1,
      MoveResourceDTO(fromFolderId = Value(f4.id), toFolderId = NullValue, resourceId = fr2.id),
    ) should be(204)

    val f4AfterFolderToRootMove = getFolderResources(feide1, f4.id)
    f4AfterFolderToRootMove.resources.length should be(0)

    val rootAfterFolderToRootMove = getRootResources(feide1)
    rootAfterFolderToRootMove.length should be(2)
    rootAfterFolderToRootMove.map(_.id) should be(List(rr2.id, fr2.id))
    rootAfterFolderToRootMove.flatMap(_.rank) should be(List(1, 2))

  }

  test("moving a resource to the same folder returns 400") {
    when(feideApiClient.getFeideGroupsAndOrganization(any)).thenReturn(Success((Seq.empty, "zxc")))

    val f1  = createFolder(feide1, "folder1", None)
    val res = addResourceToFolder(
      feide1,
      f1.id,
      api.NewResourceDTO(resourceType = Article, path = "path/to/1", tags = None, resourceId = "1"),
    )

    moveResource(
      feide1,
      MoveResourceDTO(fromFolderId = Value(f1.id), toFolderId = Value(f1.id), resourceId = res.id),
      failOnError = false,
    ) should be(400)
  }

  test("moving a resource from a folder you don't own returns 403") {
    when(feideApiClient.getFeideGroupsAndOrganization(any)).thenReturn(Success((Seq.empty, "zxc")))

    val f1  = createFolder(feide1, "folder1", None)
    val f2  = createFolder(feide1, "folder2", None)
    val res = addResourceToFolder(
      feide1,
      f1.id,
      api.NewResourceDTO(resourceType = Article, path = "path/to/1", tags = None, resourceId = "1"),
    )

    moveResource(
      feide2,
      MoveResourceDTO(fromFolderId = Value(f1.id), toFolderId = Value(f2.id), resourceId = res.id),
      failOnError = false,
    ) should be(403)
  }

  test("moving a resource to a folder you don't own returns 403") {
    when(feideApiClient.getFeideGroupsAndOrganization(any)).thenReturn(Success((Seq.empty, "zxc")))

    val f1U1 = createFolder(feide1, "folder1", None)
    val f1U2 = createFolder(feide2, "folder1", None)
    val res  = addResourceToFolder(
      feide1,
      f1U1.id,
      api.NewResourceDTO(resourceType = Article, path = "path/to/1", tags = None, resourceId = "1"),
    )

    moveResource(
      feide1,
      MoveResourceDTO(fromFolderId = Value(f1U1.id), toFolderId = Value(f1U2.id), resourceId = res.id),
      failOnError = false,
    ) should be(403)
  }

  test("moving a resource you don't own returns 403") {
    when(feideApiClient.getFeideGroupsAndOrganization(any)).thenReturn(Success((Seq.empty, "zxc")))

    val f1U1  = createFolder(feide1, "folder1", None)
    val u1Res = addResourceToFolder(
      feide1,
      f1U1.id,
      api.NewResourceDTO(resourceType = Article, path = "path/to/1", tags = None, resourceId = "1"),
    )

    val f1U2 = createFolder(feide2, "folder1", None)
    val f2U2 = createFolder(feide2, "folder2", None)

    moveResource(
      feide2,
      MoveResourceDTO(fromFolderId = Value(f1U2.id), toFolderId = Value(f2U2.id), resourceId = u1Res.id),
      failOnError = false,
    ) should be(403)
  }

  test("moving a resource to a folder that already contains it fails") {
    when(feideApiClient.getFeideGroupsAndOrganization(any)).thenReturn(Success((Seq.empty, "zxc")))

    val f1 = createFolder(feide1, "folder1", None)
    val f2 = createFolder(feide1, "folder2", None)

    val resourceDto = api.NewResourceDTO(resourceType = Article, path = "path/to/1", tags = None, resourceId = "1")
    val r1          = addResourceToFolder(feide1, f1.id, resourceDto)
    addResourceToFolder(feide1, f2.id, resourceDto)

    moveResource(
      feide1,
      MoveResourceDTO(fromFolderId = Value(f1.id), toFolderId = Value(f2.id), resourceId = r1.id),
      failOnError = false,
    ) should be(400)
  }
}
