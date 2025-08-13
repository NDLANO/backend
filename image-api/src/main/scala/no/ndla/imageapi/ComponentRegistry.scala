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
import no.ndla.database.{DBMigrator, DataSource}
import no.ndla.imageapi.controller.*
import no.ndla.imageapi.db.migrationwithdependencies.{V6__AddAgreementToImages, V7__TranslateUntranslatedAuthors}
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
import no.ndla.search.{BaseIndexService, Elastic4sClient, SearchLanguage}

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
    with SearchLanguage
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
  override lazy val props: ImageApiProperties = properties

  override lazy val migrator: DBMigrator = DBMigrator(
    new V6__AddAgreementToImages,
    new V7__TranslateUntranslatedAuthors
  )
  override lazy val dataSource: HikariDataSource = DataSource.getHikariDataSource

  override lazy val s3Client = new NdlaS3Client(props.StorageName, props.StorageRegion)

  override lazy val imageIndexService                = new ImageIndexService
  override lazy val imageSearchService               = new ImageSearchService
  override lazy val tagIndexService                  = new TagIndexService
  override lazy val tagSearchService                 = new TagSearchService
  override lazy val imageRepository                  = new ImageRepository
  override lazy val readService                      = new ReadService
  override lazy val writeService                     = new WriteService
  override lazy val validationService                = new ValidationService
  override lazy val imageStorage                     = new AmazonImageStorageService
  override lazy val ndlaClient                       = new NdlaClient
  override lazy val converterService                 = new ConverterService
  var e4sClient: NdlaE4sClient                       = Elastic4sClientFactory.getClient(props.SearchServer)
  override lazy val myndlaApiClient: MyNDLAApiClient = new MyNDLAApiClient
  override lazy val searchConverterService           = new SearchConverterService

  override lazy val imageConverter = new ImageConverter
  override lazy val clock          = new SystemClock
  override lazy val random         = new Random

  override lazy val imageControllerV2 = new ImageControllerV2
  override lazy val imageControllerV3 = new ImageControllerV3
  override lazy val rawController     = new RawController
  override lazy val internController  = new InternController
  override lazy val healthController  = new HealthController

  val swagger = new SwaggerController(
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
