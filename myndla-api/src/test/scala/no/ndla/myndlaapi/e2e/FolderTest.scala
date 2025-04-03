/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.e2e

import no.ndla.common.CirceUtil
import no.ndla.common.configuration.Prop
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.myndla.FolderStatus
import no.ndla.myndlaapi.model.api.FolderDTO
import no.ndla.myndlaapi.model.api
import no.ndla.myndlaapi.{ComponentRegistry, MainClass, MyNdlaApiProperties, UnitSuite}
import no.ndla.network.clients.FeideExtendedUserInfo
import no.ndla.scalatestsuite.IntegrationSuite
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, spy, when, withSettings}
import org.mockito.quality.Strictness
import org.testcontainers.containers.PostgreSQLContainer
import scalikejdbc.DBSession
import sttp.client3.quick.*

import java.util.UUID
import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.Success

class FolderTest
    extends IntegrationSuite(
      EnableElasticsearchContainer = false,
      EnablePostgresContainer = true,
      EnableRedisContainer = true
    )
    with UnitSuite {

  val myndlaApiPort: Int          = findFreePort
  val pgc: PostgreSQLContainer[?] = postgresContainer.get
  val redisPort: Int              = redisContainer.get.port
  val myndlaproperties: MyNdlaApiProperties = new MyNdlaApiProperties {
    override def ApplicationPort: Int       = myndlaApiPort
    override val MetaServer: Prop[String]   = Prop.propFromTestValue(pgc.getHost)
    override val MetaResource: Prop[String] = Prop.propFromTestValue(pgc.getDatabaseName)
    override val MetaUserName: Prop[String] = Prop.propFromTestValue(pgc.getUsername)
    override val MetaPassword: Prop[String] = Prop.propFromTestValue(pgc.getPassword)
    override val MetaPort: Prop[Int]        = Prop.propFromTestValue(pgc.getMappedPort(5432))
    override val MetaSchema: Prop[String]   = Prop.propFromTestValue("testschema")

    override def RedisHost: String = "localhost"
    override def RedisPort: Int    = redisPort
  }

  val feideId            = "feide"
  val destinationFeideId = "destinationFeideId"

  val myndlaApi: MainClass = new MainClass(myndlaproperties) {
    override val componentRegistry: ComponentRegistry = new ComponentRegistry(myndlaproperties) {
      override lazy val feideApiClient: FeideApiClient =
        mock[FeideApiClient](withSettings.strictness(Strictness.LENIENT))
      override lazy val clock: SystemClock = mock[SystemClock](withSettings.strictness(Strictness.LENIENT))
      override lazy val folderRepository: FolderRepository = spy(new FolderRepository)
      override lazy val userRepository: UserRepository     = spy(new UserRepository)

      when(feideApiClient.getFeideAccessTokenOrFail(any)).thenReturn(Success("notimportante"))
      when(feideApiClient.getFeideGroups(any)).thenReturn(Success(Seq.empty))
      when(feideApiClient.getFeideExtendedUser(any))
        .thenReturn(
          Success(
            FeideExtendedUserInfo("", Seq("employee"), Some("employee"), "email@ndla.no", Some(Seq("email@ndla.no")))
          )
        )
      when(feideApiClient.getOrganization(any)).thenReturn(Success("zxc"))
      when(clock.now()).thenReturn(NDLADate.of(2017, 1, 1, 1, 59))
    }
  }

  val testClock: myndlaApi.componentRegistry.SystemClock = myndlaApi.componentRegistry.clock

  val myndlaApiBaseUrl: String   = s"http://localhost:$myndlaApiPort"
  val myndlaApiFolderUrl: String = s"$myndlaApiBaseUrl/myndla-api/v1/folders"

  override def beforeAll(): Unit = {
    super.beforeAll()
    implicit val ec: ExecutionContextExecutorService =
      ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
    Future { myndlaApi.run(Array.empty) }: Unit
    Thread.sleep(4000)
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(myndlaApi.componentRegistry.folderRepository)
    reset(myndlaApi.componentRegistry.userRepository)

    implicit val session: DBSession = myndlaApi.componentRegistry.folderRepository.getSession(false)

    myndlaApi.componentRegistry.folderRepository.deleteAllUserResources("feide1")
    myndlaApi.componentRegistry.folderRepository.deleteAllUserResources("feide2")
    myndlaApi.componentRegistry.folderRepository.deleteAllUserFolders("feide1")
    myndlaApi.componentRegistry.folderRepository.deleteAllUserFolders("feide2")
    myndlaApi.componentRegistry.userRepository.deleteAllUsers
  }

  override def afterAll(): Unit = {
    super.afterAll()
  }

  // id is autogenerated in database, so we need to replace it to something constant in order to compare objects
  def replaceIdRecursively(folder: FolderDTO, newId: String): FolderDTO = {
    val updatedId          = newId
    val updatedParentId    = folder.parentId.map(_ => newId)
    val updatedBreadcrumbs = folder.breadcrumbs.map(_.copy(id = newId))
    val updatedResources   = folder.resources.map(_.copy(id = newId))
    val updatedSubfolders  = folder.subfolders.map { case child: FolderDTO => replaceIdRecursively(child, newId) }

    folder.copy(
      id = updatedId,
      parentId = updatedParentId,
      subfolders = updatedSubfolders,
      resources = updatedResources,
      breadcrumbs = updatedBreadcrumbs
    )
  }

  def createFolder(feideId: String, name: String): api.FolderDTO = {
    import io.circe.generic.auto.*
    val newFolderData = api.NewFolderDTO(
      name = name,
      status = Some(FolderStatus.SHARED.toString),
      parentId = None,
      description = None
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

  def getFolders(feideId: String): api.UserFolderDTO = {
    import io.circe.generic.auto.*
    val folders = simpleHttpClient.send(
      quickRequest
        .get(uri"$myndlaApiFolderUrl/")
        .header("FeideAuthorization", s"Bearer $feideId")
    )
    if (!folders.isSuccess)
      fail(s"Fetching all folders for $feideId failed with code ${folders.code} and body:\n${folders.body}")

    CirceUtil.unsafeParseAs[api.UserFolderDTO](folders.body)
  }

  def saveFolder(feideId: String, folderId: String): Unit = {
    val savedResponse = simpleHttpClient.send(
      quickRequest
        .post(uri"$myndlaApiFolderUrl/shared/$folderId/save")
        .header("FeideAuthorization", s"Bearer $feideId")
    )

    if (!savedResponse.isSuccess)
      fail(
        s"Saving folder $folderId for $feideId failed with code ${savedResponse.code} and body:\n${savedResponse.body}"
      )
  }

  def sortFolders(feideId: String, idsInOrder: List[String], sortShared: Boolean = false): Unit = {
    import io.circe.generic.auto.*
    val sortData = api.FolderSortRequestDTO(
      sortedIds = idsInOrder.map(UUID.fromString)
    )
    val body = CirceUtil.toJsonString(sortData)

    val requestUrl =
      if (sortShared) uri"$myndlaApiFolderUrl/sort-saved"
      else uri"$myndlaApiFolderUrl/sort-subfolders"

    val sortedFolders = simpleHttpClient.send(
      quickRequest
        .put(requestUrl)
        .header("FeideAuthorization", s"Bearer $feideId")
        .contentType("application/json")
        .body(body)
    )
    if (!sortedFolders.isSuccess)
      fail(
        s"Sorting folders for $feideId failed with code ${sortedFolders.code} and body:\n${sortedFolders.body}"
      )
  }

  test("Inserting and sorting folders") {
    val feideId1 = "feide1"
    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(eqTo(Some(feideId1)))).thenReturn(Success(feideId1))
    when(myndlaApi.componentRegistry.feideApiClient.getFeideGroups(any)).thenReturn(Success(Seq.empty))

    val f1 = createFolder(feideId1, "folder1")
    val f2 = createFolder(feideId1, "folder2")
    val f3 = createFolder(feideId1, "folder3")
    val f4 = createFolder(feideId1, "folder4")

    val foldersForU1 = getFolders(feideId1)
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

    sortFolders(feideId1, List(f4.id, f2.id, f3.id, f1.id))

    val foldersForU1Sorted = getFolders(feideId1)
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
    val feideId1 = "feide1"
    val feideId2 = "feide2"
    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(eqTo(Some(feideId1)))).thenReturn(Success(feideId1))
    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(eqTo(Some(feideId2)))).thenReturn(Success(feideId2))
    when(myndlaApi.componentRegistry.feideApiClient.getFeideGroups(any)).thenReturn(Success(Seq.empty))

    val f1 = createFolder(feideId1, "folder1")
    val f2 = createFolder(feideId1, "folder2")
    val f3 = createFolder(feideId1, "folder3")
    val f4 = createFolder(feideId1, "folder4")

    val foldersForU1 = getFolders(feideId1)
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

    sortFolders(feideId1, List(f4.id, f2.id, f3.id, f1.id))

    val foldersForU1Sorted = getFolders(feideId1)
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

    val foldersForU2 = getFolders(feideId2)
    foldersForU2.sharedFolders.length should be(0)

    saveFolder(feideId2, f1.id)
    saveFolder(feideId2, f3.id)

    val foldersForU2AfterSave = getFolders(feideId2)
    foldersForU2AfterSave.sharedFolders.length should be(2)
    foldersForU2AfterSave.sharedFolders.head.id should be(f1.id)
    foldersForU2AfterSave.sharedFolders.head.rank should be(1)
    foldersForU2AfterSave.sharedFolders(1).id should be(f3.id)
    foldersForU2AfterSave.sharedFolders(1).rank should be(2)

    sortFolders(feideId2, List(f3.id, f1.id), sortShared = true)

    val foldersForU2AfterSort = getFolders(feideId2)
    foldersForU2AfterSort.sharedFolders.length should be(2)
    foldersForU2AfterSort.sharedFolders.head.id should be(f3.id)
    foldersForU2AfterSort.sharedFolders.head.rank should be(1)
    foldersForU2AfterSort.sharedFolders(1).id should be(f1.id)
    foldersForU2AfterSort.sharedFolders(1).rank should be(2)
  }

}
