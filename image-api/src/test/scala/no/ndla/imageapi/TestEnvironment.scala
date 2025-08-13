/*
 * Part of NDLA image-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import com.zaxxer.hikari.HikariDataSource
import no.ndla.common.Clock
import no.ndla.common.aws.NdlaS3Client
import no.ndla.database.{DBMigrator, DataSource}
import no.ndla.imageapi.controller.{
  BaseImageController,
  HealthController,
  ImageControllerV2,
  ImageControllerV3,
  InternController,
  RawController
}
import no.ndla.imageapi.model.api.ErrorHandling
import no.ndla.imageapi.repository.*
import no.ndla.imageapi.service.*
import no.ndla.imageapi.service.search.{
  ImageIndexService,
  ImageSearchService,
  IndexService,
  SearchConverterService,
  SearchService,
  TagIndexService,
  TagSearchService
}
import no.ndla.network.NdlaClient
import no.ndla.network.tapir.TapirApplication
import no.ndla.search.{BaseIndexService, Elastic4sClient, SearchLanguage}
import org.scalatestplus.mockito.MockitoSugar

trait TestEnvironment
    extends TapirApplication
    with Elastic4sClient
    with IndexService
    with BaseIndexService
    with TagIndexService
    with SearchService
    with ImageSearchService
    with TagSearchService
    with SearchConverterService
    with SearchLanguage
    with DataSource
    with ConverterService
    with ValidationService
    with ImageRepository
    with ReadService
    with WriteService
    with ImageStorageService
    with NdlaS3Client
    with ImageIndexService
    with NdlaClient
    with HealthController
    with InternController
    with BaseImageController
    with ImageControllerV2
    with ImageControllerV3
    with RawController
    with ImageConverter
    with MockitoSugar
    with Clock
    with Props
    with ErrorHandling
    with DBMigrator
    with TestDataTrait
    with Random {
  override lazy val props     = new ImageApiProperties
  val TestData: TestDataClass = new TestDataClass

  override lazy val migrator: DBMigrator   = mock[DBMigrator]
  override lazy val s3Client: NdlaS3Client = mock[NdlaS3Client]

  override lazy val dataSource: HikariDataSource           = mock[HikariDataSource]
  override lazy val imageIndexService: ImageIndexService   = mock[ImageIndexService]
  override lazy val imageSearchService: ImageSearchService = mock[ImageSearchService]

  override lazy val tagIndexService: TagIndexService   = mock[TagIndexService]
  override lazy val tagSearchService: TagSearchService = mock[TagSearchService]

  override lazy val imageRepository: ImageRepository        = mock[ImageRepository]
  override lazy val readService: ReadService                = mock[ReadService]
  override lazy val writeService: WriteService              = mock[WriteService]
  override lazy val imageStorage: AmazonImageStorageService = mock[AmazonImageStorageService]

  override lazy val ndlaClient: NdlaClient                         = mock[NdlaClient]
  override lazy val myndlaApiClient: MyNDLAApiClient               = mock[MyNDLAApiClient]
  override lazy val rawController: RawController                   = mock[RawController]
  override lazy val healthController: HealthController             = mock[HealthController]
  override lazy val internController: InternController             = mock[InternController]
  override lazy val imageControllerV2: ImageControllerV2           = mock[ImageControllerV2]
  override lazy val imageControllerV3: ImageControllerV3           = mock[ImageControllerV3]
  override lazy val converterService: ConverterService             = mock[ConverterService]
  override lazy val validationService: ValidationService           = mock[ValidationService]
  var e4sClient: NdlaE4sClient                                     = mock[NdlaE4sClient]
  override lazy val searchConverterService: SearchConverterService = mock[SearchConverterService]
  override lazy val imageConverter: ImageConverter                 = mock[ImageConverter]

  override lazy val clock: SystemClock = mock[SystemClock]
  override lazy val random: Random     = mock[Random]

  def services: List[TapirController] = List.empty
  val swagger: SwaggerController      = mock[SwaggerController]
}
