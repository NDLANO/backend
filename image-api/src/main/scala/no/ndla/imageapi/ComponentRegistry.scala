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

class ComponentRegistry(properties: ImageApiProperties) extends TapirApplication[ImageApiProperties] {
  given props: ImageApiProperties = properties

  given migrator: DBMigrator = DBMigrator(
    new V6__AddAgreementToImages,
    new V7__TranslateUntranslatedAuthors
  )
  given dataSource: HikariDataSource = DataSource.getDataSource

  given s3Client = new NdlaS3Client(props.StorageName, props.StorageRegion)

  given imageIndexService                = new ImageIndexService
  given imageSearchService               = new ImageSearchService
  given tagIndexService                  = new TagIndexService
  given tagSearchService                 = new TagSearchService
  given imageRepository                  = new ImageRepository
  given readService                      = new ReadService
  given writeService                     = new WriteService
  given validationService                = new ValidationService
  given imageStorage                     = new AmazonImageStorageService
  given ndlaClient                       = new NdlaClient
  given converterService                 = new ConverterService
  var e4sClient: NdlaE4sClient           = Elastic4sClientFactory.getClient(props.SearchServer)
  given myndlaApiClient: MyNDLAApiClient = new MyNDLAApiClient
  given searchConverterService           = new SearchConverterService

  given imageConverter = new ImageConverter
  given clock          = new SystemClock
  given random         = new Random

  given imageControllerV2 = new ImageControllerV2
  given imageControllerV3 = new ImageControllerV3
  given rawController     = new RawController
  given internController  = new InternController
  given healthController  = new HealthController

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
