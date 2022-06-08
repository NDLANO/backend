/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.learningpathapi.repository

import com.zaxxer.hikari.HikariDataSource
import no.ndla.learningpathapi.model.domain.DBFolderResource
import no.ndla.learningpathapi.{TestData, TestEnvironment}
import no.ndla.scalatestsuite.IntegrationSuite
import org.scalatest.Outcome
import scalikejdbc._

import java.net.Socket
import java.time.LocalDateTime
import java.util.UUID
import scala.util.{Failure, Success, Try}

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
    val folder1 = repository.insertFolder(TestData.emptyDomainFolder)
    val folder2 = repository.insertFolder(TestData.emptyDomainFolder)
    val folder3 = repository.insertFolder(TestData.emptyDomainFolder)

    repository.folderWithId(folder1.get.id.get) should be(folder1)
    repository.folderWithId(folder2.get.id.get) should be(folder2)
    repository.folderWithId(folder3.get.id.get) should be(folder3)
  }

  test("that inserting and retrieving a resource works as expected") {
    val created = LocalDateTime.now()
    when(clock.nowLocalDateTime()).thenReturn(created)

    val resource1 = repository.insertResource(TestData.emptyDomainResource.copy(created = created))
    val resource2 = repository.insertResource(TestData.emptyDomainResource.copy(created = created))
    val resource3 = repository.insertResource(TestData.emptyDomainResource.copy(created = created))

    repository.resourceWithId(resource1.get.id.get) should be(resource1)
    repository.resourceWithId(resource2.get.id.get) should be(resource2)
    repository.resourceWithId(resource3.get.id.get) should be(resource3)
  }

  test("that connecting folders and resources works as expected") {
    val folder1 = repository.insertFolder(TestData.emptyDomainFolder)
    val folder2 = repository.insertFolder(TestData.emptyDomainFolder)

    val resource1 = repository.insertResource(TestData.emptyDomainResource)
    val resource2 = repository.insertResource(TestData.emptyDomainResource)

    repository.createFolderResourceConnection(folder1.get.id.get, resource1.get.id.get)
    repository.createFolderResourceConnection(folder1.get.id.get, resource2.get.id.get)
    repository.createFolderResourceConnection(folder2.get.id.get, resource2.get.id.get)

    folderResourcesCount should be(3)
  }

  test("that deleting a folder deletes folder-resource connection") {
    val folder1 = repository.insertFolder(TestData.emptyDomainFolder)
    val folder2 = repository.insertFolder(TestData.emptyDomainFolder)

    val resource1 = repository.insertResource(TestData.emptyDomainResource)
    val resource2 = repository.insertResource(TestData.emptyDomainResource)

    repository.createFolderResourceConnection(folder1.get.id.get, resource1.get.id.get)
    repository.createFolderResourceConnection(folder1.get.id.get, resource2.get.id.get)
    repository.createFolderResourceConnection(folder2.get.id.get, resource2.get.id.get)

    folderResourcesCount() should be(3)
    repository.deleteFolder(folder1.get.id.get)
    folderResourcesCount() should be(1)
  }

  test("that deleting a resource deletes folder-resource connection") {
    val folder1 = repository.insertFolder(TestData.emptyDomainFolder)
    val folder2 = repository.insertFolder(TestData.emptyDomainFolder)

    val resource1 = repository.insertResource(TestData.emptyDomainResource)
    val resource2 = repository.insertResource(TestData.emptyDomainResource)

    repository.createFolderResourceConnection(folder1.get.id.get, resource1.get.id.get)
    repository.createFolderResourceConnection(folder1.get.id.get, resource2.get.id.get)
    repository.createFolderResourceConnection(folder2.get.id.get, resource1.get.id.get)

    repository.folderResourceConnectionCount(resource1.get.id.get).get should be(2)
    repository.folderResourceConnectionCount(resource2.get.id.get).get should be(1)
    folderResourcesCount() should be(3)
    repository.deleteResource(resource1.get.id.get)
    folderResourcesCount() should be(1)
    repository.deleteResource(resource2.get.id.get)
    folderResourcesCount() should be(0)
  }

  test("that resourceWithPathAndFeideId works correctly") {
    val resource1 = TestData.emptyDomainResource.copy(path = "pathernity test", feideId = "feide-1")

    repository.insertResource(resource1)
    val correct = repository.resourceWithPathAndFeideId(path = "pathernity test", feideId = "feide-1")
    correct.isSuccess should be(true)
    correct.get.isDefined should be(true)

    val wrong1 = repository.resourceWithPathAndFeideId(path = "pathernity test", feideId = "wrong")
    wrong1.isSuccess should be(true)
    wrong1.get.isDefined should be(false)

    val wrong2 = repository.resourceWithPathAndFeideId(path = "pathernity", feideId = "feide-1")
    wrong2.isSuccess should be(true)
    wrong2.get.isDefined should be(false)
  }

  test("that foldersWithParentID works correctly") {
    val data  = TestData.emptyDomainFolder
    val uuid1 = UUID.randomUUID()
    val uuid2 = UUID.randomUUID()

    repository.insertFolder(data.copy(id = Some(uuid1), parentId = None, feideId = "feide"))
    repository.insertFolder(data.copy(id = Some(uuid2), parentId = None, feideId = "feide"))
    repository.insertFolder(data.copy(parentId = Some(uuid1), feideId = "feide"))
    repository.insertFolder(data.copy(parentId = Some(uuid2), feideId = "feide"))

    repository.foldersWithFeideAndParentID(None, "feide").get.length should be(2)
    repository.foldersWithFeideAndParentID(Some(uuid1), "feide").get.length should be(1)
    repository.foldersWithFeideAndParentID(Some(uuid2), "feide").get.length should be(1)
  }

  test("that getFolderResources works as expected") {
    val folderData = TestData.emptyDomainFolder

    val folder1   = repository.insertFolder(folderData.copy(parentId = None, feideId = "feide"))
    val folder2   = repository.insertFolder(folderData.copy(parentId = Some(folder1.get.id.get), feideId = "feide"))
    val resource1 = repository.insertResource(TestData.emptyDomainResource)
    val resource2 = repository.insertResource(TestData.emptyDomainResource)
    val resource3 = repository.insertResource(TestData.emptyDomainResource)

    repository.createFolderResourceConnection(folder1.get.id.get, resource1.get.id.get)
    repository.createFolderResourceConnection(folder1.get.id.get, resource2.get.id.get)
    repository.createFolderResourceConnection(folder1.get.id.get, resource3.get.id.get)
    repository.createFolderResourceConnection(folder2.get.id.get, resource1.get.id.get)

    repository.getFolderResources(folder1.get.id.get).get.length should be(3)
    repository.getFolderResources(folder2.get.id.get).get.length should be(1)
  }

  test("that resourcesWithFeideId works as expected") {
    repository.insertResource(TestData.emptyDomainResource.copy(feideId = "feide1"))
    repository.insertResource(TestData.emptyDomainResource.copy(feideId = "feide2"))
    repository.insertResource(TestData.emptyDomainResource.copy(feideId = "feide3"))
    repository.insertResource(TestData.emptyDomainResource.copy(feideId = "feide1"))
    repository.insertResource(TestData.emptyDomainResource.copy(feideId = "feide1"))
    repository.insertResource(TestData.emptyDomainResource.copy(feideId = "feide1"))

    val results = repository.resourcesWithFeideId(feideId = "feide1", size = 2)
    results.isSuccess should be(true)
    results.get.length should be(2)
    results.get.length should not be (4)
  }

}
