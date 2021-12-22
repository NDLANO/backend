/*
 * Part of NDLA draft-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.repository

import java.net.Socket
import no.ndla.draftapi.{DBMigrator, DraftApiProperties, TestData, TestEnvironment}
import no.ndla.scalatestsuite.IntegrationSuite
import org.postgresql.util.PSQLException
import org.scalatest.Outcome
import scalikejdbc._

import scala.util.{Failure, Success, Try}

class UserDataRepositoryTest extends IntegrationSuite(EnablePostgresContainer = true) with TestEnvironment {
  override val dataSource = testDataSource.get
  var repository: UserDataRepository = new UserDataRepository

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
      sql"delete from userdata;".execute()(session)
    })
  }

  private def resetIdSequence() = {
    DB autoCommit (implicit session => {
      sql"select setval('userdata_id_seq', 1, false);".execute()
    })
  }

  def serverIsListening: Boolean = {
    val server = DraftApiProperties.MetaServer
    val port = DraftApiProperties.MetaPort
    Try(new Socket(server, port)) match {
      case Success(c) =>
        c.close()
        true
      case _ =>
        false
    }
  }

  override def beforeEach(): Unit = {
    repository = new UserDataRepository
    if (serverIsListening) {
      emptyTestDatabase
    }
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    Try {
      if (serverIsListening) {
        ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))
        DBMigrator.migrate(dataSource)
      }
    }
  }

  test("that inserting records to database is generating id as expected") {
    this.resetIdSequence()

    val data1 = TestData.emptyDomainUserData.copy(userId = "user1")
    val data2 = TestData.emptyDomainUserData.copy(userId = "user2")
    val data3 = TestData.emptyDomainUserData.copy(userId = "user3")

    val res1 = repository.insert(data1)
    val res2 = repository.insert(data2)
    val res3 = repository.insert(data3)

    res1.get.id should be(Some(1))
    res2.get.id should be(Some(2))
    res3.get.id should be(Some(3))
  }

  test("that withId and withUserId returns the same userdata") {
    this.resetIdSequence()

    val data1 = TestData.emptyDomainUserData.copy(userId = "first", savedSearches = Some(Seq("eple")))
    val data2 = TestData.emptyDomainUserData.copy(userId = "second", latestEditedArticles = Some(Seq("kake")))
    val data3 = TestData.emptyDomainUserData.copy(userId = "third", favoriteSubjects = Some(Seq("bok")))

    repository.insert(data1)
    repository.insert(data2)
    repository.insert(data3)

    repository.withId(1).get should be(repository.withUserId("first").get)
    repository.withId(2).get should be(repository.withUserId("second").get)
    repository.withId(3).get should be(repository.withUserId("third").get)
  }

  test("that updating updates all fields correctly") {
    val initialUserData1 = TestData.emptyDomainUserData.copy(userId = "first")

    val initialUserData2 = TestData.emptyDomainUserData.copy(
      userId = "second",
      savedSearches = Some(Seq("Seiddit", "Emina")),
      latestEditedArticles = Some(Seq("article:6", "article:9")),
      favoriteSubjects = Some(Seq("methematics", "PEBCAK-studies"))
    )

    val inserted1 = repository.insert(initialUserData1)
    val inserted2 = repository.insert(initialUserData2)

    val updatedUserData1 = inserted1.get.copy(savedSearches = Some(Seq("1", "2")),
                                              latestEditedArticles = Some(Seq("3", "4")),
                                              favoriteSubjects = Some(Seq("5", "6")))

    val updatedUserData2 = inserted2.get.copy(savedSearches = Some(Seq("a", "b")),
                                              latestEditedArticles = None,
                                              favoriteSubjects = Some(Seq.empty))

    val res1 = repository.update(updatedUserData1)
    val res2 = repository.update(updatedUserData2)

    res1.get should be(repository.withUserId("first").get)
    res2.get should be(repository.withUserId("second").get)
  }

  test("that userDataCount returns correct amount of entries") {
    val data1 = TestData.emptyDomainUserData.copy(userId = "user1")
    val data2 = TestData.emptyDomainUserData.copy(userId = "user2")
    val data3 = TestData.emptyDomainUserData.copy(userId = "user2")

    repository.userDataCount should be(0)
    repository.insert(data1)
    repository.userDataCount should be(1)
    repository.insert(data2)
    repository.userDataCount should be(2)
    repository.insert(data3) match {
      case Success(_)                => fail()
      case Failure(_: PSQLException) => // ignore results, actually a success
      case Failure(_)                => fail()
    }
    repository.userDataCount should be(2)
  }

}
