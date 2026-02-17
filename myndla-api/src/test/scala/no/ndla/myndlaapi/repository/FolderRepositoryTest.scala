/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.repository

import cats.implicits.*
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.ResourceType
import no.ndla.common.model.domain.ResourceType.Article
import no.ndla.common.model.domain.myndla.FolderStatus
import no.ndla.database.{DBMigrator, DBUtility, DataSource}
import no.ndla.myndlaapi.model.domain.{
  BulkInserts,
  Folder,
  ResourceConnection,
  NewFolderData,
  Resource,
  ResourceDocument,
}
import no.ndla.myndlaapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.scalatestsuite.DatabaseIntegrationSuite
import org.mockito.Mockito.when
import scalikejdbc.*

import java.net.Socket
import java.util.UUID
import scala.util.{Success, Try}

class FolderRepositoryTest extends DatabaseIntegrationSuite with UnitSuite with TestEnvironment {
  override implicit lazy val dataSource: DataSource         = testDataSource.get
  override implicit lazy val migrator: DBMigrator           = new DBMigrator
  var repository: FolderRepository                          = scala.compiletime.uninitialized
  override implicit lazy val userRepository: UserRepository = new UserRepository
  override implicit lazy val DBUtil: DBUtility              = new DBUtility

  var feideId = "feide"

  def emptyTestDatabase: Boolean = {
    DBUtil.writeSession(implicit session => {
      sql"delete from my_ndla_users;".execute()(using session)
    })
  }

  def serverIsListening: Boolean = {
    val server = props.MetaServer.unsafeGet
    val port   = props.MetaPort.unsafeGet
    Try(new Socket(server, port)) match {
      case Success(c) =>
        c.close()
        true
      case _ => false
    }
  }

  override def beforeEach(): Unit = {
    repository = new FolderRepository
    if (serverIsListening) {
      emptyTestDatabase
    }
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    dataSource.connectToDatabase()
    if (serverIsListening) {
      migrator.migrate()
    }
  }

  def folderCount(implicit session: DBSession = DBUtil.autoSession): Long = {
    sql"select count(id) from ${Folder.table}".map(rs => rs.long("count")).single().getOrElse(0)
  }

  def resourceCount(implicit session: DBSession = DBUtil.autoSession): Long = {
    sql"select count(id) from ${Resource.table}".map(rs => rs.long("count")).single().getOrElse(0)
  }

  def folderResourcesCount(implicit session: DBSession = DBUtil.autoSession): Long = {
    sql"select count(resource_id) from ${ResourceConnection.table} where folder_id is not null"
      .map(rs => rs.long("count"))
      .single()
      .getOrElse(0)
  }

  def rootResourcesCount(implicit session: DBSession = DBUtil.autoSession): Long = {
    sql"select count(resource_id) from ${ResourceConnection.table} where folder_id is null"
      .map(rs => rs.long("count"))
      .single()
      .getOrElse(0)
  }

  def getAllFolders(implicit session: DBSession = DBUtil.autoSession): List[Folder] = {
    sql"select * from ${Folder.table}".map(rs => Folder.fromResultSet(rs)).list().sequence.get
  }

  test("that inserting and retrieving a folder works as expected") {
    implicit val session: DBSession = DBUtil.autoSession
    userRepository.reserveFeideIdIfNotExists(feideId)

    val created = NDLADate.now().withNano(0)
    when(clock.now()).thenReturn(created)

    val folder1 = repository.insertFolder(feideId, TestData.baseFolderDocument)
    val folder2 = repository.insertFolder(feideId, TestData.baseFolderDocument.copy(status = FolderStatus.PRIVATE))
    val folder3 = repository.insertFolder(feideId, TestData.baseFolderDocument)

    repository.folderWithId(folder1.get.id) should be(folder1)
    repository.folderWithId(folder2.get.id) should be(folder2)
    repository.folderWithId(folder3.get.id) should be(folder3)
  }

  test("that inserting and retrieving a resource works as expected") {
    implicit val session: DBSession = DBUtil.autoSession
    userRepository.reserveFeideIdIfNotExists(feideId)

    val created = NDLADate.now().withNano(0)
    when(clock.now()).thenReturn(created)

    val resource1 =
      repository.insertResource(feideId, "/path1", ResourceType.Article, created, TestData.baseResourceDocument)
    val resource2 =
      repository.insertResource(feideId, "/path2", ResourceType.Topic, created, TestData.baseResourceDocument)
    val resource3 = repository.insertResource(
      feideId,
      "/path3",
      ResourceType.Multidisciplinary,
      created,
      TestData.baseResourceDocument,
    )
    val resource4 =
      repository.insertResource(feideId, "/path4", ResourceType.Image, created, TestData.baseResourceDocument)
    val resource5 =
      repository.insertResource(feideId, "/path5", ResourceType.Audio, created, TestData.baseResourceDocument)
    val resource6 =
      repository.insertResource(feideId, "/path6", ResourceType.Concept, created, TestData.baseResourceDocument)
    val resource7 =
      repository.insertResource(feideId, "/path7", ResourceType.Learningpath, created, TestData.baseResourceDocument)
    val resource8 =
      repository.insertResource(feideId, "/path8", ResourceType.Video, created, TestData.baseResourceDocument)

    repository.resourceWithId(resource1.get.id) should be(resource1)
    repository.resourceWithId(resource2.get.id) should be(resource2)
    repository.resourceWithId(resource3.get.id) should be(resource3)
    repository.resourceWithId(resource4.get.id) should be(resource4)
    repository.resourceWithId(resource5.get.id) should be(resource5)
    repository.resourceWithId(resource6.get.id) should be(resource6)
    repository.resourceWithId(resource7.get.id) should be(resource7)
    repository.resourceWithId(resource8.get.id) should be(resource8)
  }

  test("that connecting folders and resources works as expected") {
    implicit val session: DBSession = DBUtil.autoSession
    userRepository.reserveFeideIdIfNotExists(feideId)

    val folder1 = repository.insertFolder(feideId, TestData.baseFolderDocument)
    val folder2 = repository.insertFolder(feideId, TestData.baseFolderDocument)

    val created = NDLADate.now().withNano(0)

    val resource1 =
      repository.insertResource(feideId, "/path1", ResourceType.Article, created, TestData.baseResourceDocument)
    val resource2 =
      repository.insertResource(feideId, "/path2", ResourceType.Article, created, TestData.baseResourceDocument)

    repository.createResourceConnection(folder1.toOption.map(_.id), resource1.get.id, 1, created)
    repository.createResourceConnection(folder1.toOption.map(_.id), resource2.get.id, 2, created)
    repository.createResourceConnection(folder2.toOption.map(_.id), resource2.get.id, 3, created)

    folderResourcesCount should be(3)
  }

  test("that inserting a root resource works as expected") {
    implicit val session: DBSession = DBUtil.autoSession
    userRepository.reserveFeideIdIfNotExists(feideId)

    val created = NDLADate.now().withNano(0)

    val resource1 =
      repository.insertResource(feideId, "/path1", ResourceType.Article, created, TestData.baseResourceDocument)
    val resource2 =
      repository.insertResource(feideId, "/path2", ResourceType.Article, created, TestData.baseResourceDocument)

    repository.createResourceConnection(None, resource1.get.id, 1, created)
    repository.createResourceConnection(None, resource2.get.id, 2, created)

    folderResourcesCount should be(0)
    rootResourcesCount should be(2)
  }

  test("that updateFolder updates all fields correctly") {
    implicit val session: DBSession = DBUtil.autoSession
    userRepository.reserveFeideIdIfNotExists(feideId)

    val created = NDLADate.now().withNano(0)
    when(clock.now()).thenReturn(created)

    val folderData =
      NewFolderData(parentId = None, name = "new", status = FolderStatus.PRIVATE, rank = 1, description = Some("old"))
    val updatedFolder = Folder(
      id = UUID.randomUUID(),
      feideId = feideId,
      parentId = None,
      name = "updated",
      status = FolderStatus.SHARED,
      rank = 1,
      resources = List.empty,
      subfolders = List.empty,
      created = created,
      updated = created,
      shared = None,
      description = Some("new"),
      user = None,
    )
    val expected = updatedFolder.copy(name = "updated", status = FolderStatus.SHARED, description = Some("new"))

    val inserted = repository.insertFolder(feideId = feideId, folderData = folderData)
    val result   = repository.updateFolder(id = inserted.get.id, feideId = feideId, folder = updatedFolder)
    result should be(Success(expected))
  }

  test("that deleting a folder deletes folder-resource connection") {
    implicit val session: DBSession = DBUtil.autoSession
    userRepository.reserveFeideIdIfNotExists(feideId)

    val created = NDLADate.now()

    val folder1 = repository.insertFolder(feideId, TestData.baseFolderDocument)
    val folder2 = repository.insertFolder(feideId, TestData.baseFolderDocument)

    val resource1 =
      repository.insertResource(feideId, "/path1", ResourceType.Article, created, TestData.baseResourceDocument)
    val resource2 =
      repository.insertResource(feideId, "/path2", ResourceType.Article, created, TestData.baseResourceDocument)
    repository.createResourceConnection(folder1.toOption.map(_.id), resource1.get.id, 1, created)
    repository.createResourceConnection(folder1.toOption.map(_.id), resource2.get.id, 2, created)
    repository.createResourceConnection(folder2.toOption.map(_.id), resource2.get.id, 3, created)

    folderResourcesCount() should be(3)
    repository.deleteFolder(folder1.get.id)
    folderResourcesCount() should be(1)
  }

  test("that deleting a resource deletes folder-resource connection") {
    implicit val session: DBSession = DBUtil.autoSession
    userRepository.reserveFeideIdIfNotExists(feideId)

    val created = NDLADate.now()
    when(clock.now()).thenReturn(created)

    val folder1 = repository.insertFolder(feideId, TestData.baseFolderDocument)
    val folder2 = repository.insertFolder(feideId, TestData.baseFolderDocument)

    val resource1 =
      repository.insertResource(feideId, "/path1", ResourceType.Article, created, TestData.baseResourceDocument)
    val resource2 =
      repository.insertResource(feideId, "/path2", ResourceType.Article, created, TestData.baseResourceDocument)

    repository.createResourceConnection(folder1.toOption.map(_.id), resource1.get.id, 1, created)
    repository.createResourceConnection(folder1.toOption.map(_.id), resource2.get.id, 1, created)
    repository.createResourceConnection(folder2.toOption.map(_.id), resource1.get.id, 1, created)
    repository.createResourceConnection(None, resource1.get.id, 1, created)

    repository.getConnections(None).get.size should be(1)

    repository.resourceConnectionCount(resource1.get.id).get should be(3)
    repository.resourceConnectionCount(resource2.get.id).get should be(1)
    rootResourcesCount should be(1)
    folderResourcesCount() should be(3)
    repository.deleteResource(resource1.get.id)
    folderResourcesCount() should be(1)
    rootResourcesCount should be(0)
    repository.deleteResource(resource2.get.id)
    folderResourcesCount() should be(0)
  }

  test("that resourceWithPathAndFeideId works correctly") {
    implicit val session: DBSession = DBUtil.autoSession
    userRepository.reserveFeideIdIfNotExists(feideId)

    val resource1 = TestData
      .emptyDomainResource
      .copy(path = "pathernity test", resourceType = ResourceType.Article, feideId = feideId)

    repository.insertResource(
      resource1.feideId,
      resource1.path,
      resource1.resourceType,
      resource1.created,
      ResourceDocument(resource1.tags, resource1.resourceId),
    )
    val correct = repository.resourceWithPathAndTypeAndFeideId(
      path = "pathernity test",
      resourceType = ResourceType.Article,
      feideId = feideId,
    )
    correct.isSuccess should be(true)
    correct.get.isDefined should be(true)

    val wrong1 = repository.resourceWithPathAndTypeAndFeideId(
      path = "pathernity test",
      resourceType = ResourceType.Article,
      feideId = "wrong",
    )
    wrong1.isSuccess should be(true)
    wrong1.get.isDefined should be(false)

    val wrong2 = repository.resourceWithPathAndTypeAndFeideId(
      path = "pathernity",
      resourceType = ResourceType.Article,
      feideId = feideId,
    )
    wrong2.isSuccess should be(true)
    wrong2.get.isDefined should be(false)
  }

  test("that foldersWithParentID works correctly") {
    implicit val session: DBSession = DBUtil.autoSession
    userRepository.reserveFeideIdIfNotExists(feideId)

    val parent1 = repository.insertFolder(feideId, TestData.baseFolderDocument)
    val parent2 = repository.insertFolder(feideId, TestData.baseFolderDocument)

    repository.insertFolder(feideId, TestData.baseFolderDocument.copy(parentId = Some(parent1.get.id)))
    repository.insertFolder(feideId, TestData.baseFolderDocument.copy(parentId = Some(parent2.get.id)))

    repository.foldersWithFeideAndParentID(None, feideId).get.length should be(2)
    repository.foldersWithFeideAndParentID(Some(parent1.get.id), feideId).get.length should be(1)
    repository.foldersWithFeideAndParentID(Some(parent2.get.id), feideId).get.length should be(1)
  }

  test("that getFolderResources and getRootResources works as expected") {
    implicit val session: DBSession = DBUtil.autoSession
    userRepository.reserveFeideIdIfNotExists(feideId)

    val created = NDLADate.now()
    val doc     =
      NewFolderData(parentId = None, name = "some name", status = FolderStatus.SHARED, rank = 1, description = None)

    val folder1 = repository.insertFolder(feideId, doc)
    val folder2 = repository.insertFolder(feideId, doc.copy(parentId = Some(folder1.get.id)))

    val resource1 =
      repository.insertResource(feideId, "/path1", ResourceType.Article, created, TestData.baseResourceDocument)
    val resource2 =
      repository.insertResource(feideId, "/path2", ResourceType.Article, created, TestData.baseResourceDocument)
    val resource3 =
      repository.insertResource(feideId, "/path3", ResourceType.Article, created, TestData.baseResourceDocument)

    repository.createResourceConnection(folder1.toOption.map(_.id), resource1.get.id, 1, created)
    repository.createResourceConnection(folder1.toOption.map(_.id), resource2.get.id, 2, created)
    repository.createResourceConnection(folder1.toOption.map(_.id), resource3.get.id, 3, created)
    repository.createResourceConnection(folder2.toOption.map(_.id), resource1.get.id, 4, created)

    repository.createResourceConnection(None, resource1.get.id, 1, created)
    repository.createResourceConnection(None, resource2.get.id, 1, created)
    repository.createResourceConnection(None, resource3.get.id, 1, created)

    repository.getFolderResources(folder1.get.id).get.length should be(3)
    repository.getFolderResources(folder2.get.id).get.length should be(1)

    repository.getRootResources(feideId).get.length should be(3)
  }

  test("that getConnection works with both root and folder resources") {
    implicit val session: DBSession = DBUtil.autoSession
    userRepository.reserveFeideIdIfNotExists(feideId)

    val now = NDLADate.now().withNano(0)

    val folder1   = repository.insertFolder(feideId, TestData.baseFolderDocument)
    val resource1 = repository
      .insertResource(feideId, "/path1", ResourceType.Article, now, TestData.baseResourceDocument)
      .failIfFailure

    val expectedRoot = ResourceConnection(folderId = None, resourceId = resource1.id, rank = 1, favoritedDate = now)

    val expectedFolderResource =
      ResourceConnection(folderId = Some(folder1.get.id), resourceId = resource1.id, rank = 1, favoritedDate = now)

    repository.createResourceConnection(folder1.toOption.map(_.id), resource1.id, 1, now).failIfFailure
    repository.createResourceConnection(None, resource1.id, 1, now).failIfFailure

    repository.getConnection(None, resource1.id).get should be(Some(expectedRoot))
    repository.getConnection(Some(folder1.get.id), resource1.id).get should be(Some(expectedFolderResource))
  }

  test("that resourcesWithFeideId works as expected") {
    implicit val session: DBSession = DBUtil.autoSession
    val feideId1                    = "feide1"
    val feideId2                    = "feide2"
    val feideId3                    = "feide3"
    userRepository.reserveFeideIdIfNotExists(feideId1).failIfFailure
    userRepository.reserveFeideIdIfNotExists(feideId2).failIfFailure
    userRepository.reserveFeideIdIfNotExists(feideId3).failIfFailure

    val created = NDLADate.now()

    repository.insertResource(feideId1, "/path1", ResourceType.Article, created, TestData.baseResourceDocument).get
    repository.insertResource(feideId2, "/path1", ResourceType.Article, created, TestData.baseResourceDocument).get
    repository.insertResource(feideId3, "/path1", ResourceType.Article, created, TestData.baseResourceDocument).get
    repository.insertResource(feideId1, "/path2", ResourceType.Article, created, TestData.baseResourceDocument).get
    repository.insertResource(feideId1, "/path3", ResourceType.Article, created, TestData.baseResourceDocument).get
    repository.insertResource(feideId1, "/path4", ResourceType.Article, created, TestData.baseResourceDocument).get

    val results = repository.resourcesWithFeideId(feideId = feideId1, size = 2)
    results.isSuccess should be(true)
    results.get.length should be(2)
  }

  test("Building tree-structure of folders works as expected") {
    val base = Folder(
      id = UUID.randomUUID(),
      feideId = feideId,
      parentId = None,
      name = "name",
      status = FolderStatus.SHARED,
      resources = List.empty,
      subfolders = List.empty,
      rank = 1,
      created = clock.now(),
      updated = clock.now(),
      shared = None,
      description = None,
      user = None,
    )

    val mainParent = base.copy(id = UUID.randomUUID(), parentId = None)

    val child1 = base.copy(id = UUID.randomUUID(), parentId = mainParent.id.some)

    val child2 = base.copy(id = UUID.randomUUID(), parentId = mainParent.id.some)

    val nestedChild1 = base.copy(id = UUID.randomUUID(), parentId = child1.id.some)

    val expectedResult = mainParent.copy(subfolders =
      List(child1.copy(subfolders = List(nestedChild1)), child2.copy()).sortBy(_.rank.toString)
    )

    repository.buildTreeStructureFromListOfChildren(
      mainParent.id,
      List(mainParent, child1, child2, nestedChild1),
    ) should be(Some(expectedResult))
  }

  test("inserting and fetching nested folders with resources works as expected") {
    implicit val session: DBSession = DBUtil.autoSession
    userRepository.reserveFeideIdIfNotExists(feideId)

    val created = NDLADate.now().withNano(0)
    when(clock.now()).thenReturn(created)

    val base = Folder(
      id = UUID.randomUUID(),
      feideId = feideId,
      parentId = None,
      name = "name",
      status = FolderStatus.SHARED,
      subfolders = List.empty,
      resources = List.empty,
      rank = 1,
      created = created,
      updated = created,
      shared = None,
      description = Some("desc"),
      user = None,
    )

    val baseNewFolderData = NewFolderData(
      parentId = base.parentId,
      name = base.name,
      status = base.status,
      rank = base.rank,
      description = Some("desc"),
    )

    val insertedMain   = repository.insertFolder(feideId, baseNewFolderData).failIfFailure
    val insertedChild1 = repository
      .insertFolder(feideId, baseNewFolderData.copy(parentId = insertedMain.id.some, rank = 1))
      .failIfFailure
    val insertedChild2 = repository
      .insertFolder(feideId, baseNewFolderData.copy(parentId = insertedMain.id.some, rank = 2))
      .failIfFailure
    val insertedChild3 = repository
      .insertFolder(feideId, baseNewFolderData.copy(parentId = insertedChild1.id.some, rank = 3))
      .failIfFailure
    val insertedResource = repository
      .insertResource(feideId, "/testPath", ResourceType.Article, created, ResourceDocument(List(), "1"))
      .failIfFailure
    val insertedConnection = repository
      .createResourceConnection(Some(insertedMain.id), insertedResource.id, 1, created)
      .failIfFailure

    val expectedSubfolders = List(insertedChild2, insertedChild1.copy(subfolders = List(insertedChild3)))

    val expectedResult = insertedMain.copy(
      subfolders = expectedSubfolders.sortBy(_.rank.toString),
      resources = List(insertedResource.copy(connection = Some(insertedConnection))),
    )

    val result = repository.getFolderAndChildrenSubfoldersWithResources(insertedMain.id)(using DBUtil.readOnlySession)
    result should be(Success(Some(expectedResult)))
  }

  test("that deleteAllUserFolders works as expected") {
    implicit val session: DBSession = DBUtil.autoSession
    userRepository.reserveFeideIdIfNotExists("feide1")
    userRepository.reserveFeideIdIfNotExists("feide2")
    userRepository.reserveFeideIdIfNotExists("feide3")

    repository.insertFolder("feide1", TestData.baseFolderDocument)
    repository.insertFolder("feide2", TestData.baseFolderDocument)
    repository.insertFolder("feide3", TestData.baseFolderDocument)
    repository.insertFolder("feide1", TestData.baseFolderDocument)
    repository.insertFolder("feide2", TestData.baseFolderDocument)
    repository.insertFolder("feide1", TestData.baseFolderDocument)

    folderCount() should be(6)
    repository.deleteAllUserFolders(feideId = "feide1") should be(Success(3))
    folderCount() should be(3)
  }

  test("that deleteAllUserResources works as expected") {
    implicit val session: DBSession = DBUtil.autoSession
    userRepository.reserveFeideIdIfNotExists("feide1")
    userRepository.reserveFeideIdIfNotExists("feide2")
    userRepository.reserveFeideIdIfNotExists("feide3")

    val created = NDLADate.now()

    repository.insertResource("feide1", "/path1", ResourceType.Article, created, TestData.baseResourceDocument)
    repository.insertResource("feide2", "/path1", ResourceType.Article, created, TestData.baseResourceDocument)
    repository.insertResource("feide3", "/path1", ResourceType.Article, created, TestData.baseResourceDocument)
    repository.insertResource("feide1", "/path2", ResourceType.Article, created, TestData.baseResourceDocument)
    repository.insertResource("feide1", "/path3", ResourceType.Article, created, TestData.baseResourceDocument)
    repository.insertResource("feide1", "/path4", ResourceType.Article, created, TestData.baseResourceDocument)

    resourceCount() should be(6)
    repository.deleteAllUserResources(feideId = "feide1") should be(Success(4))
    resourceCount() should be(2)
  }

  test(
    "that deleteAllUserFolders and deleteAllUserResources works as expected when folders and resources are connected"
  ) {
    implicit val session: DBSession = DBUtil.autoSession
    userRepository.reserveFeideIdIfNotExists("feide1")
    userRepository.reserveFeideIdIfNotExists("feide2")

    val created = NDLADate.now()
    when(clock.now()).thenReturn(created)

    val doc =
      NewFolderData(parentId = None, name = "some name", status = FolderStatus.SHARED, rank = 1, description = None)

    val folder1 = repository.insertFolder("feide1", doc)
    val folder2 = repository.insertFolder("feide1", doc.copy(parentId = Some(folder1.get.id)))
    val folder3 = repository.insertFolder("feide2", doc)

    val resource1 =
      repository.insertResource("feide1", "/path1", ResourceType.Article, created, TestData.baseResourceDocument)
    val resource2 =
      repository.insertResource("feide1", "/path2", ResourceType.Article, created, TestData.baseResourceDocument)
    val resource3 =
      repository.insertResource("feide1", "/path3", ResourceType.Article, created, TestData.baseResourceDocument)
    val resource4 =
      repository.insertResource("feide2", "/path4", ResourceType.Article, created, TestData.baseResourceDocument)

    repository.createResourceConnection(folder1.toOption.map(_.id), resource1.get.id, 1, created)
    repository.createResourceConnection(folder1.toOption.map(_.id), resource2.get.id, 2, created)
    repository.createResourceConnection(folder1.toOption.map(_.id), resource3.get.id, 3, created)
    repository.createResourceConnection(folder2.toOption.map(_.id), resource1.get.id, 4, created)
    repository.createResourceConnection(folder3.toOption.map(_.id), resource4.get.id, 5, created)
    repository.createResourceConnection(None, resource1.get.id, 1, created)
    repository.createResourceConnection(None, resource2.get.id, 2, created)
    repository.createResourceConnection(None, resource4.get.id, 3, created)
    folderCount() should be(3)
    resourceCount() should be(4)
    folderResourcesCount() should be(5)
    rootResourcesCount should be(3)

    repository.deleteAllUserFolders(feideId = "feide1") should be(Success(2))
    folderCount() should be(1)
    resourceCount() should be(4)
    folderResourcesCount() should be(1)

    repository.deleteAllUserResources(feideId = "feide1") should be(Success(3))
    folderCount() should be(1)
    resourceCount() should be(1)
    folderResourcesCount() should be(1)
    rootResourcesCount should be(1)
  }

  test("that getFoldersAndSubfoldersIds returns ids of folder and its subfolders") {
    implicit val session: DBSession = DBUtil.autoSession
    userRepository.reserveFeideIdIfNotExists(feideId)

    val doc =
      NewFolderData(parentId = None, name = "some name", status = FolderStatus.PRIVATE, rank = 1, description = None)

    val folder1 = repository.insertFolder(feideId, doc)
    val folder2 = repository.insertFolder(feideId, doc.copy(parentId = Some(folder1.get.id)))
    val folder3 = repository.insertFolder(feideId, doc.copy(parentId = Some(folder1.get.id)))
    val folder4 = repository.insertFolder(feideId, doc.copy(parentId = Some(folder2.get.id)))
    val folder5 = repository.insertFolder(feideId, doc.copy(parentId = Some(folder4.get.id)))
    val folder6 = repository.insertFolder(feideId, doc)
    repository.insertFolder(feideId, doc.copy(parentId = Some(folder6.get.id)))

    val ids = Seq(folder1.get.id, folder2.get.id, folder3.get.id, folder4.get.id, folder5.get.id)

    folderCount() should be(7)
    val result = repository.getFoldersAndSubfoldersIds(folder1.get.id)
    result.get.length should be(5)
    ids.sorted should be(result.get.sorted)
  }

  test("that updateFolderStatusInBulk updates status of chosen folders") {
    implicit val session: DBSession = DBUtil.autoSession
    userRepository.reserveFeideIdIfNotExists(feideId)

    val doc =
      NewFolderData(parentId = None, name = "some name", status = FolderStatus.PRIVATE, rank = 1, description = None)

    val folder1 = repository.insertFolder(feideId, doc)
    val folder2 = repository.insertFolder(feideId, doc.copy(parentId = Some(folder1.get.id)))
    val folder3 = repository.insertFolder(feideId, doc.copy(parentId = Some(folder1.get.id)))
    val folder4 = repository.insertFolder(feideId, doc.copy(parentId = Some(folder2.get.id)))
    val folder5 = repository.insertFolder(feideId, doc.copy(parentId = Some(folder4.get.id)))

    val ids = List(folder1.get.id, folder2.get.id, folder3.get.id, folder4.get.id, folder5.get.id)

    val result = repository.updateFolderStatusInBulk(ids, FolderStatus.SHARED)
    result.get.length should be(5)
    getAllFolders().map(folder => folder.status).distinct should be(List(FolderStatus.SHARED))
  }

  test("that getFolderAndChildrenSubfoldersWithResourcesWhere correctly filters data based on filter clause") {
    implicit val session: DBSession = DBUtil.autoSession
    userRepository.reserveFeideIdIfNotExists(feideId)

    val created = NDLADate.now().withNano(0)
    when(clock.now()).thenReturn(created)

    val base = Folder(
      id = UUID.randomUUID(),
      feideId = feideId,
      parentId = None,
      name = "name",
      status = FolderStatus.SHARED,
      subfolders = List.empty,
      resources = List.empty,
      rank = 1,
      created = created,
      updated = created,
      shared = None,
      description = None,
      user = None,
    )

    val baseNewFolderData = NewFolderData(
      parentId = base.parentId,
      name = base.name,
      status = base.status,
      rank = base.rank,
      description = None,
    )

    val insertedMain   = repository.insertFolder(feideId, baseNewFolderData).failIfFailure
    val insertedChild1 = repository
      .insertFolder(feideId, baseNewFolderData.copy(parentId = insertedMain.id.some, rank = 1))
      .failIfFailure
    val insertedChild2 = repository
      .insertFolder(
        feideId,
        baseNewFolderData.copy(parentId = insertedMain.id.some, status = FolderStatus.PRIVATE, rank = 2),
      )
      .failIfFailure
    val insertedChild3 = repository
      .insertFolder(feideId, baseNewFolderData.copy(parentId = insertedChild1.id.some, rank = 3))
      .failIfFailure
    val insertedResource = repository
      .insertResource(
        feideId,
        "/testPath",
        ResourceType.Article,
        NDLADate.now().withNano(0),
        ResourceDocument(List(), "1"),
      )
      .failIfFailure
    val insertedConnection = repository
      .createResourceConnection(Some(insertedMain.id), insertedResource.id, 1, created)
      .failIfFailure

    val expectedSubfolders = List(insertedChild2, insertedChild1.copy(subfolders = List(insertedChild3)))

    val expectedResultNormal = insertedMain.copy(
      subfolders = expectedSubfolders.sortBy(_.rank.toString),
      resources = List(insertedResource.copy(connection = Some(insertedConnection))),
    )

    val expectedResultFiltered = insertedMain.copy(
      subfolders = expectedSubfolders.filter(_.isShared).sortBy(_.id.toString),
      resources = List(insertedResource.copy(connection = Some(insertedConnection))),
    )

    val resultNormal =
      repository.getFolderAndChildrenSubfoldersWithResources(insertedMain.id)(using DBUtil.readOnlySession)
    resultNormal should be(Success(Some(expectedResultNormal)))

    val resultFiltered =
      repository.getFolderAndChildrenSubfoldersWithResources(insertedMain.id, FolderStatus.SHARED, None)(using
        DBUtil.readOnlySession
      )
    resultFiltered should be(Success(Some(expectedResultFiltered)))
  }

  test("that retrieving folder with subfolder via getFolderAndChildrenSubfolders works as expected") {
    implicit val session: DBSession = DBUtil.autoSession
    userRepository.reserveFeideIdIfNotExists(feideId)

    val folder1 = repository.insertFolder(feideId, TestData.baseFolderDocument)
    val folder2 = repository.insertFolder(feideId, TestData.baseFolderDocument.copy(parentId = Some(folder1.get.id)))

    val res = repository.getFolderAndChildrenSubfolders(folder1.get.id)
    res.get.get should be(folder1.get.copy(subfolders = List(folder2.get)))
  }

  test("that creating folder user connection works") {
    implicit val session: DBSession = DBUtil.autoSession
    userRepository.reserveFeideIdIfNotExists(feideId)

    val created = NDLADate.now().withNano(0)
    when(clock.now()).thenReturn(created)

    val folder1 = repository
      .insertFolder(feideId, TestData.baseFolderDocument.copy(status = FolderStatus.SHARED))
      .failIfFailure

    repository.createFolderUserConnection(folder1.id, feideId, 1).failIfFailure

    val res = repository.getSavedSharedFolders(feideId)

    res.get should have.length(1)
    res.get should contain(folder1)
  }

  test("that deleting folder user connection works") {
    implicit val session: DBSession = DBUtil.autoSession
    userRepository.reserveFeideIdIfNotExists(feideId)

    val created = NDLADate.now().withNano(0)
    when(clock.now()).thenReturn(created)

    val folder1 = repository
      .insertFolder(feideId, TestData.baseFolderDocument.copy(status = FolderStatus.SHARED))
      .failIfFailure
    val userFolder = repository.createFolderUserConnection(folder1.id, feideId, 1)
    val numRows    = repository.deleteFolderUserConnection(folder1.id.some, feideId.some)

    val res = repository.getSavedSharedFolders(feideId).failIfFailure

    numRows.get should be(1)
    res should have.length(0)
    res should not contain userFolder

  }

  test("that fetched saved folders come with the rank of the user that saved them") {
    implicit val session: DBSession = DBUtil.autoSession
    val feideId1                    = "feide1"
    val feideId2                    = "feide2"
    userRepository.reserveFeideIdIfNotExists(feideId1)
    userRepository.reserveFeideIdIfNotExists(feideId2)

    val created = NDLADate.now().withNano(0)
    when(clock.now()).thenReturn(created)

    val folder1 = repository
      .insertFolder(feideId1, TestData.baseFolderDocument.copy(status = FolderStatus.SHARED, rank = 1))
      .failIfFailure
    val folder2 = repository
      .insertFolder(feideId1, TestData.baseFolderDocument.copy(status = FolderStatus.SHARED, rank = 2))
      .failIfFailure
    val folder3 = repository
      .insertFolder(feideId1, TestData.baseFolderDocument.copy(status = FolderStatus.SHARED, rank = 3))
      .failIfFailure
    val folder4 = repository
      .insertFolder(feideId1, TestData.baseFolderDocument.copy(status = FolderStatus.SHARED, rank = 4))
      .failIfFailure

    repository.createFolderUserConnection(folder3.id, feideId2, 1).failIfFailure
    repository.createFolderUserConnection(folder4.id, feideId2, 2).failIfFailure

    val user1Shared = repository.getSavedSharedFolders(feideId1).failIfFailure
    user1Shared should be(List.empty)

    val user1Folders = repository.foldersWithFeideAndParentID(None, feideId1).failIfFailure
    user1Folders.map { f =>
      (f.id, f.rank)
    } should be(List((folder1.id, 1), (folder2.id, 2), (folder3.id, 3), (folder4.id, 4)))

    val user2Shared = repository.getSavedSharedFolders(feideId2).failIfFailure
    user2Shared.map { f =>
      (f.id, f.rank)
    } should be(List((folder3.id, 1), (folder4.id, 2)))

    val user2Folders = repository.foldersWithFeideAndParentID(None, feideId2).failIfFailure
    user2Folders should be(List.empty)
  }

  test("that number of users with/without favourites return correct amount") {
    implicit val session: DBSession = DBUtil.autoSession
    val feideId1                    = "feide1"
    val feideId2                    = "feide2"
    val feideId3                    = "feide3"
    userRepository.reserveFeideIdIfNotExists(feideId1).failIfFailure
    userRepository.reserveFeideIdIfNotExists(feideId2).failIfFailure
    userRepository.reserveFeideIdIfNotExists(feideId3).failIfFailure

    repository.insertResource(feideId1, "", Article, NDLADate.now(), ResourceDocument(List(), "")).failIfFailure

    val numberOfUsers                  = userRepository.numberOfUsers().failIfFailure.getOrElse(0L)
    val numberOfUsersWithFavourites    = repository.numberOfUsersWithFavourites().failIfFailure.getOrElse(0L)
    val numberOfUsersWithoutFavourites = repository.numberOfUsersWithoutFavourites().failIfFailure.getOrElse(0L)

    numberOfUsersWithFavourites should be(1)
    numberOfUsersWithoutFavourites should be(numberOfUsers - numberOfUsersWithFavourites)
  }

  test("that inserting in batches works as expected") {
    implicit val session: DBSession = DBUtil.autoSession
    userRepository.reserveFeideIdIfNotExists("feide1")

    val now     = NDLADate.now().withNano(0)
    val id1     = UUID.randomUUID()
    val id2     = UUID.randomUUID()
    val folder1 = Folder(
      id = id1,
      feideId = "feide1",
      parentId = None,
      name = "folder1",
      status = FolderStatus.PRIVATE,
      description = Some("Beskrivelse 1"),
      rank = 1,
      created = now,
      updated = now,
      resources = List.empty,
      subfolders = List.empty,
      shared = None,
      user = None,
    )

    val folder2 = Folder(
      id = id2,
      feideId = "feide1",
      parentId = None,
      name = "folder1",
      status = FolderStatus.PRIVATE,
      description = Some("Beskrivelse 1"),
      rank = 2,
      created = now,
      updated = now,
      resources = List.empty,
      subfolders = List.empty,
      shared = None,
      user = None,
    )

    val resource1 = Resource(
      id = UUID.randomUUID(),
      feideId = "feide1",
      created = now,
      path = "/r/norsk-sf-vg2/an-be-het-else-ord/140d6a7263",
      resourceType = ResourceType.Article,
      tags = List("tag"),
      resourceId = "16434",
      connection = None,
    )

    val resource2 = Resource(
      id = UUID.randomUUID(),
      feideId = "feide1",
      created = now,
      path = "/r/norsk-sf-vg2/hvordan-skrive-kortsvar-om-grammatikk/c44c43b139",
      resourceType = ResourceType.Article,
      tags = List("tag"),
      resourceId = "35549",
      connection = None,
    )

    val resource3 = resource2.copy(id = UUID.randomUUID())

    val folderResource1 =
      ResourceConnection(folderId = Some(folder1.id), resourceId = resource1.id, rank = 1, favoritedDate = now)

    val folderResource2 =
      ResourceConnection(folderId = Some(folder2.id), resourceId = resource3.id, rank = 1, favoritedDate = now)

    val bulkInserts = BulkInserts(
      folders = List(folder1, folder2),
      resources = List(resource1, resource2, resource3),
      connections = List(folderResource1, folderResource2),
    )
    repository.insertFolderInBulk(bulkInserts).get

    repository.folderWithId(id1).get should be(folder1)
    repository.folderWithId(id2).get should be(folder2)

    repository.insertResourcesInBulk(bulkInserts.copy(resources = List(resource2))).get
    repository.resourceWithId(resource2.id).get should be(resource2)

    repository.insertResourcesInBulk(bulkInserts).get
    repository.resourceWithId(resource1.id).get should be(resource1)
    repository.resourceWithId(resource2.id).get should be(resource2)
    val err = repository.resourceWithId(resource3.id)
    err.isFailure should be(true)

    repository.insertResourceConnectionInBulk(bulkInserts).get

    val conn1 = repository.getConnection(Some(folder1.id), resource1.id).get
    conn1 should be(Some(folderResource1))

    // Make sure folderResource connections are replaced with correct resources
    // so even if we reference resource3 in the connection we get a connection to 2 since there is a conflict
    val conn2 = repository.getConnection(Some(folder2.id), resource2.id).get
    conn2 should be(Some(folderResource2.copy(resourceId = resource2.id)))
  }

  test("That getting all resource connections works as intended") {
    implicit val session: DBSession = DBUtil.autoSession
    val feideId                     = "feide1"
    val feideId2                    = "feide2"
    userRepository.reserveFeideIdIfNotExists(feideId)
    userRepository.reserveFeideIdIfNotExists(feideId2)

    val now = NDLADate.now().withNano(0)
    val id1 = UUID.randomUUID()
    val id2 = UUID.randomUUID()

    val folder1 = Folder(
      id = id1,
      feideId = feideId,
      parentId = None,
      name = "folder1",
      status = FolderStatus.PRIVATE,
      description = Some("Beskrivelse 1"),
      rank = 1,
      created = now,
      updated = now,
      resources = List.empty,
      subfolders = List.empty,
      shared = None,
      user = None,
    )

    val folder2 = Folder(
      id = id2,
      feideId = feideId,
      parentId = None,
      name = "folder2",
      status = FolderStatus.PRIVATE,
      description = Some("Beskrivelse 2"),
      rank = 2,
      created = now,
      updated = now,
      resources = List.empty,
      subfolders = List.empty,
      shared = None,
      user = None,
    )

    val resource1 = Resource(
      id = UUID.randomUUID(),
      feideId = feideId,
      created = now,
      path = "/r/norsk-sf-vg2/an-be-het-else-ord/140d6a7263",
      resourceType = ResourceType.Article,
      tags = List("tag"),
      resourceId = "16434",
      connection = None,
    )

    val resource2 = Resource(
      id = UUID.randomUUID(),
      feideId = feideId,
      created = now,
      path = "/r/norsk-sf-vg2/hvordan-skrive-kortsvar-om-grammatikk/c44c43b139",
      resourceType = ResourceType.Article,
      tags = List("tag"),
      resourceId = "35549",
      connection = None,
    )

    val resource3 = resource2.copy(id = UUID.randomUUID(), feideId = feideId2)

    val conn1 =
      ResourceConnection(folderId = Some(folder1.id), resourceId = resource1.id, rank = 1, favoritedDate = now)

    val conn2 =
      ResourceConnection(folderId = Some(folder1.id), resourceId = resource2.id, rank = 2, favoritedDate = now)

    val conn3 =
      ResourceConnection(folderId = Some(folder2.id), resourceId = resource2.id, rank = 1, favoritedDate = now)

    val conn4 = ResourceConnection(folderId = None, resourceId = resource2.id, rank = 1, favoritedDate = now)

    val conn5 = ResourceConnection(folderId = None, resourceId = resource3.id, rank = 1, favoritedDate = now)

    val bulkInserts = BulkInserts(
      folders = List(folder1, folder2),
      resources = List(resource1, resource2, resource3),
      connections = List(conn1, conn2, conn3, conn4, conn5),
    )

    repository.insertFolderInBulk(bulkInserts).get
    repository.insertResourcesInBulk(bulkInserts).get
    repository.insertResourceConnectionInBulk(bulkInserts).get

    val connections = repository.getConnectionsByPath(resource2.path, feideId).get

    connections.size should be(3)
    connections.map(_.resourceId).toSet should be(Set(resource2.id))
    connections.filter(_.folderId.isEmpty).size should be(1)

  }
}
