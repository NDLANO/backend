/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi

import no.ndla.common.converter.CommonConverter
import no.ndla.common.{Clock, UUIDUtil}
import no.ndla.database.{DBMigrator, DBUtility, DataSource}
import no.ndla.learningpathapi.controller.{
  ControllerErrorHandling,
  InternController,
  LearningpathControllerV2,
  StatsController,
}
import no.ndla.learningpathapi.db.migrationwithdependencies.*
import no.ndla.learningpathapi.integration.*
import no.ndla.learningpathapi.model.domain.DBLearningPath
import no.ndla.learningpathapi.repository.LearningPathRepository
import no.ndla.learningpathapi.service.*
import no.ndla.learningpathapi.service.search.{SearchConverterServiceComponent, SearchIndexService, SearchService}
import no.ndla.learningpathapi.validation.*
import no.ndla.network.NdlaClient
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.network.jwt.{DefaultJwsKeySelectorFactory, JwsKeySelectorFactory}
import no.ndla.network.tapir.auth.{CombinedAuth, FeideAuth, NdlaAuth}
import no.ndla.network.tapir.*
import no.ndla.search.{Elastic4sClientFactory, NdlaE4sClient, SearchLanguage}

class ComponentRegistry(properties: LearningpathApiProperties) extends TapirApplication[LearningpathApiProperties] {
  given props: LearningpathApiProperties = properties
  given dbLearningPath: DBLearningPath   = new DBLearningPath
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

  given errorHelpers: ErrorHelpers                        = new ErrorHelpers
  given errorHandling: ErrorHandling                      = new ControllerErrorHandling
  implicit lazy val taxonomyApiClient: TaxonomyApiClient  = new TaxonomyApiClient
  protected def buildMyNDLAApiClient: MyNDLAApiClient     = new MyNDLAApiClient
  implicit lazy val myndlaApiClient: MyNDLAApiClient      = buildMyNDLAApiClient
  protected def buildSearchApiClient: SearchApiClient     = new SearchApiHttpClient
  given searchApiClient: SearchApiClient                  = buildSearchApiClient
  protected def buildOembedProxyClient: OembedProxyClient = new OembedProxyHttpClient
  given oembedProxyClient: OembedProxyClient              = buildOembedProxyClient

  implicit val jwsKeySelectorFactory: JwsKeySelectorFactory = DefaultJwsKeySelectorFactory
  given ndlaAuth: NdlaAuth                                  = NdlaAuth()
  given feideAuth: FeideAuth                                = FeideAuth()
  given combinedAuth: CombinedAuth                          = CombinedAuth()

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

  given swaggerInfo: SwaggerInfo =
    SwaggerInfo(prefix = "learningpath-api", description = "Services for accessing learningpaths")
  given swagger: SwaggerController = new SwaggerController(
    learningpathControllerV2,
    internController,
    new LegacyPrefixAlias(internController, "intern"),
    statsController,
    healthController,
  )

  given services: List[TapirController] = swagger.allServices
  given routes: Routes                  = new Routes
}
