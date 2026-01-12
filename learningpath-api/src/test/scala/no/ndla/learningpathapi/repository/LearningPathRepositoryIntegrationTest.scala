/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.repository

import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.learningpath.{
  Description,
  EmbedType,
  EmbedUrl,
  Introduction,
  LearningPath,
  LearningPathStatus,
  LearningPathVerificationStatus,
  LearningStep,
  LearningpathCopyright,
  Message,
  StepStatus,
  StepType,
}
import no.ndla.common.model.domain.{Author, ContributorType, Tag, Title}
import no.ndla.learningpathapi.*
import no.ndla.learningpathapi.model.domain.*
import no.ndla.mapping.License
import no.ndla.database.{DBMigrator, DataSource}
import no.ndla.scalatestsuite.DatabaseIntegrationSuite
import org.mockito.Mockito.when
import scalikejdbc.*

import scala.util.Try
import no.ndla.common.model.domain.Priority
import no.ndla.common.model.domain.RevisionMeta

class LearningPathRepositoryIntegrationTest extends DatabaseIntegrationSuite with UnitSuite with TestEnvironment {
  override lazy val schemaName                      = "learningpathapi_test"
  override implicit lazy val dataSource: DataSource = testDataSource.get
  override implicit lazy val migrator: DBMigrator   = new DBMigrator

  var repository: LearningPathRepository = scala.compiletime.uninitialized

  val today: NDLADate                  = NDLADate.now().withNano(0)
  val clinton: Author                  = Author(ContributorType.Writer, "Hilla the Hun")
  val license: String                  = License.PublicDomain.toString
  val copyright: LearningpathCopyright = LearningpathCopyright(license, List(clinton))

  val DefaultLearningPath: LearningPath = LearningPath(
    id = None,
    revision = None,
    externalId = None,
    isBasedOn = None,
    title = List(Title("UNIT-TEST-1", "unknown")),
    description = List(Description("UNIT-TEST", "unknown")),
    introduction = List(Introduction("<section><p>UNIT-TEST</p></section>", "unknown")),
    coverPhotoId = None,
    duration = None,
    status = LearningPathStatus.PRIVATE,
    verificationStatus = LearningPathVerificationStatus.EXTERNAL,
    created = today,
    lastUpdated = today,
    tags = List(),
    owner = "UNIT-TEST",
    copyright = copyright,
    isMyNDLAOwner = false,
    learningsteps = Seq.empty,
    responsible = None,
    comments = Seq.empty,
    priority = Priority.Unspecified,
    revisionMeta = RevisionMeta.default,
    grepCodes = Seq.empty,
  )

  val DefaultLearningStep: LearningStep = LearningStep(
    id = None,
    revision = None,
    externalId = None,
    learningPathId = None,
    seqNo = 0,
    title = List(Title("UNIT-TEST", "unknown")),
    introduction = List(Introduction("UNIT-TEST", "unknown")),
    description = List(Description("UNIT-TEST", "unknown")),
    embedUrl = List(EmbedUrl("http://www.vg.no", "unknown", EmbedType.OEmbed)),
    articleId = None,
    `type` = StepType.TEXT,
    copyright = None,
    created = today,
    lastUpdated = today,
    owner = "UNIT-TEST",
    showTitle = true,
    status = StepStatus.ACTIVE,
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
    dataSource.connectToDatabase()
    if (serverIsListening) {
      migrator.migrate()
    }
  }

  def databaseIsAvailable: Boolean = {
    val res = Try(repository.learningPathCount)
    res.isSuccess
  }

  def serverIsListening: Boolean = {
    import java.net.Socket
    val server = props.MetaServer.unsafeGet
    val port   = props.MetaPort.unsafeGet
    scala.util.Try(new Socket(server, port)) match {
      case scala.util.Success(c) =>
        c.close()
        true
      case _ => false
    }
  }

  override def beforeEach(): Unit = {
    repository = new LearningPathRepository
    if (serverIsListening) {
      emptyTestDatabase
    }
  }

  test("That insert, fetch and delete works happy-day") {
    repository.inTransaction { implicit session =>
      val inserted = repository.insert(DefaultLearningPath).get
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
      repository.inTransaction { implicit session =>
        repository.insert(DefaultLearningPath.copy(owner = owner)).get
        throw new RuntimeException("Provoking exception inside transaction")
      }
      fail("Exception should prevent normal execution")
    } catch {
      case _: Throwable => repository.withOwner(owner).length should be(0)
    }
  }

  test("That updating several times is not throwing optimistic locking exception") {
    val inserted     = repository.insert(DefaultLearningPath).get
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
    val inserted = repository.insert(DefaultLearningPath).get
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

  test("That trying to update a learningPath with old revision while modifying steps throws optimistic locking exception") {
    val learningPath = repository.insert(DefaultLearningPath.copy(learningsteps = Seq(DefaultLearningStep))).get
    repository.update(learningPath.copy(learningsteps = Seq(DefaultLearningStep.copy(title = List(Title("First change", "unknown"))))))

    assertResult(
      s"Conflicting revision is detected for learningPath with id = ${learningPath.id} and revision = ${learningPath.revision}"
    ) {
      intercept[OptimisticLockException] {
        repository.update(
          learningPath.copy(learningsteps = Seq(DefaultLearningStep.copy(title = List(Title("Second change", "unknown")))))
        )
      }.getMessage
    }

    repository.deletePath(learningPath.id.get)
  }

  test("That learningPathsWithIsBasedOn returns all learningpaths that has one is based on id") {
    val learningPath1 = repository.insert(DefaultLearningPath).get
    val learningPath2 = repository.insert(DefaultLearningPath).get

    val copiedLearningPath1 = repository.insert(learningPath1.copy(id = None, isBasedOn = learningPath1.id)).get
    val copiedLearningPath2 = repository.insert(learningPath1.copy(id = None, isBasedOn = learningPath1.id)).get

    val learningPaths = repository.learningPathsWithIsBasedOn(learningPath1.id.get)

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
    val publicPath = repository
      .insert(
        DefaultLearningPath.copy(
          status = LearningPathStatus.PUBLISHED,
          tags = List(Tag(Seq("aaa"), "nb"), Tag(Seq("bbb"), "nn"), Tag(Seq("ccc"), "en")),
        )
      )
      .get

    val privatePath = repository.insert(DefaultLearningPath.copy(tags = List(Tag(Seq("ddd"), "nb")))).get

    val publicTags = repository.allPublishedTags
    publicTags should contain(Tag(Seq("aaa"), "nb"))
    publicTags should contain(Tag(Seq("bbb"), "nn"))
    publicTags should contain(Tag(Seq("ccc"), "en"))
    publicTags should not contain Tag(Seq("ddd"), "nb")

    repository.deletePath(publicPath.id.get)
    repository.deletePath(privatePath.id.get)
  }

  test("That allPublishedTags removes duplicates") {
    val publicPath1 = repository
      .insert(
        DefaultLearningPath.copy(
          status = LearningPathStatus.PUBLISHED,
          tags = List(Tag(Seq("aaa"), "nb"), Tag(Seq("aaa"), "nn")),
        )
      )
      .get
    val publicPath2 = repository
      .insert(
        DefaultLearningPath.copy(status = LearningPathStatus.PUBLISHED, tags = List(Tag(Seq("aaa", "bbb"), "nb")))
      )
      .get

    val publicTags = repository.allPublishedTags
    publicTags should contain(Tag(Seq("aaa", "bbb"), "nb"))
    publicTags should contain(Tag(Seq("aaa"), "nn"))

    publicTags.find(_.language.contains("nb")).map(_.tags.count(_ == "aaa")).getOrElse(0) should be(1)

    repository.deletePath(publicPath1.id.get)
    repository.deletePath(publicPath2.id.get)
  }

  test("That allPublishedContributors returns only published contributors") {
    val publicPath = repository
      .insert(
        DefaultLearningPath.copy(
          status = LearningPathStatus.PUBLISHED,
          copyright = LearningpathCopyright(
            "by",
            List(
              Author(ContributorType.Writer, "James Bond"),
              Author(ContributorType.Writer, "Christian Bond"),
              Author(ContributorType.Writer, "Jens Petrius"),
            ),
          ),
        )
      )
      .get

    val privatePath = repository
      .insert(
        DefaultLearningPath.copy(copyright =
          LearningpathCopyright("by", List(Author(ContributorType.Writer, "Test testesen")))
        )
      )
      .get

    val publicContributors = repository.allPublishedContributors
    publicContributors should contain(Author(ContributorType.Writer, "James Bond"))
    publicContributors should contain(Author(ContributorType.Writer, "Christian Bond"))
    publicContributors should contain(Author(ContributorType.Writer, "Jens Petrius"))
    publicContributors should not contain Author(ContributorType.Writer, "Test testesen")

    repository.deletePath(publicPath.id.get)
    repository.deletePath(privatePath.id.get)
  }

  test("That allPublishedContributors removes duplicates") {
    val publicPath1 = repository
      .insert(
        DefaultLearningPath.copy(
          status = LearningPathStatus.PUBLISHED,
          copyright = LearningpathCopyright(
            "by",
            List(
              Author(ContributorType.Writer, "James Bond"),
              Author(ContributorType.Writer, "Christian Bond"),
              Author(ContributorType.Writer, "Jens Petrius"),
            ),
          ),
        )
      )
      .get
    val publicPath2 = repository
      .insert(
        DefaultLearningPath.copy(
          status = LearningPathStatus.PUBLISHED,
          copyright = LearningpathCopyright(
            "by",
            List(Author(ContributorType.Writer, "James Bond"), Author(ContributorType.Writer, "Test testesen")),
          ),
        )
      )
      .get

    val publicContributors = repository.allPublishedContributors
    publicContributors should contain(Author(ContributorType.Writer, "James Bond"))
    publicContributors should contain(Author(ContributorType.Writer, "Christian Bond"))
    publicContributors should contain(Author(ContributorType.Writer, "Jens Petrius"))
    publicContributors should contain(Author(ContributorType.Writer, "Test testesen"))

    publicContributors.count(_.name == "James Bond") should be(1)

    repository.deletePath(publicPath1.id.get)
    repository.deletePath(publicPath2.id.get)
  }

  test("That only learningsteps with status ACTIVE are returned together with a learningpath") {
    val learningPath = repository
      .insert(
        DefaultLearningPath.copy(
          learningsteps = Seq(
            DefaultLearningStep,
            DefaultLearningStep,
            DefaultLearningStep.copy(status = StepStatus.DELETED),
          )
        )
      )
      .get

    learningPath.id.isDefined should be(true)
    val savedLearningPath = repository.withId(learningPath.id.get)
    savedLearningPath.isDefined should be(true)
    savedLearningPath.get.learningsteps.size should be(2)
    savedLearningPath.get.learningsteps.forall(_.status == StepStatus.ACTIVE) should be(true)

    repository.deletePath(learningPath.id.get)
  }

  test("That getLearningPathByPage returns correct result when pageSize is smaller than amount of steps") {
    when(clock.now()).thenReturn(NDLADate.fromUnixTime(0))
    val steps = List(DefaultLearningStep, DefaultLearningStep, DefaultLearningStep)

    val learningPath = repository
      .insert(DefaultLearningPath.copy(learningsteps = steps, status = LearningPathStatus.PUBLISHED))
      .get

    val page1 = repository.getPublishedLearningPathByPage(2, 0)
    val page2 = repository.getPublishedLearningPathByPage(2, 2)

    page1.length should be(List(learningPath).length)
    page2 should be(List.empty)

    repository.deletePath(learningPath.id.get)
  }

  test("That getLearningPathByPage returns only published results") {
    val steps = List(DefaultLearningStep, DefaultLearningStep, DefaultLearningStep)

    val learningPath1 = repository
      .insert(DefaultLearningPath.copy(learningsteps = steps, status = LearningPathStatus.PRIVATE))
      .get
    val learningPath2 = repository
      .insert(DefaultLearningPath.copy(learningsteps = steps, status = LearningPathStatus.PRIVATE))
      .get
    val learningPath3 = repository
      .insert(DefaultLearningPath.copy(learningsteps = steps, status = LearningPathStatus.PUBLISHED))
      .get

    val page1 = repository.getPublishedLearningPathByPage(2, 0)
    val page2 = repository.getPublishedLearningPathByPage(2, 2)

    page1.length should be(List(learningPath3).length)
    page2 should be(List.empty)

    repository.deletePath(learningPath1.id.get)
    repository.deletePath(learningPath2.id.get)
    repository.deletePath(learningPath3.id.get)
  }

  test("That inserted and fetched entry stays the same") {
    when(clock.now()).thenReturn(today)
    val steps = Vector(DefaultLearningStep, DefaultLearningStep, DefaultLearningStep)

    val path = DefaultLearningPath.copy(
      learningsteps = steps,
      status = LearningPathStatus.PRIVATE,
      owner = "123",
      message = Some(Message("this is message", "kwawk", clock.now())),
    )

    val inserted = repository.insert(path).get
    val fetched  = repository.withId(inserted.id.get)

    inserted should be(fetched.get)
    repository.deletePath(inserted.id.get)
  }

  test("That get by ids gets all results") {
    val learningPath1 = repository.insert(DefaultLearningPath).get
    val learningPath2 = repository.insert(DefaultLearningPath).get
    val learningPath3 = repository.insert(DefaultLearningPath).get
    val learningPath4 = repository.insert(DefaultLearningPath).get

    val learningpaths = repository.pageWithIds(
      Seq(learningPath1.id.get, learningPath2.id.get, learningPath3.id.get, learningPath4.id.get),
      10,
      0,
    )
    learningpaths.length should be(4)
  }

  test("That learning step sample only returns learningpaths containing a learningstep with an external link") {
    repository
      .insert(
        DefaultLearningPath.copy(
          learningsteps = List(DefaultLearningStep, DefaultLearningStep.copy(embedUrl = List.empty)),
          status = LearningPathStatus.UNLISTED,
          isMyNDLAOwner = true,
        )
      )
      .get
    repository
      .insert(
        DefaultLearningPath.copy(
          learningsteps = List(
            DefaultLearningStep.copy(embedUrl = List.empty, articleId = Some(1)),
            DefaultLearningStep.copy(embedUrl = List.empty, articleId = Some(2)),
          ),
          status = LearningPathStatus.UNLISTED,
          isMyNDLAOwner = true,
        )
      )
      .get
    val lp3 = repository
      .insert(
        DefaultLearningPath.copy(
          learningsteps = List(DefaultLearningStep),
          status = LearningPathStatus.UNLISTED,
          isMyNDLAOwner = true,
        )
      )
      .get

    val learningpaths = repository.getExternalLinkStepSamples()
    learningpaths.length should be(2)
    learningpaths.map(_.id) should contain(lp3.id)
  }

  test("That learning step sample only returns published learningpaths containing an external link") {
    repository
      .insert(DefaultLearningPath.copy(learningsteps = List(DefaultLearningStep), isMyNDLAOwner = true))
      .get
    val lp2 = repository.insert(
      DefaultLearningPath.copy(
        learningsteps = List(DefaultLearningStep),
        status = LearningPathStatus.UNLISTED,
        isMyNDLAOwner = true,
      )
    ).get

    val learningpaths = repository.getExternalLinkStepSamples()
    learningpaths.length should be(1)
    learningpaths.head.id should be(lp2.id)
  }

  test("That learning step sample only returns learningpaths owned by MyNDLA") {
    repository.insert(
      DefaultLearningPath.copy(
        learningsteps = List(DefaultLearningStep),
        status = LearningPathStatus.UNLISTED,
        isMyNDLAOwner = false,
      )
    ).get
    val lp2 = repository.insert(
      DefaultLearningPath.copy(
        learningsteps = List(DefaultLearningStep),
        status = LearningPathStatus.UNLISTED,
        isMyNDLAOwner = true,
      )
    ).get

    val learningpaths = repository.getExternalLinkStepSamples()
    learningpaths.length should be(1)
    learningpaths.head.id should be(lp2.id)
  }

  test("That learning step sample only returns learningpaths with an active step with an external link") {
    repository.insert(
      DefaultLearningPath.copy(
        learningsteps = List(DefaultLearningStep.copy(status = StepStatus.DELETED)),
        status = LearningPathStatus.UNLISTED,
        isMyNDLAOwner = true,
      )
    ).get
    val lp2 = repository.insert(
      DefaultLearningPath.copy(
        learningsteps = List(DefaultLearningStep),
        status = LearningPathStatus.UNLISTED,
        isMyNDLAOwner = true,
      )
    ).get

    val learningpaths = repository.getExternalLinkStepSamples()
    learningpaths.length should be(1)
    learningpaths.head.id should be(lp2.id)
  }

  def emptyTestDatabase: Boolean = {
    DB autoCommit (implicit session => {
      sql"delete from learningpaths;".execute()(using session)
      sql"delete from learningsteps;".execute()(using session)
    })
  }

  def deleteAllWithOwner(owner: String): Unit = {
    repository.inTransaction { implicit session =>
      repository.withOwner(owner).foreach(lp => repository.deletePath(lp.id.get))
    }
  }
}
