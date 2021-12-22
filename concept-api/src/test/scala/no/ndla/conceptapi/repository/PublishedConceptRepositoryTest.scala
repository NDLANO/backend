/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.repository

import java.net.Socket
import java.util.Date
import no.ndla.conceptapi._
import no.ndla.conceptapi.model.domain
import no.ndla.conceptapi.model.domain.PublishedConcept
import no.ndla.scalatestsuite.IntegrationSuite
import org.scalatest.Outcome
import scalikejdbc.{ConnectionPool, DB, DataSourceConnectionPool, _}

import scala.util.{Failure, Success, Try}

class PublishedConceptRepositoryTest extends IntegrationSuite(EnablePostgresContainer = true) with TestEnvironment {

  override val dataSource = testDataSource.get
  var repository: PublishedConceptRepository = _

  // Skip tests if no docker environment available
  override def withFixture(test: NoArgTest): Outcome = {
    postgresContainer match {
      case Failure(ex) =>
        println(s"Postgres container not running, cancelling '${this.getClass.getName}'")
        println(s"Got exception: ${ex.getMessage}")
        ex.printStackTrace()
      case _ =>
    }
    if (!sys.env.getOrElse("CI", "false").toBoolean) {
      assume(postgresContainer.isSuccess, "Docker environment unavailable for postgres container")
    }
    super.withFixture(test)
  }

  def emptyTestDatabase = {
    DB autoCommit (implicit session => {
      sql"delete from ${PublishedConcept.table};".execute()(session)
    })
  }

  override def beforeEach(): Unit = {
    repository = new PublishedConceptRepository
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

  def serverIsListening: Boolean = {
    Try(new Socket(ConceptApiProperties.MetaServer, ConceptApiProperties.MetaPort)) match {
      case Success(c) =>
        c.close()
        true
      case _ => false
    }
  }

  test("That inserting and updating works") {
    val consistentDate = new Date(0)
    val concept1 = TestData.domainConcept.copy(
      id = Some(10),
      title = Seq(domain.ConceptTitle("Yes", "nb")),
      created = consistentDate,
      updated = consistentDate
    )
    val concept2 = TestData.domainConcept.copy(
      id = Some(10),
      title = Seq(domain.ConceptTitle("No", "nb")),
      created = consistentDate,
      updated = consistentDate
    )
    val concept3 = TestData.domainConcept.copy(
      id = Some(11),
      title = Seq(domain.ConceptTitle("Yolo", "nb")),
      created = consistentDate,
      updated = consistentDate
    )

    repository.insertOrUpdate(concept1)
    repository.insertOrUpdate(concept3)
    repository.withId(10) should be(Some(concept1))
    repository.withId(11) should be(Some(concept3))

    repository.insertOrUpdate(concept2)
    repository.withId(10) should be(Some(concept2))
    repository.withId(11) should be(Some(concept3))
  }

  test("That deletion works as expected") {
    val consistentDate = new Date(0)
    val concept1 = TestData.domainConcept.copy(
      id = Some(10),
      title = Seq(domain.ConceptTitle("Yes", "nb")),
      created = consistentDate,
      updated = consistentDate
    )
    val concept2 = TestData.domainConcept.copy(
      id = Some(11),
      title = Seq(domain.ConceptTitle("Yolo", "nb")),
      created = consistentDate,
      updated = consistentDate
    )

    repository.insertOrUpdate(concept1)
    repository.insertOrUpdate(concept2)
    repository.withId(10) should be(Some(concept1))
    repository.withId(11) should be(Some(concept2))

    repository.delete(10).isSuccess should be(true)

    repository.withId(10) should be(None)
    repository.withId(11) should be(Some(concept2))

    repository.delete(10).isSuccess should be(false)
  }

  test("That getting subjects works as expected") {
    val concept1 = TestData.domainConcept.copy(id = Some(1), subjectIds = Set("urn:subject:1", "urn:subject:2"))
    val concept2 = TestData.domainConcept.copy(id = Some(2), subjectIds = Set("urn:subject:1", "urn:subject:19"))
    val concept3 = TestData.domainConcept.copy(id = Some(3), subjectIds = Set("urn:subject:12"))

    repository.insertOrUpdate(concept1)
    repository.insertOrUpdate(concept2)
    repository.insertOrUpdate(concept3)

    repository.allSubjectIds should be(
      Set(
        "urn:subject:1",
        "urn:subject:2",
        "urn:subject:12",
        "urn:subject:19"
      )
    )
  }

  test("Fetching concepts tags works as expected") {
    val concept1 =
      TestData.domainConcept.copy(
        id = Some(1),
        tags = Seq(
          domain.ConceptTags(Seq("konge", "bror"), "nb"),
          domain.ConceptTags(Seq("konge", "brur"), "nn"),
          domain.ConceptTags(Seq("king", "bro"), "en"),
          domain.ConceptTags(Seq("zing", "xiongdi"), "zh")
        )
      )
    val concept2 =
      TestData.domainConcept.copy(
        id = Some(2),
        tags = Seq(
          domain.ConceptTags(Seq("konge", "lol", "meme"), "nb"),
          domain.ConceptTags(Seq("konge", "lel", "meem"), "nn"),
          domain.ConceptTags(Seq("king", "lul", "maymay"), "en"),
          domain.ConceptTags(Seq("zing", "kek", "mimi"), "zh")
        )
      )
    val concept3 =
      TestData.domainConcept.copy(
        id = Some(3),
        tags = Seq()
      )

    repository.insertOrUpdate(concept1)
    repository.insertOrUpdate(concept2)
    repository.insertOrUpdate(concept3)

    repository.everyTagFromEveryConcept should be(
      List(
        List(
          domain.ConceptTags(Seq("konge", "bror"), "nb"),
          domain.ConceptTags(Seq("konge", "brur"), "nn"),
          domain.ConceptTags(Seq("king", "bro"), "en"),
          domain.ConceptTags(Seq("zing", "xiongdi"), "zh"),
        ),
        List(
          domain.ConceptTags(Seq("konge", "lol", "meme"), "nb"),
          domain.ConceptTags(Seq("konge", "lel", "meem"), "nn"),
          domain.ConceptTags(Seq("king", "lul", "maymay"), "en"),
          domain.ConceptTags(Seq("zing", "kek", "mimi"), "zh")
        )
      )
    )
  }

  test("That count works as expected") {
    val consistentDate = new Date(0)
    val concept1 = TestData.domainConcept.copy(
      id = Some(10),
      created = consistentDate,
      updated = consistentDate
    )
    val concept2 = TestData.domainConcept.copy(
      id = Some(11),
      created = consistentDate,
      updated = consistentDate
    )
    val concept3 = TestData.domainConcept.copy(
      id = Some(11),
      created = consistentDate,
      updated = consistentDate
    )
    val concept4 = TestData.domainConcept.copy(
      id = Some(12),
      created = consistentDate,
      updated = consistentDate
    )
    repository.conceptCount should be(0)

    repository.insertOrUpdate(concept1)
    repository.conceptCount should be(1)

    repository.insertOrUpdate(concept2)
    repository.conceptCount should be(2)

    repository.insertOrUpdate(concept3)
    repository.conceptCount should be(2)

    repository.insertOrUpdate(concept4)
    repository.conceptCount should be(3)
  }

  test("That getByPage returns all concepts in database") {
    val con1 = TestData.domainConcept.copy(
      id = Some(1),
      content = Seq(domain.ConceptContent("Hei", "nb")),
      updated = new Date(0),
      created = new Date(0)
    )
    val con2 = TestData.domainConcept.copy(
      id = Some(2),
      revision = Some(100),
      content = Seq(domain.ConceptContent("På", "nb")),
      updated = new Date(0),
      created = new Date(0)
    )
    val con3 = TestData.domainConcept.copy(
      id = Some(3),
      content = Seq(domain.ConceptContent("Deg", "nb")),
      updated = new Date(0),
      created = new Date(0)
    )

    val Success(ins1) = repository.insertOrUpdate(con1)
    val Success(ins2) = repository.insertOrUpdate(con2)
    val Success(ins3) = repository.insertOrUpdate(con3)

    repository.getByPage(10, 0).sortBy(_.id.get) should be(Seq(ins1, ins2, ins3))
  }

}
