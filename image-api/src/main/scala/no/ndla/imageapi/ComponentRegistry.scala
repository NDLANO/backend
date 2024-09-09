/*
 * Part of NDLA image-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.zaxxer.hikari.HikariDataSource
import no.ndla.common.Clock
import no.ndla.common.configuration.BaseComponentRegistry
import no.ndla.imageapi.controller.*
import no.ndla.imageapi.integration.*
import no.ndla.imageapi.model.api.ErrorHelpers
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
import no.ndla.network.tapir.{Routes, Service, SwaggerControllerConfig, TapirErrorHelpers, TapirHealthController}
import no.ndla.search.{BaseIndexService, Elastic4sClient}

class ComponentRegistry(properties: ImageApiProperties)
    extends BaseComponentRegistry[ImageApiProperties]
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
    with AmazonClient
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
    with ErrorHelpers
    with Random
    with Routes[Eff]
    with TapirErrorHelpers
    with SwaggerControllerConfig
    with SwaggerDocControllerConfig
    with TapirHealthController {
  override val props: ImageApiProperties = properties

  override val migrator                     = new DBMigrator
  override val dataSource: HikariDataSource = DataSource.getHikariDataSource
  DataSource.connectToDatabase()

  val amazonClient: AmazonS3 =
    AmazonS3ClientBuilder
      .standard()
      .withRegion(props.StorageRegion.toString)
      .build()

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
    List[Service[Eff]](
      imageControllerV2,
      imageControllerV3,
      rawController,
      internController,
      healthController
    ),
    SwaggerDocControllerConfig.swaggerInfo
  )

  override def services: List[Service[Eff]] = swagger.getServices()
}
