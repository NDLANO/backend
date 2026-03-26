/*
 * Part of NDLA frontpage-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.frontpageapi.repository

import no.ndla.common.model.domain.frontpage.SubjectPage
import no.ndla.database.{DBMigrator, DBUtility, DataSource}
import no.ndla.frontpageapi.model.domain.DBSubjectPage
import no.ndla.frontpageapi.{TestData, TestEnvironment}
import no.ndla.scalatestsuite.DatabaseIntegrationSuite
import org.mockito.Mockito.{spy, times, verify}
import org.mockito.ArgumentMatchers.any
import scalikejdbc.*

import java.net.Socket
import scala.util.{Success, Try}

class SubjectPageRepositoryTest extends DatabaseIntegrationSuite with TestEnvironment {
  override implicit lazy val dbUtility: DBUtility         = new DBUtility
  override implicit lazy val dataSource: DataSource       = testDataSource.get
  override implicit lazy val migrator: DBMigrator         = new DBMigrator
  override implicit lazy val dBSubjectPage: DBSubjectPage = new DBSubjectPage
  var repository: SubjectPageRepository                   = scala.compiletime.uninitialized

  def emptyTestDatabase(): Unit = dbUtility.writeSession(implicit session => {
    sql"delete from subjectpage;".execute()(using session)
  })

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
    repository = new SubjectPageRepository()
    if (serverIsListening) {
      emptyTestDatabase()
    }
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    dataSource.connectToDatabase()
    if (serverIsListening) {
      migrator.migrate()
    }
  }

  private def insertSubject(externalId: String, name: String = "Samfunnsfag"): SubjectPage = {
    val toInsert = TestData.domainSubjectPage.copy(id = None, name = name)
    repository.newSubjectPage(toInsert, externalId).failIfFailure
  }

  test("withId should return None if no subject page exists") {
    repository.withId(1234L) should be(Success(None))
  }

  test("newSubjectPage inserts the page and assigns an id retrievable via withId") {
    val inserted = insertSubject("ext-1")
    inserted.id should not be empty
    repository.withId(inserted.id.get).failIfFailure should be(Some(inserted))
  }

  test("getIdFromExternalId returns the id of an inserted page, or None when missing") {
    val inserted = insertSubject("external-42")
    repository.getIdFromExternalId("external-42").failIfFailure should be(Some(inserted.id.get))
    repository.getIdFromExternalId("does-not-exist").failIfFailure should be(None)
  }

  test("exists reflects whether a subject page id is stored") {
    val inserted = insertSubject("ext-exists")
    repository.exists(inserted.id.get).failIfFailure should be(true)
    repository.exists(inserted.id.get + 9999L).failIfFailure should be(false)
  }

  test("updateSubjectPage replaces the stored document for that id") {
    val inserted = insertSubject("ext-update")
    val updated  = inserted.copy(name = "Endret navn")

    repository.updateSubjectPage(updated).failIfFailure should be(updated)
    repository.withId(inserted.id.get).failIfFailure should be(Some(updated))
  }

  test("totalCount returns the number of stored subject pages") {
    repository.totalCount.failIfFailure should be(0L)
    insertSubject("ext-a")
    insertSubject("ext-b")
    insertSubject("ext-c")
    repository.totalCount.failIfFailure should be(3L)
  }

  test("all returns subject pages ordered by id, with offset and limit applied") {
    val a = insertSubject("ext-a", "A")
    val b = insertSubject("ext-b", "B")
    val c = insertSubject("ext-c", "C")

    repository.all(offset = 0, limit = 10).failIfFailure should be(List(a, b, c))
    repository.all(offset = 1, limit = 1).failIfFailure should be(List(b))
    repository.all(offset = 3, limit = 10).failIfFailure should be(List.empty)
  }

  test("withIds returns only the requested subject pages") {
    val a = insertSubject("ext-a", "A")
    val b = insertSubject("ext-b", "B")
    val c = insertSubject("ext-c", "C")

    repository
      .withIds(List(a.id.get, c.id.get), offset = 0, pageSize = 10)
      .failIfFailure should contain theSameElementsAs List(a, c)

    repository.withIds(List(b.id.get), offset = 1, pageSize = 10).failIfFailure should be(List.empty)
  }

  test("subjectPageIterator yields every stored subject page exactly once") {
    val pages = (
      1 to 5
    ).map(i => insertSubject(s"ext-$i", s"Subject $i")).toList

    // Drain the iterator inside the read-only session so DB fetches happen while it's open.
    val collected: List[SubjectPage] = dbUtility.readOnly { implicit session =>
      repository.subjectPageIterator.map(_.failIfFailure).toList
    }

    collected should contain theSameElementsAs pages
  }

  test("subjectPageIterator on an empty table yields no elements") {
    val collected: List[SubjectPage] = dbUtility.readOnly { implicit session =>
      repository.subjectPageIterator.map(_.failIfFailure).toList
    }

    collected should be(List.empty)
  }

  test("subjectPageIterator only calls database once for each page") {
    val repositorySpy = spy(new SubjectPageRepository())
    repository = repositorySpy

    verify(repositorySpy, times(0)).all(any, any)(using any)

    val pages = (
      1 to 5
    ).map(i => insertSubject(s"ext-$i", s"Subject $i")).toList

    val collected: List[SubjectPage] = dbUtility.readOnly { implicit session =>
      repository.subjectPageIterator.map(_.failIfFailure).toList
    }

    collected should contain theSameElementsAs pages
    verify(repositorySpy, times(1)).all(any, any)(using any)
  }
}
