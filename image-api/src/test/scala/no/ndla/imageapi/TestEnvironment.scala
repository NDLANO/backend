/*
 * Part of NDLA image-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import no.ndla.common.Clock
import no.ndla.common.aws.NdlaS3Client
import no.ndla.common.configuration.BaseProps
import no.ndla.database.{DBMigrator, DataSource, DatabaseProps}
import no.ndla.imageapi.controller.{
  BaseImageController,
  HealthController,
  ImageControllerV2,
  ImageControllerV3,
  InternController,
  RawController
}
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
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.network.tapir.{
  AllErrors,
  ErrorHandling,
  ErrorHelpers,
  Routes,
  SwaggerController,
  TapirApplication,
  TapirController,
  TapirErrorHandling,
  TapirHealthController
}
import no.ndla.search.{BaseIndexService, Elastic4sClientFactory, NdlaE4sClient, SearchLanguage}
import org.scalatestplus.mockito.MockitoSugar

trait TestEnvironment extends TapirApplication[ImageApiProperties] with MockitoSugar {
  implicit lazy val props: ImageApiProperties       = new ImageApiProperties
  implicit lazy val clock: Clock                    = mock[Clock]
  implicit lazy val errorHelpers: ErrorHelpers      = mock[ErrorHelpers]
  implicit lazy val errorHandling: ErrorHandling    = mock[ErrorHandling]
  implicit lazy val routes: Routes                  = mock[Routes]
  implicit lazy val services: List[TapirController] = List.empty

  val TestData: TestData = new TestData

  implicit lazy val migrator: DBMigrator   = mock[DBMigrator]
  implicit lazy val s3Client: NdlaS3Client = mock[NdlaS3Client]
  implicit lazy val dataSource: DataSource = mock[DataSource]

  implicit lazy val imageIndexService: ImageIndexService   = mock[ImageIndexService]
  implicit lazy val imageSearchService: ImageSearchService = mock[ImageSearchService]

  implicit lazy val tagIndexService: TagIndexService   = mock[TagIndexService]
  implicit lazy val tagSearchService: TagSearchService = mock[TagSearchService]

  implicit lazy val imageRepository: ImageRepository  = mock[ImageRepository]
  implicit lazy val readService: ReadService          = mock[ReadService]
  implicit lazy val writeService: WriteService        = mock[WriteService]
  implicit lazy val imageStorage: ImageStorageService = mock[ImageStorageService]

  implicit lazy val ndlaClient: NdlaClient                         = mock[NdlaClient]
  implicit lazy val myndlaApiClient: MyNDLAApiClient               = mock[MyNDLAApiClient]
  implicit lazy val rawController: RawController                   = mock[RawController]
  implicit lazy val healthController: TapirHealthController        = mock[TapirHealthController]
  implicit lazy val internController: InternController             = mock[InternController]
  implicit lazy val imageControllerV2: ImageControllerV2           = mock[ImageControllerV2]
  implicit lazy val imageControllerV3: ImageControllerV3           = mock[ImageControllerV3]
  implicit lazy val converterService: ConverterService             = mock[ConverterService]
  implicit lazy val validationService: ValidationService           = mock[ValidationService]
  implicit lazy val e4sClient: NdlaE4sClient                       = mock[NdlaE4sClient]
  implicit lazy val searchConverterService: SearchConverterService = mock[SearchConverterService]
  implicit lazy val imageConverter: ImageConverter                 = mock[ImageConverter]
  implicit lazy val random: Random                                 = mock[Random]

  implicit lazy val swagger: SwaggerController = mock[SwaggerController]
}
