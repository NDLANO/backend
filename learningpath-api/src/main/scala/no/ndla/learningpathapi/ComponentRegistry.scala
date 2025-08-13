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
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
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

class ComponentRegistry(properties: LearningpathApiProperties)
    extends BaseComponentRegistry[LearningpathApiProperties]
    with TapirApplication
    with LearningpathControllerV2
    with InternController
    with StatsController
    with LearningPathRepositoryComponent
    with ReadService
    with UpdateService
    with DBUtility
    with SearchConverterServiceComponent
    with SearchService
    with SearchIndexService
    with BaseIndexService
    with TaxonomyApiClient
    with NdlaClient
    with CommonConverter
    with ConverterService
    with OembedProxyClient
    with Elastic4sClient
    with DataSource
    with Clock
    with UUIDUtil
    with MyNDLAApiClient
    with LanguageValidator
    with LearningPathValidator
    with LearningStepValidator
    with TitleValidator
    with SearchApiClient
    with Props
    with DBMigrator
    with TextValidator
    with UrlValidator
    with ErrorHandling
    with SwaggerDocControllerConfig
    with SearchLanguage {
  override lazy val props: LearningpathApiProperties = properties
  override lazy val migrator: DBMigrator             = DBMigrator(
    new V11__CreatedByNdlaStatusForOwnersWithRoles,
    new V13__StoreNDLAStepsAsIframeTypes,
    new V14__ConvertLanguageUnknown,
    new V15__MergeDuplicateLanguageFields,
    new V31__ArenaDefaultEnabledOrgs,
    new V33__AiDefaultEnabledOrgs
  )
  override lazy val dataSource: HikariDataSource = DataSource.getHikariDataSource
  override lazy val DBUtil: DBUtility            = new DBUtility

  override lazy val learningPathRepository = new LearningPathRepository
  override lazy val readService            = new ReadService
  override lazy val updateService          = new UpdateService
  override lazy val searchConverterService = new SearchConverterService
  override lazy val searchService          = new SearchService
  override lazy val searchIndexService     = new SearchIndexService
  override lazy val converterService       = new ConverterService
  override lazy val clock                  = new SystemClock
  override lazy val uuidUtil               = new UUIDUtil
  override lazy val taxonomyApiClient      = new TaxonomyApiClient
  override lazy val ndlaClient             = new NdlaClient
  override lazy val languageValidator      = new LanguageValidator
  override lazy val titleValidator         = new TitleValidator
  override lazy val learningPathValidator  = new LearningPathValidator
  override lazy val learningStepValidator  = new LearningStepValidator
  var e4sClient: NdlaE4sClient             = Elastic4sClientFactory.getClient(props.SearchServer)
  override lazy val searchApiClient        = new SearchApiClient
  override lazy val oembedProxyClient      = new OembedProxyClient
  override lazy val myndlaApiClient        = new MyNDLAApiClient

  override lazy val learningpathControllerV2                = new LearningpathControllerV2
  override lazy val internController                        = new InternController
  override lazy val statsController                         = new StatsController
  override lazy val healthController: TapirHealthController = new TapirHealthController

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
