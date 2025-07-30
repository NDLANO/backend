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

class ComponentRegistry(properties: LearningpathApiProperties)
    extends BaseComponentRegistry[LearningpathApiProperties]
    with TapirApplication
    with LearningpathControllerV2
    with InternController
    with StatsController
    with LearningPathRepositoryComponent
    with ReadService
    with UpdateService
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
  override val props: LearningpathApiProperties = properties
  override val migrator: DBMigrator = DBMigrator(
    new V11__CreatedByNdlaStatusForOwnersWithRoles,
    new V13__StoreNDLAStepsAsIframeTypes,
    new V14__ConvertLanguageUnknown,
    new V15__MergeDuplicateLanguageFields,
    new V31__ArenaDefaultEnabledOrgs,
    new V33__AiDefaultEnabledOrgs
  )
  override lazy val dataSource: HikariDataSource = DataSource.getHikariDataSource

  lazy val learningPathRepository = new LearningPathRepository
  lazy val readService            = new ReadService
  lazy val updateService          = new UpdateService
  lazy val searchConverterService = new SearchConverterService
  lazy val searchService          = new SearchService
  lazy val searchIndexService     = new SearchIndexService
  lazy val converterService       = new ConverterService
  lazy val clock                  = new SystemClock
  lazy val uuidUtil               = new UUIDUtil
  lazy val taxonomyApiClient      = new TaxonomyApiClient
  lazy val ndlaClient             = new NdlaClient
  lazy val languageValidator      = new LanguageValidator
  lazy val titleValidator         = new TitleValidator
  lazy val learningPathValidator  = new LearningPathValidator
  lazy val learningStepValidator  = new LearningStepValidator
  var e4sClient: NdlaE4sClient    = Elastic4sClientFactory.getClient(props.SearchServer)
  lazy val searchApiClient        = new SearchApiClient
  lazy val oembedProxyClient      = new OembedProxyClient
  lazy val myndlaApiClient        = new MyNDLAApiClient

  lazy val learningpathControllerV2                = new LearningpathControllerV2
  lazy val internController                        = new InternController
  lazy val statsController                         = new StatsController
  lazy val healthController: TapirHealthController = new TapirHealthController

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
