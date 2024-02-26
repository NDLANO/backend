/*
 * Part of NDLA learningpath-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.learningpathapi

import com.zaxxer.hikari.HikariDataSource
import no.ndla.common.Clock
import no.ndla.common.configuration.BaseComponentRegistry
import no.ndla.learningpathapi.controller.{
  InternController,
  LearningpathControllerV2,
  StatsController,
  SwaggerDocControllerConfig
}
import no.ndla.learningpathapi.integration._
import no.ndla.learningpathapi.model.api.ErrorHelpers
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.service._
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
import no.ndla.network.clients.{FeideApiClient, RedisClient}
import no.ndla.network.tapir.{
  NdlaMiddleware,
  Routes,
  Service,
  SwaggerControllerConfig,
  TapirErrorHelpers,
  TapirHealthController
}
import no.ndla.search.{BaseIndexService, Elastic4sClient}

class ComponentRegistry(properties: LearningpathApiProperties)
    extends BaseComponentRegistry[LearningpathApiProperties]
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
    with ImageApiClientComponent
    with ConverterService
    with FeideApiClient
    with OembedProxyClient
    with Elastic4sClient
    with DataSource
    with Clock
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
    with ErrorHelpers
    with RedisClient
    with Routes[Eff]
    with NdlaMiddleware
    with TapirErrorHelpers
    with SwaggerControllerConfig
    with SwaggerDocControllerConfig
    with TapirHealthController {
  override val props: LearningpathApiProperties = properties
  override val migrator                         = new DBMigrator
  override val dataSource: HikariDataSource     = DataSource.getHikariDataSource
  DataSource.connectToDatabase()

  lazy val learningPathRepository = new LearningPathRepository
  lazy val readService            = new ReadService
  lazy val updateService          = new UpdateService
  lazy val searchConverterService = new SearchConverterService
  lazy val searchService          = new SearchService
  lazy val searchIndexService     = new SearchIndexService
  lazy val converterService       = new ConverterService
  lazy val clock                  = new SystemClock
  lazy val taxonomyApiClient      = new TaxonomyApiClient
  lazy val ndlaClient             = new NdlaClient
  lazy val imageApiClient         = new ImageApiClient
  lazy val feideApiClient         = new FeideApiClient
  lazy val languageValidator      = new LanguageValidator
  lazy val titleValidator         = new TitleValidator
  lazy val learningPathValidator  = new LearningPathValidator
  lazy val learningStepValidator  = new LearningStepValidator
  var e4sClient: NdlaE4sClient    = Elastic4sClientFactory.getClient(props.SearchServer)
  lazy val searchApiClient        = new SearchApiClient
  lazy val oembedProxyClient      = new OembedProxyClient
  lazy val redisClient            = new RedisClient(props.RedisHost, props.RedisPort)
  lazy val myndlaApiClient        = new MyNDLAApiClient

  lazy val learningpathControllerV2 = new LearningpathControllerV2
  lazy val internController         = new InternController
  lazy val statsController          = new StatsController
  lazy val healthController         = new TapirHealthController[Eff]

  private val swagger = new SwaggerController[Eff](
    List[Service[Eff]](
      learningpathControllerV2,
      internController,
      statsController,
      healthController
    ),
    SwaggerDocControllerConfig.swaggerInfo
  )

  override val services: List[Service[Eff]] = swagger.getServices()

}
