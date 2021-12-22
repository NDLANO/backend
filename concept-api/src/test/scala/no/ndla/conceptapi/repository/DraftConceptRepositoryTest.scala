/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.repository

import java.net.Socket
import java.util.Date
import no.ndla.conceptapi.model.domain
import no.ndla.conceptapi.{ConceptApiProperties, DBMigrator, TestData, TestEnvironment, UnitSuite}
import scalikejdbc.{ConnectionPool, DB, DataSourceConnectionPool}
import no.ndla.conceptapi.TestData._
import no.ndla.conceptapi.model.api.OptimisticLockException
import no.ndla.scalatestsuite.IntegrationSuite
import org.scalatest.Outcome

import scala.util.{Failure, Success, Try}
import scalikejdbc._

class DraftConceptRepositoryTest
    extends IntegrationSuite(EnablePostgresContainer = true)
    with UnitSuite
    with TestEnvironment {

  override val dataSource = testDataSource.get
  var repository: DraftConceptRepository = _

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
      sql"delete from conceptdata;".execute()(session)
    })
  }

  override def beforeEach(): Unit = {
    repository = new DraftConceptRepository
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

  test("Inserting and Updating an concept should work as expected") {
    val art1 = domainConcept.copy()
    val art2 = domainConcept.copy()
    val art3 = domainConcept.copy()

    val id1 = repository.insert(art1).id.get
    val id2 = repository.insert(art2).id.get
    val id3 = repository.insert(art3).id.get

    val updatedContent = Seq(domain.ConceptContent("What u do mr", "nb"))
    repository.update(art1.copy(id = Some(id1), content = updatedContent))

    repository.withId(id1).get.content should be(updatedContent)
    repository.withId(id2).get.content should be(art2.content)
    repository.withId(id3).get.content should be(art3.content)
  }

  test("Inserting and fetching with listing id works as expected") {
    val concept1 = domainConcept.copy(title = Seq(domain.ConceptTitle("Really good title", "nb")))
    val concept2 = domainConcept.copy(title = Seq(domain.ConceptTitle("Not so bad title", "nb")))
    val concept3 = domainConcept.copy(title = Seq(domain.ConceptTitle("Whatchu doin", "nb")))

    val insertedConcept1 = repository.insertwithListingId(concept1, 55555)
    val insertedConcept2 = repository.insertwithListingId(concept2, 66666)
    val insertedConcept3 = repository.insertwithListingId(concept3, 77777)

    val result1 = repository.withListingId(55555)
    val expected1 =
      Some(concept1.copy(id = insertedConcept1.id, created = result1.get.created, updated = result1.get.updated))
    result1 should be(expected1)

    val result2 = repository.withListingId(66666)
    val expected2 =
      Some(concept2.copy(id = insertedConcept2.id, created = result2.get.created, updated = result2.get.updated))
    result2 should be(expected2)

    val result3 = repository.withListingId(77777)
    val expected3 =
      Some(concept3.copy(id = insertedConcept3.id, created = result3.get.created, updated = result3.get.updated))
    result3 should be(expected3)
  }

  test("That getting subjects works as expected") {
    val concept1 = TestData.domainConcept.copy(id = Some(1), subjectIds = Set("urn:subject:1", "urn:subject:2"))
    val concept2 = TestData.domainConcept.copy(id = Some(2), subjectIds = Set("urn:subject:1", "urn:subject:19"))
    val concept3 = TestData.domainConcept.copy(id = Some(3), subjectIds = Set("urn:subject:12"))

    repository.insert(concept1)
    repository.insert(concept2)
    repository.insert(concept3)

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

    repository.insert(concept1)
    repository.insert(concept2)
    repository.insert(concept3)

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

  test("getTags returns non-duplicate tags and correct number of them") {
    val sampleArticle1 = TestData.domainConcept.copy(
      tags = Seq(domain.ConceptTags(Seq("abc", "bcd", "ddd"), "nb"), domain.ConceptTags(Seq("abc", "bcd"), "nn")))
    val sampleArticle2 = TestData.domainConcept.copy(
      tags = Seq(domain.ConceptTags(Seq("bcd", "cde"), "nb"), domain.ConceptTags(Seq("bcd", "cde"), "nn")))
    val sampleArticle3 =
      TestData.domainConcept.copy(
        tags = Seq(domain.ConceptTags(Seq("def"), "nb"), domain.ConceptTags(Seq("d", "def", "asd"), "nn")))
    val sampleArticle4 = TestData.domainConcept.copy(tags = Seq.empty)

    repository.insert(sampleArticle1)
    repository.insert(sampleArticle2)
    repository.insert(sampleArticle3)
    repository.insert(sampleArticle4)

    val (tags1, tagsCount1) = repository.getTags("", 5, 0, "nb")
    tags1 should equal(Seq("abc", "bcd", "cde", "ddd", "def"))
    tags1.length should be(5)
    tagsCount1 should be(5)

    val (tags2, tagsCount2) = repository.getTags("", 2, 0, "nb")
    tags2 should equal(Seq("abc", "bcd"))
    tags2.length should be(2)
    tagsCount2 should be(5)

    val (tags3, tagsCount3) = repository.getTags("", 2, 3, "nn")
    tags3 should equal(Seq("cde", "d"))
    tags3.length should be(2)
    tagsCount3 should be(6)

    val (tags4, tagsCount4) = repository.getTags("", 1, 3, "nn")
    tags4 should equal(Seq("cde"))
    tags4.length should be(1)
    tagsCount4 should be(6)

    val (tags5, tagsCount5) = repository.getTags("", 10, 0, "*")
    tags5 should equal(Seq("abc", "asd", "bcd", "cde", "d", "ddd", "def"))
    tags5.length should be(7)
    tagsCount5 should be(7)

    val (tags6, tagsCount6) = repository.getTags("d", 5, 0, "")
    tags6 should equal(Seq("d", "ddd", "def"))
    tags6.length should be(3)
    tagsCount6 should be(3)

    val (tags7, tagsCount7) = repository.getTags("%b", 5, 0, "")
    tags7 should equal(Seq("bcd"))
    tags7.length should be(1)
    tagsCount7 should be(1)

    val (tags8, tagsCount8) = repository.getTags("a", 10, 0, "")
    tags8 should equal(Seq("abc", "asd"))
    tags8.length should be(2)
    tagsCount8 should be(2)

    val (tags9, tagsCount9) = repository.getTags("A", 10, 0, "")
    tags9 should equal(Seq("abc", "asd"))
    tags9.length should be(2)
    tagsCount9 should be(2)
  }

  test("Revision mismatch fail with optimistic lock exception") {
    val art1 = domainConcept.copy(
      revision = None,
      content = Seq(domain.ConceptContent("Originalpls", "nb")),
      created = new Date(0),
      updated = new Date(0)
    )

    val insertedConcept = repository.insert(art1)
    val insertedId = insertedConcept.id.get

    repository.withId(insertedId).get.revision should be(Some(1))

    val updatedContent = Seq(domain.ConceptContent("Updatedpls", "nb"))
    val updatedArt1 = art1.copy(revision = Some(10), id = Some(insertedId), content = updatedContent)

    val updateResult1 = repository.update(updatedArt1)
    updateResult1 should be(Failure(new OptimisticLockException))

    val fetched1 = repository.withId(insertedId).get
    fetched1 should be(insertedConcept)

    val updatedArt2 = fetched1.copy(content = updatedContent)
    val updateResult2 = repository.update(updatedArt2)
    updateResult2 should be(Success(updatedArt2.copy(revision = Some(2))))

    val fetched2 = repository.withId(insertedId).get
    fetched2.revision should be(Some(2))
    fetched2.content should be(updatedContent)
  }

  test("That getByPage returns all concepts in database") {
    val con1 = domainConcept.copy(
      content = Seq(domain.ConceptContent("Hei", "nb")),
      updated = new Date(0),
      created = new Date(0)
    )
    val con2 = domainConcept.copy(
      content = Seq(domain.ConceptContent("På", "nb")),
      updated = new Date(0),
      created = new Date(0),
    )
    val con3 = domainConcept.copy(
      content = Seq(domain.ConceptContent("Deg", "nb")),
      updated = new Date(0),
      created = new Date(0)
    )

    val ins1 = repository.insert(con1)
    val ins2 = repository.insert(con2)
    val ins3 = repository.insert(con3)

    repository.getByPage(10, 0).sortBy(_.id.get) should be(Seq(ins1, ins2, ins3))
  }

}
