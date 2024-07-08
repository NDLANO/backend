/*
 * Part of NDLA image-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import com.amazonaws.services.s3.AmazonS3
import com.zaxxer.hikari.HikariDataSource
import no.ndla.common.Clock
import no.ndla.imageapi.controller.{
  BaseImageController,
  HealthController,
  ImageControllerV2,
  ImageControllerV3,
  InternController,
  RawController
}
import no.ndla.imageapi.integration.*
import no.ndla.imageapi.model.api.ErrorHelpers
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
import no.ndla.network.tapir.{Routes, Service, TapirHealthController}
import no.ndla.search.{BaseIndexService, Elastic4sClient}
import org.scalatestplus.mockito.MockitoSugar

trait TestEnvironment
    extends Elastic4sClient
    with IndexService
    with BaseIndexService
    with TagIndexService
    with SearchService
    with ImageSearchService
    with TagSearchService
    with SearchConverterService
    with DataSource
    with ConverterService
    with ValidationService
    with ImageRepository
    with ReadService
    with WriteService
    with AmazonClient
    with ImageStorageService
    with ImageIndexService
    with NdlaClient
    with HealthController
    with TapirHealthController
    with InternController
    with BaseImageController
    with ImageControllerV2
    with ImageControllerV3
    with RawController
    with ImageConverter
    with MockitoSugar
    with Clock
    with Props
    with ErrorHelpers
    with DBMigrator
    with TestData
    with Random
    with Routes[Eff] {
  val props    = new ImageApiProperties
  val TestData = new TestData

  val migrator: DBMigrator   = mock[DBMigrator]
  val amazonClient: AmazonS3 = mock[AmazonS3]

  val dataSource: HikariDataSource           = mock[HikariDataSource]
  val imageIndexService: ImageIndexService   = mock[ImageIndexService]
  val imageSearchService: ImageSearchService = mock[ImageSearchService]

  val tagIndexService: TagIndexService   = mock[TagIndexService]
  val tagSearchService: TagSearchService = mock[TagSearchService]

  val imageRepository: ImageRepository        = mock[ImageRepository]
  val readService: ReadService                = mock[ReadService]
  val writeService: WriteService              = mock[WriteService]
  val imageStorage: AmazonImageStorageService = mock[AmazonImageStorageService]

  val ndlaClient: NdlaClient                         = mock[NdlaClient]
  val rawController: RawController                   = mock[RawController]
  val healthController: HealthController             = mock[HealthController]
  val internController: InternController             = mock[InternController]
  val imageControllerV2: ImageControllerV2           = mock[ImageControllerV2]
  val imageControllerV3: ImageControllerV3           = mock[ImageControllerV3]
  val converterService: ConverterService             = mock[ConverterService]
  val validationService: ValidationService           = mock[ValidationService]
  var e4sClient: NdlaE4sClient                       = mock[NdlaE4sClient]
  val searchConverterService: SearchConverterService = mock[SearchConverterService]
  val imageConverter: ImageConverter                 = mock[ImageConverter]

  val clock: SystemClock = mock[SystemClock]
  val random: Random     = mock[Random]

  def services: List[Service[Eff]] = List.empty
}
