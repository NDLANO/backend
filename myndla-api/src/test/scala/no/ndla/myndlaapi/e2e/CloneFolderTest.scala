/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.e2e

import io.circe.parser
import no.ndla.common.{CirceUtil, Clock}
import no.ndla.common.configuration.Prop
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.ResourceType
import no.ndla.common.model.domain.myndla.FolderStatus
import no.ndla.myndlaapi.model.api.{BreadcrumbDTO, FolderDTO, OwnerDTO}
import no.ndla.myndlaapi.model.{api, domain}
import no.ndla.myndlaapi.model.domain.{NewFolderData, ResourceDocument}
import no.ndla.myndlaapi.repository.{FolderRepository, UserRepository}
import no.ndla.myndlaapi.{ComponentRegistry, MainClass, MyNdlaApiProperties, TestData, TestEnvironment, UnitSuite}
import no.ndla.network.clients.{FeideApiClient, FeideExtendedUserInfo}
import no.ndla.scalatestsuite.{DatabaseIntegrationSuite, RedisIntegrationSuite}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, spy, times, verify, when, withSettings}
import org.mockito.quality.Strictness
import org.testcontainers.postgresql.PostgreSQLContainer
import scalikejdbc.AutoSession
import sttp.client3.quick.*

import java.util.UUID
import java.util.concurrent.Executors
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Failure, Success}

class CloneFolderTest extends DatabaseIntegrationSuite with RedisIntegrationSuite with UnitSuite with TestEnvironment {
  val myndlaApiPort: Int                                  = findFreePort
  val pgc: PostgreSQLContainer                            = postgresContainer.get
  val redisPort: Int                                      = redisContainer.get.port
  implicit lazy val myndlaproperties: MyNdlaApiProperties = new MyNdlaApiProperties {
    override def ApplicationPort: Int       = myndlaApiPort
    override val MetaServer: Prop[String]   = propFromTestValue("META_SERVER", pgc.getHost)
    override val MetaResource: Prop[String] = propFromTestValue("META_RESOURCE", pgc.getDatabaseName)
    override val MetaUserName: Prop[String] = propFromTestValue("META_USER_NAME", pgc.getUsername)
    override val MetaPassword: Prop[String] = propFromTestValue("META_PASSWORD", pgc.getPassword)
    override val MetaPort: Prop[Int]        = propFromTestValue("META_PORT", pgc.getMappedPort(5432))
    override val MetaSchema: Prop[String]   = propFromTestValue("META_SCHEMA", "testschema")

    override def RedisHost: String = "localhost"
    override def RedisPort: Int    = redisPort
  }

  val feideId            = "feide"
  val destinationFeideId = "destinationFeideId"

  val myndlaApi: MainClass = new MainClass(myndlaproperties) {
    override val componentRegistry: ComponentRegistry = new ComponentRegistry(myndlaproperties) {
      override implicit lazy val feideApiClient: FeideApiClient =
        mock[FeideApiClient](withSettings.strictness(Strictness.LENIENT))
      override implicit lazy val clock: Clock                       = mock[Clock](withSettings.strictness(Strictness.LENIENT))
      override implicit lazy val folderRepository: FolderRepository = spy(new FolderRepository)
      override implicit lazy val userRepository: UserRepository     = spy(new UserRepository)

      when(feideApiClient.getFeideID(any)).thenReturn(Success("q"))
      when(feideApiClient.getFeideAccessTokenOrFail(any)).thenReturn(Success("notimportante"))
      when(feideApiClient.getFeideGroups(any)).thenReturn(Success(Seq.empty))
      when(feideApiClient.getFeideExtendedUser(any)).thenReturn(
        Success(
          FeideExtendedUserInfo("", Seq("employee"), Some("employee"), "email@ndla.no", Some(Seq("email@ndla.no")))
        )
      )
      when(feideApiClient.getOrganization(any)).thenReturn(Success("zxc"))
      when(clock.now()).thenReturn(NDLADate.of(2017, 1, 1, 1, 59))
    }
  }

  val testClock: Clock = myndlaApi.componentRegistry.clock

  val myndlaApiBaseUrl: String   = s"http://localhost:$myndlaApiPort"
  val myndlaApiFolderUrl: String = s"$myndlaApiBaseUrl/myndla-api/v1/folders"

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
    implicit val session: AutoSession.type = AutoSession
    myndlaApi.componentRegistry.userRepository.deleteAllUsers.get

    myndlaApi.componentRegistry.userRepository.reserveFeideIdIfNotExists(feideId).get
    myndlaApi.componentRegistry.userRepository.insertUser(feideId, TestData.userDocument).get
    myndlaApi.componentRegistry.userRepository.reserveFeideIdIfNotExists(destinationFeideId).get
    myndlaApi.componentRegistry.userRepository.insertUser(destinationFeideId, TestData.userDocument).get
  }

  override def afterAll(): Unit = {
    super.afterAll()
  }

  // id is autogenerated in the database, so we need to replace it with something constant to compare objects
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

  def prepareFolderToClone(): UUID = {
    val folderRepository = myndlaApi.componentRegistry.folderRepository
    val parent           = NewFolderData(
      parentId = None,
      name = "parent",
      status = FolderStatus.SHARED,
      rank = 1,
      description = Some("samling 0"),
    )
    val pId     = folderRepository.insertFolder(feideId, folderData = parent).get.id
    val pChild1 = NewFolderData(
      parentId = Some(pId),
      name = "p_child1",
      status = FolderStatus.SHARED,
      rank = 1,
      description = Some("samling 1"),
    )
    val pChild2 = NewFolderData(
      parentId = Some(pId),
      name = "p_child2",
      status = FolderStatus.SHARED,
      rank = 2,
      description = Some("samling 2"),
    )
    folderRepository.insertFolder(feideId, folderData = pChild1)
    folderRepository.insertFolder(feideId, folderData = pChild2)

    val document = ResourceDocument(tags = List("a", "b"), resourceId = "1")
    val rId      = folderRepository.insertResource(feideId, "/path", ResourceType.Article, testClock.now(), document).get.id
    folderRepository.createFolderResourceConnection(pId, rId, 1, testClock.now())

    pId
  }

  test("that cloning a folder without destination works as expected") {
    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(any)).thenReturn(Success(destinationFeideId))
    when(myndlaApi.componentRegistry.feideApiClient.getFeideGroups(any)).thenReturn(Success(Seq.empty))
    val folderRepository = myndlaApi.componentRegistry.folderRepository

    val sourceFolderId = prepareFolderToClone()
    val customId       = UUID.randomUUID()
    val parentId       = Some(customId)

    val parentChild1 = api.FolderDTO(
      id = customId,
      name = "p_child1",
      status = "private",
      parentId = parentId,
      breadcrumbs =
        List(BreadcrumbDTO(id = customId, name = "parent"), BreadcrumbDTO(id = customId, name = "p_child1")),
      subfolders = List.empty,
      resources = List.empty,
      rank = 1,
      created = testClock.now(),
      updated = testClock.now(),
      shared = None,
      description = Some("samling 1"),
      owner = Some(OwnerDTO("")),
    )

    val parentChild2 = api.FolderDTO(
      id = customId,
      name = "p_child2",
      status = "private",
      parentId = parentId,
      breadcrumbs =
        List(BreadcrumbDTO(id = customId, name = "parent"), BreadcrumbDTO(id = customId, name = "p_child2")),
      subfolders = List.empty,
      resources = List.empty,
      rank = 2,
      created = testClock.now(),
      updated = testClock.now(),
      shared = None,
      description = Some("samling 2"),
      owner = Some(OwnerDTO("")),
    )

    val parentChild3 = api.ResourceDTO(
      id = customId,
      resourceType = ResourceType.Article,
      path = "/path",
      created = testClock.now(),
      tags = List(), // No tags since we are not the owner
      resourceId = "1",
      rank = Some(1),
    )

    val expectedFolder = api.FolderDTO(
      id = customId,
      name = "parent",
      status = "private",
      parentId = None,
      breadcrumbs = List(BreadcrumbDTO(id = customId, name = "parent")),
      subfolders = List(parentChild1, parentChild2),
      resources = List(parentChild3),
      rank = 1,
      created = testClock.now(),
      updated = testClock.now(),
      shared = None,
      description = Some("samling 0"),
      owner = Some(OwnerDTO("")),
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
    val deserialized = CirceUtil.unsafeParseAs[api.FolderDTO](bod)
    val result       = replaceIdRecursively(deserialized, customId)
    result should be(expectedFolder)
  }

  test("that cloning a folder clones only folders with status SHARED") {
    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(any)).thenReturn(Success(destinationFeideId))
    val folderRepository = myndlaApi.componentRegistry.folderRepository

    val sourceFolderId = prepareFolderToClone()
    val customId       = UUID.randomUUID()
    val parentId       = Some(customId)

    val folderThatShouldNotBeCloned = NewFolderData(
      parentId = Some(sourceFolderId),
      name = "doesnt matter",
      status = FolderStatus.PRIVATE,
      rank = 10,
      description = None,
    )
    val noCloneId                    = folderRepository.insertFolder(feideId, folderData = folderThatShouldNotBeCloned).get.id
    val folderThatShouldNotBeCloned2 = NewFolderData(
      parentId = Some(noCloneId),
      name = "doesnt matter2",
      status = FolderStatus.PRIVATE,
      rank = 11,
      description = Some("spilleringenrolle"),
    )
    folderRepository.insertFolder(feideId, folderData = folderThatShouldNotBeCloned2).get.id
    val childrenFolderThatShouldNotBeCloned = NewFolderData(
      parentId = Some(sourceFolderId),
      name = "doesnt matter3",
      status = FolderStatus.PRIVATE,
      rank = 1,
      description = None,
    )
    folderRepository.insertFolder(feideId, folderData = childrenFolderThatShouldNotBeCloned).get.id

    val parentChild1 = api.FolderDTO(
      id = customId,
      name = "p_child1",
      status = "private",
      parentId = parentId,
      breadcrumbs =
        List(BreadcrumbDTO(id = customId, name = "parent"), BreadcrumbDTO(id = customId, name = "p_child1")),
      subfolders = List.empty,
      resources = List.empty,
      rank = 1,
      created = testClock.now(),
      updated = testClock.now(),
      shared = None,
      description = Some("samling 1"),
      owner = Some(OwnerDTO("")),
    )

    val parentChild2 = api.FolderDTO(
      id = customId,
      name = "p_child2",
      status = "private",
      parentId = parentId,
      breadcrumbs =
        List(BreadcrumbDTO(id = customId, name = "parent"), BreadcrumbDTO(id = customId, name = "p_child2")),
      subfolders = List.empty,
      resources = List.empty,
      rank = 2,
      created = testClock.now(),
      updated = testClock.now(),
      shared = None,
      description = Some("samling 2"),
      owner = Some(OwnerDTO("")),
    )

    val parentChild3 = api.ResourceDTO(
      id = customId,
      resourceType = ResourceType.Article,
      path = "/path",
      created = testClock.now(),
      tags = List(), // No tags since we are not the owner
      resourceId = "1",
      rank = Some(1),
    )

    val expectedFolder = api.FolderDTO(
      id = customId,
      name = "parent",
      status = "private",
      parentId = None,
      breadcrumbs = List(BreadcrumbDTO(id = customId, name = "parent")),
      subfolders = List(parentChild1, parentChild2),
      resources = List(parentChild3),
      rank = 1,
      created = testClock.now(),
      updated = testClock.now(),
      shared = None,
      description = Some("samling 0"),
      owner = Some(OwnerDTO("")),
    )

    val destinationFoldersBefore = folderRepository.foldersWithFeideAndParentID(None, destinationFeideId)
    destinationFoldersBefore.get.length should be(0)

    val response = simpleHttpClient.send(
      quickRequest
        .post(uri"$myndlaApiFolderUrl/clone/$sourceFolderId")
        .readTimeout(10.seconds)
        .header("FeideAuthorization", s"Bearer asd")
    )

    val destinationFoldersAfter = folderRepository.foldersWithFeideAndParentID(None, destinationFeideId)
    destinationFoldersAfter.get.length should be(1)

    val deserialized = CirceUtil.unsafeParseAs[api.FolderDTO](response.body)
    val result       = replaceIdRecursively(deserialized, customId)
    result should be(expectedFolder)
  }

  test("that cloning a folder with destination works as expected") {
    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(any)).thenReturn(Success(destinationFeideId))
    val folderRepository = myndlaApi.componentRegistry.folderRepository

    val sourceFolderId = prepareFolderToClone()
    val customId       = UUID.randomUUID()
    val parentId       = Some(customId)

    val destinationFolder = NewFolderData(
      parentId = None,
      name = "destination",
      status = FolderStatus.PRIVATE,
      rank = 1,
      description = Some("desc hue"),
    )
    val destinationFolderId = folderRepository.insertFolder(destinationFeideId, folderData = destinationFolder).get.id

    val parentChild1 = api.FolderDTO(
      id = customId,
      name = "p_child1",
      status = "private",
      parentId = parentId,
      breadcrumbs = List(
        BreadcrumbDTO(id = customId, name = destinationFolder.name),
        BreadcrumbDTO(id = customId, name = "parent"),
        BreadcrumbDTO(id = customId, name = "p_child1"),
      ),
      subfolders = List.empty,
      resources = List.empty,
      rank = 1,
      created = testClock.now(),
      updated = testClock.now(),
      shared = None,
      description = Some("samling 1"),
      owner = Some(OwnerDTO("")),
    )

    val parentChild2 = api.FolderDTO(
      id = customId,
      name = "p_child2",
      status = "private",
      parentId = parentId,
      breadcrumbs = List(
        BreadcrumbDTO(id = customId, name = destinationFolder.name),
        BreadcrumbDTO(id = customId, name = "parent"),
        BreadcrumbDTO(id = customId, name = "p_child2"),
      ),
      subfolders = List.empty,
      resources = List.empty,
      rank = 2,
      created = testClock.now(),
      updated = testClock.now(),
      shared = None,
      description = Some("samling 2"),
      owner = Some(OwnerDTO("")),
    )

    val parentChild3 = api.ResourceDTO(
      id = customId,
      resourceType = ResourceType.Article,
      path = "/path",
      created = testClock.now(),
      tags = List(), // No tags since we are not the owner
      resourceId = "1",
      rank = Some(1),
    )

    val parent = api.FolderDTO(
      id = customId,
      name = "parent",
      status = "private",
      parentId = parentId,
      breadcrumbs = List(
        BreadcrumbDTO(id = customId, name = destinationFolder.name),
        BreadcrumbDTO(id = customId, name = "parent"),
      ),
      subfolders = List(parentChild1, parentChild2),
      resources = List(parentChild3),
      rank = 1,
      created = testClock.now(),
      updated = testClock.now(),
      shared = None,
      description = Some("samling 0"),
      owner = Some(OwnerDTO("")),
    )

    val response = simpleHttpClient.send(
      quickRequest
        .post(
          uri"$myndlaApiFolderUrl/clone/$sourceFolderId".withParam(
            "destination-folder-id",
            destinationFolderId.toString,
          )
        )
        .readTimeout(10.seconds)
        .header("FeideAuthorization", s"Bearer asd")
    )

    val deserialized = CirceUtil.unsafeParseAs[api.FolderDTO](response.body)
    val result       = replaceIdRecursively(deserialized, customId)
    result should be(parent)
  }

  test("that cloning a folder with destination fails if destination-folder-id is not found") {
    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(any)).thenReturn(Success(destinationFeideId))

    val sourceFolderId = prepareFolderToClone()
    val wrongId        = UUID.randomUUID()

    val response = simpleHttpClient.send(
      quickRequest
        .post(uri"$myndlaApiFolderUrl/clone/$sourceFolderId".addParam("destination-folder-id", wrongId.toString))
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

    // We want to fail on only the connection insertion
    when(myndlaApi.componentRegistry.folderRepository.insertResourceConnectionInBulk(any)(using any)).thenReturn(
      Failure(new RuntimeException("bad"))
    )

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

    verify(folderRepository, times(1)).insertFolderInBulk(any)(using any)

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
    val folderRepository  = myndlaApi.componentRegistry.folderRepository
    val destinationFolder =
      NewFolderData(parentId = None, name = "destination", status = FolderStatus.PRIVATE, rank = 1, description = None)
    val destinationFolderId = folderRepository.insertFolder(destinationFeideId, folderData = destinationFolder).get.id

    val response = simpleHttpClient.send(
      quickRequest
        .patch(uri"$myndlaApiFolderUrl/$destinationFolderId")
        .readTimeout(10.seconds)
        .header("FeideAuthorization", s"Bearer asd")
        .header("Content-Type", "application/json", replaceExisting = true)
        .body("""{"status":"shared"}""")
    )

    val result = CirceUtil.unsafeParseAs[api.FolderDTO](response.body)
    result.shared should be(Some(shareTime))
  }

  test("that sharing a folder with subfolders will update shared field to current date for each subfolder") {
    implicit val session: AutoSession.type = AutoSession

    val created = NDLADate.of(2023, 1, 1, 1, 59)
    val shared  = NDLADate.of(2024, 1, 1, 1, 59)
    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    when(testClock.now()).thenReturn(created, created, created, shared)
    val folderRepository = myndlaApi.componentRegistry.folderRepository

    val parent =
      NewFolderData(parentId = None, name = "parent", status = FolderStatus.PRIVATE, rank = 1, description = None)
    val parentId = folderRepository.insertFolder(feideId, folderData = parent).get.id
    val child    = NewFolderData(
      parentId = Some(parentId),
      name = "child",
      status = FolderStatus.PRIVATE,
      rank = 1,
      description = None,
    )
    val childId    = folderRepository.insertFolder(feideId, folderData = child).get.id
    val childChild = NewFolderData(
      parentId = Some(childId),
      name = "childchild",
      status = FolderStatus.PRIVATE,
      rank = 1,
      description = None,
    )
    val childChildId = folderRepository.insertFolder(feideId, folderData = childChild).get.id

    val expectedChildChild: domain.Folder = domain.Folder(
      id = childChildId,
      feideId = feideId,
      parentId = Some(childId),
      name = "childchild",
      status = FolderStatus.SHARED,
      rank = 1,
      created = created,
      updated = created,
      resources = List(),
      subfolders = List(),
      shared = Some(shared),
      description = None,
      user = None,
    )
    val expectedChild: domain.Folder = domain.Folder(
      id = childId,
      feideId = feideId,
      parentId = Some(parentId),
      name = "child",
      status = FolderStatus.SHARED,
      rank = 1,
      created = created,
      updated = created,
      resources = List(),
      subfolders = List(expectedChildChild),
      shared = Some(shared),
      description = None,
      user = None,
    )
    val expectedParent: domain.Folder = domain.Folder(
      id = parentId,
      feideId = feideId,
      parentId = None,
      name = "parent",
      status = FolderStatus.SHARED,
      rank = 1,
      created = created,
      updated = created,
      resources = List(),
      subfolders = List(expectedChild),
      shared = Some(shared),
      description = None,
      user = None,
    )

    val response = simpleHttpClient.send(
      quickRequest
        .patch(uri"$myndlaApiFolderUrl/shared/$parentId?folder-status=shared")
        .readTimeout(10.seconds)
        .header("FeideAuthorization", s"Bearer asd")
    )

    val results            = CirceUtil.unsafeParseAs[List[UUID]](response.body)
    val resultParentId     = results.find(uuid => uuid == parentId).get
    val domainParentFolder = folderRepository.getFolderAndChildrenSubfolders(resultParentId).get.get

    domainParentFolder should be(expectedParent)
  }

  test("that updating a folder correctly updates the updated field") {
    val created = NDLADate.of(2023, 1, 1, 1, 59)
    val updated = NDLADate.of(2024, 1, 1, 1, 59)
    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(any)).thenReturn(Success(destinationFeideId))
    when(testClock.now()).thenReturn(created, updated)
    val folderRepository  = myndlaApi.componentRegistry.folderRepository
    val destinationFolder =
      NewFolderData(parentId = None, name = "destination", status = FolderStatus.PRIVATE, rank = 1, description = None)
    val destinationFolderId = folderRepository.insertFolder(destinationFeideId, folderData = destinationFolder).get.id

    val response = simpleHttpClient.send(
      quickRequest
        .patch(uri"$myndlaApiFolderUrl/$destinationFolderId")
        .readTimeout(10.seconds)
        .header("FeideAuthorization", s"Bearer asd")
        .header("Content-Type", "application/json", replaceExisting = true)
        .body("""{"name":"newname1"}""")
    )

    val result = CirceUtil.unsafeParseAs[api.FolderDTO](response.body)
    result.updated should not be result.created
    result.updated should be(updated)
  }

  test("that cloning a folder with destination with conflicting sibling works as expected") {
    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(any)).thenReturn(Success(feideId))
    val folderRepository = myndlaApi.componentRegistry.folderRepository

    val toCopy = NewFolderData(
      parentId = None,
      name = "toCopy",
      status = FolderStatus.SHARED,
      rank = 1,
      description = Some("desc hue"),
    )
    val toCopyId = folderRepository.insertFolder(feideId, toCopy).get.id

    val existingChild = NewFolderData(
      parentId = Some(toCopyId),
      name = "toCopyChild",
      status = FolderStatus.SHARED,
      rank = 1,
      description = Some("desc hue"),
    )
    folderRepository.insertFolder(feideId, existingChild).failIfFailure

    val destinationFolder = NewFolderData(
      parentId = None,
      name = "destination",
      status = FolderStatus.SHARED,
      rank = 1,
      description = Some("desc hue"),
    )
    val destinationId = folderRepository.insertFolder(feideId, destinationFolder).get.id

    val conflictingChild = NewFolderData(
      parentId = Some(destinationId),
      name = "toCopy",
      status = FolderStatus.SHARED,
      rank = 1,
      description = Some("desc hue"),
    )
    folderRepository.insertFolder(feideId, conflictingChild).failIfFailure

    val response = simpleHttpClient.send(
      quickRequest
        .post(uri"$myndlaApiFolderUrl/clone/$toCopyId".withParam("destination-folder-id", destinationId.toString))
        .readTimeout(10.seconds)
        .header("FeideAuthorization", s"Bearer asd")
    )
    response.code.code should be(200)

    val allFolders = myndlaApi
      .componentRegistry
      .folderReadService
      .getFolders(includeSubfolders = true, includeResources = true, Some(feideId))
      .get
      .folders

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

  test("that cloning a folder twice works as expected") {
    when(myndlaApi.componentRegistry.feideApiClient.getFeideID(any)).thenReturn(Success(destinationFeideId))
    when(myndlaApi.componentRegistry.feideApiClient.getFeideGroups(any)).thenReturn(Success(Seq.empty))
    val folderRepository = myndlaApi.componentRegistry.folderRepository

    val sourceFolderId = prepareFolderToClone()
    val customId       = UUID.randomUUID()
    val parentId       = Some(customId)

    val parentChild1 = api.FolderDTO(
      id = customId,
      name = "p_child1",
      status = "private",
      parentId = parentId,
      breadcrumbs =
        List(BreadcrumbDTO(id = customId, name = "parent"), BreadcrumbDTO(id = customId, name = "p_child1")),
      subfolders = List.empty,
      resources = List.empty,
      rank = 1,
      created = testClock.now(),
      updated = testClock.now(),
      shared = None,
      description = Some("samling 1"),
      owner = Some(OwnerDTO("")),
    )

    val parentChild2 = api.FolderDTO(
      id = customId,
      name = "p_child2",
      status = "private",
      parentId = parentId,
      breadcrumbs =
        List(BreadcrumbDTO(id = customId, name = "parent"), BreadcrumbDTO(id = customId, name = "p_child2")),
      subfolders = List.empty,
      resources = List.empty,
      rank = 2,
      created = testClock.now(),
      updated = testClock.now(),
      shared = None,
      description = Some("samling 2"),
      owner = Some(OwnerDTO("")),
    )

    val parentChild3 = api.ResourceDTO(
      id = customId,
      resourceType = ResourceType.Article,
      path = "/path",
      created = testClock.now(),
      tags = List(), // No tags since we are not the owner
      resourceId = "1",
      rank = Some(1),
    )

    val expectedFolder = api.FolderDTO(
      id = customId,
      name = "parent",
      status = "private",
      parentId = None,
      breadcrumbs = List(BreadcrumbDTO(id = customId, name = "parent")),
      subfolders = List(parentChild1, parentChild2),
      resources = List(parentChild3),
      rank = 1,
      created = testClock.now(),
      updated = testClock.now(),
      shared = None,
      description = Some("samling 0"),
      owner = Some(OwnerDTO("")),
    )

    val destinationFoldersBefore = folderRepository.foldersWithFeideAndParentID(None, destinationFeideId)
    destinationFoldersBefore.get.length should be(0)

    {
      val response = simpleHttpClient.send(
        quickRequest
          .post(uri"$myndlaApiFolderUrl/clone/$sourceFolderId")
          .header("FeideAuthorization", s"Bearer asd")
          .readTimeout(10.seconds)
      )

      val destinationFoldersAfter = folderRepository.foldersWithFeideAndParentID(None, destinationFeideId)
      destinationFoldersAfter.get.length should be(1)

      val bod          = response.body
      val deserialized = CirceUtil.unsafeParseAs[api.FolderDTO](bod)
      val result       = replaceIdRecursively(deserialized, customId)
      result should be(expectedFolder)
    }

    {
      val response2 = simpleHttpClient.send(
        quickRequest
          .post(uri"$myndlaApiFolderUrl/clone/$sourceFolderId")
          .header("FeideAuthorization", s"Bearer asd")
          .readTimeout(10.seconds)
      )

      val destinationFoldersAfter2 = folderRepository.foldersWithFeideAndParentID(None, destinationFeideId)
      destinationFoldersAfter2.get.length should be(2)

      def replaceCrumbsRecursively(f: api.FolderDTO, before: String, after: String): api.FolderDTO = {
        val updatedSubfolders = f
          .subfolders
          .map { case child: FolderDTO =>
            replaceCrumbsRecursively(child, before, after)
          }
        val updatedCrumbs = f.breadcrumbs.map(b => b.copy(name = b.name.replace(before, after)))
        f.copy(name = f.name.replace(before, after), breadcrumbs = updatedCrumbs, subfolders = updatedSubfolders)
      }

      val bod2          = response2.body
      val deserialized2 = CirceUtil.unsafeParseAs[api.FolderDTO](bod2)
      val result2       = replaceIdRecursively(deserialized2, customId)
      val expected2     = replaceCrumbsRecursively(expectedFolder, "parent", "parent_Kopi").copy(rank = 2)
      result2 should be(expected2)
    }
  }

}
