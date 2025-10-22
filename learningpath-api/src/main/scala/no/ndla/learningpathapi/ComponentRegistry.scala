/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi

import no.ndla.common.{Clock, UUIDUtil}
import no.ndla.common.converter.CommonConverter
import no.ndla.database.{DBMigrator, DataSource}
import no.ndla.learningpathapi.controller.{
  ControllerErrorHandling,
  InternController,
  LearningpathControllerV2,
  StatsController,
  SwaggerDocControllerConfig,
}
import no.ndla.learningpathapi.db.migrationwithdependencies.{
  V11__CreatedByNdlaStatusForOwnersWithRoles,
  V13__StoreNDLAStepsAsIframeTypes,
  V14__ConvertLanguageUnknown,
  V15__MergeDuplicateLanguageFields,
  V31__ArenaDefaultEnabledOrgs,
  V33__AiDefaultEnabledOrgs,
}
import no.ndla.learningpathapi.integration.*
import no.ndla.learningpathapi.repository.LearningPathRepository
import no.ndla.learningpathapi.service.*
import no.ndla.learningpathapi.service.search.{SearchConverterServiceComponent, SearchIndexService, SearchService}
import no.ndla.learningpathapi.validation.{
  LanguageValidator,
  LearningPathValidator,
  LearningStepValidator,
  TitleValidator,
  UrlValidator,
}
import no.ndla.network.NdlaClient
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.network.tapir.{
  ErrorHandling,
  ErrorHelpers,
  Routes,
  SwaggerController,
  TapirApplication,
  TapirController,
  TapirHealthController,
}
import no.ndla.search.{Elastic4sClientFactory, NdlaE4sClient, SearchLanguage}
import no.ndla.database.DBUtility

class ComponentRegistry(properties: LearningpathApiProperties) extends TapirApplication[LearningpathApiProperties] {
  given props: LearningpathApiProperties = properties
  given dataSource: DataSource           = DataSource.getDataSource
  implicit lazy val clock: Clock         = new Clock
  given uuidUtil: UUIDUtil               = new UUIDUtil
  given DBUtil: DBUtility                = new DBUtility
  given migrator: DBMigrator             = DBMigrator(
    new V11__CreatedByNdlaStatusForOwnersWithRoles,
    new V13__StoreNDLAStepsAsIframeTypes,
    new V14__ConvertLanguageUnknown,
    new V15__MergeDuplicateLanguageFields,
    new V31__ArenaDefaultEnabledOrgs,
    new V33__AiDefaultEnabledOrgs,
  )
  given e4sClient: NdlaE4sClient = Elastic4sClientFactory.getClient(props.SearchServer)
  given ndlaClient: NdlaClient   = new NdlaClient

  given errorHelpers: ErrorHelpers                       = new ErrorHelpers
  given errorHandling: ErrorHandling                     = new ControllerErrorHandling
  implicit lazy val taxonomyApiClient: TaxonomyApiClient = new TaxonomyApiClient
  implicit lazy val myndlaApiClient: MyNDLAApiClient     = new MyNDLAApiClient
  given searchApiClient: SearchApiClient                 = new SearchApiClient
  given oembedProxyClient: OembedProxyClient             = new OembedProxyClient

  given learningPathRepository: LearningPathRepository          = new LearningPathRepository
  given languageValidator: LanguageValidator                    = new LanguageValidator
  given titleValidator: TitleValidator                          = new TitleValidator
  given commonConverter: CommonConverter                        = new CommonConverter
  given learningPathValidator: LearningPathValidator            = new LearningPathValidator
  given learningStepValidator: LearningStepValidator            = new LearningStepValidator
  given converterService: ConverterService                      = new ConverterService
  given searchLanguage: SearchLanguage                          = new SearchLanguage
  given readService: ReadService                                = new ReadService
  given searchConverterService: SearchConverterServiceComponent = new SearchConverterServiceComponent
  given searchIndexService: SearchIndexService                  = new SearchIndexService
  given updateService: UpdateService                            = new UpdateService
  given searchService: SearchService                            = new SearchService
  given urlValidator: UrlValidator                              = new UrlValidator

  given learningpathControllerV2: LearningpathControllerV2 = new LearningpathControllerV2
  given internController: InternController                 = new InternController
  given statsController: StatsController                   = new StatsController
  given healthController: TapirHealthController            = new TapirHealthController

  given swagger: SwaggerController = new SwaggerController(
    List[TapirController](learningpathControllerV2, internController, statsController, healthController),
    SwaggerDocControllerConfig.swaggerInfo,
  )

  given services: List[TapirController] = swagger.getServices()
  given routes: Routes                  = new Routes
}
