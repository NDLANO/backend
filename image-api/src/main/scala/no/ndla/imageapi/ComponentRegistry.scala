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
import no.ndla.database.{DBMigrator, DBUtility, DataSource}
import no.ndla.imageapi.controller.*
import no.ndla.imageapi.db.migrationwithdependencies.{
  V25__FixUrlEncodedFileNames,
  V6__AddAgreementToImages,
  V7__TranslateUntranslatedAuthors,
}
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.*
import no.ndla.imageapi.service.search.{
  ImageIndexService,
  ImageSearchService,
  SearchConverterService,
  TagIndexService,
  TagSearchService,
}
import no.ndla.network.NdlaClient
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.network.tapir.{ErrorHandling, ErrorHelpers, Routes, SwaggerController, TapirApplication, TapirController}
import no.ndla.search.{Elastic4sClientFactory, NdlaE4sClient, SearchLanguage}

class ComponentRegistry(properties: ImageApiProperties) extends TapirApplication[ImageApiProperties] {
  given props: ImageApiProperties    = properties
  given dataSource: DataSource       = DataSource.getDataSource
  given clock: Clock                 = new Clock
  given errorHelpers: ErrorHelpers   = new ErrorHelpers
  given errorHandling: ErrorHandling = new ControllerErrorHandling

  given s3Client: NdlaS3Client                         = new NdlaS3Client(props.StorageName, props.StorageRegion)
  given ndlaClient: NdlaClient                         = new NdlaClient
  given e4sClient: NdlaE4sClient                       = Elastic4sClientFactory.getClient(props.SearchServer)
  given searchLanguage: SearchLanguage                 = new SearchLanguage
  given imageConverter: ImageConverter                 = new ImageConverter
  given random: Random                                 = new Random
  given converterService: ConverterService             = new ConverterService
  given myndlaApiClient: MyNDLAApiClient               = new MyNDLAApiClient
  given searchConverterService: SearchConverterService = new SearchConverterService
  given imageRepository: ImageRepository               = new ImageRepository
  given imageIndexService: ImageIndexService           = new ImageIndexService
  given imageSearchService: ImageSearchService         = new ImageSearchService
  given tagIndexService: TagIndexService               = new TagIndexService
  given tagSearchService: TagSearchService             = new TagSearchService
  given validationService: ValidationService           = new ValidationService
  given readService: ReadService                       = new ReadService
  given imageStorage: ImageStorageService              = new ImageStorageService
  given dbUtility: DBUtility                           = new DBUtility // TODO: Remove this after completing variants migration of existing images
  given writeService: WriteService                     = new WriteService

  given migrator: DBMigrator =
    DBMigrator(new V6__AddAgreementToImages, new V7__TranslateUntranslatedAuthors, new V25__FixUrlEncodedFileNames)

  given imageControllerV2: ImageControllerV2 = new ImageControllerV2
  given imageControllerV3: ImageControllerV3 = new ImageControllerV3
  given rawController: RawController         = new RawController
  given internController: InternController   = new InternController
  given healthController: HealthController   = new HealthController

  given swagger: SwaggerController = new SwaggerController(
    List[TapirController](imageControllerV2, imageControllerV3, rawController, internController, healthController),
    SwaggerDocControllerConfig.swaggerInfo,
  )

  given services: List[TapirController] = swagger.getServices()
  given routes: Routes                  = new Routes
}
