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
import no.ndla.common.configuration.BaseComponentRegistry
import no.ndla.imageapi.controller.*
import no.ndla.imageapi.integration.*
import no.ndla.imageapi.model.api.ErrorHandling
import no.ndla.imageapi.repository.ImageRepository
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

import no.ndla.search.{BaseIndexService, Elastic4sClient}

class ComponentRegistry(properties: ImageApiProperties)
    extends BaseComponentRegistry[ImageApiProperties]
    with TapirApplication
    with Elastic4sClient
    with IndexService
    with BaseIndexService
    with TagIndexService
    with ImageIndexService
    with SearchService
    with ImageSearchService
    with TagSearchService
    with SearchConverterService
    with DataSource
    with ImageRepository
    with ReadService
    with WriteService
    with ImageStorageService
    with NdlaClient
    with ConverterService
    with ValidationService
    with BaseImageController
    with ImageControllerV2
    with ImageControllerV3
    with RawController
    with InternController
    with HealthController
    with ImageConverter
    with Clock
    with Props
    with DBMigrator
    with ErrorHandling
    with Random
    with NdlaS3Client
    with SwaggerDocControllerConfig {
  override val props: ImageApiProperties = properties

  override val migrator                     = new DBMigrator
  override val dataSource: HikariDataSource = DataSource.getHikariDataSource
  DataSource.connectToDatabase()

  lazy val s3Client = new NdlaS3Client(props.StorageName, props.StorageRegion)

  lazy val imageIndexService      = new ImageIndexService
  lazy val imageSearchService     = new ImageSearchService
  lazy val tagIndexService        = new TagIndexService
  lazy val tagSearchService       = new TagSearchService
  lazy val imageRepository        = new ImageRepository
  lazy val readService            = new ReadService
  lazy val writeService           = new WriteService
  lazy val validationService      = new ValidationService
  lazy val imageStorage           = new AmazonImageStorageService
  lazy val ndlaClient             = new NdlaClient
  lazy val converterService       = new ConverterService
  var e4sClient: NdlaE4sClient    = Elastic4sClientFactory.getClient(props.SearchServer)
  lazy val searchConverterService = new SearchConverterService

  lazy val imageConverter = new ImageConverter
  lazy val clock          = new SystemClock
  lazy val random         = new Random

  lazy val imageControllerV2 = new ImageControllerV2
  lazy val imageControllerV3 = new ImageControllerV3
  lazy val rawController     = new RawController
  lazy val internController  = new InternController
  lazy val healthController  = new HealthController

  private val swagger = new SwaggerController(
    List[TapirController](
      imageControllerV2,
      imageControllerV3,
      rawController,
      internController,
      healthController
    ),
    SwaggerDocControllerConfig.swaggerInfo
  )

  override def services: List[TapirController] = swagger.getServices()
}
