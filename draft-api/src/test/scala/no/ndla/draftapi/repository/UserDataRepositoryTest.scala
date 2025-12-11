/*
 * Part of NDLA draft-api
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.repository

import no.ndla.database.{DBMigrator, DataSource}
import no.ndla.draftapi.model.api.SavedSearchDTO

import java.net.Socket
import no.ndla.draftapi.{TestData, TestEnvironment}
import no.ndla.scalatestsuite.DatabaseIntegrationSuite
import scalikejdbc.*

import scala.util.{Success, Try}

class UserDataRepositoryTest extends DatabaseIntegrationSuite with TestEnvironment {
  override implicit lazy val dataSource: DataSource = testDataSource.get
  override implicit lazy val migrator: DBMigrator   = new DBMigrator
  var repository: UserDataRepository                = scala.compiletime.uninitialized

  def emptyTestDatabase: Boolean = {
    DB autoCommit (implicit session => {
      sql"delete from userdata;".execute()(using session)
    })
  }

  private def resetIdSequence() = {
    DB autoCommit (implicit session => {
      sql"select setval('userdata_id_seq', 1, false);".execute()
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
    repository = new UserDataRepository
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

  test("that inserting records to database is generating id as expected") {
    this.resetIdSequence()

    val data1 = TestData.emptyDomainUserData.copy(userId = "user1")
    val data2 = TestData.emptyDomainUserData.copy(userId = "user2")
    val data3 = TestData.emptyDomainUserData.copy(userId = "user3")

    val res1 = repository.insert(data1)(using AutoSession)
    val res2 = repository.insert(data2)(using AutoSession)
    val res3 = repository.insert(data3)(using AutoSession)

    res1.get.id should be(Some(1))
    res2.get.id should be(Some(2))
    res3.get.id should be(Some(3))
  }

  test("that withId and withUserId returns the same userdata") {
    this.resetIdSequence()

    val data1 = TestData
      .emptyDomainUserData
      .copy(userId = "first", savedSearches = Some(Seq(SavedSearchDTO("eple", "eple"))))
    val data2 = TestData.emptyDomainUserData.copy(userId = "second", latestEditedArticles = Some(Seq("kake")))
    val data3 = TestData.emptyDomainUserData.copy(userId = "third", favoriteSubjects = Some(Seq("bok")))

    repository.insert(data1)(using AutoSession).get
    repository.insert(data2)(using AutoSession).get
    repository.insert(data3)(using AutoSession).get

    repository.withId(1)(using AutoSession).get.get should be(repository.withUserId("first")(using AutoSession).get.get)
    repository.withId(2)(using AutoSession).get.get should be(
      repository.withUserId("second")(using AutoSession).get.get
    )
    repository.withId(3)(using AutoSession).get.get should be(repository.withUserId("third")(using AutoSession).get.get)
  }

  test("that updating updates all fields correctly") {
    val initialUserData1 = TestData.emptyDomainUserData.copy(userId = "first")

    val initialUserData2 = TestData
      .emptyDomainUserData
      .copy(
        userId = "second",
        savedSearches = Some(Seq(SavedSearchDTO("Seiddit", "Seiddit"), SavedSearchDTO("Emina", "Emina"))),
        latestEditedArticles = Some(Seq("article:6", "article:9")),
        favoriteSubjects = Some(Seq("methematics", "PEBCAK-studies")),
      )

    val inserted1 = repository.insert(initialUserData1)(using AutoSession)
    val inserted2 = repository.insert(initialUserData2)(using AutoSession)

    val updatedUserData1 = inserted1
      .get
      .copy(
        savedSearches = Some(Seq(SavedSearchDTO("1", "1"), SavedSearchDTO("2", "2"))),
        latestEditedArticles = Some(Seq("3", "4")),
        favoriteSubjects = Some(Seq("5", "6")),
      )

    val updatedUserData2 = inserted2
      .get
      .copy(
        savedSearches = Some(Seq(SavedSearchDTO("a", "a"), SavedSearchDTO("b", "b"))),
        latestEditedArticles = None,
        favoriteSubjects = Some(Seq.empty),
      )

    val res1 = repository.update(updatedUserData1)(using AutoSession)
    val res2 = repository.update(updatedUserData2)(using AutoSession)

    res1.get should be(repository.withUserId("first")(using AutoSession).get.get)
    res2.get should be(repository.withUserId("second")(using AutoSession).get.get)
  }
}
