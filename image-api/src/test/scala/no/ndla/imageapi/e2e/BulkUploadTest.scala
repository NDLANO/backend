/*
 * Part of NDLA image-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.e2e

import no.ndla.common.CirceUtil
import no.ndla.common.aws.{NdlaCloudFrontClient, NdlaS3Client}
import no.ndla.common.configuration.Prop
import no.ndla.common.model.api.{AuthorDTO, CopyrightDTO, LicenseDTO}
import no.ndla.common.model.domain.ContributorType
import no.ndla.imageapi.model.api.NewImageMetaInformationV2DTO
import no.ndla.imageapi.model.api.bulk.{
  BulkUploadItemStatus,
  BulkUploadStartedDTO,
  BulkUploadStateDTO,
  BulkUploadStatus,
}
import no.ndla.imageapi.service.ImageStorageService
import no.ndla.imageapi.service.search.{ImageIndexService, ImageSearchService, TagIndexService, TagSearchService}
import no.ndla.imageapi.{ComponentRegistry, ImageApiProperties, MainClass, UnitSuite}
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.scalatestsuite.{DatabaseIntegrationSuite, RedisIntegrationSuite}
import no.ndla.search.NdlaE4sClient
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{when, withSettings}
import org.mockito.quality.Strictness
import org.testcontainers.postgresql.PostgreSQLContainer
import scalikejdbc.{DBSession, scalikejdbcSQLInterpolationImplicitDef}
import sttp.client3.{multipart, multipartFile}
import sttp.client3.quick.*

import java.nio.file.{Files, Path}
import java.util.UUID
import java.util.concurrent.Executors
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Success, Try}

class BulkUploadTest extends DatabaseIntegrationSuite with RedisIntegrationSuite with UnitSuite {

  val imageApiPort: Int                       = findFreePort
  val pgc: PostgreSQLContainer                = postgresContainer.get
  val redisPort: Int                          = redisContainer.get.port
  override lazy val props: ImageApiProperties = new ImageApiProperties

  lazy val imageApiProperties: ImageApiProperties = new ImageApiProperties {
    override val ApplicationPort: Int       = imageApiPort
    override val MetaServer: Prop[String]   = propFromTestValue("META_SERVER", pgc.getHost)
    override val MetaResource: Prop[String] = propFromTestValue("META_RESOURCE", pgc.getDatabaseName)
    override val MetaUserName: Prop[String] = propFromTestValue("META_USER_NAME", pgc.getUsername)
    override val MetaPassword: Prop[String] = propFromTestValue("META_PASSWORD", pgc.getPassword)
    override val MetaPort: Prop[Int]        = propFromTestValue("META_PORT", pgc.getMappedPort(5432))
    override val MetaSchema: Prop[String]   = propFromTestValue("META_SCHEMA", schemaName)

    override def RedisHost: String      = "localhost"
    override def RedisPort: Int         = redisPort
    override def disableWarmup: Boolean = true
  }

  val imageApi: MainClass = new MainClass(imageApiProperties) {
    override val componentRegistry: ComponentRegistry = new ComponentRegistry(imageApiProperties) {
      override implicit lazy val s3Client: NdlaS3Client =
        mock[NdlaS3Client](withSettings.strictness(Strictness.LENIENT))
      override implicit lazy val e4sClient: NdlaE4sClient =
        mock[NdlaE4sClient](withSettings.strictness(Strictness.LENIENT))
      override implicit lazy val cloudFrontClient: NdlaCloudFrontClient =
        mock[NdlaCloudFrontClient](withSettings.strictness(Strictness.LENIENT))
      override implicit lazy val myndlaApiClient: MyNDLAApiClient =
        mock[MyNDLAApiClient](withSettings.strictness(Strictness.LENIENT))

      override implicit lazy val imageStorage: ImageStorageService = {
        val m = mock[ImageStorageService](withSettings.strictness(Strictness.LENIENT))
        when(m.objectExists(any[String])).thenReturn(false)
        when(m.uploadFromStream(any, any, any, any)).thenAnswer(i => Success(i.getArgument[String](0)))
        when(m.deleteObject(any)).thenReturn(Success(()))
        when(m.deleteObjects(any)).thenReturn(Success(()))
        when(m.checkBucketAccess()).thenReturn(Success(()))
        m
      }

      override implicit lazy val imageIndexService: ImageIndexService = {
        val m = mock[ImageIndexService](withSettings.strictness(Strictness.LENIENT))
        when(m.indexDocument(any)).thenAnswer(i => Success(i.getArgument(0)))
        when(m.deleteDocument(any[Long])).thenAnswer(i => Success(i.getArgument(0)))
        m
      }

      override implicit lazy val tagIndexService: TagIndexService = {
        val m = mock[TagIndexService](withSettings.strictness(Strictness.LENIENT))
        when(m.indexDocument(any)).thenAnswer(i => Success(i.getArgument(0)))
        when(m.deleteDocument(any[Long])).thenAnswer(i => Success(i.getArgument(0)))
        m
      }

      override implicit lazy val imageSearchService: ImageSearchService =
        mock[ImageSearchService](withSettings.strictness(Strictness.LENIENT))
      override implicit lazy val tagSearchService: TagSearchService =
        mock[TagSearchService](withSettings.strictness(Strictness.LENIENT))
    }
  }

  val baseUrl: String  = s"http://localhost:$imageApiPort"
  val batchUrl: String = s"$baseUrl/image-api/v1/bulk"

  val authHeaderWithBatchRole: String =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6Inh4eHl5eSIsImlzcyI6Imh0dHBzOi8vbmRsYS5ldS5hdXRoMC5jb20vIiwic3ViIjoieHh4eXl5QGNsaWVudHMiLCJhdWQiOiJuZGxhX3N5c3RlbSIsImlhdCI6MTUxMDMwNTc3MywiZXhwIjoxNTEwMzkyMTczLCJwZXJtaXNzaW9ucyI6WyJpbWFnZXM6YmF0Y2giLCJpbWFnZXM6d3JpdGUiXSwiZ3R5IjoiY2xpZW50LWNyZWRlbnRpYWxzIn0.1_j9R9KML2LTqeAE4bpRByJcR6m6Tv3pTOozpYCnTC8"

  val authHeaderWithoutAnyRoles: String =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vbmRsYV9pZCI6Inh4eHl5eSIsImlzcyI6Imh0dHBzOi8vbmRsYS5ldS5hdXRoMC5jb20vIiwic3ViIjoieHh4eXl5QGNsaWVudHMiLCJhdWQiOiJuZGxhX3N5c3RlbSIsImlhdCI6MTUxMDMwNTc3MywiZXhwIjoxNTEwMzkyMTczLCJwZXJtaXNzaW9ucyI6W10sImd0eSI6ImNsaWVudC1jcmVkZW50aWFscyJ9.vw9YhRtgUQr_vuDhLNHfBsZz-4XLhCc1Kwxi0w0_qGI"

  override def beforeAll(): Unit = {
    super.beforeAll()
    implicit val ec: ExecutionContextExecutorService =
      ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
    Future {
      imageApi.run(Array.empty)
    }: Unit
    blockUntilHealthy(s"$baseUrl/health/readiness")
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    implicit val session: DBSession = imageApi.componentRegistry.dbUtility.autoSession
    sql"delete from imagemetadata".update.apply(): Unit
  }

  private def buildJpegFile(name: String): Path = {
    val bytes  = getClass.getResourceAsStream("/ndla_logo.jpg").readAllBytes()
    val tmpDir = Files.createTempDirectory("bulk-upload-test-")
    val target = tmpDir.resolve(name)
    Files.write(target, bytes)
    target
  }

  private val sampleCopyright = CopyrightDTO(
    license = LicenseDTO("CC-BY-4.0", None, None),
    origin = None,
    creators = Seq(AuthorDTO(ContributorType.Photographer, "Test Author")),
    processors = Seq.empty,
    rightsholders = Seq.empty,
    validFrom = None,
    validTo = None,
    processed = false,
  )

  private def newMeta(title: String): NewImageMetaInformationV2DTO = NewImageMetaInformationV2DTO(
    title = title,
    alttext = Some(s"$title alt"),
    copyright = sampleCopyright,
    tags = Seq("test"),
    caption = s"$title caption",
    language = "nb",
    modelReleased = None,
  )

  private def waitForFinalState(uploadId: UUID, timeout: Long = 30000): BulkUploadStateDTO = {
    val store                               = imageApi.componentRegistry.bulkUploadStore
    val deadline                            = System.currentTimeMillis() + timeout
    var current: Option[BulkUploadStateDTO] = None
    while (System.currentTimeMillis() < deadline) {
      current = store.get(uploadId).getOrElse(None)
      current match {
        case Some(s) if s.status == BulkUploadStatus.Complete || s.status == BulkUploadStatus.Failed => return s
        case _                                                                                       => Thread.sleep(100)
      }
    }
    fail(s"Bulk upload $uploadId did not reach a terminal state within ${timeout}ms (last state = $current)")
  }

  private def imageRowCount(): Long = {
    implicit val session: DBSession = imageApi.componentRegistry.dbUtility.readOnlySession
    sql"select count(*) as c from imagemetadata".map(_.long("c")).single().getOrElse(0L)
  }

  test("POST / starts a bulk upload that completes asynchronously and stores all images") {
    val file1 = buildJpegFile("first.jpg")
    val file2 = buildJpegFile("second.jpg")

    val res = simpleHttpClient.send(
      quickRequest
        .post(uri"$batchUrl")
        .multipartBody[Any](
          multipart("metadatas", CirceUtil.toJsonString(newMeta("First"))).contentType("application/json"),
          multipart("metadatas", CirceUtil.toJsonString(newMeta("Second"))).contentType("application/json"),
          multipartFile("files", file1.toFile).contentType("image/jpeg"),
          multipartFile("files", file2.toFile).contentType("image/jpeg"),
        )
        .header("Authorization", authHeaderWithBatchRole)
        .readTimeout(30.seconds)
    )

    res.code.code should be(200)
    val started = CirceUtil.unsafeParseAs[BulkUploadStartedDTO](res.body)

    val finalState = waitForFinalState(started.uploadId)
    finalState.status should be(BulkUploadStatus.Complete)
    finalState.total should be(2)
    finalState.completed should be(2)
    finalState.failed should be(0)
    finalState.items.foreach(_.status should be(BulkUploadItemStatus.Done))
    finalState.items.foreach(_.image should not be None)
    finalState.items.map(_.image.get.title.title) should contain theSameElementsAs Seq("First", "Second")

    imageRowCount() should be(2L)
  }

  test("POST / returns 400 when the number of file parts does not match the number of metadata parts") {
    val file = buildJpegFile("only.jpg")

    val res = simpleHttpClient.send(
      quickRequest
        .post(uri"$batchUrl")
        .multipartBody[Any](
          multipart("metadatas", CirceUtil.toJsonString(newMeta("First"))).contentType("application/json"),
          multipart("metadatas", CirceUtil.toJsonString(newMeta("Second"))).contentType("application/json"),
          multipartFile("files", file.toFile).contentType("image/jpeg"),
        )
        .header("Authorization", authHeaderWithBatchRole)
    )

    res.code.code should be(400)
    imageRowCount() should be(0L)
  }

  test("POST / returns 401 when the auth header is missing") {
    val file = buildJpegFile("missing-auth.jpg")
    val res  = simpleHttpClient.send(
      quickRequest
        .post(uri"$batchUrl")
        .multipartBody[Any](
          multipart("metadatas", CirceUtil.toJsonString(newMeta("First"))).contentType("application/json"),
          multipartFile("files", file.toFile).contentType("image/jpeg"),
        )
    )
    res.code.code should be(401)
  }

  test("POST / returns 403 when the auth header lacks the images:batch permission") {
    val file = buildJpegFile("no-perm.jpg")
    val res  = simpleHttpClient.send(
      quickRequest
        .post(uri"$batchUrl")
        .multipartBody[Any](
          multipart("metadatas", CirceUtil.toJsonString(newMeta("First"))).contentType("application/json"),
          multipartFile("files", file.toFile).contentType("image/jpeg"),
        )
        .header("Authorization", authHeaderWithoutAnyRoles)
    )
    res.code.code should be(403)
  }

  test("GET /status/<upload-id> returns 404 for an unknown upload id") {
    val res = simpleHttpClient.send(
      quickRequest
        .get(uri"$baseUrl/image-api/v1/bulk/status/${UUID.randomUUID()}")
        .header("Authorization", authHeaderWithBatchRole)
    )
    res.code.code should be(404)
  }

  test("Bulk upload rolls back previously stored images and reports Failed when one item fails") {
    val goodFile = buildJpegFile("good.jpg")
    val badFile  = {
      val tmp = Files.createTempFile("bulk-upload-bad-", ".txt")
      Files.write(tmp, "not an image".getBytes)
      tmp
    }

    val res = simpleHttpClient.send(
      quickRequest
        .post(uri"$batchUrl")
        .multipartBody[Any](
          multipart("metadatas", CirceUtil.toJsonString(newMeta("Good"))).contentType("application/json"),
          multipart("metadatas", CirceUtil.toJsonString(newMeta("Bad"))).contentType("application/json"),
          multipartFile("files", goodFile.toFile).contentType("image/jpeg"),
          multipartFile("files", badFile.toFile).contentType("text/plain"),
        )
        .header("Authorization", authHeaderWithBatchRole)
        .readTimeout(30.seconds)
    )

    res.code.code should be(200)
    val started = CirceUtil.unsafeParseAs[BulkUploadStartedDTO](res.body)

    val finalState = waitForFinalState(started.uploadId)
    finalState.status should be(BulkUploadStatus.Failed)
    finalState.failed should be(1)
    finalState.error should not be None
    finalState.items.head.status should be(BulkUploadItemStatus.Done)
    finalState.items(1).status should be(BulkUploadItemStatus.Failed)

    // Successful item must have been rolled back from the database when a later item failed.
    blockUntil(() => Try(imageRowCount()).toOption.contains(0L))
    imageRowCount() should be(0L)
  }
}
