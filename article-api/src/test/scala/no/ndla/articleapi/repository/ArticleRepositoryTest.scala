/*
 * Part of NDLA article-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.repository

import cats.implicits.*
import no.ndla.articleapi.*
import no.ndla.articleapi.model.domain.{ArticleIds, DBArticle}
import no.ndla.common.model.domain.Tag
import no.ndla.common.model.domain.article.Article
import no.ndla.database.{DBMigrator, DataSource}
import no.ndla.scalatestsuite.DatabaseIntegrationSuite
import scalikejdbc.{AutoSession, DB, ReadOnlyAutoSession}
import org.scalatest.EitherValues.convertEitherToValuable

import java.net.Socket
import scala.util.{Success, Try}

class ArticleRepositoryTest extends DatabaseIntegrationSuite with UnitSuite with TestEnvironment {
  override implicit lazy val dbArticle: DBArticle   = new DBArticle
  override implicit lazy val dataSource: DataSource = testDataSource.get
  override implicit lazy val migrator: DBMigrator   = new DBMigrator
  var repository: ArticleRepository                 = scala.compiletime.uninitialized

  lazy val sampleArticle: Article = TestData.sampleArticleWithByNcSa

  def serverIsListening: Boolean = {
    Try(new Socket(props.MetaServer.unsafeGet, props.MetaPort.unsafeGet)) match {
      case Success(c) =>
        c.close()
        true
      case _ => false
    }
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    dataSource.connectToDatabase()
    if (serverIsListening) {
      migrator.migrate()
    }
  }

  override def beforeEach(): Unit = {
    repository = new ArticleRepository
    DB.autoCommit { implicit session =>
      repository
        .getAllIds
        .flatMap(_.traverse(articleId => repository.deleteMaxRevision(articleId.articleId)))
        .failIfFailure
    }
  }

  override def afterEach(): Unit = DB.autoCommit { implicit session =>
    repository
      .getAllIds
      .flatMap(_.traverse(articleId => repository.deleteMaxRevision(articleId.articleId)))
      .failIfFailure
  }

  test("getAllIds returns a list with all ids in the database") {
    val externalIdsAndRegularIds = (
      100 to 150
    ).map(_.toString).zipWithIndex
    externalIdsAndRegularIds.foreach { case (exId, id) =>
      repository.updateArticleFromDraftApi(sampleArticle.copy(id = Some(id.toLong)), List(exId))(using AutoSession)
    }
    val expected = externalIdsAndRegularIds
      .map { case (exId, id) =>
        ArticleIds(id.toLong, List(exId))
      }
      .toList
    repository.getAllIds(using ReadOnlyAutoSession).failIfFailure should equal(expected)
  }

  test("getIdFromExternalId works with all ids") {
    val inserted1 = repository
      .updateArticleFromDraftApi(sampleArticle.copy(id = Some(1)), List("6000", "10"))(using AutoSession)
      .failIfFailure
    val inserted2 = repository
      .updateArticleFromDraftApi(sampleArticle.copy(id = Some(2)), List("6001", "11"))(using AutoSession)
      .failIfFailure

    repository.getIdFromExternalId("6000")(using ReadOnlyAutoSession).failIfFailure should be(inserted1.id.get)
    repository.getIdFromExternalId("6001")(using ReadOnlyAutoSession).failIfFailure should be(inserted2.id.get)
    repository.getIdFromExternalId("10")(using ReadOnlyAutoSession).failIfFailure should be(inserted1.id.get)
    repository.getIdFromExternalId("11")(using ReadOnlyAutoSession).failIfFailure should be(inserted2.id.get)
  }

  test("getArticleIdsFromExternalId should return ArticleIds object with externalIds") {
    val externalIds = List("1", "6010", "6011", "5084", "763", "8881", "1919")
    val inserted    = repository.updateArticleFromDraftApi(sampleArticle, externalIds)(using AutoSession).failIfFailure
    val inserted2   = repository
      .updateArticleFromDraftApi(sampleArticle.copy(revision = Some(2)), externalIds)(using AutoSession)
      .failIfFailure

    repository.getArticleIdsFromExternalId("6011")(using ReadOnlyAutoSession).failIfFailure.get.externalId should be(
      externalIds
    )
    repository.deleteMaxRevision(inserted.id.get)(using AutoSession)
    repository.deleteMaxRevision(inserted2.id.get)(using AutoSession)
  }

  test("updateArticleFromDraftApi should update all columns with data from draft-api") {

    val externalIds            = List("123", "456")
    val sampleArticle: Article = TestData.sampleDomainArticle.copy(id = Some(5), revision = Some(42))
    val res                    = repository.updateArticleFromDraftApi(sampleArticle, externalIds)(using AutoSession).failIfFailure

    res.id.isDefined should be(true)
    repository.withId(res.id.get)(using AutoSession).failIfFailure.get.article.get should be(sampleArticle)
  }

  test("Fetching external ids works as expected") {
    val externalIds     = List("1", "2", "3")
    val idWithExternals =
      repository.updateArticleFromDraftApi(sampleArticle.copy(id = Some(1)), externalIds)(using AutoSession)
    val idWithoutExternals =
      repository.updateArticleFromDraftApi(sampleArticle.copy(id = Some(2)), List.empty)(using AutoSession)

    val result1 = repository.getExternalIdsFromId(idWithExternals.get.id.get)(using ReadOnlyAutoSession).failIfFailure
    result1 should be(externalIds)
    val result2 = repository.getExternalIdsFromId(idWithoutExternals.get.id.get)(using AutoSession).failIfFailure
    result2 should be(List.empty)

    repository.deleteMaxRevision(idWithExternals.get.id.get)(using AutoSession)
    repository.deleteMaxRevision(idWithoutExternals.get.id.get)(using AutoSession)
  }

  test("updating with a valid article with a that is not in database will be recreated") {
    val article = TestData.sampleDomainArticle.copy(id = Some(110))

    val updatedArticle = repository.updateArticleFromDraftApi(article, List.empty)(using AutoSession).failIfFailure

    repository.deleteMaxRevision(updatedArticle.id.get)(using AutoSession)
  }

  test("deleting article should ignore missing articles") {
    val article = TestData.sampleDomainArticle.copy(id = Some(Integer.MAX_VALUE))

    val deletedId = repository.deleteMaxRevision(article.id.get)(using AutoSession).failIfFailure

    deletedId should be(Integer.MAX_VALUE)
  }

  test("That getArticlesByPage returns all latest articles") {
    val art1 = repository
      .updateArticleFromDraftApi(sampleArticle.copy(id = Some(1)), List.empty)(using AutoSession)
      .failIfFailure
    val art2 = repository
      .updateArticleFromDraftApi(sampleArticle.copy(id = Some(2)), List.empty)(using AutoSession)
      .failIfFailure
    val art3 = repository
      .updateArticleFromDraftApi(sampleArticle.copy(id = Some(3)), List.empty)(using AutoSession)
      .failIfFailure
    val art4 = repository
      .updateArticleFromDraftApi(sampleArticle.copy(id = Some(4)), List.empty)(using AutoSession)
      .failIfFailure
    val art5 = repository
      .updateArticleFromDraftApi(sampleArticle.copy(id = Some(5)), List.empty)(using AutoSession)
      .failIfFailure
    val art6 = repository
      .updateArticleFromDraftApi(sampleArticle.copy(id = Some(6)), List.empty)(using AutoSession)
      .failIfFailure

    val pageSize = 4
    repository.getArticlesByPage(pageSize, pageSize * 0)(using ReadOnlyAutoSession).failIfFailure should be(
      Seq(art1, art2, art3, art4)
    )
    repository.getArticlesByPage(pageSize, pageSize * 1)(using ReadOnlyAutoSession).failIfFailure should be(
      Seq(art5, art6)
    )
  }

  test("That stored articles are retrieved exactly as they were stored") {
    val art1 = repository
      .updateArticleFromDraftApi(TestData.sampleArticleWithByNcSa.copy(id = Some(1)), List.empty)(using AutoSession)
      .failIfFailure
    val art2 = repository
      .updateArticleFromDraftApi(TestData.sampleArticleWithPublicDomain.copy(id = Some(2)), List.empty)(using
        AutoSession
      )
      .failIfFailure
    val art3 = repository
      .updateArticleFromDraftApi(TestData.sampleArticleWithCopyrighted.copy(id = Some(3)), List.empty)(using
        AutoSession
      )
      .failIfFailure

    repository.withId(1)(using ReadOnlyAutoSession).failIfFailure.get.article should be(Some(art1))
    repository.withId(2)(using ReadOnlyAutoSession).failIfFailure.get.article should be(Some(art2))
    repository.withId(3)(using ReadOnlyAutoSession).failIfFailure.get.article should be(Some(art3))
  }

  test("getTags returns non-duplicate tags and correct number of them") {
    val sampleArticle1 = TestData
      .sampleDomainArticle2
      .copy(
        id = Some(1L),
        revision = Some(0),
        tags = Seq(Tag(Seq("abc", "bcd", "ddd"), "nb"), Tag(Seq("abc", "bcd"), "nn")),
      )
    val sampleArticle2 = TestData
      .sampleDomainArticle2
      .copy(id = Some(2L), revision = Some(0), tags = Seq(Tag(Seq("bcd", "cde"), "nb"), Tag(Seq("bcd", "cde"), "nn")))
    val sampleArticle3 = TestData
      .sampleDomainArticle2
      .copy(id = Some(3L), revision = Some(0), tags = Seq(Tag(Seq("def"), "nb"), Tag(Seq("d", "def", "asd"), "nn")))
    val sampleArticle4 = TestData.sampleDomainArticle2.copy(id = Some(4L), revision = Some(0), tags = Seq.empty)

    repository.updateArticleFromDraftApi(sampleArticle1, List.empty)(using AutoSession).failIfFailure
    repository.updateArticleFromDraftApi(sampleArticle2, List.empty)(using AutoSession).failIfFailure
    repository.updateArticleFromDraftApi(sampleArticle3, List.empty)(using AutoSession).failIfFailure
    repository.updateArticleFromDraftApi(sampleArticle4, List.empty)(using AutoSession).failIfFailure

    val (tags1, tagsCount1) = repository.getTags("", 5, 0, "nb")(using ReadOnlyAutoSession).failIfFailure
    tags1 should equal(Seq("abc", "bcd", "cde", "ddd", "def"))
    tags1.length should be(5)
    tagsCount1 should be(5)

    val (tags2, tagsCount2) = repository.getTags("", 2, 0, "nb")(using ReadOnlyAutoSession).failIfFailure
    tags2 should equal(Seq("abc", "bcd"))
    tags2.length should be(2)
    tagsCount2 should be(5)

    val (tags3, tagsCount3) = repository.getTags("", 2, 3, "nn")(using ReadOnlyAutoSession).failIfFailure
    tags3 should equal(Seq("cde", "d"))
    tags3.length should be(2)
    tagsCount3 should be(6)

    val (tags4, tagsCount4) = repository.getTags("", 1, 3, "nn")(using ReadOnlyAutoSession).failIfFailure
    tags4 should equal(Seq("cde"))
    tags4.length should be(1)
    tagsCount4 should be(6)

    val (tags5, tagsCount5) = repository.getTags("", 10, 0, "*")(using ReadOnlyAutoSession).failIfFailure
    tags5 should equal(Seq("abc", "asd", "bcd", "cde", "d", "ddd", "def"))
    tags5.length should be(7)
    tagsCount5 should be(7)

    val (tags6, tagsCount6) = repository.getTags("d", 5, 0, "")(using ReadOnlyAutoSession).failIfFailure
    tags6 should equal(Seq("d", "ddd", "def"))
    tags6.length should be(3)
    tagsCount6 should be(3)

    val (tags7, tagsCount7) = repository.getTags("%b", 5, 0, "")(using ReadOnlyAutoSession).failIfFailure
    tags7 should equal(Seq("bcd"))
    tags7.length should be(1)
    tagsCount7 should be(1)

    val (tags8, tagsCount8) = repository.getTags("a", 10, 0, "")(using ReadOnlyAutoSession).failIfFailure
    tags8 should equal(Seq("abc", "asd"))
    tags8.length should be(2)
    tagsCount8 should be(2)

    val (tags9, tagsCount9) = repository.getTags("A", 10, 0, "")(using ReadOnlyAutoSession).failIfFailure
    tags9 should equal(Seq("abc", "asd"))
    tags9.length should be(2)
    tagsCount9 should be(2)
  }

  test("withId parse relatedContent correctly") {
    repository
      .updateArticleFromDraftApi(sampleArticle.copy(id = Some(1), relatedContent = Seq(Right(2))), List("6000", "10"))(
        using AutoSession
      )
      .failIfFailure

    val relatedId = repository
      .withId(1)(using ReadOnlyAutoSession)
      .failIfFailure
      .toArticle
      .get
      .relatedContent
      .head
      .value
    relatedId should be(2L)
  }

  test("Dumping articles should ignore unpublished ones") {
    val articleId = 110L
    val article   = TestData.sampleDomainArticle.copy(id = Some(articleId))

    repository.updateArticleFromDraftApi(article.copy(revision = 1.some), List.empty)(using AutoSession).failIfFailure
    repository.updateArticleFromDraftApi(article.copy(revision = 2.some), List.empty)(using AutoSession).failIfFailure
    repository.updateArticleFromDraftApi(article.copy(revision = 3.some), List.empty)(using AutoSession).failIfFailure

    val resultBefore = repository.getArticlesByPage(10, 0)(using ReadOnlyAutoSession).failIfFailure
    resultBefore.size should be(1)

    repository.unpublishMaxRevision(articleId)(using AutoSession).failIfFailure

    val resultAfter = repository.getArticlesByPage(10, 0)(using ReadOnlyAutoSession).failIfFailure
    resultAfter.size should be(0)
  }

  test("That fetching with slugs works as expected with revisions") {
    val articleId = 110L
    val article   = TestData.sampleDomainArticle.copy(id = Some(articleId), slug = Some("Detti-er-ein-slug"))

    val article1 = article.copy(revision = 1.some)
    val article2 = article.copy(revision = 2.some)
    val article3 = article.copy(revision = 3.some)

    repository.updateArticleFromDraftApi(article1, List.empty)(using AutoSession).failIfFailure
    repository.updateArticleFromDraftApi(article2, List.empty)(using AutoSession).failIfFailure
    val inserted3 = repository.updateArticleFromDraftApi(article3, List.empty)(using AutoSession).failIfFailure

    repository.withSlug("Detti-er-ein-slug")(using ReadOnlyAutoSession).failIfFailure.toArticle.get should be(inserted3)
  }

  test("That fetching articles in range excludes unpublished") {
    val article = TestData.sampleDomainArticle.copy(id = Some(1L))

    val article1 = article.copy(id = 1L.some, revision = 1.some)
    val article2 = article.copy(id = 2L.some, revision = 1.some)
    val article3 = article.copy(id = 3L.some, revision = 1.some)
    val article4 = article.copy(id = 4L.some, revision = 1.some)

    repository.updateArticleFromDraftApi(article1, List.empty)(using AutoSession).failIfFailure
    repository.updateArticleFromDraftApi(article2, List.empty)(using AutoSession).failIfFailure
    repository.updateArticleFromDraftApi(article3, List.empty)(using AutoSession).failIfFailure
    repository.updateArticleFromDraftApi(article4, List.empty)(using AutoSession).failIfFailure

    repository.updateArticleFromDraftApi(article3.copy(revision = 2.some), List.empty)(using AutoSession).failIfFailure
    repository.updateArticleFromDraftApi(article3.copy(revision = 3.some), List.empty)(using AutoSession).failIfFailure
    val resultBefore = repository.documentsWithIdBetween(1, 10)(using ReadOnlyAutoSession).failIfFailure
    resultBefore.size should be(4)

    repository.unpublishMaxRevision(3)(using AutoSession).failIfFailure

    val result = repository.documentsWithIdBetween(1, 10)(using ReadOnlyAutoSession).failIfFailure
    result.size should be(3)
  }

}
