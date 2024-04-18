/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.e2e

import io.circe.parser
import no.ndla.common.CirceUtil
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.ResourceType
import no.ndla.myndlaapi.model.api
import no.ndla.myndlaapi.model.domain
import no.ndla.myndlaapi.model.api.{Breadcrumb, Folder}
import no.ndla.myndlaapi.model.domain.{FolderStatus, NewFolderData, ResourceDocument}
import no.ndla.myndlaapi.{ComponentRegistry, MainClass, MyNdlaApiProperties, UnitSuite}
import no.ndla.network.clients.FeideExtendedUserInfo
import no.ndla.scalatestsuite.IntegrationSuite
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, spy, when, withSettings}
import org.mockito.quality.Strictness
import org.testcontainers.containers.PostgreSQLContainer
import sttp.client3.quick.*

import java.util.UUID
import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

class CloneFolderTest
    extends IntegrationSuite(
      EnableElasticsearchContainer = false,
      EnablePostgresContainer = true,
      EnableRedisContainer = true
    )
    with UnitSuite {

  val myndlaApiPort: Int          = findFreePort
  val pgc: PostgreSQLContainer[_] = postgresContainer.get
  val redisPort: Int              = redisContainer.get.port
  val myndlaproperties: MyNdlaApiProperties = new MyNdlaApiProperties {
    override def ApplicationPort: Int = myndlaApiPort
    override def MetaServer: String   = pgc.getHost
    override def MetaResource: String = pgc.getDatabaseName
    override def MetaUserName: String = pgc.getUsername
    override def MetaPassword: String = pgc.getPassword
    override def MetaPort: Int        = pgc.getMappedPort(5432)
    override def MetaSchema: String   = "testschema"

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

      when(feideApiClient.getFeideID(any)).thenReturn(Success("q"))
      when(feideApiClient.getFeideAccessTokenOrFail(any)).thenReturn(Success("notimportante"))
      when(feideApiClient.getFeideGroups(any)).thenReturn(Success(Seq.empty))
      when(feideApiClient.getFeideExtendedUser(any))
        .thenReturn(
          Success(FeideExtendedUserInfo("", Seq("employee"), Some("employee"), "email@ndla.no", Seq("email@ndla.no")))
        )
      when(feideApiClient.getOrganization(any)).thenReturn(Success("zxc"))
      when(clock.now()).thenReturn(NDLADate.of(2017, 1, 1, 1, 59))
    }
  }

  val testClock: myndlaApi.componentRegistry.SystemClock = myndlaApi.componentRegistry.clock

  val myndlaApiBaseUrl: String   = s"http://localhost:$myndlaApiPort"
  val myndlaApiFolderUrl: String = s"$myndlaApiBaseUrl/myndla-api/v1/folders"

  override def beforeAll(): Unit = {
    implicit val ec = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
    Future { myndlaApi.run() }: Unit
    Thread.sleep(4000)
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(myndlaApi.componentRegistry.folderRepository)
    reset(myndlaApi.componentRegistry.userRepository)

    myndlaApi.componentRegistry.folderRepository.deleteAllUserResources(feideId)
    myndlaApi.componentRegistry.folderRepository.deleteAllUserResources(destinationFeideId)
    myndlaApi.componentRegistry.folderRepository.deleteAllUserFolders(feideId)
    myndlaApi.componentRegistry.folderRepository.deleteAllUserFolders(destinationFeideId)
    myndlaApi.componentRegistry.userRepository.deleteUser(feideId)
    myndlaApi.componentRegistry.userRepository.deleteUser(destinationFeideId)
  }

  override def afterAll(): Unit = {
    super.afterAll()
  }

  // id is autogenerated in database, so we need to replace it to something constant in order to compare objects
  def replaceIdRecursively(folder: Folder, newId: String): Folder = {
    val updatedId          = newId
    val updatedParentId    = folder.parentId.map(_ => newId)
    val updatedBreadcrumbs = folder.breadcrumbs.map(_.copy(id = newId))
    val updatedResources   = folder.resources.map(_.copy(id = newId))
    val updatedSubfolders  = folder.subfolders.map { case child: Folder => replaceIdRecursively(child, newId) }

    folder.copy(
      id = updatedId,
      parentId = updatedParentId,
      subfolders = updatedSubfolders,
      resources = updatedResources,
      breadcrumbs = updatedBreadcrumbs
    )
  }

  def prepareFolderToClone(): UUID = {
    val folderRepository = myndlaApi.componentRegistry.folderRepository
    val parent =
      NewFolderData(
        parentId = None,
        name = "parent",
        status = FolderStatus.SHARED,
        rank = Some(1),
        description = Some("samling 0")
      )
    val pId = folderRepository.insertFolder(feideId, folderData = parent).get.id
    val pChild1 = NewFolderData(
      parentId = Some(pId),
      name = "p_child1",
      status = FolderStatus.SHARED,
      rank = Some(1),
      description = Some("samling 1")
    )
    val pChild2 = NewFolderData(
      parentId = Some(pId),
      name = "p_child2",
      status = FolderStatus.SHARED,
      rank = Some(2),
      description = Some("samling 2")
    )
    folderRepository.insertFolder(feideId, folderData = pChild1)
    folderRepository.insertFolder(feideId, folderData = pChild2)

    val document = ResourceDocument(tags = List("a", "b"), resourceId = "1")
    val rId = folderRepository.insertResource(feideId, "/path", ResourceType.Article, testClock.now(), document).get.id
    folderRepository.createFolderResourceConnection(pId, rId, 1, testClock.now())

    pId
  }

  test("that cloning a folder without destination works as expected") {
    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(any)).thenReturn(Success(destinationFeideId))
    when(myndlaApi.componentRegistry.feideApiClient.getFeideGroups(any)).thenReturn(Success(Seq.empty))
    val folderRepository = myndlaApi.componentRegistry.folderRepository

    val sourceFolderId = prepareFolderToClone()
    val customId       = "someid"
    val parentId       = Some(customId)

    val parentChild1 = api.Folder(
      id = customId,
      name = "p_child1",
      status = "private",
      parentId = parentId,
      breadcrumbs = List(Breadcrumb(id = customId, name = "parent"), Breadcrumb(id = customId, name = "p_child1")),
      subfolders = List.empty,
      resources = List.empty,
      rank = Some(1),
      created = testClock.now(),
      updated = testClock.now(),
      shared = None,
      description = Some("samling 1"),
      owner = None
    )

    val parentChild2 = api.Folder(
      id = customId,
      name = "p_child2",
      status = "private",
      parentId = parentId,
      breadcrumbs = List(Breadcrumb(id = customId, name = "parent"), Breadcrumb(id = customId, name = "p_child2")),
      subfolders = List.empty,
      resources = List.empty,
      rank = Some(2),
      created = testClock.now(),
      updated = testClock.now(),
      shared = None,
      description = Some("samling 2"),
      owner = None
    )

    val parentChild3 = api.Resource(
      id = customId,
      resourceType = ResourceType.Article,
      path = "/path",
      created = testClock.now(),
      tags = List(), // No tags since we are not owner
      resourceId = "1",
      rank = Some(1)
    )

    val expectedFolder = api.Folder(
      id = customId,
      name = "parent",
      status = "private",
      parentId = None,
      breadcrumbs = List(Breadcrumb(id = customId, name = "parent")),
      subfolders = List(parentChild1, parentChild2),
      resources = List(parentChild3),
      rank = Some(1),
      created = testClock.now(),
      updated = testClock.now(),
      shared = None,
      description = Some("samling 0"),
      owner = None
    )

    val destinationFoldersBefore = folderRepository.foldersWithFeideAndParentID(None, destinationFeideId)
    destinationFoldersBefore.get.length should be(0)

    val response = simpleHttpClient.send(
      quickRequest
        .post(uri"$myndlaApiFolderUrl/clone/$sourceFolderId")
        .header("FeideAuthorization", s"Bearer asd")
        .readTimeout(10.seconds)
    )

    val destinationFoldersAfter = folderRepository.foldersWithFeideAndParentID(None, destinationFeideId)
    destinationFoldersAfter.get.length should be(1)

    val bod          = response.body
    val deserialized = CirceUtil.unsafeParseAs[api.Folder](bod)
    val result       = replaceIdRecursively(deserialized, customId)
    result should be(expectedFolder)
  }

  test("that cloning a folder clones only folders with status SHARED") {
    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(any)).thenReturn(Success(destinationFeideId))
    val folderRepository = myndlaApi.componentRegistry.folderRepository

    val sourceFolderId = prepareFolderToClone()
    val customId       = "someid"
    val parentId       = Some(customId)

    val folderThatShouldNotBeCloned = NewFolderData(
      parentId = Some(sourceFolderId),
      name = "doesnt matter",
      status = FolderStatus.PRIVATE,
      rank = Some(10),
      description = None
    )
    val noCloneId = folderRepository.insertFolder(feideId, folderData = folderThatShouldNotBeCloned).get.id
    val folderThatShouldNotBeCloned2 = NewFolderData(
      parentId = Some(noCloneId),
      name = "doesnt matter2",
      status = FolderStatus.PRIVATE,
      rank = Some(11),
      description = Some("spilleringenrolle")
    )
    folderRepository.insertFolder(feideId, folderData = folderThatShouldNotBeCloned2).get.id
    val childrenFolderThatShouldNotBeCloned = NewFolderData(
      parentId = Some(sourceFolderId),
      name = "doesnt matter3",
      status = FolderStatus.PRIVATE,
      rank = Some(1),
      description = None
    )
    folderRepository.insertFolder(feideId, folderData = childrenFolderThatShouldNotBeCloned).get.id

    val parentChild1 = api.Folder(
      id = customId,
      name = "p_child1",
      status = "private",
      parentId = parentId,
      breadcrumbs = List(Breadcrumb(id = customId, name = "parent"), Breadcrumb(id = customId, name = "p_child1")),
      subfolders = List.empty,
      resources = List.empty,
      rank = Some(1),
      created = testClock.now(),
      updated = testClock.now(),
      shared = None,
      description = Some("samling 1"),
      owner = None
    )

    val parentChild2 = api.Folder(
      id = customId,
      name = "p_child2",
      status = "private",
      parentId = parentId,
      breadcrumbs = List(Breadcrumb(id = customId, name = "parent"), Breadcrumb(id = customId, name = "p_child2")),
      subfolders = List.empty,
      resources = List.empty,
      rank = Some(2),
      created = testClock.now(),
      updated = testClock.now(),
      shared = None,
      description = Some("samling 2"),
      owner = None
    )

    val parentChild3 = api.Resource(
      id = customId,
      resourceType = ResourceType.Article,
      path = "/path",
      created = testClock.now(),
      tags = List(), // No tags since we are not owner
      resourceId = "1",
      rank = Some(1)
    )

    val expectedFolder = api.Folder(
      id = customId,
      name = "parent",
      status = "private",
      parentId = None,
      breadcrumbs = List(Breadcrumb(id = customId, name = "parent")),
      subfolders = List(parentChild1, parentChild2),
      resources = List(parentChild3),
      rank = Some(1),
      created = testClock.now(),
      updated = testClock.now(),
      shared = None,
      description = Some("samling 0"),
      owner = None
    )

    val destinationFoldersBefore = folderRepository.foldersWithFeideAndParentID(None, destinationFeideId)
    destinationFoldersBefore.get.length should be(0)

    val response =
      simpleHttpClient.send(
        quickRequest
          .post(uri"$myndlaApiFolderUrl/clone/$sourceFolderId")
          .readTimeout(10.seconds)
          .header("FeideAuthorization", s"Bearer asd")
      )

    val destinationFoldersAfter = folderRepository.foldersWithFeideAndParentID(None, destinationFeideId)
    destinationFoldersAfter.get.length should be(1)

    val deserialized = CirceUtil.unsafeParseAs[api.Folder](response.body)
    val result       = replaceIdRecursively(deserialized, customId)
    result should be(expectedFolder)
  }

  test("that cloning a folder with destination works as expected") {
    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(any)).thenReturn(Success(destinationFeideId))
    val folderRepository = myndlaApi.componentRegistry.folderRepository

    val sourceFolderId = prepareFolderToClone()
    val customId       = "someid"
    val parentId       = Some(customId)

    val destinationFolder =
      NewFolderData(
        parentId = None,
        name = "destination",
        status = FolderStatus.PRIVATE,
        rank = Some(1),
        description = Some("desc hue")
      )
    val destinationFolderId = folderRepository.insertFolder(destinationFeideId, folderData = destinationFolder).get.id

    val parentChild1 = api.Folder(
      id = customId,
      name = "p_child1",
      status = "private",
      parentId = parentId,
      breadcrumbs = List(
        Breadcrumb(id = customId, name = destinationFolder.name),
        Breadcrumb(id = customId, name = "parent"),
        Breadcrumb(id = customId, name = "p_child1")
      ),
      subfolders = List.empty,
      resources = List.empty,
      rank = Some(1),
      created = testClock.now(),
      updated = testClock.now(),
      shared = None,
      description = Some("samling 1"),
      owner = None
    )

    val parentChild2 = api.Folder(
      id = customId,
      name = "p_child2",
      status = "private",
      parentId = parentId,
      breadcrumbs = List(
        Breadcrumb(id = customId, name = destinationFolder.name),
        Breadcrumb(id = customId, name = "parent"),
        Breadcrumb(id = customId, name = "p_child2")
      ),
      subfolders = List.empty,
      resources = List.empty,
      rank = Some(2),
      created = testClock.now(),
      updated = testClock.now(),
      shared = None,
      description = Some("samling 2"),
      owner = None
    )

    val parentChild3 = api.Resource(
      id = customId,
      resourceType = ResourceType.Article,
      path = "/path",
      created = testClock.now(),
      tags = List(), // No tags since we are not owner
      resourceId = "1",
      rank = Some(1)
    )

    val parent = api.Folder(
      id = customId,
      name = "parent",
      status = "private",
      parentId = parentId,
      breadcrumbs = List(
        Breadcrumb(id = customId, name = destinationFolder.name),
        Breadcrumb(id = customId, name = "parent")
      ),
      subfolders = List(parentChild1, parentChild2),
      resources = List(parentChild3),
      rank = Some(1),
      created = testClock.now(),
      updated = testClock.now(),
      shared = None,
      description = Some("samling 0"),
      owner = None
    )

    val response = simpleHttpClient.send(
      quickRequest
        .post(
          uri"$myndlaApiFolderUrl/clone/$sourceFolderId"
            .withParam("destination-folder-id", destinationFolderId.toString)
        )
        .readTimeout(10.seconds)
        .header("FeideAuthorization", s"Bearer asd")
    )

    val deserialized = CirceUtil.unsafeParseAs[api.Folder](response.body)
    val result       = replaceIdRecursively(deserialized, customId)
    result should be(parent)
  }

  test("that cloning a folder with destination fails if destination-folder-id is not found") {
    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(any)).thenReturn(Success(destinationFeideId))

    val sourceFolderId = prepareFolderToClone()
    val wrongId        = UUID.randomUUID()

    val response = simpleHttpClient
      .send(
        quickRequest
          .post(
            uri"$myndlaApiFolderUrl/clone/$sourceFolderId".addParam("destination-folder-id", wrongId.toString)
          )
          .header("FeideAuthorization", s"Bearer asd")
          .readTimeout(10.seconds)
      )

    val error = parser.parse(response.body).toTry.get
    error.hcursor.downField("code").as[String].toTry.get should be("NOT_FOUND")
    error.hcursor.downField("description").as[String].toTry.get should be(
      s"Folder with id ${wrongId.toString} does not exist"
    )
  }

  test(
    "that cloning a folder happens during one db transaction, if a fail occurs during inserting no new folders nor resources will be created"
  ) {
    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(any)).thenReturn(Success(destinationFeideId))
    val folderRepository = myndlaApi.componentRegistry.folderRepository
    val sourceFolderId   = prepareFolderToClone()

    // We want to fail on the next to last insertion to ensure that the previous insertions will be rollbacked
    when(myndlaApi.componentRegistry.folderRepository.insertFolder(any, any)(any))
      .thenCallRealMethod()
      .thenCallRealMethod()
      .thenReturn(Failure(new RuntimeException("bad")))
      .thenCallRealMethod()

    val destinationFoldersBefore   = folderRepository.foldersWithFeideAndParentID(None, destinationFeideId)
    val destinationResourcesBefore = folderRepository.resourcesWithFeideId(destinationFeideId, 10)
    destinationFoldersBefore.get.length should be(0)
    destinationResourcesBefore.get.length should be(0)

    simpleHttpClient.send(
      quickRequest
        .post(uri"$myndlaApiFolderUrl/clone/$sourceFolderId")
        .readTimeout(10.seconds)
        .header("FeideAuthorization", s"Bearer asd")
    )

    val destinationFoldersAfter   = folderRepository.foldersWithFeideAndParentID(None, destinationFeideId)
    val destinationResourcesAfter = folderRepository.resourcesWithFeideId(destinationFeideId, 10)
    destinationFoldersAfter.get.length should be(0)
    destinationResourcesAfter.get.length should be(0)
  }

  test("that sharing a folder will update shared field to current date") {
    reset(testClock)
    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(any)).thenReturn(Success(destinationFeideId))
    val shareTime = NDLADate.now().withNano(0)
    when(testClock.now()).thenReturn(shareTime)
    val folderRepository = myndlaApi.componentRegistry.folderRepository
    val destinationFolder =
      NewFolderData(
        parentId = None,
        name = "destination",
        status = FolderStatus.PRIVATE,
        rank = Some(1),
        description = None
      )
    val destinationFolderId = folderRepository.insertFolder(destinationFeideId, folderData = destinationFolder).get.id

    val response = simpleHttpClient.send(
      quickRequest
        .patch(uri"$myndlaApiFolderUrl/$destinationFolderId")
        .readTimeout(10.seconds)
        .header("FeideAuthorization", s"Bearer asd")
        .header("Content-Type", "application/json", replaceExisting = true)
        .body("""{"status":"shared"}""")
    )

    val result = CirceUtil.unsafeParseAs[api.Folder](response.body)
    result.shared should be(Some(shareTime))
  }

  test("that sharing a folder with subfolders will update shared field to current date for each subfolder") {
    val created = NDLADate.of(2023, 1, 1, 1, 59)
    val shared  = NDLADate.of(2024, 1, 1, 1, 59)
    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    when(testClock.now()).thenReturn(created, created, created, shared)
    val folderRepository = myndlaApi.componentRegistry.folderRepository
    val session          = folderRepository.getSession(true)

    val parent =
      NewFolderData(parentId = None, name = "parent", status = FolderStatus.PRIVATE, rank = Some(1), description = None)
    val parentId = folderRepository.insertFolder(feideId, folderData = parent).get.id
    val child = NewFolderData(
      parentId = Some(parentId),
      name = "child",
      status = FolderStatus.PRIVATE,
      rank = Some(1),
      description = None
    )
    val childId = folderRepository.insertFolder(feideId, folderData = child).get.id
    val childChild = NewFolderData(
      parentId = Some(childId),
      name = "childchild",
      status = FolderStatus.PRIVATE,
      rank = Some(1),
      description = None
    )
    val childChildId = folderRepository.insertFolder(feideId, folderData = childChild).get.id

    val expectedChildChild: domain.Folder = domain.Folder(
      id = childChildId,
      feideId = feideId,
      parentId = Some(childId),
      name = "childchild",
      status = FolderStatus.SHARED,
      rank = Some(1),
      created = created,
      updated = created,
      resources = List(),
      subfolders = List(),
      shared = Some(shared),
      description = None
    )
    val expectedChild: domain.Folder = domain.Folder(
      id = childId,
      feideId = feideId,
      parentId = Some(parentId),
      name = "child",
      status = FolderStatus.SHARED,
      rank = Some(1),
      created = created,
      updated = created,
      resources = List(),
      subfolders = List(expectedChildChild),
      shared = Some(shared),
      description = None
    )
    val expectedParent: domain.Folder = domain.Folder(
      id = parentId,
      feideId = feideId,
      parentId = None,
      name = "parent",
      status = FolderStatus.SHARED,
      rank = Some(1),
      created = created,
      updated = created,
      resources = List(),
      subfolders = List(expectedChild),
      shared = Some(shared),
      description = None
    )

    val response = simpleHttpClient.send(
      quickRequest
        .patch(uri"$myndlaApiFolderUrl/shared/$parentId?folder-status=shared")
        .readTimeout(10.seconds)
        .header("FeideAuthorization", s"Bearer asd")
    )

    val results            = CirceUtil.unsafeParseAs[List[UUID]](response.body)
    val resultParentId     = results.find(uuid => uuid == parentId).get
    val domainParentFolder = folderRepository.getFolderAndChildrenSubfolders(resultParentId)(session).get.get

    domainParentFolder should be(expectedParent)
  }

  test("that updating a folder correctly updates the updated field") {
    val created = NDLADate.of(2023, 1, 1, 1, 59)
    val updated = NDLADate.of(2024, 1, 1, 1, 59)
    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(any)).thenReturn(Success(destinationFeideId))
    when(testClock.now()).thenReturn(created, updated)
    val folderRepository = myndlaApi.componentRegistry.folderRepository
    val destinationFolder =
      NewFolderData(
        parentId = None,
        name = "destination",
        status = FolderStatus.PRIVATE,
        rank = Some(1),
        description = None
      )
    val destinationFolderId = folderRepository.insertFolder(destinationFeideId, folderData = destinationFolder).get.id

    val response = simpleHttpClient.send(
      quickRequest
        .patch(uri"$myndlaApiFolderUrl/$destinationFolderId")
        .readTimeout(10.seconds)
        .header("FeideAuthorization", s"Bearer asd")
        .header("Content-Type", "application/json", replaceExisting = true)
        .body("""{"name":"newname1"}""")
    )

    val result = CirceUtil.unsafeParseAs[api.Folder](response.body)
    result.updated should not be result.created
    result.updated should be(updated)
  }

  test("that cloning a folder with destination with conflicting sibling works as expected") {
    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    val folderRepository = myndlaApi.componentRegistry.folderRepository

    val toCopy =
      NewFolderData(
        parentId = None,
        name = "toCopy",
        status = FolderStatus.SHARED,
        rank = Some(1),
        description = Some("desc hue")
      )
    val toCopyId = folderRepository.insertFolder(feideId, toCopy).get.id

    val existingChild = NewFolderData(
      parentId = Some(toCopyId),
      name = "toCopyChild",
      status = FolderStatus.SHARED,
      rank = Some(1),
      description = Some("desc hue")
    )
    folderRepository.insertFolder(feideId, existingChild).failIfFailure

    val destinationFolder =
      NewFolderData(
        parentId = None,
        name = "destination",
        status = FolderStatus.SHARED,
        rank = Some(1),
        description = Some("desc hue")
      )
    val destinationId = folderRepository.insertFolder(feideId, destinationFolder).get.id

    val conflictingChild = NewFolderData(
      parentId = Some(destinationId),
      name = "toCopy",
      status = FolderStatus.SHARED,
      rank = Some(1),
      description = Some("desc hue")
    )
    folderRepository.insertFolder(feideId, conflictingChild).failIfFailure

    val response = simpleHttpClient.send(
      quickRequest
        .post(
          uri"$myndlaApiFolderUrl/clone/$toCopyId"
            .withParam("destination-folder-id", destinationId.toString)
        )
        .readTimeout(10.seconds)
        .header("FeideAuthorization", s"Bearer asd")
    )
    response.code.code should be(200)

    val allFolders = myndlaApi.componentRegistry.folderReadService
      .getFolders(
        includeSubfolders = true,
        includeResources = true,
        Some(feideId)
      )
      .get

    allFolders.size should be(2)
    allFolders.head.name should be("toCopy")

    val destFolder = allFolders(1)
    destFolder.name should be("destination")

    val destSubFolders = destFolder.subfolders
    destSubFolders.head.name should be("toCopy")
    destSubFolders(1).name should be("toCopy_Kopi")

    val List(copySubFolder) = destSubFolders(1).subfolders
    copySubFolder.name should be("toCopyChild")
  }

}
