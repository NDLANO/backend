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

trait TestEnvironment extends TapirApplication with MockitoSugar with TestDataTrait {
  given props                 = new ImageApiProperties
  val TestData: TestDataClass = new TestDataClass

  given migrator: DBMigrator   = mock[DBMigrator]
  given s3Client: NdlaS3Client = mock[NdlaS3Client]

  given dataSource: HikariDataSource           = mock[HikariDataSource]
  given imageIndexService: ImageIndexService   = mock[ImageIndexService]
  given imageSearchService: ImageSearchService = mock[ImageSearchService]

  given tagIndexService: TagIndexService   = mock[TagIndexService]
  given tagSearchService: TagSearchService = mock[TagSearchService]

  given imageRepository: ImageRepository        = mock[ImageRepository]
  given readService: ReadService                = mock[ReadService]
  given writeService: WriteService              = mock[WriteService]
  given imageStorage: AmazonImageStorageService = mock[AmazonImageStorageService]

  given ndlaClient: NdlaClient                         = mock[NdlaClient]
  given myndlaApiClient: MyNDLAApiClient               = mock[MyNDLAApiClient]
  given rawController: RawController                   = mock[RawController]
  given healthController: HealthController             = mock[HealthController]
  given internController: InternController             = mock[InternController]
  given imageControllerV2: ImageControllerV2           = mock[ImageControllerV2]
  given imageControllerV3: ImageControllerV3           = mock[ImageControllerV3]
  given converterService: ConverterService             = mock[ConverterService]
  given validationService: ValidationService           = mock[ValidationService]
  var e4sClient: NdlaE4sClient                         = mock[NdlaE4sClient]
  given searchConverterService: SearchConverterService = mock[SearchConverterService]
  given imageConverter: ImageConverter                 = mock[ImageConverter]

  given clock: SystemClock = mock[SystemClock]
  given random: Random     = mock[Random]

  def services: List[TapirController] = List.empty
  val swagger: SwaggerController      = mock[SwaggerController]
}
