/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import no.ndla.common.Clock
import no.ndla.common.configuration.BaseComponentRegistry
import no.ndla.imageapi.controller._
import no.ndla.imageapi.integration._
import no.ndla.imageapi.model.api.ErrorHelpers
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service._
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
import no.ndla.network.tapir.{
  NdlaMiddleware,
  Routes,
  Service,
  SwaggerControllerConfig,
  TapirErrorHelpers,
  TapirHealthController
}
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
    with ImageConverter
    with Clock
    with Props
    with DBMigrator
    with ErrorHelpers
    with Random
    with Routes[Eff]
    with NdlaMiddleware
    with TapirErrorHelpers
    with SwaggerControllerConfig
    with SwaggerDocControllerConfig
    with TapirHealthController {
  override val props: ImageApiProperties = properties

  override val migrator   = new DBMigrator
  override val dataSource = DataSource.getHikariDataSource
  DataSource.connectToDatabase()

  val amazonClient: AmazonS3 =
    AmazonS3ClientBuilder
      .standard()
      .withRegion(props.StorageRegion)
      .build()

  val imageIndexService      = new ImageIndexService
  val imageSearchService     = new ImageSearchService
  val tagIndexService        = new TagIndexService
  val tagSearchService       = new TagSearchService
  val imageRepository        = new ImageRepository
  val readService            = new ReadService
  val writeService           = new WriteService
  val validationService      = new ValidationService
  val imageStorage           = new AmazonImageStorageService
  val ndlaClient             = new NdlaClient
  val converterService       = new ConverterService
  var e4sClient: NdlaE4sClient    = Elastic4sClientFactory.getClient(props.SearchServer)
  val searchConverterService = new SearchConverterService

  val imageConverter = new ImageConverter
  val clock          = new SystemClock
  val random         = new Random

  val imageControllerV2                            = new ImageControllerV2
  val imageControllerV3                            = new ImageControllerV3
  val rawController                                = new RawController
  val internController                             = new InternController
  val healthController: TapirHealthController[Eff] = new TapirHealthController[Eff]

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
