/*
 * Part of NDLA learningpath-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi

import com.zaxxer.hikari.HikariDataSource
import no.ndla.common.{Clock, UUIDUtil}
import no.ndla.common.configuration.BaseComponentRegistry
import no.ndla.common.converter.CommonConverter
import no.ndla.database.{DBMigrator, DataSource}
import no.ndla.learningpathapi.controller.{
  InternController,
  LearningpathControllerV2,
  StatsController,
  SwaggerDocControllerConfig
}
import no.ndla.learningpathapi.db.migrationwithdependencies.{
  V11__CreatedByNdlaStatusForOwnersWithRoles,
  V13__StoreNDLAStepsAsIframeTypes,
  V14__ConvertLanguageUnknown,
  V15__MergeDuplicateLanguageFields,
  V31__ArenaDefaultEnabledOrgs,
  V33__AiDefaultEnabledOrgs
}
import no.ndla.learningpathapi.integration.*
import no.ndla.learningpathapi.model.api.ErrorHandling
import no.ndla.learningpathapi.repository.LearningPathRepository
import no.ndla.learningpathapi.service.*
import no.ndla.learningpathapi.service.search.{SearchConverterServiceComponent, SearchIndexService, SearchService}
import no.ndla.learningpathapi.validation.{
  LanguageValidator,
  LearningPathValidator,
  LearningStepValidator,
  TextValidator,
  TitleValidator,
  UrlValidator
}
import no.ndla.network.NdlaClient
import no.ndla.network.clients.MyNDLAApiClient
import no.ndla.network.tapir.TapirApplication
import no.ndla.search.{BaseIndexService, Elastic4sClient, SearchLanguage}
import no.ndla.database.DBUtility

class ComponentRegistry(properties: LearningpathApiProperties) extends TapirApplication[LearningpathApiProperties] {
  given props: LearningpathApiProperties = properties
  given migrator: DBMigrator             = DBMigrator(
    new V11__CreatedByNdlaStatusForOwnersWithRoles,
    new V13__StoreNDLAStepsAsIframeTypes,
    new V14__ConvertLanguageUnknown,
    new V15__MergeDuplicateLanguageFields,
    new V31__ArenaDefaultEnabledOrgs,
    new V33__AiDefaultEnabledOrgs
  )
  given dataSource: HikariDataSource = DataSource.getDataSource
  given DBUtil: DBUtility            = new DBUtility

  given learningPathRepository = new LearningPathRepository
  given readService            = new ReadService
  given updateService          = new UpdateService
  given searchConverterService = new SearchConverterService
  given searchService          = new SearchService
  given searchIndexService     = new SearchIndexService
  given converterService       = new ConverterService
  given clock                  = new SystemClock
  given uuidUtil               = new UUIDUtil
  given taxonomyApiClient      = new TaxonomyApiClient
  given ndlaClient             = new NdlaClient
  given languageValidator      = new LanguageValidator
  given titleValidator         = new TitleValidator
  given learningPathValidator  = new LearningPathValidator
  given learningStepValidator  = new LearningStepValidator
  var e4sClient: NdlaE4sClient = Elastic4sClientFactory.getClient(props.SearchServer)
  given searchApiClient        = new SearchApiClient
  given oembedProxyClient      = new OembedProxyClient
  given myndlaApiClient        = new MyNDLAApiClient

  given learningpathControllerV2                = new LearningpathControllerV2
  given internController                        = new InternController
  given statsController                         = new StatsController
  given healthController: TapirHealthController = new TapirHealthController

  val swagger = new SwaggerController(
    List[TapirController](
      learningpathControllerV2,
      internController,
      statsController,
      healthController
    ),
    SwaggerDocControllerConfig.swaggerInfo
  )

  override def services: List[TapirController] = swagger.getServices()

}
