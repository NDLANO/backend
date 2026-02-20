/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.e2e

import io.circe.Decoder
import no.ndla.common.{CirceUtil, Clock}
import no.ndla.common.configuration.Prop
import no.ndla.common.model.NDLADate
import no.ndla.common.model.api.{AuthorDTO, LicenseDTO}
import no.ndla.common.model.domain.learningpath.{EmbedType, LearningPath, StepType}
import no.ndla.learningpathapi.model.api.*
import no.ndla.learningpathapi.*
import no.ndla.learningpathapi.integration.{Node, TaxonomyApiClient}
import no.ndla.scalatestsuite.{DatabaseIntegrationSuite, ElasticsearchIntegrationSuite}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{when, withSettings}
import org.mockito.invocation.InvocationOnMock
import org.mockito.quality.Strictness
import org.testcontainers.postgresql.PostgreSQLContainer
import sttp.client3.{Identity, RequestT, Response}
import sttp.client3.quick.*

import java.util.concurrent.Executors
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.Success

class LearningPathAndStepCreationTests
    extends ElasticsearchIntegrationSuite
    with DatabaseIntegrationSuite
    with UnitSuite
    with TestEnvironment {

  val learningpathApiPort: Int                             = findFreePort
  val pgc: PostgreSQLContainer                             = postgresContainer.get
  val learningpathApiProperties: LearningpathApiProperties = new LearningpathApiProperties {
    override def ApplicationPort: Int       = learningpathApiPort
    override val MetaServer: Prop[String]   = propFromTestValue("META_SERVER", pgc.getHost)
    override val MetaResource: Prop[String] = propFromTestValue("META_RESOURCE", pgc.getDatabaseName)
    override val MetaUserName: Prop[String] = propFromTestValue("META_USER_NAME", pgc.getUsername)
    override val MetaPassword: Prop[String] = propFromTestValue("META_PASSWORD", pgc.getPassword)
    override val MetaPort: Prop[Int]        = propFromTestValue("META_PORT", pgc.getMappedPort(5432))
    override val MetaSchema: Prop[String]   = propFromTestValue("META_SCHEMA", "testschema")
    override def disableWarmup: Boolean     = true
    override def SearchServer: String       = elasticSearchHost.get
  }

  val someDate: NDLADate = NDLADate.of(2017, 1, 1, 1, 59)

  lazy val learningpathApi: MainClass = new MainClass(learningpathApiProperties) {
    override val componentRegistry: ComponentRegistry = new ComponentRegistry(learningpathApiProperties) {
      override implicit lazy val clock: Clock = {
        val mockClock = mock[Clock](withSettings.strictness(Strictness.LENIENT))
        when(mockClock.now()).thenReturn(someDate)
        mockClock
      }
      override given taxonomyApiClient: TaxonomyApiClient = {
        val client = mock[TaxonomyApiClient]
        when(client.updateTaxonomyForLearningPath(any, any, any)).thenAnswer { (i: InvocationOnMock) =>
          Success(i.getArgument[LearningPath](0))
        }
        when(client.queryNodes(any[Long])).thenReturn(Success(List.empty[Node]))
        client
      }
    }
  }

  lazy val testClock: Clock = learningpathApi.componentRegistry.clock

  val learningpathApiBaseUrl: String = s"http://localhost:$learningpathApiPort"
  val learningpathApiLPUrl: String   = s"$learningpathApiBaseUrl/learningpath-api/v2/learningpaths"

  override def beforeAll(): Unit = {
    super.beforeAll()
    implicit val ec: ExecutionContextExecutorService =
      ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
    Future {
      learningpathApi.run(Array.empty)
    }: Unit
    blockUntilHealthy(s"$learningpathApiBaseUrl/health/readiness")
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    learningpathApi
      .componentRegistry
      .learningPathRepository
      .inTransaction(implicit session => {
        learningpathApi.componentRegistry.learningPathRepository.deleteAllPathsAndSteps(using session)
      })
  }

  override def afterAll(): Unit = {
    super.afterAll()
  }

  val fakeToken =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6Inh4eHl5eSIsImlzcyI6Imh0dHBzOi8vbmRsYS5ldS5hdXRoMC5jb20vIiwic3ViIjoieHh4eXl5QGNsaWVudHMiLCJhdWQiOiJuZGxhX3N5c3RlbSIsImlhdCI6MTUxMDMwNTc3MywiZXhwIjoxNTEwMzkyMTczLCJwZXJtaXNzaW9ucyI6WyJhcnRpY2xlczpwdWJsaXNoIiwiZHJhZnRzOndyaXRlIiwiZHJhZnRzOnNldF90b19wdWJsaXNoIiwiYXJ0aWNsZXM6d3JpdGUiLCJsZWFybmluZ3BhdGg6d3JpdGUiLCJsZWFybmluZ3BhdGg6cHVibGlzaCIsImxlYXJuaW5ncGF0aDphZG1pbiJdLCJndHkiOiJjbGllbnQtY3JlZGVudGlhbHMifQ.XnP0ywYk-A0j9bGZJBCDNA5fZ4OuGRLkXFBBr3IYD50"

  private def sendAuthed(request: RequestT[Identity, String, Any]): Response[String] = {
    simpleHttpClient.send(
      request
        .header("Content-type", "application/json")
        .header("Authorization", s"Bearer $fakeToken")
        .readTimeout(10.seconds)
    )
  }

  private def parseAs[T: Decoder](res: Response[String]): T = {
    CirceUtil.unsafeParseAs[T](res.body)
  }

  def createLearningpath(
      title: String,
      tags: Option[Seq[String]] = None,
      language: String = "nb",
      duration: Option[Int] = None,
  ): LearningPathV2DTO = {
    val dto = NewLearningPathV2DTO(
      title = title,
      description = None,
      introduction = None,
      coverPhotoMetaUrl = None,
      duration = duration,
      tags = tags,
      language = language,
      copyright = None,
      responsibleId = None,
      comments = None,
      priority = None,
      revisionMeta = None,
      grepCodes = None,
    )

    val res = sendAuthed(quickRequest.post(uri"$learningpathApiLPUrl").body(CirceUtil.toJsonString(dto)))
    res.code.code should be(201)
    parseAs[LearningPathV2DTO](res)
  }

  def copyLearningpath(pathId: Long, title: String): LearningPathV2DTO = {
    val dto = NewCopyLearningPathV2DTO(
      title = title,
      introduction = None,
      description = None,
      language = "nb",
      coverPhotoMetaUrl = None,
      duration = None,
      tags = None,
      copyright = None,
    )
    val res = sendAuthed(quickRequest.post(uri"$learningpathApiLPUrl/$pathId/copy").body(CirceUtil.toJsonString(dto)))
    res.code.code should be(201)
    parseAs[LearningPathV2DTO](res)
  }

  def createLearningStep(
      pathId: Long,
      title: String,
      language: String = "nb",
      embedUrl: Option[EmbedUrlV2DTO] =
        Some(EmbedUrlV2DTO(url = "https://www.example.com/", embedType = EmbedType.External.entryName)),
      articleId: Option[Long] = None,
  ): LearningStepV2DTO = {
    val dto = NewLearningStepV2DTO(
      title = title,
      introduction = None,
      description = None,
      language = language,
      embedUrl = embedUrl,
      articleId = articleId,
      showTitle = false,
      `type` = StepType.TEXT.toString,
      license = None,
      copyright = None,
    )
    val res =
      sendAuthed(quickRequest.post(uri"$learningpathApiLPUrl/$pathId/learningsteps").body(CirceUtil.toJsonString(dto)))
    res.code.code should be(201)
    parseAs[LearningStepV2DTO](res)
  }

  def getLearningPathResponse(pathId: Long): Response[String] = {
    sendAuthed(quickRequest.get(uri"$learningpathApiLPUrl/$pathId"))
  }

  def getLearningPath(pathId: Long): LearningPathV2DTO = {
    val res = getLearningPathResponse(pathId)
    res.code.code should be(200)
    parseAs[LearningPathV2DTO](res)
  }

  def getLearningStepResponse(pathId: Long, stepId: Long): Response[String] = {
    sendAuthed(quickRequest.get(uri"$learningpathApiLPUrl/$pathId/learningsteps/$stepId"))
  }

  def getLearningStep(pathId: Long, stepId: Long): LearningStepV2DTO = {
    val res = getLearningStepResponse(pathId, stepId)
    res.code.code should be(200)
    parseAs[LearningStepV2DTO](res)
  }

  def updateLearningPathTitle(pathId: Long, revision: Int, language: String, title: String): LearningPathV2DTO = {
    val body = s"""{"revision":$revision,"title":"$title","language":"$language"}"""
    val res  = sendAuthed(quickRequest.patch(uri"$learningpathApiLPUrl/$pathId").body(body))
    res.code.code should be(200)
    parseAs[LearningPathV2DTO](res)
  }

  def updateLearningStepTitle(
      pathId: Long,
      stepId: Long,
      revision: Int,
      language: String,
      title: String,
  ): LearningStepV2DTO = {
    val body = s"""{"revision":$revision,"title":"$title","language":"$language"}"""
    val res  = sendAuthed(quickRequest.patch(uri"$learningpathApiLPUrl/$pathId/learningsteps/$stepId").body(body))
    res.code.code should be(200)
    parseAs[LearningStepV2DTO](res)
  }

  def getLearningPathStatus(pathId: Long): LearningPathStatusDTO = {
    val res = sendAuthed(quickRequest.get(uri"$learningpathApiLPUrl/$pathId/status"))
    res.code.code should be(200)
    parseAs[LearningPathStatusDTO](res)
  }

  def getLearningStepStatus(pathId: Long, stepId: Long): LearningStepStatusDTO = {
    val res = sendAuthed(quickRequest.get(uri"$learningpathApiLPUrl/$pathId/learningsteps/$stepId/status"))
    res.code.code should be(200)
    parseAs[LearningStepStatusDTO](res)
  }

  def updateLearningPathStatus(pathId: Long, status: String, message: Option[String] = None): LearningPathV2DTO = {
    val dto = UpdateLearningPathStatusDTO(status = status, message = message)
    val res = sendAuthed(quickRequest.put(uri"$learningpathApiLPUrl/$pathId/status").body(CirceUtil.toJsonString(dto)))
    res.code.code should be(200)
    parseAs[LearningPathV2DTO](res)
  }

  def updateLearningStepStatus(pathId: Long, stepId: Long, status: String): LearningStepV2DTO = {
    val dto = LearningStepStatusDTO(status = status)
    val res = sendAuthed(
      quickRequest
        .put(uri"$learningpathApiLPUrl/$pathId/learningsteps/$stepId/status")
        .body(CirceUtil.toJsonString(dto))
    )
    res.code.code should be(200)
    parseAs[LearningStepV2DTO](res)
  }

  def updateLearningStepSeqNo(pathId: Long, stepId: Long, seqNo: Int): LearningStepSeqNoDTO = {
    val dto = LearningStepSeqNoDTO(seqNo)
    val res = sendAuthed(
      quickRequest.put(uri"$learningpathApiLPUrl/$pathId/learningsteps/$stepId/seqNo").body(CirceUtil.toJsonString(dto))
    )
    res.code.code should be(200)
    parseAs[LearningStepSeqNoDTO](res)
  }

  def deleteLearningPathLanguage(pathId: Long, language: String): LearningPathV2DTO = {
    val res = sendAuthed(quickRequest.delete(uri"$learningpathApiLPUrl/$pathId/language/$language"))
    res.code.code should be(200)
    parseAs[LearningPathV2DTO](res)
  }

  def deleteLearningStepLanguage(pathId: Long, stepId: Long, language: String): LearningStepV2DTO = {
    val res =
      sendAuthed(quickRequest.delete(uri"$learningpathApiLPUrl/$pathId/learningsteps/$stepId/language/$language"))
    res.code.code should be(200)
    parseAs[LearningStepV2DTO](res)
  }

  def updateTaxonomyForLearningPath(pathId: Long): LearningPathV2DTO = {
    val res = sendAuthed(
      quickRequest.post(
        uri"$learningpathApiLPUrl/$pathId/update-taxonomy?language=nb&fallback=true&create-if-missing=true"
      )
    )
    res.code.code should be(200)
    parseAs[LearningPathV2DTO](res)
  }

  def deleteLearningPath(pathId: Long): Response[String] = {
    sendAuthed(quickRequest.delete(uri"$learningpathApiLPUrl/$pathId"))
  }

  def deleteLearningStep(pathId: Long, stepId: Long): Response[String] = {
    sendAuthed(quickRequest.delete(uri"$learningpathApiLPUrl/$pathId/learningsteps/$stepId"))
  }

  test("Learningpath endpoints support full CRUD and management operations") {
    val created = createLearningpath(title = "LearningPath CRUD", tags = Some(Seq("ndla-tag")))
    created.title.title should be("LearningPath CRUD")

    val fetched = getLearningPath(created.id)
    fetched.id should be(created.id)

    val updated = updateLearningPathTitle(created.id, fetched.revision, "nb", "LearningPath Updated")
    updated.title.title should be("LearningPath Updated")

    val statusBeforeUpdate = getLearningPathStatus(created.id)
    statusBeforeUpdate.status should be("PRIVATE")

    val unlistedPath = updateLearningPathStatus(created.id, "UNLISTED", Some("Ready for sharing"))
    unlistedPath.status should be("UNLISTED")

    val withStatusRes = sendAuthed(quickRequest.get(uri"$learningpathApiLPUrl/status/UNLISTED"))
    withStatusRes.code.code should be(200)
    val withStatusPaths = parseAs[List[LearningPathV2DTO]](withStatusRes)
    withStatusPaths.map(_.id) should contain(created.id)

    val englishPathVersion = updateLearningPathTitle(created.id, unlistedPath.revision, "en", "LearningPath English")
    englishPathVersion.supportedLanguages should contain("en")

    val afterLanguageDelete = deleteLearningPathLanguage(created.id, "en")
    afterLanguageDelete.supportedLanguages should not contain "en"

    val taxonomyUpdated = updateTaxonomyForLearningPath(created.id)
    taxonomyUpdated.id should be(created.id)

    val copied = copyLearningpath(created.id, "LearningPath Copy")
    copied.isBasedOn should be(Some(created.id))

    val idsQuery = s"${created.id},${copied.id}"
    val byIdsRes = sendAuthed(quickRequest.get(uri"$learningpathApiLPUrl/ids?ids=$idsQuery&language=nb&fallback=true"))
    byIdsRes.code.code should be(200)
    val byIds = parseAs[List[LearningPathV2DTO]](byIdsRes)
    byIds.map(_.id).toSet should be(Set(created.id, copied.id))

    val mineRes = sendAuthed(quickRequest.get(uri"$learningpathApiLPUrl/mine"))
    mineRes.code.code should be(200)
    val mine = parseAs[List[LearningPathV2DTO]](mineRes)
    mine.map(_.id) should contain(created.id)
    mine.map(_.id) should contain(copied.id)

    val deleteCopyRes = deleteLearningPath(copied.id)
    deleteCopyRes.code.code should be(204)

    val getDeletedCopyRes = getLearningPathResponse(copied.id)
    getDeletedCopyRes.code.code should be(404)
  }

  test("Learningstep endpoints support full CRUD and management operations") {
    val learningPath = createLearningpath("Path for step CRUD")
    val step1        = createLearningStep(learningPath.id, "Step One")
    val step2        = createLearningStep(learningPath.id, "Step Two")

    val stepsRes = sendAuthed(
      quickRequest.get(uri"$learningpathApiLPUrl/${learningPath.id}/learningsteps?language=nb&fallback=true")
    )
    stepsRes.code.code should be(200)
    val stepContainer = parseAs[LearningStepContainerSummaryDTO](stepsRes)
    stepContainer.learningsteps.map(_.id).toSet should be(Set(step1.id, step2.id))

    val fetchedStep = getLearningStep(learningPath.id, step1.id)
    fetchedStep.title.title should be("Step One")

    val updatedStep = updateLearningStepTitle(
      pathId = learningPath.id,
      stepId = step1.id,
      revision = fetchedStep.revision,
      language = "nb",
      title = "Step One Updated",
    )
    updatedStep.title.title should be("Step One Updated")

    val initialStepStatus = getLearningStepStatus(learningPath.id, step1.id)
    initialStepStatus.status should be("ACTIVE")

    val movedStepSeq = updateLearningStepSeqNo(learningPath.id, step2.id, 0)
    movedStepSeq.seqNo should be(0)

    val pathAfterSeqUpdate = getLearningPath(learningPath.id)
    val seqMap             = pathAfterSeqUpdate.learningsteps.map(step => step.id -> step.seqNo).toMap
    seqMap(step2.id) should be(0)
    seqMap(step1.id) should be(1)

    val deletedStep = updateLearningStepStatus(learningPath.id, step1.id, "DELETED")
    deletedStep.status should be("DELETED")

    val trashRes = sendAuthed(
      quickRequest.get(uri"$learningpathApiLPUrl/${learningPath.id}/learningsteps/trash?language=nb&fallback=true")
    )
    trashRes.code.code should be(200)
    val trashContainer = parseAs[LearningStepContainerSummaryDTO](trashRes)
    trashContainer.learningsteps.map(_.id) should contain(step1.id)

    val reactivatedStep = updateLearningStepStatus(learningPath.id, step1.id, "ACTIVE")
    reactivatedStep.status should be("ACTIVE")

    val englishStepVersion = updateLearningStepTitle(
      pathId = learningPath.id,
      stepId = step1.id,
      revision = reactivatedStep.revision,
      language = "en",
      title = "Step One English",
    )
    englishStepVersion.supportedLanguages should contain("en")

    val stepAfterLanguageDelete = deleteLearningStepLanguage(learningPath.id, step1.id, "en")
    stepAfterLanguageDelete.supportedLanguages should not contain "en"

    val deleteStepRes = deleteLearningStep(learningPath.id, step1.id)
    deleteStepRes.code.code should be(204)

    val deleteStepAgainRes = deleteLearningStep(learningPath.id, step1.id)
    deleteStepAgainRes.code.code should be(404)

    val getDeletedStepRes = getLearningStepResponse(learningPath.id, step1.id)
    getDeletedStepRes.code.code should be(200)
    parseAs[LearningStepV2DTO](getDeletedStepRes).status should be("DELETED")
  }

  test("Search and metadata endpoints return valid payloads") {
    val lpWithExternalAndArticleStep = createLearningpath("Path for metadata endpoints")
    val publishedTaggedPath          =
      createLearningpath(title = "Published tags path", tags = Some(Seq("published-tag")), duration = Some(10))
    updateLearningPathStatus(publishedTaggedPath.id, "PUBLISHED")
    createLearningStep(lpWithExternalAndArticleStep.id, "External step")
    createLearningStep(
      pathId = lpWithExternalAndArticleStep.id,
      title = "Article step",
      embedUrl = None,
      articleId = Some(424242L),
    )
    updateLearningPathStatus(lpWithExternalAndArticleStep.id, "UNLISTED")

    val listRes = sendAuthed(quickRequest.get(uri"$learningpathApiLPUrl"))
    listRes.code.code should be(200)
    parseAs[SearchResultV2DTO](listRes)

    val postSearchRes = sendAuthed(quickRequest.post(uri"$learningpathApiLPUrl/search").body("{}"))
    postSearchRes.code.code should be(200)
    parseAs[SearchResultV2DTO](postSearchRes)

    val tagsRes = sendAuthed(quickRequest.get(uri"$learningpathApiLPUrl/tags?language=nb&fallback=true"))
    tagsRes.code.code should be(200)
    parseAs[LearningPathTagsSummaryDTO](tagsRes)

    val licensesRes = sendAuthed(quickRequest.get(uri"$learningpathApiLPUrl/licenses"))
    licensesRes.code.code should be(200)
    val licenses = parseAs[Seq[LicenseDTO]](licensesRes)
    licenses should not be empty

    val contributorsRes = sendAuthed(quickRequest.get(uri"$learningpathApiLPUrl/contributors"))
    contributorsRes.code.code should be(200)
    parseAs[List[AuthorDTO]](contributorsRes)

    val containsArticleRes = sendAuthed(quickRequest.get(uri"$learningpathApiLPUrl/contains-article/424242"))
    containsArticleRes.code.code should be(200)
    parseAs[Seq[LearningPathSummaryV2DTO]](containsArticleRes)

    val externalSamplesRes = sendAuthed(quickRequest.get(uri"$learningpathApiLPUrl/external-samples"))
    externalSamplesRes.code.code should be(200)
    val samples = parseAs[List[LearningPathV2DTO]](externalSamplesRes)
    samples.size should be <= 5
  }

  test("That sequence numbers of learningsteps are updated correctly") {
    val x  = createLearningpath("Test1")
    val s1 = createLearningStep(x.id, "Step1")
    createLearningStep(x.id, "Step2")
    createLearningStep(x.id, "Step3")
    createLearningStep(x.id, "Step4")
    createLearningStep(x.id, "Step5")
    val pathBeforeDelete = getLearningPath(x.id)
    pathBeforeDelete.learningsteps.map(_.seqNo) should be(Seq(0, 1, 2, 3, 4))

    deleteLearningStep(x.id, s1.id).code.code should be(204)
    deleteLearningStep(x.id, s1.id).code.code should be(404)
    deleteLearningStep(x.id, s1.id).code.code should be(404)
    deleteLearningStep(x.id, s1.id).code.code should be(404)
    deleteLearningStep(x.id, s1.id).code.code should be(404)

    val path = getLearningPath(x.id)
    path.learningsteps.map(_.seqNo) should be(Seq(0, 1, 2, 3))
  }

}
