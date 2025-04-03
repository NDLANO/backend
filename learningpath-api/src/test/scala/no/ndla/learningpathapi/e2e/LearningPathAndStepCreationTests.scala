/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi.e2e

import no.ndla.common.CirceUtil
import no.ndla.common.configuration.Prop
import no.ndla.common.model.NDLADate
import no.ndla.common.model.domain.learningpath.{EmbedType, LearningPath, StepType}
import no.ndla.learningpathapi.model.api.{
  EmbedUrlV2DTO,
  LearningPathV2DTO,
  LearningStepV2DTO,
  NewLearningPathV2DTO,
  NewLearningStepV2DTO
}
import no.ndla.learningpathapi.{ComponentRegistry, LearningpathApiProperties, MainClass, UnitSuite}
import no.ndla.scalatestsuite.IntegrationSuite
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{spy, when, withSettings}
import org.mockito.invocation.InvocationOnMock
import org.mockito.quality.Strictness
import org.testcontainers.containers.PostgreSQLContainer
import sttp.client3.quick.*

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import scala.util.Success

class LearningPathAndStepCreationTests
    extends IntegrationSuite(
      EnableElasticsearchContainer = true,
      EnablePostgresContainer = true,
      EnableRedisContainer = false
    )
    with UnitSuite {

  val learningpathApiPort: Int    = findFreePort
  val pgc: PostgreSQLContainer[_] = postgresContainer.get
  val learningpathApiProperties: LearningpathApiProperties = new LearningpathApiProperties {
    override def ApplicationPort: Int       = learningpathApiPort
    override val MetaServer: Prop[String]   = Prop.propFromTestValue(pgc.getHost)
    override val MetaResource: Prop[String] = Prop.propFromTestValue(pgc.getDatabaseName)
    override val MetaUserName: Prop[String] = Prop.propFromTestValue(pgc.getUsername)
    override val MetaPassword: Prop[String] = Prop.propFromTestValue(pgc.getPassword)
    override val MetaPort: Prop[Int]        = Prop.propFromTestValue(pgc.getMappedPort(5432))
    override val MetaSchema: Prop[String]   = Prop.propFromTestValue("testschema")
    override def disableWarmup: Boolean     = true
    override def SearchServer: String       = elasticSearchHost.get
  }

  val someDate: NDLADate = NDLADate.of(2017, 1, 1, 1, 59)

  val learningpathApi: MainClass = new MainClass(learningpathApiProperties) {
    override val componentRegistry: ComponentRegistry = new ComponentRegistry(learningpathApiProperties) {
      override lazy val clock: SystemClock = mock[SystemClock](withSettings.strictness(Strictness.LENIENT))
      override lazy val myndlaApiClient: MyNDLAApiClient     = spy(new MyNDLAApiClient)
      override lazy val taxonomyApiClient: TaxonomyApiClient = mock[TaxonomyApiClient]

      when(clock.now()).thenReturn(someDate)
      when(myndlaApiClient.isWriteRestricted).thenReturn(Success(false))
      when(taxonomyApiClient.updateTaxonomyForLearningPath(any, any, any)).thenAnswer { (i: InvocationOnMock) =>
        Success(i.getArgument[LearningPath](0))
      }
    }
  }

  val testClock: learningpathApi.componentRegistry.SystemClock = learningpathApi.componentRegistry.clock

  val learningpathApiBaseUrl: String = s"http://localhost:$learningpathApiPort"
  val learningpathApiLPUrl: String   = s"$learningpathApiBaseUrl/learningpath-api/v2/learningpaths"

  override def beforeAll(): Unit = {
    super.beforeAll()
    implicit val ec = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
    Future { learningpathApi.run(Array.empty) }: Unit
    Thread.sleep(5000)
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    learningpathApi.componentRegistry.inTransaction(implicit session => {
      learningpathApi.componentRegistry.learningPathRepository.deleteAllPathsAndSteps(session)
    })
  }

  override def afterAll(): Unit = {
    super.afterAll()
  }

  val fakeToken =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6Inh4eHl5eSIsImlzcyI6Imh0dHBzOi8vbmRsYS5ldS5hdXRoMC5jb20vIiwic3ViIjoieHh4eXl5QGNsaWVudHMiLCJhdWQiOiJuZGxhX3N5c3RlbSIsImlhdCI6MTUxMDMwNTc3MywiZXhwIjoxNTEwMzkyMTczLCJwZXJtaXNzaW9ucyI6WyJhcnRpY2xlczpwdWJsaXNoIiwiZHJhZnRzOndyaXRlIiwiZHJhZnRzOnNldF90b19wdWJsaXNoIiwiYXJ0aWNsZXM6d3JpdGUiLCJsZWFybmluZ3BhdGg6d3JpdGUiLCJsZWFybmluZ3BhdGg6cHVibGlzaCIsImxlYXJuaW5ncGF0aDphZG1pbiJdLCJndHkiOiJjbGllbnQtY3JlZGVudGlhbHMifQ.XnP0ywYk-A0j9bGZJBCDNA5fZ4OuGRLkXFBBr3IYD50"

  def createLearningpath(title: String, shouldSucceed: Boolean = true): LearningPathV2DTO = {
    val dto = NewLearningPathV2DTO(
      title = title,
      description = None,
      coverPhotoMetaUrl = None,
      duration = None,
      tags = None,
      language = "nb",
      copyright = None
    )

    val x = CirceUtil.toJsonString(dto)

    val res = simpleHttpClient.send(
      quickRequest
        .post(uri"$learningpathApiLPUrl")
        .body(x)
        .header("Content-type", "application/json")
        .header("Authorization", s"Bearer $fakeToken")
        .readTimeout(10.seconds)
    )
    if (shouldSucceed) { res.code.code should be(201) }
    CirceUtil.unsafeParseAs[LearningPathV2DTO](res.body)
  }

  def createLearningStep(pathId: Long, title: String, shouldSucceed: Boolean = true): LearningStepV2DTO = {
    val dto = NewLearningStepV2DTO(
      title = title,
      introduction = None,
      description = None,
      language = "nb",
      embedUrl = Some(
        EmbedUrlV2DTO(
          url = "https://www.example.com/",
          embedType = EmbedType.External.entryName
        )
      ),
      showTitle = false,
      `type` = StepType.TEXT.toString,
      license = None
    )
    val x = CirceUtil.toJsonString(dto)
    val res = simpleHttpClient.send(
      quickRequest
        .post(uri"$learningpathApiLPUrl/$pathId/learningsteps")
        .body(x)
        .header("Content-type", "application/json")
        .header("Authorization", s"Bearer $fakeToken")
        .readTimeout(10.seconds)
    )
    if (shouldSucceed) { res.code.code should be(201) }
    CirceUtil.unsafeParseAs[LearningStepV2DTO](res.body)
  }

  def getLearningPath(pathId: Long, shouldSucceed: Boolean = true): LearningPathV2DTO = {
    val res = simpleHttpClient.send(
      quickRequest
        .get(uri"$learningpathApiLPUrl/$pathId")
        .header("Content-type", "application/json")
        .header("Authorization", s"Bearer $fakeToken")
        .readTimeout(10.seconds)
    )
    if (shouldSucceed) { res.code.code should be(200) }
    CirceUtil.unsafeParseAs[LearningPathV2DTO](res.body)
  }

  def deleteStep(pathId: Long, stepId: Long, maybeExpectedCode: Option[Int] = Some(204)): Unit = {
    val res = simpleHttpClient.send(
      quickRequest
        .delete(uri"$learningpathApiLPUrl/$pathId/learningsteps/$stepId")
        .header("Content-type", "application/json")
        .header("Authorization", s"Bearer $fakeToken")
        .readTimeout(10.seconds)
    )
    maybeExpectedCode match {
      case None               =>
      case Some(expectedCode) => res.code.code should be(expectedCode)
    }
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

    deleteStep(x.id, s1.id)
    deleteStep(x.id, s1.id, Some(404))
    deleteStep(x.id, s1.id, Some(404))
    deleteStep(x.id, s1.id, Some(404))
    deleteStep(x.id, s1.id, Some(404))

    val path = getLearningPath(x.id)
    path.learningsteps.map(_.seqNo) should be(Seq(0, 1, 2, 3))
  }

}
