/*
 * Part of NDLA myndla-api.
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi.repository

import cats.implicits._
import com.zaxxer.hikari.HikariDataSource
import no.ndla.common.model.NDLADate
import no.ndla.myndla.model.domain._
import no.ndla.myndlaapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.scalatestsuite.IntegrationSuite
import org.scalatest.Outcome
import scalikejdbc._

import java.net.Socket
import java.util.UUID
import scala.util.{Failure, Success, Try}

class FolderRepositoryTest
    extends IntegrationSuite(EnablePostgresContainer = true)
    with UnitSuite
    with TestEnvironment {
  override val dataSource: HikariDataSource = testDataSource.get
  override val migrator: DBMigrator         = new DBMigrator
  var repository: FolderRepository          = _

  // Skip tests if no docker environment available
  override def withFixture(test: NoArgTest): Outcome = {
    postgresContainer match {
      case Failure(ex) =>
        println(s"Postgres container not running, cancelling '${this.getClass.getName}'")
        println(s"Got exception: ${ex.getMessage}")
        ex.printStackTrace()
      case _ =>
    }
    assume(postgresContainer.isSuccess)
    super.withFixture(test)
  }

  def emptyTestDatabase: Boolean = {
    DB autoCommit (implicit session => {
      sql"delete from folders;".execute()(session)
      sql"delete from resources;".execute()(session)
      sql"delete from folder_resources;".execute()(session)
    })
  }

  def serverIsListening: Boolean = {
    val server = props.MetaServer
    val port   = props.MetaPort
    Try(new Socket(server, port)) match {
      case Success(c) =>
        c.close()
        true
      case _ =>
        false
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
    Try {
      DataSource.connectToDatabase()
      if (serverIsListening) {
        migrator.migrate()
      }
    }
  }

  def folderCount(implicit session: DBSession = AutoSession): Long = {
    sql"select count(id) from ${DBFolder.table}"
      .map(rs => rs.long("count"))
      .single()
      .getOrElse(0)
  }

  def resourceCount(implicit session: DBSession = AutoSession): Long = {
    sql"select count(id) from ${DBResource.table}"
      .map(rs => rs.long("count"))
      .single()
      .getOrElse(0)
  }

  def folderResourcesCount(implicit session: DBSession = AutoSession): Long = {
    sql"select count(folder_id) from ${DBFolderResource.table}"
      .map(rs => rs.long("count"))
      .single()
      .getOrElse(0)
  }

  def getAllFolders(implicit session: DBSession = AutoSession): List[Folder] = {
    sql"select * from ${DBFolder.table}"
      .map(rs => DBFolder.fromResultSet(rs))
      .list()
      .sequence
      .get
  }

  test("that inserting and retrieving a folder works as expected") {
    val created = NDLADate.now().withNano(0)
    when(clock.now()).thenReturn(created)

    val folder1 = repository.insertFolder("feide", TestData.baseFolderDocument)
    val folder2 = repository.insertFolder("feide", TestData.baseFolderDocument.copy(status = FolderStatus.PRIVATE))
    val folder3 = repository.insertFolder("feide", TestData.baseFolderDocument)

    repository.folderWithId(folder1.get.id) should be(folder1)
    repository.folderWithId(folder2.get.id) should be(folder2)
    repository.folderWithId(folder3.get.id) should be(folder3)
  }

  test("that inserting and retrieving a resource works as expected") {
    val created = NDLADate.now().withNano(0)
    when(clock.now()).thenReturn(created)

    val resource1 =
      repository.insertResource("feide", "/path1", ResourceType.Article, created, TestData.baseResourceDocument)
    val resource2 =
      repository.insertResource("feide", "/path2", ResourceType.Article, created, TestData.baseResourceDocument)
    val resource3 =
      repository.insertResource("feide", "/path3", ResourceType.Article, created, TestData.baseResourceDocument)

    repository.resourceWithId(resource1.get.id) should be(resource1)
    repository.resourceWithId(resource2.get.id) should be(resource2)
    repository.resourceWithId(resource3.get.id) should be(resource3)
  }

  test("that connecting folders and resources works as expected") {
    val folder1 = repository.insertFolder("feide", TestData.baseFolderDocument)
    val folder2 = repository.insertFolder("feide", TestData.baseFolderDocument)

    val created = NDLADate.now().withNano(0)

    val resource1 =
      repository.insertResource("feide", "/path1", ResourceType.Article, created, TestData.baseResourceDocument)
    val resource2 =
      repository.insertResource("feide", "/path2", ResourceType.Article, created, TestData.baseResourceDocument)

    repository.createFolderResourceConnection(folder1.get.id, resource1.get.id, 1)
    repository.createFolderResourceConnection(folder1.get.id, resource2.get.id, 2)
    repository.createFolderResourceConnection(folder2.get.id, resource2.get.id, 3)

    folderResourcesCount should be(3)
  }

  test("that updateFolder updates all fields correctly") {
    val created = NDLADate.now().withNano(0)
    when(clock.now()).thenReturn(created)

    val folderData =
      NewFolderData(
        parentId = None,
        name = "new",
        status = FolderStatus.PRIVATE,
        rank = None,
        description = Some("old")
      )
    val updatedFolder = Folder(
      id = UUID.randomUUID(),
      feideId = "feide",
      parentId = None,
      name = "updated",
      status = FolderStatus.SHARED,
      rank = None,
      resources = List.empty,
      subfolders = List.empty,
      created = created,
      updated = created,
      shared = None,
      description = Some("new")
    )
    val expected = updatedFolder.copy(name = "updated", status = FolderStatus.SHARED, description = Some("new"))

    val inserted = repository.insertFolder(feideId = "feide", folderData = folderData)
    val result   = repository.updateFolder(id = inserted.get.id, feideId = "feide", folder = updatedFolder)
    result should be(Success(expected))
  }

  test("that deleting a folder deletes folder-resource connection") {
    val created = NDLADate.now()

    val folder1 = repository.insertFolder("feide", TestData.baseFolderDocument)
    val folder2 = repository.insertFolder("feide", TestData.baseFolderDocument)

    val resource1 =
      repository.insertResource("feide", "/path1", ResourceType.Article, created, TestData.baseResourceDocument)
    val resource2 =
      repository.insertResource("feide", "/path2", ResourceType.Article, created, TestData.baseResourceDocument)
    repository.createFolderResourceConnection(folder1.get.id, resource1.get.id, 1)
    repository.createFolderResourceConnection(folder1.get.id, resource2.get.id, 2)
    repository.createFolderResourceConnection(folder2.get.id, resource2.get.id, 3)

    folderResourcesCount() should be(3)
    repository.deleteFolder(folder1.get.id)
    folderResourcesCount() should be(1)
  }

  test("that deleting a resource deletes folder-resource connection") {
    val created = NDLADate.now()

    val folder1 = repository.insertFolder("feide", TestData.baseFolderDocument)
    val folder2 = repository.insertFolder("feide", TestData.baseFolderDocument)

    val resource1 =
      repository.insertResource("feide", "/path1", ResourceType.Article, created, TestData.baseResourceDocument)
    val resource2 =
      repository.insertResource("feide", "/path2", ResourceType.Article, created, TestData.baseResourceDocument)

    repository.createFolderResourceConnection(folder1.get.id, resource1.get.id, 1)
    repository.createFolderResourceConnection(folder1.get.id, resource2.get.id, 1)
    repository.createFolderResourceConnection(folder2.get.id, resource1.get.id, 1)

    repository.folderResourceConnectionCount(resource1.get.id).get should be(2)
    repository.folderResourceConnectionCount(resource2.get.id).get should be(1)
    folderResourcesCount() should be(3)
    repository.deleteResource(resource1.get.id)
    folderResourcesCount() should be(1)
    repository.deleteResource(resource2.get.id)
    folderResourcesCount() should be(0)
  }

  test("that resourceWithPathAndFeideId works correctly") {
    val resource1 =
      TestData.emptyDomainResource.copy(
        path = "pathernity test",
        resourceType = ResourceType.Article,
        feideId = "feide-1"
      )

    repository.insertResource(
      resource1.feideId,
      resource1.path,
      resource1.resourceType,
      resource1.created,
      ResourceDocument(resource1.tags, resource1.resourceId)
    )
    val correct =
      repository.resourceWithPathAndTypeAndFeideId(
        path = "pathernity test",
        resourceType = ResourceType.Article,
        feideId = "feide-1"
      )
    correct.isSuccess should be(true)
    correct.get.isDefined should be(true)

    val wrong1 =
      repository.resourceWithPathAndTypeAndFeideId(
        path = "pathernity test",
        resourceType = ResourceType.Article,
        feideId = "wrong"
      )
    wrong1.isSuccess should be(true)
    wrong1.get.isDefined should be(false)

    val wrong2 =
      repository.resourceWithPathAndTypeAndFeideId(
        path = "pathernity",
        resourceType = ResourceType.Article,
        feideId = "feide-1"
      )
    wrong2.isSuccess should be(true)
    wrong2.get.isDefined should be(false)
  }

  test("that foldersWithParentID works correctly") {
    val parent1 = repository.insertFolder("feide", TestData.baseFolderDocument)
    val parent2 = repository.insertFolder("feide", TestData.baseFolderDocument)

    repository.insertFolder("feide", TestData.baseFolderDocument.copy(parentId = Some(parent1.get.id)))
    repository.insertFolder("feide", TestData.baseFolderDocument.copy(parentId = Some(parent2.get.id)))

    repository.foldersWithFeideAndParentID(None, "feide").get.length should be(2)
    repository.foldersWithFeideAndParentID(Some(parent1.get.id), "feide").get.length should be(1)
    repository.foldersWithFeideAndParentID(Some(parent2.get.id), "feide").get.length should be(1)
  }

  test("that getFolderResources works as expected") {
    val created = NDLADate.now()
    val doc =
      NewFolderData(parentId = None, name = "some name", status = FolderStatus.SHARED, rank = None, description = None)

    val folder1 = repository.insertFolder("feide", doc)
    val folder2 = repository.insertFolder("feide", doc.copy(parentId = Some(folder1.get.id)))

    val resource1 =
      repository.insertResource("feide", "/path1", ResourceType.Article, created, TestData.baseResourceDocument)
    val resource2 =
      repository.insertResource("feide", "/path2", ResourceType.Article, created, TestData.baseResourceDocument)
    val resource3 =
      repository.insertResource("feide", "/path3", ResourceType.Article, created, TestData.baseResourceDocument)

    repository.createFolderResourceConnection(folder1.get.id, resource1.get.id, 1)
    repository.createFolderResourceConnection(folder1.get.id, resource2.get.id, 2)
    repository.createFolderResourceConnection(folder1.get.id, resource3.get.id, 3)
    repository.createFolderResourceConnection(folder2.get.id, resource1.get.id, 4)

    repository.getFolderResources(folder1.get.id).get.length should be(3)
    repository.getFolderResources(folder2.get.id).get.length should be(1)
  }

  test("that resourcesWithFeideId works as expected") {
    val created = NDLADate.now()

    repository.insertResource("feide1", "/path1", ResourceType.Article, created, TestData.baseResourceDocument)
    repository.insertResource("feide2", "/path1", ResourceType.Article, created, TestData.baseResourceDocument)
    repository.insertResource("feide3", "/path1", ResourceType.Article, created, TestData.baseResourceDocument)
    repository.insertResource("feide1", "/path1", ResourceType.Article, created, TestData.baseResourceDocument)
    repository.insertResource("feide1", "/path1", ResourceType.Article, created, TestData.baseResourceDocument)
    repository.insertResource("feide1", "/path1", ResourceType.Article, created, TestData.baseResourceDocument)

    val results = repository.resourcesWithFeideId(feideId = "feide1", size = 2)
    results.isSuccess should be(true)
    results.get.length should be(2)
  }

  test("Building tree-structure of folders works as expected") {
    val base =
      Folder(
        id = UUID.randomUUID(),
        feideId = "feide",
        parentId = None,
        name = "name",
        status = FolderStatus.SHARED,
        resources = List.empty,
        subfolders = List.empty,
        rank = None,
        created = clock.now(),
        updated = clock.now(),
        shared = None,
        description = None
      )

    val mainParent = base.copy(
      id = UUID.randomUUID(),
      parentId = None
    )

    val child1 = base.copy(
      id = UUID.randomUUID(),
      parentId = mainParent.id.some
    )

    val child2 = base.copy(
      id = UUID.randomUUID(),
      parentId = mainParent.id.some
    )

    val nestedChild1 = base.copy(
      id = UUID.randomUUID(),
      parentId = child1.id.some
    )

    val expectedResult = mainParent.copy(
      subfolders = List(
        child1.copy(
          subfolders = List(nestedChild1)
        ),
        child2.copy()
      ).sortBy(_.id.toString)
    )

    repository.buildTreeStructureFromListOfChildren(
      mainParent.id,
      List(mainParent, child1, child2, nestedChild1)
    ) should be(
      Some(expectedResult)
    )
  }

  test("inserting and fetching nested folders with resources works as expected") {
    val created = NDLADate.now().withNano(0)
    when(clock.now()).thenReturn(created)

    val base =
      Folder(
        id = UUID.randomUUID(),
        feideId = "feide",
        parentId = None,
        name = "name",
        status = FolderStatus.SHARED,
        subfolders = List.empty,
        resources = List.empty,
        rank = None,
        created = created,
        updated = created,
        shared = None,
        description = Some("desc")
      )

    val baseNewFolderData = NewFolderData(
      parentId = base.parentId,
      name = base.name,
      status = base.status,
      rank = base.rank,
      description = Some("desc")
    )

    val insertedMain = repository.insertFolder("feide", baseNewFolderData).failIfFailure
    val insertedChild1 =
      repository.insertFolder("feide", baseNewFolderData.copy(parentId = insertedMain.id.some)).failIfFailure
    val insertedChild2 =
      repository.insertFolder("feide", baseNewFolderData.copy(parentId = insertedMain.id.some)).failIfFailure
    val insertedChild3 =
      repository.insertFolder("feide", baseNewFolderData.copy(parentId = insertedChild1.id.some)).failIfFailure
    val insertedResource = repository
      .insertResource(
        "feide",
        "/testPath",
        ResourceType.Article,
        NDLADate.now().withNano(0),
        ResourceDocument(List(), "1")
      )
      .failIfFailure
    val insertedConnection =
      repository.createFolderResourceConnection(insertedMain.id, insertedResource.id, 1).failIfFailure

    val expectedSubfolders = List(
      insertedChild2,
      insertedChild1.copy(
        subfolders = List(
          insertedChild3
        )
      )
    )

    val expectedResult = insertedMain.copy(
      subfolders = expectedSubfolders.sortBy(_.id.toString),
      resources = List(insertedResource.copy(connection = Some(insertedConnection)))
    )

    val result = repository.getFolderAndChildrenSubfoldersWithResources(insertedMain.id)(ReadOnlyAutoSession)
    result should be(Success(Some(expectedResult)))
  }

  test("that deleteAllUserFolders works as expected") {
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
    val created = NDLADate.now()

    repository.insertResource("feide1", "/path1", ResourceType.Article, created, TestData.baseResourceDocument)
    repository.insertResource("feide2", "/path1", ResourceType.Article, created, TestData.baseResourceDocument)
    repository.insertResource("feide3", "/path1", ResourceType.Article, created, TestData.baseResourceDocument)
    repository.insertResource("feide1", "/path1", ResourceType.Article, created, TestData.baseResourceDocument)
    repository.insertResource("feide1", "/path1", ResourceType.Article, created, TestData.baseResourceDocument)
    repository.insertResource("feide1", "/path1", ResourceType.Article, created, TestData.baseResourceDocument)

    resourceCount() should be(6)
    repository.deleteAllUserResources(feideId = "feide1") should be(Success(4))
    resourceCount() should be(2)
  }

  test(
    "that deleteAllUserFolders and deleteAllUserResources works as expected when folders and resources are connected"
  ) {
    val created = NDLADate.now()
    when(clock.now()).thenReturn(created)
    val doc =
      NewFolderData(parentId = None, name = "some name", status = FolderStatus.SHARED, rank = None, description = None)

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

    repository.createFolderResourceConnection(folder1.get.id, resource1.get.id, 1)
    repository.createFolderResourceConnection(folder1.get.id, resource2.get.id, 2)
    repository.createFolderResourceConnection(folder1.get.id, resource3.get.id, 3)
    repository.createFolderResourceConnection(folder2.get.id, resource1.get.id, 4)
    repository.createFolderResourceConnection(folder3.get.id, resource4.get.id, 5)

    folderCount() should be(3)
    resourceCount() should be(4)
    folderResourcesCount() should be(5)

    repository.deleteAllUserFolders(feideId = "feide1") should be(Success(2))
    folderCount() should be(1)
    resourceCount() should be(4)
    folderResourcesCount() should be(1)

    repository.deleteAllUserResources(feideId = "feide1") should be(Success(3))
    folderCount() should be(1)
    resourceCount() should be(1)
    folderResourcesCount() should be(1)
  }

  test("that getFoldersAndSubfoldersIds returns ids of folder and its subfolders") {
    val doc =
      NewFolderData(parentId = None, name = "some name", status = FolderStatus.PRIVATE, rank = None, description = None)

    val folder1 = repository.insertFolder("feide1", doc)
    val folder2 = repository.insertFolder("feide1", doc.copy(parentId = Some(folder1.get.id)))
    val folder3 = repository.insertFolder("feide1", doc.copy(parentId = Some(folder1.get.id)))
    val folder4 = repository.insertFolder("feide1", doc.copy(parentId = Some(folder2.get.id)))
    val folder5 = repository.insertFolder("feide1", doc.copy(parentId = Some(folder4.get.id)))
    val folder6 = repository.insertFolder("feide1", doc)
    repository.insertFolder("feide1", doc.copy(parentId = Some(folder6.get.id)))

    val ids = Seq(folder1.get.id, folder2.get.id, folder3.get.id, folder4.get.id, folder5.get.id)

    folderCount() should be(7)
    val result = repository.getFoldersAndSubfoldersIds(folder1.get.id)
    result.get.length should be(5)
    ids.sorted should be(result.get.sorted)
  }

  test("that updateFolderStatusInBulk updates status of chosen folders") {
    val doc =
      NewFolderData(parentId = None, name = "some name", status = FolderStatus.PRIVATE, rank = None, description = None)

    val folder1 = repository.insertFolder("feide1", doc)
    val folder2 = repository.insertFolder("feide1", doc.copy(parentId = Some(folder1.get.id)))
    val folder3 = repository.insertFolder("feide1", doc.copy(parentId = Some(folder1.get.id)))
    val folder4 = repository.insertFolder("feide1", doc.copy(parentId = Some(folder2.get.id)))
    val folder5 = repository.insertFolder("feide1", doc.copy(parentId = Some(folder4.get.id)))

    val ids = List(folder1.get.id, folder2.get.id, folder3.get.id, folder4.get.id, folder5.get.id)

    val result = repository.updateFolderStatusInBulk(ids, FolderStatus.SHARED)
    result.get.length should be(5)
    getAllFolders().map(folder => folder.status).distinct should be(List(FolderStatus.SHARED))
  }

  test("that getFolderAndChildrenSubfoldersWithResourcesWhere correctly filters data based on filter clause") {
    val created = NDLADate.now().withNano(0)
    when(clock.now()).thenReturn(created)

    val base =
      Folder(
        id = UUID.randomUUID(),
        feideId = "feide",
        parentId = None,
        name = "name",
        status = FolderStatus.SHARED,
        subfolders = List.empty,
        resources = List.empty,
        rank = None,
        created = created,
        updated = created,
        shared = None,
        description = None
      )

    val baseNewFolderData = NewFolderData(
      parentId = base.parentId,
      name = base.name,
      status = base.status,
      rank = base.rank,
      description = None
    )

    val insertedMain = repository.insertFolder("feide", baseNewFolderData).failIfFailure
    val insertedChild1 =
      repository.insertFolder("feide", baseNewFolderData.copy(parentId = insertedMain.id.some)).failIfFailure
    val insertedChild2 =
      repository
        .insertFolder("feide", baseNewFolderData.copy(parentId = insertedMain.id.some, status = FolderStatus.PRIVATE))
        .failIfFailure
    val insertedChild3 =
      repository.insertFolder("feide", baseNewFolderData.copy(parentId = insertedChild1.id.some)).failIfFailure
    val insertedResource = repository
      .insertResource(
        "feide",
        "/testPath",
        ResourceType.Article,
        NDLADate.now().withNano(0),
        ResourceDocument(List(), "1")
      )
      .failIfFailure
    val insertedConnection =
      repository.createFolderResourceConnection(insertedMain.id, insertedResource.id, 1).failIfFailure

    val expectedSubfolders = List(
      insertedChild2,
      insertedChild1.copy(
        subfolders = List(
          insertedChild3
        )
      )
    )

    val expectedResultNormal = insertedMain.copy(
      subfolders = expectedSubfolders.sortBy(_.id.toString),
      resources = List(insertedResource.copy(connection = Some(insertedConnection)))
    )

    val expectedResultFiltered = insertedMain.copy(
      subfolders = expectedSubfolders.filter(_.isShared).sortBy(_.id.toString),
      resources = List(insertedResource.copy(connection = Some(insertedConnection)))
    )

    val resultNormal = repository.getFolderAndChildrenSubfoldersWithResources(insertedMain.id)(ReadOnlyAutoSession)
    resultNormal should be(Success(Some(expectedResultNormal)))

    val resultFiltered =
      repository.getFolderAndChildrenSubfoldersWithResources(insertedMain.id, FolderStatus.SHARED, None)(
        ReadOnlyAutoSession
      )
    resultFiltered should be(Success(Some(expectedResultFiltered)))
  }

  test("that retrieving folder with subfolder via getFolderAndChildrenSubfolders works as expected") {
    implicit val session: AutoSession.type = AutoSession

    val folder1 = repository.insertFolder("feide", TestData.baseFolderDocument)
    val folder2 = repository.insertFolder("feide", TestData.baseFolderDocument.copy(parentId = Some(folder1.get.id)))

    val res = repository.getFolderAndChildrenSubfolders(folder1.get.id)
    res.get.get should be(folder1.get.copy(subfolders = List(folder2.get)))
  }

}
