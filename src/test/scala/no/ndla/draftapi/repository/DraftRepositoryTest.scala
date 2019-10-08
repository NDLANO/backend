/*
 * Part of NDLA draft_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.repository

import java.net.Socket

import no.ndla.draftapi.model.domain._
import no.ndla.draftapi._
import no.ndla.draftapi.model.domain
import org.joda.time.DateTime
import scalikejdbc._

import scala.util.{Success, Try}

class DraftRepositoryTest extends IntegrationSuite with TestEnvironment {
  var repository: ArticleRepository = _

  val sampleArticle: Article = TestData.sampleArticleWithByNcSa

  def emptyTestDatabase = {
    DB autoCommit (implicit session => {
      sql"delete from draftapitest.articledata;".execute.apply()(session)
    })
  }

  def serverIsListening: Boolean = {
    Try(new Socket(DraftApiProperties.MetaServer, DraftApiProperties.MetaPort)) match {
      case Success(c) =>
        c.close()
        true
      case _ => false
    }
  }
  def databaseIsAvailable: Boolean = Try(repository.articleCount).isSuccess

  override def beforeEach(): Unit = {
    repository = new ArticleRepository()
    if (databaseIsAvailable) {
      emptyTestDatabase
    }
  }

  override def beforeAll(): Unit = {
    Try {
      val datasource = testDataSource.get
      if (serverIsListening) {
        ConnectionPool.singleton(new DataSourceConnectionPool(datasource))
        DBMigrator.migrate(datasource)
      }
    }
  }

  test("Fetching external ids works as expected") {
    assume(databaseIsAvailable, "Database is unavailable")
    val externalIds = List("1", "2", "3")
    val idWithExternals = 1
    val idWithoutExternals = 2
    repository.insertWithExternalIds(sampleArticle.copy(id = Some(idWithExternals)), externalIds, List.empty, None)
    repository.insertWithExternalIds(sampleArticle.copy(id = Some(idWithoutExternals)), List.empty, List.empty, None)

    val result1 = repository.getExternalIdsFromId(idWithExternals)
    result1 should be(externalIds)
    val result2 = repository.getExternalIdsFromId(idWithoutExternals)
    result2 should be(List.empty)
  }

  test("withId only returns non-archieved articles") {
    assume(databaseIsAvailable, "Database is unavailable")
    repository.insert(sampleArticle.copy(id = Some(1), status = domain.Status(domain.ArticleStatus.DRAFT, Set.empty)))
    repository.insert(
      sampleArticle.copy(id = Some(2), status = domain.Status(domain.ArticleStatus.ARCHIVED, Set.empty)))

    repository.withId(1).isDefined should be(true)
    repository.withId(2).isDefined should be(false)
  }

  test("that importIdOfArticle works correctly") {
    assume(databaseIsAvailable, "Database is unavailable")
    val externalIds = List("1", "2", "3")
    val uuid = "d4e84cd3-ab94-46d5-9839-47ec682d27c2"
    val id1 = 1
    val id2 = 2
    repository.insertWithExternalIds(sampleArticle.copy(id = Some(id1)), externalIds, List.empty, Some(uuid))
    repository.insertWithExternalIds(sampleArticle.copy(id = Some(id2)), List.empty, List.empty, Some(uuid))

    val result1 = repository.importIdOfArticle("1")
    result1.get should be(ImportId(Some(uuid)))
    val result2 = repository.importIdOfArticle("2")
    result2.get should be(ImportId(Some(uuid)))

    repository.deleteArticle(id1)
    repository.deleteArticle(id2)
  }

  test("ExternalIds should not contains NULLs") {
    assume(databaseIsAvailable, "Database is unavailable")
    val art1 = sampleArticle.copy(id = Some(10))
    repository.insertWithExternalIds(art1, null, List.empty, None)
    val result1 = repository.getExternalIdsFromId(10)

    result1 should be(List.empty)
  }

  test("Updating an article should work as expected") {
    assume(databaseIsAvailable, "Database is unavailable")
    val art1 = sampleArticle.copy(id = Some(1), status = domain.Status(domain.ArticleStatus.DRAFT, Set.empty))
    val art2 = sampleArticle.copy(id = Some(2), status = domain.Status(domain.ArticleStatus.DRAFT, Set.empty))
    val art3 = sampleArticle.copy(id = Some(3), status = domain.Status(domain.ArticleStatus.DRAFT, Set.empty))
    val art4 = sampleArticle.copy(id = Some(4), status = domain.Status(domain.ArticleStatus.DRAFT, Set.empty))

    repository.insert(art1)
    repository.insert(art2)
    repository.insert(art3)
    repository.insert(art4)

    val updatedContent = Seq(ArticleContent("What u do mr", "nb"))

    repository.updateArticle(art1.copy(content = updatedContent))

    repository.withId(art1.id.get).get.content should be(updatedContent)
    repository.withId(art2.id.get).get.content should be(art2.content)
    repository.withId(art3.id.get).get.content should be(art3.content)
    repository.withId(art4.id.get).get.content should be(art4.content)
  }

  test("That storing an article an retrieving it returns the original article") {
    assume(databaseIsAvailable, "Database is unavailable")
    val art1 = sampleArticle.copy(id = Some(1), status = domain.Status(domain.ArticleStatus.DRAFT, Set.empty))
    val art2 = sampleArticle.copy(id = Some(2), status = domain.Status(domain.ArticleStatus.PUBLISHED, Set.empty))
    val art3 = sampleArticle.copy(id = Some(3),
                                  status = domain.Status(domain.ArticleStatus.AWAITING_QUALITY_ASSURANCE, Set.empty))
    val art4 = sampleArticle.copy(id = Some(4), status = domain.Status(domain.ArticleStatus.DRAFT, Set.empty))

    repository.insert(art1)
    repository.insertWithExternalIds(art2, List("1234", "5678"), List.empty, None)
    repository.insert(art3)
    repository.insert(art4)

    repository.withId(art1.id.get).get should be(art1)
    repository.withId(art2.id.get).get should be(art2)
    repository.withId(art3.id.get).get should be(art3)
    repository.withId(art4.id.get).get should be(art4)
  }

  test("That updateWithExternalIds updates article correctly") {
    assume(databaseIsAvailable, "Database is unavailable")
    val art1 = sampleArticle.copy(id = Some(1), status = domain.Status(domain.ArticleStatus.DRAFT, Set.empty))
    repository.insertWithExternalIds(art1, List("1234", "5678"), List.empty, None)

    val updatedContent = Seq(ArticleContent("This is updated with external ids yo", "en"))
    val updatedArt = art1.copy(content = updatedContent)
    repository.updateWithExternalIds(updatedArt, List("1234", "5678"), List.empty, None)
    repository.withId(art1.id.get).get should be(updatedArt)
  }

  test("That getAllIds returns all articles") {
    assume(databaseIsAvailable, "Database is unavailable")
    val art1 = sampleArticle.copy(id = Some(1), status = domain.Status(domain.ArticleStatus.DRAFT, Set.empty))
    val art2 = sampleArticle.copy(id = Some(2), status = domain.Status(domain.ArticleStatus.PUBLISHED, Set.empty))
    val art3 = sampleArticle.copy(id = Some(3), status = domain.Status(domain.ArticleStatus.USER_TEST, Set.empty))
    val art4 = sampleArticle.copy(id = Some(4), status = domain.Status(domain.ArticleStatus.DRAFT, Set.empty))

    repository.insert(art1)
    repository.insertWithExternalIds(art2, List("1234", "5678"), List.empty, None)
    repository.insert(art3)
    repository.insert(art4)

    repository.getAllIds should be(
      Seq(
        domain.ArticleIds(art1.id.get, List.empty),
        domain.ArticleIds(art2.id.get, List("1234", "5678")),
        domain.ArticleIds(art3.id.get, List.empty),
        domain.ArticleIds(art4.id.get, List.empty),
      ))
  }

  test("that getIdFromExternalId returns id of article correctly") {
    assume(databaseIsAvailable, "Database is unavailable")
    val art1 = sampleArticle.copy(id = Some(14), status = domain.Status(domain.ArticleStatus.DRAFT, Set.empty))
    repository.insert(art1)
    repository.insertWithExternalIds(art1.copy(revision = Some(3)), List("5678"), List.empty, None)

    repository.getIdFromExternalId("5678") should be(art1.id)
    repository.getIdFromExternalId("9999") should be(None)
  }

  test("That newArticleId creates the latest available article_id") {
    assume(databaseIsAvailable, "Database is unavailable")
    repository.insert(sampleArticle.copy(id = Some(1), status = domain.Status(domain.ArticleStatus.DRAFT, Set.empty)))
    repository.insert(sampleArticle.copy(id = Some(1), status = domain.Status(domain.ArticleStatus.DRAFT, Set.empty)))
    repository.insert(sampleArticle.copy(id = Some(2), status = domain.Status(domain.ArticleStatus.DRAFT, Set.empty)))
    repository.insert(sampleArticle.copy(id = Some(3), status = domain.Status(domain.ArticleStatus.DRAFT, Set.empty)))
    repository.insert(sampleArticle.copy(id = Some(4), status = domain.Status(domain.ArticleStatus.DRAFT, Set.empty)))
    repository.insert(sampleArticle.copy(id = Some(5), status = domain.Status(domain.ArticleStatus.DRAFT, Set.empty)))

    repository.newArticleId() should be(Success(6))
  }

  test("That idsWithStatus returns correct drafts") {
    assume(databaseIsAvailable, "Database is unavailable")
    repository.insert(sampleArticle.copy(id = Some(1), status = domain.Status(domain.ArticleStatus.DRAFT, Set.empty)))
    repository.insert(sampleArticle.copy(id = Some(2), status = domain.Status(domain.ArticleStatus.DRAFT, Set.empty)))
    repository.insert(
      sampleArticle.copy(id = Some(3), status = domain.Status(domain.ArticleStatus.PROPOSAL, Set.empty)))
    repository.insert(sampleArticle.copy(id = Some(4), status = domain.Status(domain.ArticleStatus.DRAFT, Set.empty)))
    repository.insertWithExternalIds(
      sampleArticle.copy(id = Some(5), status = domain.Status(domain.ArticleStatus.PROPOSAL, Set.empty)),
      List("1234"),
      List.empty,
      None)
    repository.insert(
      sampleArticle.copy(id = Some(6), status = domain.Status(domain.ArticleStatus.PUBLISHED, Set.empty)))
    repository.insert(
      sampleArticle.copy(id = Some(7), status = domain.Status(domain.ArticleStatus.QUEUED_FOR_PUBLISHING, Set.empty)))
    repository.insertWithExternalIds(
      sampleArticle.copy(id = Some(8), status = domain.Status(domain.ArticleStatus.PROPOSAL, Set.empty)),
      List("5678", "1111"),
      List.empty,
      None)

    repository.idsWithStatus(ArticleStatus.DRAFT) should be(
      Success(List(ArticleIds(1, List.empty), ArticleIds(2, List.empty), ArticleIds(4, List.empty))))

    repository.idsWithStatus(ArticleStatus.PROPOSAL) should be(
      Success(List(ArticleIds(3, List.empty), ArticleIds(5, List("1234")), ArticleIds(8, List("5678", "1111")))))

    repository.idsWithStatus(ArticleStatus.PUBLISHED) should be(Success(List(ArticleIds(6, List.empty))))

    repository.idsWithStatus(ArticleStatus.QUEUED_FOR_PUBLISHING) should be(Success(List(ArticleIds(7, List.empty))))
  }

  test("That getArticlesByPage returns all latest articles") {
    assume(databaseIsAvailable, "Database is unavailable")
    val art1 = sampleArticle.copy(id = Some(1), status = domain.Status(domain.ArticleStatus.DRAFT, Set.empty))
    val art2 = sampleArticle.copy(id = Some(1),
                                  revision = Some(2),
                                  status = domain.Status(domain.ArticleStatus.DRAFT, Set.empty))
    val art3 = sampleArticle.copy(id = Some(2), status = domain.Status(domain.ArticleStatus.DRAFT, Set.empty))
    val art4 = sampleArticle.copy(id = Some(3), status = domain.Status(domain.ArticleStatus.DRAFT, Set.empty))
    val art5 = sampleArticle.copy(id = Some(4), status = domain.Status(domain.ArticleStatus.DRAFT, Set.empty))
    val art6 = sampleArticle.copy(id = Some(5), status = domain.Status(domain.ArticleStatus.DRAFT, Set.empty))
    repository.insert(art1)
    repository.insert(art2)
    repository.insert(art3)
    repository.insert(art4)
    repository.insert(art5)
    repository.insert(art6)

    val pageSize = 4
    repository.getArticlesByPage(pageSize, pageSize * 0) should be(
      Seq(
        art2,
        art3,
        art4,
        art5
      ))
    repository.getArticlesByPage(pageSize, pageSize * 1) should be(
      Seq(
        art6
      ))
  }

  test("published article creates new db version and bumps revision by two") {
    assume(databaseIsAvailable, "Database is unavailable")
    val article = TestData.sampleDomainArticle.copy(status = domain.Status(domain.ArticleStatus.UNPUBLISHED, Set.empty),
                                                    revision = Some(3))
    repository.insert(article)
    val oldCount = repository.articlesWithId(article.id.get).size
    val publishedArticle = article.copy(status = domain.Status(domain.ArticleStatus.PUBLISHED, Set.empty))
    val updatedArticle = repository.updateArticle(publishedArticle).get
    updatedArticle.revision should be(Some(5))

    updatedArticle.notes.length should be(0)
    updatedArticle should equal(publishedArticle.copy(notes = Seq(), revision = Some(5)))

    val count = repository.articlesWithId(article.id.get).size
    count should be(oldCount + 1)

  }

  test("published article keeps revison on import") {
    assume(databaseIsAvailable, "Database is unavailable")
    val article = TestData.sampleDomainArticle.copy(status = domain.Status(domain.ArticleStatus.IMPORTED, Set.empty),
                                                    revision = Some(1))
    repository.insert(article)
    val oldCount = repository.articlesWithId(article.id.get).size
    val publishedArticle = article.copy(status = domain.Status(domain.ArticleStatus.PUBLISHED, Set.empty))
    val updatedArticle = repository.updateArticle(publishedArticle, isImported = true).get
    updatedArticle.revision should be(Some(1))

    updatedArticle.notes.length should be(0)
    updatedArticle should equal(publishedArticle.copy(notes = Seq(), revision = Some(1)))

    val count = repository.articlesWithId(article.id.get).size
    count should be(oldCount)

  }

  test("published article keeps old notes in hidden field and notes is emptied") {
    assume(databaseIsAvailable, "Database is unavailable")
    val timeToFreeze = new DateTime().withMillisOfSecond(0)
    withFrozenTime(timeToFreeze) {
      val status = domain.Status(domain.ArticleStatus.DRAFT, Set.empty)
      val prevNotes1 = Seq(
        domain.EditorNote("Note1", "SomeId", status, new DateTime().toDate),
        domain.EditorNote("Note2", "SomeId", status, new DateTime().toDate),
        domain.EditorNote("Note3", "SomeId", status, new DateTime().toDate),
        domain.EditorNote("Note4", "SomeId", status, new DateTime().toDate)
      )

      val prevNotes2 = Seq(
        domain.EditorNote("Note5", "SomeId", status, new DateTime().toDate),
        domain.EditorNote("Note6", "SomeId", status, new DateTime().toDate),
        domain.EditorNote("Note7", "SomeId", status, new DateTime().toDate),
        domain.EditorNote("Note8", "SomeId", status, new DateTime().toDate)
      )
      val draftArticle1 = TestData.sampleDomainArticle.copy(
        status = domain.Status(domain.ArticleStatus.UNPUBLISHED, Set.empty),
        revision = Some(3),
        notes = prevNotes1
      )

      val inserted = repository.insert(draftArticle1)
      val fetched = repository.withId(inserted.id.get).get
      fetched.notes should be(prevNotes1)
      fetched.previousVersionNotes should be(Seq.empty)

      val toPublish1 = inserted.copy(status = domain.Status(domain.ArticleStatus.PUBLISHED, Set.empty))
      val updatedArticle1 = repository.updateArticle(toPublish1).get
      updatedArticle1.notes should be(Seq.empty)
      updatedArticle1.previousVersionNotes should be(prevNotes1)

      val draftArticle2 =
        updatedArticle1.copy(status = domain.Status(domain.ArticleStatus.DRAFT, Set.empty), notes = prevNotes2)
      val updatedArticle2 = repository.updateArticle(draftArticle2).get
      updatedArticle2.notes should be(prevNotes2)
      updatedArticle2.previousVersionNotes should be(prevNotes1)

      val publishedArticle2 = updatedArticle2.copy(status = domain.Status(domain.ArticleStatus.PUBLISHED, Set.empty))
      val updatedArticle3 = repository.updateArticle(publishedArticle2).get
      updatedArticle3.notes should be(Seq.empty)
      updatedArticle3.previousVersionNotes should be(prevNotes1 ++ prevNotes2)
    }

  }
}
