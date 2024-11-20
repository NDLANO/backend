/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.repository

import com.zaxxer.hikari.HikariDataSource
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.learningpath.{
  Description,
  EmbedType,
  EmbedUrl,
  LearningPath,
  LearningPathStatus,
  LearningPathVerificationStatus,
  LearningStep,
  LearningpathCopyright,
  Message,
  StepStatus,
  StepType
}
import no.ndla.common.model.domain.{Author, Tag, Title}
import no.ndla.learningpathapi.*
import no.ndla.learningpathapi.model.domain.*
import no.ndla.scalatestsuite.IntegrationSuite
import org.mockito.Mockito.when
import scalikejdbc.*

import scala.util.Try

class LearningPathRepositoryComponentIntegrationTest
    extends IntegrationSuite(EnablePostgresContainer = true, schemaName = "learningpathapi_test")
    with UnitSuite
    with TestEnvironment {

  override val dataSource: HikariDataSource = testDataSource.get
  override val migrator: DBMigrator         = DBMigrator()

  var repository: LearningPathRepository = _

  val clinton: Author                  = Author("author", "Hilla the Hun")
  val license                          = "publicdomain"
  val copyright: LearningpathCopyright = LearningpathCopyright(license, List(clinton))

  val DefaultLearningPath: LearningPath = LearningPath(
    None,
    None,
    None,
    None,
    List(Title("UNIT-TEST-1", "unknown")),
    List(Description("UNIT-TEST", "unknown")),
    None,
    None,
    LearningPathStatus.PRIVATE,
    LearningPathVerificationStatus.EXTERNAL,
    NDLADate.now().withNano(0),
    NDLADate.now().withNano(0),
    List(),
    "UNIT-TEST",
    copyright
  )

  val DefaultLearningStep: LearningStep = LearningStep(
    None,
    None,
    None,
    None,
    0,
    List(Title("UNIT-TEST", "unknown")),
    List(Description("UNIT-TEST", "unknown")),
    List(EmbedUrl("http://www.vg.no", "unknown", EmbedType.OEmbed)),
    StepType.TEXT,
    None,
    showTitle = true,
    StepStatus.ACTIVE
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
    DataSource.connectToDatabase()
    migrator.migrate()
  }

  def databaseIsAvailable: Boolean = {
    val res = Try(repository.learningPathCount)
    res.isSuccess
  }

  override def beforeEach(): Unit = {
    repository = new LearningPathRepository
    if (databaseIsAvailable) {
      emptyTestDatabase
    }
  }

  test("That insert, fetch and delete works happy-day") {
    inTransaction { implicit session =>
      val inserted = repository.insert(DefaultLearningPath)
      inserted.id.isDefined should be(true)

      val fetched = repository.withId(inserted.id.get)
      fetched.isDefined should be(true)
      fetched.get.id.get should equal(inserted.id.get)

      repository.deletePath(inserted.id.get)
    }
  }

  test("That transaction is rolled back if exception is thrown") {
    val owner = s"unit-test-${System.currentTimeMillis()}"
    deleteAllWithOwner(owner)

    try {
      inTransaction { implicit session =>
        repository.insert(DefaultLearningPath.copy(owner = owner))
        throw new RuntimeException("Provoking exception inside transaction")
      }
      fail("Exception should prevent normal execution")
    } catch {
      case _: Throwable => repository.withOwner(owner).length should be(0)
    }
  }

  test("That updating several times is not throwing optimistic locking exception") {
    val inserted     = repository.insert(DefaultLearningPath)
    val firstUpdate  = repository.update(inserted.copy(title = List(Title("First change", "unknown"))))
    val secondUpdate = repository.update(firstUpdate.copy(title = List(Title("Second change", "unknown"))))
    val thirdUpdate  = repository.update(secondUpdate.copy(title = List(Title("Third change", "unknown"))))

    inserted.revision should equal(Some(1))
    firstUpdate.revision should equal(Some(2))
    secondUpdate.revision should equal(Some(3))
    thirdUpdate.revision should equal(Some(4))

    repository.deletePath(thirdUpdate.id.get)
  }

  test("That trying to update a learningPath with old revision number throws optimistic locking exception") {
    val inserted = repository.insert(DefaultLearningPath)
    repository.update(inserted.copy(title = List(Title("First change", "unknown"))))

    assertResult(
      s"Conflicting revision is detected for learningPath with id = ${inserted.id} and revision = ${inserted.revision}"
    ) {
      intercept[OptimisticLockException] {
        repository.update(inserted.copy(title = List(Title("Second change, but with old revision", "unknown"))))
      }.getMessage
    }

    repository.deletePath(inserted.id.get)
  }

  test("That trying to update a learningStep with old revision throws optimistic locking exception") {
    val learningPath = repository.insert(DefaultLearningPath)
    val insertedStep = repository.insertLearningStep(DefaultLearningStep.copy(learningPathId = learningPath.id))
    repository.updateLearningStep(insertedStep.copy(title = List(Title("First change", "unknown"))))

    assertResult(
      s"Conflicting revision is detected for learningStep with id = ${insertedStep.id} and revision = ${insertedStep.revision}"
    ) {
      intercept[OptimisticLockException] {
        repository.updateLearningStep(insertedStep.copy(title = List(Title("First change", "unknown"))))
      }.getMessage
    }

    repository.deletePath(learningPath.id.get)
  }

  test("That learningPathsWithIsBasedOn returns all learningpaths that has one is based on id") {
    val learningPath1 = repository.insert(DefaultLearningPath)
    val learningPath2 = repository.insert(DefaultLearningPath)

    val copiedLearningPath1 = repository.insert(
      learningPath1.copy(
        id = None,
        isBasedOn = learningPath1.id
      )
    )
    val copiedLearningPath2 = repository.insert(
      learningPath1.copy(
        id = None,
        isBasedOn = learningPath1.id
      )
    )

    val learningPaths =
      repository.learningPathsWithIsBasedOn(learningPath1.id.get)

    learningPaths.map(_.id) should contain(copiedLearningPath1.id)
    learningPaths.map(_.id) should contain(copiedLearningPath2.id)
    learningPaths.map(_.id) should not contain learningPath1.id
    learningPaths.map(_.id) should not contain learningPath2.id
    learningPaths should have length 2

    repository.deletePath(learningPath1.id.get)
    repository.deletePath(learningPath2.id.get)
    repository.deletePath(copiedLearningPath1.id.get)
    repository.deletePath(copiedLearningPath2.id.get)

  }

  test("That allPublishedTags returns only published tags") {
    val publicPath = repository.insert(
      DefaultLearningPath.copy(
        status = LearningPathStatus.PUBLISHED,
        tags = List(
          Tag(Seq("aaa"), "nb"),
          Tag(Seq("bbb"), "nn"),
          Tag(Seq("ccc"), "en")
        )
      )
    )

    val privatePath = repository.insert(DefaultLearningPath.copy(tags = List(Tag(Seq("ddd"), "nb"))))

    val publicTags = repository.allPublishedTags
    publicTags should contain(Tag(Seq("aaa"), "nb"))
    publicTags should contain(Tag(Seq("bbb"), "nn"))
    publicTags should contain(Tag(Seq("ccc"), "en"))
    publicTags should not contain Tag(Seq("ddd"), "nb")

    repository.deletePath(publicPath.id.get)
    repository.deletePath(privatePath.id.get)
  }

  test("That allPublishedTags removes duplicates") {
    val publicPath1 = repository.insert(
      DefaultLearningPath.copy(
        status = LearningPathStatus.PUBLISHED,
        tags = List(Tag(Seq("aaa"), "nb"), Tag(Seq("aaa"), "nn"))
      )
    )
    val publicPath2 = repository.insert(
      DefaultLearningPath.copy(
        status = LearningPathStatus.PUBLISHED,
        tags = List(Tag(Seq("aaa", "bbb"), "nb"))
      )
    )

    val publicTags = repository.allPublishedTags
    publicTags should contain(Tag(Seq("aaa", "bbb"), "nb"))
    publicTags should contain(Tag(Seq("aaa"), "nn"))

    publicTags
      .find(_.language.contains("nb"))
      .map(_.tags.count(_ == "aaa"))
      .getOrElse(0) should be(1)

    repository.deletePath(publicPath1.id.get)
    repository.deletePath(publicPath2.id.get)
  }

  test("That allPublishedContributors returns only published contributors") {
    val publicPath = repository.insert(
      DefaultLearningPath.copy(
        status = LearningPathStatus.PUBLISHED,
        copyright = LearningpathCopyright(
          "by",
          List(
            Author("forfatter", "James Bond"),
            Author("forfatter", "Christian Bond"),
            Author("forfatter", "Jens Petrius")
          )
        )
      )
    )

    val privatePath = repository.insert(
      DefaultLearningPath.copy(
        copyright = LearningpathCopyright("by", List(Author("forfatter", "Test testesen")))
      )
    )

    val publicContributors = repository.allPublishedContributors
    publicContributors should contain(Author("forfatter", "James Bond"))
    publicContributors should contain(Author("forfatter", "Christian Bond"))
    publicContributors should contain(Author("forfatter", "Jens Petrius"))
    publicContributors should not contain Author("forfatter", "Test testesen")

    repository.deletePath(publicPath.id.get)
    repository.deletePath(privatePath.id.get)
  }

  test("That allPublishedContributors removes duplicates") {
    val publicPath1 = repository.insert(
      DefaultLearningPath.copy(
        status = LearningPathStatus.PUBLISHED,
        copyright = LearningpathCopyright(
          "by",
          List(
            Author("forfatter", "James Bond"),
            Author("forfatter", "Christian Bond"),
            Author("forfatter", "Jens Petrius")
          )
        )
      )
    )
    val publicPath2 = repository.insert(
      DefaultLearningPath.copy(
        status = LearningPathStatus.PUBLISHED,
        copyright =
          LearningpathCopyright("by", List(Author("forfatter", "James Bond"), Author("forfatter", "Test testesen")))
      )
    )

    val publicContributors = repository.allPublishedContributors
    publicContributors should contain(Author("forfatter", "James Bond"))
    publicContributors should contain(Author("forfatter", "Christian Bond"))
    publicContributors should contain(Author("forfatter", "Jens Petrius"))
    publicContributors should contain(Author("forfatter", "Test testesen"))

    publicContributors.count(_.name == "James Bond") should be(1)

    repository.deletePath(publicPath1.id.get)
    repository.deletePath(publicPath2.id.get)
  }

  test("That only learningsteps with status ACTIVE are returned together with a learningpath") {
    val learningPath = repository.insert(DefaultLearningPath)
    repository.insertLearningStep(DefaultLearningStep.copy(learningPathId = learningPath.id))
    repository.insertLearningStep(DefaultLearningStep.copy(learningPathId = learningPath.id))
    repository.insertLearningStep(
      DefaultLearningStep.copy(learningPathId = learningPath.id, status = StepStatus.DELETED)
    )

    learningPath.id.isDefined should be(true)
    val savedLearningPath = repository.withId(learningPath.id.get)
    savedLearningPath.isDefined should be(true)
    savedLearningPath.get.learningsteps.get.size should be(2)
    savedLearningPath.get.learningsteps.get
      .forall(_.status == StepStatus.ACTIVE) should be(true)

    repository.deletePath(learningPath.id.get)
  }

  test("That getLearningPathByPage returns correct result when pageSize is smaller than amount of steps") {
    val steps = List(
      DefaultLearningStep,
      DefaultLearningStep,
      DefaultLearningStep
    )

    val learningPath =
      repository.insert(DefaultLearningPath.copy(learningsteps = Some(steps), status = LearningPathStatus.PUBLISHED))

    val page1 = repository.getPublishedLearningPathByPage(2, 0)
    val page2 = repository.getPublishedLearningPathByPage(2, 2)

    page1 should be(List(learningPath))
    page2 should be(List.empty)

    repository.deletePath(learningPath.id.get)
  }

  test("That getLeraningPathByPage returns only published results") {
    val steps = List(
      DefaultLearningStep,
      DefaultLearningStep,
      DefaultLearningStep
    )

    val learningPath1 =
      repository.insert(DefaultLearningPath.copy(learningsteps = Some(steps), status = LearningPathStatus.PRIVATE))
    val learningPath2 =
      repository.insert(DefaultLearningPath.copy(learningsteps = Some(steps), status = LearningPathStatus.PRIVATE))
    val learningPath3 =
      repository.insert(DefaultLearningPath.copy(learningsteps = Some(steps), status = LearningPathStatus.PUBLISHED))

    val page1 = repository.getPublishedLearningPathByPage(2, 0)
    val page2 = repository.getPublishedLearningPathByPage(2, 2)

    page1 should be(List(learningPath3))
    page2 should be(List.empty)

    repository.deletePath(learningPath1.id.get)
    repository.deletePath(learningPath2.id.get)
    repository.deletePath(learningPath3.id.get)
  }

  test("That inserted and fetched entry stays the same") {
    when(clock.now()).thenReturn(NDLADate.fromUnixTime(0))
    val steps = Vector(
      DefaultLearningStep,
      DefaultLearningStep,
      DefaultLearningStep
    )

    val path = DefaultLearningPath.copy(
      learningsteps = Some(steps),
      status = LearningPathStatus.PRIVATE,
      owner = "123",
      message = Some(Message("this is message", "kwawk", clock.now()))
    )

    val inserted = repository.insert(path)
    val fetched  = repository.withId(inserted.id.get)

    inserted should be(fetched.get)
    repository.deletePath(inserted.id.get)
  }

  test("That get by ids gets all results") {
    val learningPath1 = repository.insert(DefaultLearningPath)
    val learningPath2 = repository.insert(DefaultLearningPath)
    val learningPath3 = repository.insert(DefaultLearningPath)
    val learningPath4 = repository.insert(DefaultLearningPath)

    val learningpaths = repository.pageWithIds(
      Seq(learningPath1.id.get, learningPath2.id.get, learningPath3.id.get, learningPath4.id.get),
      10,
      0
    )
    learningpaths.length should be(4)
  }

  def emptyTestDatabase: Boolean = {
    DB autoCommit (implicit session => {
      sql"delete from learningpaths;".execute()(session)
      sql"delete from learningsteps;".execute()(session)
    })
  }

  def deleteAllWithOwner(owner: String): Unit = {
    inTransaction { implicit session =>
      repository
        .withOwner(owner)
        .foreach(lp => repository.deletePath(lp.id.get))
    }
  }
}
