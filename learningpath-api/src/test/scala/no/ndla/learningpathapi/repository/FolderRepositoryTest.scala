/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.repository

import com.zaxxer.hikari.HikariDataSource
import no.ndla.learningpathapi.model.domain
import no.ndla.learningpathapi.model.domain.{DBFolderResource, FolderDocument, FolderStatus, ResourceDocument}
import no.ndla.learningpathapi.{TestData, TestEnvironment}
import no.ndla.scalatestsuite.IntegrationSuite
import org.scalatest.Outcome
import scalikejdbc._

import java.net.Socket
import java.time.LocalDateTime
import java.util.UUID
import scala.util.{Failure, Success, Try}
import cats.implicits._

class FolderRepositoryTest
    extends IntegrationSuite(EnablePostgresContainer = true)
    with TestEnvironment
    with DBFolderResource {
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

  def emptyTestDatabase = {
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

  def folderResourcesCount(implicit session: DBSession = AutoSession): Long = {
    sql"select count(folder_id) from ${DBFolderResource.table}"
      .map(rs => rs.long("count"))
      .single()
      .getOrElse(0)
  }

  test("that inserting and retrieving a folder works as expected") {
    val folder1 = repository.insertFolder("feide", None, TestData.baseFolderDocument)
    val folder2 = repository.insertFolder("feide", None, TestData.baseFolderDocument)
    val folder3 = repository.insertFolder("feide", None, TestData.baseFolderDocument)

    repository.folderWithId(folder1.get.id) should be(folder1)
    repository.folderWithId(folder2.get.id) should be(folder2)
    repository.folderWithId(folder3.get.id) should be(folder3)
  }

  test("that inserting and retrieving a resource works as expected") {
    val created = LocalDateTime.now()
    when(clock.nowLocalDateTime()).thenReturn(created)

    val resource1 = repository.insertResource("feide", "/path1", "type", created, TestData.baseResourceDocument)
    val resource2 = repository.insertResource("feide", "/path2", "type", created, TestData.baseResourceDocument)
    val resource3 = repository.insertResource("feide", "/path3", "type", created, TestData.baseResourceDocument)

    repository.resourceWithId(resource1.get.id) should be(resource1)
    repository.resourceWithId(resource2.get.id) should be(resource2)
    repository.resourceWithId(resource3.get.id) should be(resource3)
  }

  test("that connecting folders and resources works as expected") {
    val folder1 = repository.insertFolder("feide", None, TestData.baseFolderDocument)
    val folder2 = repository.insertFolder("feide", None, TestData.baseFolderDocument)

    val created = LocalDateTime.now()

    val resource1 = repository.insertResource("feide", "/path1", "type", created, TestData.baseResourceDocument)
    val resource2 = repository.insertResource("feide", "/path2", "type", created, TestData.baseResourceDocument)

    repository.createFolderResourceConnection(folder1.get.id, resource1.get.id)
    repository.createFolderResourceConnection(folder1.get.id, resource2.get.id)
    repository.createFolderResourceConnection(folder2.get.id, resource2.get.id)

    folderResourcesCount should be(3)
  }

  test("that deleting a folder deletes folder-resource connection") {
    val created = LocalDateTime.now()

    val folder1 = repository.insertFolder("feide", None, TestData.baseFolderDocument)
    val folder2 = repository.insertFolder("feide", None, TestData.baseFolderDocument)

    val resource1 = repository.insertResource("feide", "/path1", "type", created, TestData.baseResourceDocument)
    val resource2 = repository.insertResource("feide", "/path2", "type", created, TestData.baseResourceDocument)
    repository.createFolderResourceConnection(folder1.get.id, resource1.get.id)
    repository.createFolderResourceConnection(folder1.get.id, resource2.get.id)
    repository.createFolderResourceConnection(folder2.get.id, resource2.get.id)

    folderResourcesCount() should be(3)
    repository.deleteFolder(folder1.get.id)
    folderResourcesCount() should be(1)
  }

  test("that deleting a resource deletes folder-resource connection") {
    val created = LocalDateTime.now()

    val folder1 = repository.insertFolder("feide", None, TestData.baseFolderDocument)
    val folder2 = repository.insertFolder("feide", None, TestData.baseFolderDocument)

    val resource1 = repository.insertResource("feide", "/path1", "type", created, TestData.baseResourceDocument)
    val resource2 = repository.insertResource("feide", "/path2", "type", created, TestData.baseResourceDocument)

    repository.createFolderResourceConnection(folder1.get.id, resource1.get.id)
    repository.createFolderResourceConnection(folder1.get.id, resource2.get.id)
    repository.createFolderResourceConnection(folder2.get.id, resource1.get.id)

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
      TestData.emptyDomainResource.copy(path = "pathernity test", resourceType = "type", feideId = "feide-1")

    repository.insertResource(
      resource1.feideId,
      resource1.path,
      resource1.resourceType,
      resource1.created,
      ResourceDocument(resource1.tags, resource1.resourceId)
    )
    val correct =
      repository.resourceWithPathAndTypeAndFeideId(path = "pathernity test", resourceType = "type", feideId = "feide-1")
    correct.isSuccess should be(true)
    correct.get.isDefined should be(true)

    val wrong1 =
      repository.resourceWithPathAndTypeAndFeideId(path = "pathernity test", resourceType = "type", feideId = "wrong")
    wrong1.isSuccess should be(true)
    wrong1.get.isDefined should be(false)

    val wrong2 =
      repository.resourceWithPathAndTypeAndFeideId(path = "pathernity", resourceType = "type", feideId = "feide-1")
    wrong2.isSuccess should be(true)
    wrong2.get.isDefined should be(false)
  }

  test("that foldersWithParentID works correctly") {
    val parent1 = repository.insertFolder("feide", None, TestData.baseFolderDocument)
    val parent2 = repository.insertFolder("feide", None, TestData.baseFolderDocument)

    repository.insertFolder("feide", Some(parent1.get.id), TestData.baseFolderDocument)
    repository.insertFolder("feide", Some(parent2.get.id), TestData.baseFolderDocument)

    repository.foldersWithFeideAndParentID(None, "feide").get.length should be(2)
    repository.foldersWithFeideAndParentID(Some(parent1.get.id), "feide").get.length should be(1)
    repository.foldersWithFeideAndParentID(Some(parent2.get.id), "feide").get.length should be(1)
  }

  test("that getFolderResources works as expected") {
    val created = LocalDateTime.now()
    val doc     = FolderDocument(isFavorite = false, name = "some name", status = FolderStatus.PUBLIC)

    val folder1 = repository.insertFolder("feide", None, doc)
    val folder2 = repository.insertFolder("feide", Some(folder1.get.id), doc)

    val resource1 = repository.insertResource("feide", "/path1", "type", created, TestData.baseResourceDocument)
    val resource2 = repository.insertResource("feide", "/path2", "type", created, TestData.baseResourceDocument)
    val resource3 = repository.insertResource("feide", "/path3", "type", created, TestData.baseResourceDocument)

    repository.createFolderResourceConnection(folder1.get.id, resource1.get.id)
    repository.createFolderResourceConnection(folder1.get.id, resource2.get.id)
    repository.createFolderResourceConnection(folder1.get.id, resource3.get.id)
    repository.createFolderResourceConnection(folder2.get.id, resource1.get.id)

    repository.getFolderResources(folder1.get.id).get.length should be(3)
    repository.getFolderResources(folder2.get.id).get.length should be(1)
  }

  test("that resourcesWithFeideId works as expected") {
    val created = LocalDateTime.now()

    repository.insertResource("feide1", "/path1", "type", created, TestData.baseResourceDocument)
    repository.insertResource("feide2", "/path1", "type", created, TestData.baseResourceDocument)
    repository.insertResource("feide3", "/path1", "type", created, TestData.baseResourceDocument)
    repository.insertResource("feide1", "/path1", "type", created, TestData.baseResourceDocument)
    repository.insertResource("feide1", "/path1", "type", created, TestData.baseResourceDocument)
    repository.insertResource("feide1", "/path1", "type", created, TestData.baseResourceDocument)

    val results = repository.resourcesWithFeideId(feideId = "feide1", size = 2)
    results.isSuccess should be(true)
    results.get.length should be(2)
  }

  test("Building tree-structure of folders works as expected") {
    val base =
      domain.Folder(
        id = UUID.randomUUID(),
        feideId = "feide",
        parentId = None,
        name = "name",
        status = FolderStatus.PUBLIC,
        isFavorite = false,
        resources = List.empty,
        subfolders = List.empty
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
    val base =
      domain.Folder(
        id = UUID.randomUUID(),
        feideId = "feide",
        parentId = None,
        name = "name",
        status = FolderStatus.PUBLIC,
        isFavorite = false,
        subfolders = List.empty,
        resources = List.empty
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

    val insertedMain   = repository.insertFolder("feide", None, mainParent.toDocument()).failIfFailure
    val insertedChild1 = repository.insertFolder("feide", insertedMain.id.some, child1.toDocument()).failIfFailure
    val insertedChild2 = repository.insertFolder("feide", insertedMain.id.some, child2.toDocument()).failIfFailure
    val insertedChild3 =
      repository.insertFolder("feide", insertedChild1.id.some, nestedChild1.toDocument()).failIfFailure
    val insertedResource = repository
      .insertResource(
        "feide",
        "/testPath",
        "resourceType",
        LocalDateTime.now(),
        ResourceDocument(List(), 1)
      )
      .failIfFailure
    repository.createFolderResourceConnection(insertedMain.id, insertedResource.id).failIfFailure

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
      resources = List(insertedResource)
    )

    val result = repository.getFolderAndChildrenSubfoldersWithResources(insertedMain.id)(ReadOnlyAutoSession)
    result should be(Success(Some(expectedResult)))
  }

}
