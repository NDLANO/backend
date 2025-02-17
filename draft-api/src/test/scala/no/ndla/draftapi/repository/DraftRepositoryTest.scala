/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.repository

import com.zaxxer.hikari.HikariDataSource
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.draft.{Comment, Draft, DraftStatus}
import no.ndla.common.model.domain.{ArticleContent, EditorNote, Status}
import no.ndla.draftapi.*
import no.ndla.draftapi.model.domain.*
import no.ndla.network.tapir.auth.{Permission, TokenUser}
import no.ndla.scalatestsuite.IntegrationSuite
import org.mockito.Mockito.when
import scalikejdbc.*

import java.net.Socket
import java.util.UUID
import scala.util.{Success, Try}

class DraftRepositoryTest extends IntegrationSuite(EnablePostgresContainer = true) with TestEnvironment {
  override val dataSource: HikariDataSource = testDataSource.get
  override val migrator: DBMigrator         = new DBMigrator
  var repository: DraftRepository           = _
  val sampleArticle: Draft                  = TestData.sampleArticleWithByNcSa

  def emptyTestDatabase(): Unit = DB autoCommit (implicit session => {
    sql"delete from articledata;".execute()(session)
  })

  private def resetIdSequence(): Boolean = {
    DB autoCommit (implicit session => {
      sql"select setval('article_id_sequence', 1, false);".execute()
    })
  }

  def serverIsListening: Boolean = {
    val server = props.MetaServer
    val port   = props.MetaPort
    Try(new Socket(server, port)) match {
      case Success(c) =>
        c.close()
        true
      case _ => false
    }
  }

  override def beforeEach(): Unit = {
    repository = new DraftRepository()
    if (serverIsListening) {
      emptyTestDatabase()
    }
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    DataSource.connectToDatabase()
    if (serverIsListening) {
      migrator.migrate()
    }
  }

  test("Fetching external ids works as expected") {
    val externalIds        = List("1", "2", "3")
    val idWithExternals    = 1L
    val idWithoutExternals = 2L
    repository.insertWithExternalIds(sampleArticle.copy(id = Some(idWithExternals)), externalIds, List.empty, None)(
      AutoSession
    )
    repository.insertWithExternalIds(sampleArticle.copy(id = Some(idWithoutExternals)), List.empty, List.empty, None)(
      AutoSession
    )

    val result1 = repository.getExternalIdsFromId(idWithExternals)(AutoSession)
    result1 should be(externalIds)
    val result2 = repository.getExternalIdsFromId(idWithoutExternals)(AutoSession)
    result2 should be(List.empty)
  }

  test("withId also returns archieved articles") {
    repository.insert(sampleArticle.copy(id = Some(1), status = Status(DraftStatus.PLANNED, Set.empty)))(AutoSession)
    repository.insert(
      sampleArticle.copy(id = Some(2), status = Status(DraftStatus.ARCHIVED, Set.empty))
    )(AutoSession)

    repository.withId(1)(ReadOnlyAutoSession).isDefined should be(true)
    repository.withId(2)(ReadOnlyAutoSession).isDefined should be(true)
  }

  test("that importIdOfArticle works correctly") {
    val externalIds = List("1", "2", "3")
    val uuid        = "d4e84cd3-ab94-46d5-9839-47ec682d27c2"
    val id1         = 1L
    val id2         = 2L
    repository.insertWithExternalIds(sampleArticle.copy(id = Some(id1)), externalIds, List.empty, Some(uuid))(
      AutoSession
    )
    repository.insertWithExternalIds(sampleArticle.copy(id = Some(id2)), List.empty, List.empty, Some(uuid))(
      AutoSession
    )

    val result1 = repository.importIdOfArticle("1")
    result1.get should be(ImportId(Some(uuid)))
    val result2 = repository.importIdOfArticle("2")
    result2.get should be(ImportId(Some(uuid)))

    repository.deleteArticle(id1)(AutoSession)
    repository.deleteArticle(id2)(AutoSession)
  }

  test("ExternalIds should not contains NULLs") {
    val art1 = sampleArticle.copy(id = Some(10L))
    repository.insertWithExternalIds(art1, null, List.empty, None)(AutoSession)
    val result1 = repository.getExternalIdsFromId(10)(AutoSession)

    result1 should be(List.empty)
  }

  test("Updating an article should work as expected") {
    val art1 = sampleArticle.copy(id = Some(1), status = Status(DraftStatus.PLANNED, Set.empty))
    val art2 = sampleArticle.copy(id = Some(2), status = Status(DraftStatus.PLANNED, Set.empty))
    val art3 = sampleArticle.copy(id = Some(3), status = Status(DraftStatus.PLANNED, Set.empty))
    val art4 = sampleArticle.copy(id = Some(4), status = Status(DraftStatus.PLANNED, Set.empty))

    repository.insert(art1)(AutoSession)
    repository.insert(art2)(AutoSession)
    repository.insert(art3)(AutoSession)
    repository.insert(art4)(AutoSession)

    val updatedContent = Seq(ArticleContent("What u do mr", "nb"))

    repository.updateArticle(art1.copy(content = updatedContent))(AutoSession)

    repository.withId(art1.id.get)(ReadOnlyAutoSession).get.content should be(updatedContent)
    repository.withId(art2.id.get)(ReadOnlyAutoSession).get.content should be(art2.content)
    repository.withId(art3.id.get)(ReadOnlyAutoSession).get.content should be(art3.content)
    repository.withId(art4.id.get)(ReadOnlyAutoSession).get.content should be(art4.content)
  }

  test("That storing an article an retrieving it returns the original article") {
    val art1 = sampleArticle.copy(id = Some(1), status = Status(DraftStatus.PLANNED, Set.empty))
    val art2 = sampleArticle.copy(id = Some(2), status = Status(DraftStatus.PUBLISHED, Set.empty))
    val art3 = sampleArticle.copy(
      id = Some(3),
      status = Status(DraftStatus.INTERNAL_REVIEW, Set.empty)
    )
    val art4 = sampleArticle.copy(id = Some(4), status = Status(DraftStatus.PLANNED, Set.empty))

    repository.insert(art1)(AutoSession)
    repository.insertWithExternalIds(art2, List("1234", "5678"), List.empty, None)(AutoSession)
    repository.insert(art3)(AutoSession)
    repository.insert(art4)(AutoSession)

    repository.withId(art1.id.get)(ReadOnlyAutoSession).get should be(art1)
    repository.withId(art2.id.get)(ReadOnlyAutoSession).get should be(art2)
    repository.withId(art3.id.get)(ReadOnlyAutoSession).get should be(art3)
    repository.withId(art4.id.get)(ReadOnlyAutoSession).get should be(art4)
  }

  test("That updateWithExternalIds updates article correctly") {
    val art1 = sampleArticle.copy(id = Some(1), status = Status(DraftStatus.PLANNED, Set.empty))
    repository.insertWithExternalIds(art1, List("1234", "5678"), List.empty, None)(AutoSession)

    val updatedContent = Seq(ArticleContent("This is updated with external ids yo", "en"))
    val updatedArt     = art1.copy(content = updatedContent)
    repository.updateWithExternalIds(updatedArt, List("1234", "5678"), List.empty, None)(AutoSession)
    repository.withId(art1.id.get)(ReadOnlyAutoSession).get should be(updatedArt)
  }

  test("That getAllIds returns all articles") {
    val art1 = sampleArticle.copy(id = Some(1), status = Status(DraftStatus.PLANNED, Set.empty))
    val art2 = sampleArticle.copy(id = Some(2), status = Status(DraftStatus.PUBLISHED, Set.empty))
    val art3 = sampleArticle.copy(id = Some(3), status = Status(DraftStatus.EXTERNAL_REVIEW, Set.empty))
    val art4 = sampleArticle.copy(id = Some(4), status = Status(DraftStatus.PLANNED, Set.empty))

    repository.insert(art1)(AutoSession)
    repository.insertWithExternalIds(art2, List("1234", "5678"), List.empty, None)(AutoSession)
    repository.insert(art3)(AutoSession)
    repository.insert(art4)(AutoSession)

    repository.getAllIds(AutoSession) should be(
      Seq(
        ArticleIds(art1.id.get, List.empty),
        ArticleIds(art2.id.get, List("1234", "5678")),
        ArticleIds(art3.id.get, List.empty),
        ArticleIds(art4.id.get, List.empty)
      )
    )
  }

  test("that getIdFromExternalId returns id of article correctly") {
    val art1 = sampleArticle.copy(id = Some(14), status = Status(DraftStatus.PLANNED, Set.empty))
    repository.insert(art1)(AutoSession)
    repository.insertWithExternalIds(art1.copy(revision = Some(3)), List("5678"), List.empty, None)(AutoSession)

    repository.getIdFromExternalId("5678")(AutoSession) should be(art1.id)
    repository.getIdFromExternalId("9999")(AutoSession) should be(None)
  }

  test("That newEmptyArticle creates the latest available article_id") {
    this.resetIdSequence()

    repository.newEmptyArticleId()(AutoSession) should be(Success(1))
    repository.newEmptyArticleId()(AutoSession) should be(Success(2))
    repository.newEmptyArticleId()(AutoSession) should be(Success(3))
    repository.newEmptyArticleId()(AutoSession) should be(Success(4))
    repository.newEmptyArticleId()(AutoSession) should be(Success(5))
    repository.newEmptyArticleId()(AutoSession) should be(Success(6))
    repository.newEmptyArticleId()(AutoSession) should be(Success(7))
  }
  test("That idsWithStatus returns correct drafts") {
    repository.insert(sampleArticle.copy(id = Some(1), status = Status(DraftStatus.PLANNED, Set.empty)))(AutoSession)
    repository.insert(sampleArticle.copy(id = Some(2), status = Status(DraftStatus.PLANNED, Set.empty)))(AutoSession)
    repository.insert(
      sampleArticle.copy(id = Some(3), status = Status(DraftStatus.IN_PROGRESS, Set.empty))
    )(AutoSession)
    repository.insert(sampleArticle.copy(id = Some(4), status = Status(DraftStatus.PLANNED, Set.empty)))(AutoSession)
    repository.insertWithExternalIds(
      sampleArticle.copy(id = Some(5), status = Status(DraftStatus.IN_PROGRESS, Set.empty)),
      List("1234"),
      List.empty,
      None
    )(AutoSession)
    repository.insert(
      sampleArticle.copy(id = Some(6), status = Status(DraftStatus.PUBLISHED, Set.empty))
    )(AutoSession)
    repository.insert(
      sampleArticle.copy(id = Some(7), status = Status(DraftStatus.END_CONTROL, Set.empty))
    )(AutoSession)
    repository.insertWithExternalIds(
      sampleArticle.copy(id = Some(8), status = Status(DraftStatus.IN_PROGRESS, Set.empty)),
      List("5678", "1111"),
      List.empty,
      None
    )(AutoSession)

    repository.idsWithStatus(DraftStatus.PLANNED)(AutoSession) should be(
      Success(List(ArticleIds(1, List.empty), ArticleIds(2, List.empty), ArticleIds(4, List.empty)))
    )

    repository.idsWithStatus(DraftStatus.IN_PROGRESS)(AutoSession) should be(
      Success(List(ArticleIds(3, List.empty), ArticleIds(5, List("1234")), ArticleIds(8, List("5678", "1111"))))
    )

    repository.idsWithStatus(DraftStatus.PUBLISHED)(AutoSession) should be(Success(List(ArticleIds(6, List.empty))))

    repository.idsWithStatus(DraftStatus.END_CONTROL)(AutoSession) should be(Success(List(ArticleIds(7, List.empty))))
  }

  test("That getArticlesByPage returns all latest articles") {
    val art1 = sampleArticle.copy(id = Some(1), status = Status(DraftStatus.PLANNED, Set.empty))
    val art2 = sampleArticle.copy(
      id = Some(1),
      revision = Some(2),
      status = Status(DraftStatus.PLANNED, Set.empty)
    )
    val art3 = sampleArticle.copy(id = Some(2), status = Status(DraftStatus.PLANNED, Set.empty))
    val art4 = sampleArticle.copy(id = Some(3), status = Status(DraftStatus.PLANNED, Set.empty))
    val art5 = sampleArticle.copy(id = Some(4), status = Status(DraftStatus.PLANNED, Set.empty))
    val art6 = sampleArticle.copy(id = Some(5), status = Status(DraftStatus.PLANNED, Set.empty))
    repository.insert(art1)(AutoSession)
    repository.insert(art2)(AutoSession)
    repository.insert(art3)(AutoSession)
    repository.insert(art4)(AutoSession)
    repository.insert(art5)(AutoSession)
    repository.insert(art6)(AutoSession)

    val pageSize = 4
    repository.getArticlesByPage(pageSize, pageSize * 0)(AutoSession) should be(
      Seq(
        art2,
        art3,
        art4,
        art5
      )
    )
    repository.getArticlesByPage(pageSize, pageSize * 1)(AutoSession) should be(
      Seq(
        art6
      )
    )
  }

  test("published, then copied article creates new db version and bumps revision by two") {
    val article = TestData.sampleDomainArticle.copy(
      status = Status(DraftStatus.UNPUBLISHED, Set.empty),
      revision = Some(3)
    )
    repository.insert(article)(AutoSession)
    val oldCount                = repository.articlesWithId(article.id.get).size
    val publishedArticle        = article.copy(status = Status(DraftStatus.PUBLISHED, Set.empty))
    val updatedArticle          = repository.updateArticle(publishedArticle)(AutoSession).get
    val updatedAndCopiedArticle = repository.storeArticleAsNewVersion(updatedArticle, None)(AutoSession).get

    updatedAndCopiedArticle.revision should be(Some(5))

    updatedAndCopiedArticle.notes.length should be(0)
    updatedAndCopiedArticle should equal(publishedArticle.copy(notes = Seq(), revision = Some(5)))

    val count = repository.articlesWithId(article.id.get).size
    count should be(oldCount + 1)

  }

  test("published article keeps revison on import") {
    val article = TestData.sampleDomainArticle.copy(revision = Some(1))
    repository.insert(article)(AutoSession)
    val oldCount         = repository.articlesWithId(article.id.get).size
    val publishedArticle = article.copy(status = Status(DraftStatus.PUBLISHED, Set.empty))
    val updatedArticle   = repository.updateArticle(publishedArticle, isImported = true)(AutoSession).get
    updatedArticle.revision should be(Some(1))

    updatedArticle.notes.length should be(0)
    updatedArticle should equal(publishedArticle.copy(notes = Seq(), revision = Some(1)))

    val count = repository.articlesWithId(article.id.get).size
    count should be(oldCount)

  }

  test("published, then copied article keeps old notes in hidden field and notes is emptied") {
    val now = NDLADate.now().withNano(0)
    when(clock.now()).thenReturn(now)
    val status = Status(DraftStatus.PLANNED, Set.empty)
    val prevNotes1 = Seq(
      EditorNote("Note1", "SomeId", status, now),
      EditorNote("Note2", "SomeId", status, now),
      EditorNote("Note3", "SomeId", status, now),
      EditorNote("Note4", "SomeId", status, now)
    )

    val prevNotes2 = Seq(
      EditorNote("Note5", "SomeId", status, now),
      EditorNote("Note6", "SomeId", status, now),
      EditorNote("Note7", "SomeId", status, now),
      EditorNote("Note8", "SomeId", status, now)
    )
    val draftArticle1 = TestData.sampleDomainArticle.copy(
      status = Status(DraftStatus.UNPUBLISHED, Set.empty),
      revision = Some(3),
      notes = prevNotes1
    )

    val inserted = repository.insert(draftArticle1)(AutoSession)
    val fetched  = repository.withId(inserted.id.get)(ReadOnlyAutoSession).get
    fetched.notes should be(prevNotes1)
    fetched.previousVersionsNotes should be(Seq.empty)

    val toPublish1      = inserted.copy(status = Status(DraftStatus.PUBLISHED, Set.empty))
    val updatedArticle1 = repository.updateArticle(toPublish1)(AutoSession).get

    updatedArticle1.notes should be(prevNotes1)
    updatedArticle1.previousVersionsNotes should be(Seq.empty)

    val copiedArticle1 = repository.storeArticleAsNewVersion(updatedArticle1, None)(AutoSession).get
    copiedArticle1.notes should be(Seq.empty)
    copiedArticle1.previousVersionsNotes should be(prevNotes1)

    val draftArticle2 = copiedArticle1.copy(
      status = Status(DraftStatus.PUBLISHED, Set.empty),
      notes = prevNotes2
    )
    val updatedArticle2 = repository.updateArticle(draftArticle2)(AutoSession).get
    updatedArticle2.notes should be(prevNotes2)
    updatedArticle2.previousVersionsNotes should be(prevNotes1)

    val copiedArticle2 = repository.storeArticleAsNewVersion(updatedArticle2, None)(AutoSession).get
    copiedArticle2.notes should be(Seq.empty)
    copiedArticle2.previousVersionsNotes should be(prevNotes1 ++ prevNotes2)

  }

  test("copied article should have new note about copying if user present") {
    val now = NDLADate.now().withNano(0)
    when(clock.now()).thenReturn(now)

    val draftArticle1 = TestData.sampleDomainArticle.copy(
      status = Status(DraftStatus.PLANNED, Set.empty),
      notes = Seq.empty
    )
    repository.insert(draftArticle1)(AutoSession)

    val copiedArticle1 =
      repository
        .storeArticleAsNewVersion(draftArticle1, Some(TokenUser("user-id", Set(Permission.DRAFT_API_WRITE), None)))(
          AutoSession
        )
        .get
    copiedArticle1.notes.length should be(1)
    copiedArticle1.notes.head.user should be("user-id")
    copiedArticle1.previousVersionsNotes should be(Seq.empty)
  }

  test("withId parse relatedContent correctly") {
    repository.insert(sampleArticle.copy(id = Some(1), relatedContent = Seq(Right(2))))(AutoSession)

    val Right(relatedId) = repository.withId(1)(ReadOnlyAutoSession).get.relatedContent.head
    relatedId should be(2L)

  }

  test("That slugs are stored and extracted as lowercase") {
    val article = sampleArticle.copy(id = Some(1), slug = Some("ApeKaTt"))

    val inserted = repository.insert(article)(AutoSession)
    val fetched  = repository.withSlug("aPEkAtT")(ReadOnlyAutoSession).get
    fetched should be(inserted)
  }

  test("Comments are kept on publishing topic-articles") {
    val now      = NDLADate.now().withNano(0)
    val comments = Seq(Comment(UUID.randomUUID(), now, now, "hei", isOpen = false, solved = true))
    val article = TestData.sampleDomainArticle.copy(
      status = Status(DraftStatus.IN_PROGRESS, Set.empty),
      comments = comments,
      revision = Some(1)
    )
    val topicArticle = TestData.sampleTopicArticle.copy(
      id = Some(123L),
      status = Status(DraftStatus.IN_PROGRESS, Set.empty),
      comments = comments,
      revision = Some(1)
    )

    repository.insert(article)(AutoSession)
    repository.insert(topicArticle)(AutoSession)
    val publishedArticle      = repository.storeArticleAsNewVersion(article, None)(AutoSession).get
    val publishedTopicArticle = repository.storeArticleAsNewVersion(topicArticle, None)(AutoSession).get

    publishedArticle.comments should be(Seq())
    publishedTopicArticle.comments should be(comments)
  }
}
