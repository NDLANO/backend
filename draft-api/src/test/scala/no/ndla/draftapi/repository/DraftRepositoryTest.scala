/*
 * Part of NDLA draft-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.draftapi.repository

import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.draft.{Draft, DraftStatus}
import no.ndla.common.model.domain.{ArticleContent, Comment, EditorNote, Status}
import no.ndla.database.{DBMigrator, DataSource}
import no.ndla.draftapi.*
import no.ndla.draftapi.model.domain.*
import no.ndla.network.tapir.auth.{Permission, TokenUser}
import no.ndla.scalatestsuite.DatabaseIntegrationSuite
import org.mockito.Mockito.when
import scalikejdbc.*

import java.net.Socket
import java.util.UUID
import scala.util.{Success, Try}

class DraftRepositoryTest extends DatabaseIntegrationSuite with TestEnvironment {
  override implicit lazy val dataSource: DataSource = testDataSource.get
  override implicit lazy val migrator: DBMigrator   = new DBMigrator
  var repository: DraftRepository                   = scala.compiletime.uninitialized
  val sampleArticle: Draft                          = TestData.sampleArticleWithByNcSa

  def emptyTestDatabase(): Unit = DB autoCommit (implicit session => {
    sql"delete from articledata;".execute()(using session)
  })

  private def resetIdSequence(): Boolean = {
    DB autoCommit (implicit session => {
      sql"select setval('article_id_sequence', 1, false);".execute()
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
    repository = new DraftRepository()
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

  test("withId also returns archieved articles") {
    repository.insert(sampleArticle.copy(id = Some(1), status = Status(DraftStatus.PLANNED, Set.empty)))(using
      AutoSession
    )
    repository.insert(sampleArticle.copy(id = Some(2), status = Status(DraftStatus.ARCHIVED, Set.empty)))(using
      AutoSession
    )

    repository.withId(1)(using ReadOnlyAutoSession).isDefined should be(true)
    repository.withId(2)(using ReadOnlyAutoSession).isDefined should be(true)
  }

  test("Updating an article should work as expected") {
    val art1 = sampleArticle.copy(id = Some(1), status = Status(DraftStatus.PLANNED, Set.empty))
    val art2 = sampleArticle.copy(id = Some(2), status = Status(DraftStatus.PLANNED, Set.empty))
    val art3 = sampleArticle.copy(id = Some(3), status = Status(DraftStatus.PLANNED, Set.empty))
    val art4 = sampleArticle.copy(id = Some(4), status = Status(DraftStatus.PLANNED, Set.empty))

    repository.insert(art1)(using AutoSession)
    repository.insert(art2)(using AutoSession)
    repository.insert(art3)(using AutoSession)
    repository.insert(art4)(using AutoSession)

    val updatedContent = Seq(ArticleContent("What u do mr", "nb"))

    repository.updateArticle(art1.copy(content = updatedContent))(using AutoSession)

    repository.withId(art1.id.get)(using ReadOnlyAutoSession).get.content should be(updatedContent)
    repository.withId(art2.id.get)(using ReadOnlyAutoSession).get.content should be(art2.content)
    repository.withId(art3.id.get)(using ReadOnlyAutoSession).get.content should be(art3.content)
    repository.withId(art4.id.get)(using ReadOnlyAutoSession).get.content should be(art4.content)
  }

  test("Updating an article with notes should merge the notes") {
    val art1     = sampleArticle.copy(id = Some(1), status = Status(DraftStatus.PLANNED, Set.empty))
    val inserted = repository.insert(art1)(using AutoSession)
    val numNotes = inserted.notes.length

    val updatedNotes = Seq(EditorNote("A note", "SomeId", art1.status, NDLADate.now()))
    repository.updateArticleNotes(art1.id.get, updatedNotes)(using AutoSession)

    val updated = repository.withId(art1.id.get)(using ReadOnlyAutoSession).get
    updated.notes.length should be(numNotes + 1)
    updated.revision should be(art1.revision)
  }

  test("That storing an article an retrieving it returns the original article") {
    val art1 = sampleArticle.copy(id = Some(1), status = Status(DraftStatus.PLANNED, Set.empty))
    val art2 = sampleArticle.copy(id = Some(2), status = Status(DraftStatus.PUBLISHED, Set.empty))
    val art3 = sampleArticle.copy(id = Some(3), status = Status(DraftStatus.INTERNAL_REVIEW, Set.empty))
    val art4 = sampleArticle.copy(id = Some(4), status = Status(DraftStatus.PLANNED, Set.empty))

    repository.insert(art1)(using AutoSession)
    repository.insert(art2)(using AutoSession)
    repository.insert(art3)(using AutoSession)
    repository.insert(art4)(using AutoSession)

    repository.withId(art1.id.get)(using ReadOnlyAutoSession).get should be(art1)
    repository.withId(art2.id.get)(using ReadOnlyAutoSession).get should be(art2)
    repository.withId(art3.id.get)(using ReadOnlyAutoSession).get should be(art3)
    repository.withId(art4.id.get)(using ReadOnlyAutoSession).get should be(art4)
  }

  test("That getAllIds returns all articles") {
    val art1 = sampleArticle.copy(id = Some(1), status = Status(DraftStatus.PLANNED, Set.empty))
    val art2 = sampleArticle.copy(id = Some(2), status = Status(DraftStatus.PUBLISHED, Set.empty))
    val art3 = sampleArticle.copy(id = Some(3), status = Status(DraftStatus.EXTERNAL_REVIEW, Set.empty))
    val art4 = sampleArticle.copy(id = Some(4), status = Status(DraftStatus.PLANNED, Set.empty))

    repository.insert(art1)(using AutoSession)
    repository.insert(art2)(using AutoSession)
    repository.insert(art3)(using AutoSession)
    repository.insert(art4)(using AutoSession)

    repository.getAllIds(using AutoSession) should be(
      Seq(
        ArticleIds(art1.id.get, List.empty),
        ArticleIds(art2.id.get, List.empty),
        ArticleIds(art3.id.get, List.empty),
        ArticleIds(art4.id.get, List.empty),
      )
    )
  }

  test("That newEmptyArticle creates the latest available article_id") {
    this.resetIdSequence()

    repository.newEmptyArticleId()(using AutoSession) should be(Success(1))
    repository.newEmptyArticleId()(using AutoSession) should be(Success(2))
    repository.newEmptyArticleId()(using AutoSession) should be(Success(3))
    repository.newEmptyArticleId()(using AutoSession) should be(Success(4))
    repository.newEmptyArticleId()(using AutoSession) should be(Success(5))
    repository.newEmptyArticleId()(using AutoSession) should be(Success(6))
    repository.newEmptyArticleId()(using AutoSession) should be(Success(7))
  }
  test("That idsWithStatus returns correct drafts") {
    repository.insert(sampleArticle.copy(id = Some(1), status = Status(DraftStatus.PLANNED, Set.empty)))(using
      AutoSession
    )
    repository.insert(sampleArticle.copy(id = Some(2), status = Status(DraftStatus.PLANNED, Set.empty)))(using
      AutoSession
    )
    repository.insert(sampleArticle.copy(id = Some(3), status = Status(DraftStatus.IN_PROGRESS, Set.empty)))(using
      AutoSession
    )
    repository.insert(sampleArticle.copy(id = Some(4), status = Status(DraftStatus.PLANNED, Set.empty)))(using
      AutoSession
    )
    repository.insert(sampleArticle.copy(id = Some(5), status = Status(DraftStatus.IN_PROGRESS, Set.empty)))(using
      AutoSession
    )
    repository.insert(sampleArticle.copy(id = Some(6), status = Status(DraftStatus.PUBLISHED, Set.empty)))(using
      AutoSession
    )
    repository.insert(sampleArticle.copy(id = Some(7), status = Status(DraftStatus.END_CONTROL, Set.empty)))(using
      AutoSession
    )
    repository.insert(sampleArticle.copy(id = Some(8), status = Status(DraftStatus.IN_PROGRESS, Set.empty)))(using
      AutoSession
    )

    repository.idsWithStatus(DraftStatus.PLANNED)(using AutoSession) should be(
      Success(List(ArticleIds(1, List.empty), ArticleIds(2, List.empty), ArticleIds(4, List.empty)))
    )

    repository.idsWithStatus(DraftStatus.IN_PROGRESS)(using AutoSession) should be(
      Success(List(ArticleIds(3, List.empty), ArticleIds(5, List.empty), ArticleIds(8, List.empty)))
    )

    repository.idsWithStatus(DraftStatus.PUBLISHED)(using AutoSession) should be(
      Success(List(ArticleIds(6, List.empty)))
    )

    repository.idsWithStatus(DraftStatus.END_CONTROL)(using AutoSession) should be(
      Success(List(ArticleIds(7, List.empty)))
    )
  }

  test("That getArticlesByPage returns all latest articles") {
    val art1 = sampleArticle.copy(id = Some(1), status = Status(DraftStatus.PLANNED, Set.empty))
    val art2 = sampleArticle.copy(id = Some(1), revision = Some(2), status = Status(DraftStatus.PLANNED, Set.empty))
    val art3 = sampleArticle.copy(id = Some(2), status = Status(DraftStatus.PLANNED, Set.empty))
    val art4 = sampleArticle.copy(id = Some(3), status = Status(DraftStatus.PLANNED, Set.empty))
    val art5 = sampleArticle.copy(id = Some(4), status = Status(DraftStatus.PLANNED, Set.empty))
    val art6 = sampleArticle.copy(id = Some(5), status = Status(DraftStatus.PLANNED, Set.empty))
    repository.insert(art1)(using AutoSession)
    repository.insert(art2)(using AutoSession)
    repository.insert(art3)(using AutoSession)
    repository.insert(art4)(using AutoSession)
    repository.insert(art5)(using AutoSession)
    repository.insert(art6)(using AutoSession)

    val pageSize = 4
    repository.getArticlesByPage(pageSize, pageSize * 0)(using AutoSession) should be(Seq(art2, art3, art4, art5))
    repository.getArticlesByPage(pageSize, pageSize * 1)(using AutoSession) should be(Seq(art6))
  }

  test("published, then copied article creates new db version and bumps revision by two") {
    val article = TestData
      .sampleDomainArticle
      .copy(status = Status(DraftStatus.UNPUBLISHED, Set.empty), revision = Some(3))
    repository.insert(article)(using AutoSession)
    val oldCount                = repository.articlesWithId(article.id.get).size
    val publishedArticle        = article.copy(status = Status(DraftStatus.PUBLISHED, Set.empty))
    val updatedArticle          = repository.updateArticle(publishedArticle)(using AutoSession).get
    val updatedAndCopiedArticle = repository.storeArticleAsNewVersion(updatedArticle, None)(using AutoSession).get

    updatedAndCopiedArticle.revision should be(Some(5))

    updatedAndCopiedArticle.notes.length should be(0)
    updatedAndCopiedArticle should equal(publishedArticle.copy(notes = Seq(), revision = Some(5)))

    val count = repository.articlesWithId(article.id.get).size
    count should be(oldCount + 1)

  }

  test("published, then copied article keeps old notes in hidden field and notes is emptied") {
    val now = NDLADate.now().withNano(0)
    when(clock.now()).thenReturn(now)
    val status     = Status(DraftStatus.PLANNED, Set.empty)
    val prevNotes1 = Seq(
      EditorNote("Note1", "SomeId", status, now),
      EditorNote("Note2", "SomeId", status, now),
      EditorNote("Note3", "SomeId", status, now),
      EditorNote("Note4", "SomeId", status, now),
    )

    val prevNotes2 = Seq(
      EditorNote("Note5", "SomeId", status, now),
      EditorNote("Note6", "SomeId", status, now),
      EditorNote("Note7", "SomeId", status, now),
      EditorNote("Note8", "SomeId", status, now),
    )
    val draftArticle1 = TestData
      .sampleDomainArticle
      .copy(status = Status(DraftStatus.UNPUBLISHED, Set.empty), revision = Some(3), notes = prevNotes1)

    val inserted = repository.insert(draftArticle1)(using AutoSession)
    val fetched  = repository.withId(inserted.id.get)(using ReadOnlyAutoSession).get
    fetched.notes should be(prevNotes1)
    fetched.previousVersionsNotes should be(Seq.empty)

    val toPublish1      = inserted.copy(status = Status(DraftStatus.PUBLISHED, Set.empty))
    val updatedArticle1 = repository.updateArticle(toPublish1)(using AutoSession).get

    updatedArticle1.notes should be(prevNotes1)
    updatedArticle1.previousVersionsNotes should be(Seq.empty)

    val copiedArticle1 = repository.storeArticleAsNewVersion(updatedArticle1, None)(using AutoSession).get
    copiedArticle1.notes should be(Seq.empty)
    copiedArticle1.previousVersionsNotes should be(prevNotes1)

    val draftArticle2   = copiedArticle1.copy(status = Status(DraftStatus.PUBLISHED, Set.empty), notes = prevNotes2)
    val updatedArticle2 = repository.updateArticle(draftArticle2)(using AutoSession).get
    updatedArticle2.notes should be(prevNotes2)
    updatedArticle2.previousVersionsNotes should be(prevNotes1)

    val copiedArticle2 = repository.storeArticleAsNewVersion(updatedArticle2, None)(using AutoSession).get
    copiedArticle2.notes should be(Seq.empty)
    copiedArticle2.previousVersionsNotes should be(prevNotes1 ++ prevNotes2)

  }

  test("copied article should have new note about copying if user present") {
    val now = NDLADate.now().withNano(0)
    when(clock.now()).thenReturn(now)

    val draftArticle1 = TestData
      .sampleDomainArticle
      .copy(status = Status(DraftStatus.PLANNED, Set.empty), notes = Seq.empty)
    repository.insert(draftArticle1)(using AutoSession)

    val copiedArticle1 = repository
      .storeArticleAsNewVersion(draftArticle1, Some(TokenUser("user-id", Set(Permission.DRAFT_API_WRITE), None)))(using
        AutoSession
      )
      .get
    copiedArticle1.notes.length should be(1)
    copiedArticle1.notes.head.user should be("user-id")
    copiedArticle1.previousVersionsNotes should be(Seq.empty)
  }

  test("withId parse relatedContent correctly") {
    repository.insert(sampleArticle.copy(id = Some(1), relatedContent = Seq(Right(2))))(using AutoSession)

    val Right(relatedId) = repository.withId(1)(using ReadOnlyAutoSession).get.relatedContent.head: @unchecked
    relatedId should be(2L)

  }

  test("That slugs are stored and extracted as lowercase") {
    val article = sampleArticle.copy(id = Some(1), slug = Some("ApeKaTt"))

    val inserted = repository.insert(article)(using AutoSession)
    val fetched  = repository.withSlug("aPEkAtT")(using ReadOnlyAutoSession).get
    fetched should be(inserted)
  }

  test("Comments are kept on publishing topic-articles") {
    val now      = NDLADate.now().withNano(0)
    val comments = Seq(Comment(UUID.randomUUID(), now, now, "hei", isOpen = false, solved = true))
    val article  = TestData
      .sampleDomainArticle
      .copy(status = Status(DraftStatus.IN_PROGRESS, Set.empty), comments = comments, revision = Some(1))
    val topicArticle = TestData
      .sampleTopicArticle
      .copy(
        id = Some(123L),
        status = Status(DraftStatus.IN_PROGRESS, Set.empty),
        comments = comments,
        revision = Some(1),
      )

    repository.insert(article)(using AutoSession)
    repository.insert(topicArticle)(using AutoSession)
    val publishedArticle      = repository.storeArticleAsNewVersion(article, None)(using AutoSession).get
    val publishedTopicArticle = repository.storeArticleAsNewVersion(topicArticle, None)(using AutoSession).get

    publishedArticle.comments should be(Seq())
    publishedTopicArticle.comments should be(comments)
  }

  test("That editornotes are kept both from regular update and through updateArticleNotes") {
    val now     = NDLADate.now().withNano(0)
    val article = TestData
      .sampleDomainArticle
      .copy(revision = Some(1), notes = Seq(EditorNote("note1", "user1", Status(DraftStatus.PLANNED, Set.empty), now)))
    val inserted = repository.insert(article)(using AutoSession)
    repository.updateArticleNotes(1L, Seq(EditorNote("note2", "user2", Status(DraftStatus.PLANNED, Set.empty), now)))(
      using AutoSession
    )
    repository.updateArticle(
      inserted.copy(notes = article.notes :+ EditorNote("note3", "user3", Status(DraftStatus.PLANNED, Set.empty), now))
    )(using AutoSession)
    val updated = repository.withId(inserted.id.get)(using AutoSession)
    updated.get.notes.length should be(3)
  }
}
