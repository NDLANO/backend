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
  ConfigController,
  FolderController,
  HealthController,
  InternController,
  LearningpathControllerV2,
  NdlaController,
  StatsController,
  UserController
}
import no.ndla.learningpathapi.integration._
import no.ndla.learningpathapi.model.api.ErrorHelpers
import no.ndla.learningpathapi.model.domain.{DBLearningPath, DBLearningStep}
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
import no.ndla.myndla.repository.{ConfigRepository, FolderRepository, UserRepository}
import no.ndla.myndla.service.{
  ConfigService,
  FolderConverterService,
  FolderReadService,
  FolderWriteService,
  UserService
}
import no.ndla.network.NdlaClient
import no.ndla.network.clients.{FeideApiClient, RedisClient}
import no.ndla.network.scalatra.{NdlaControllerBase, NdlaSwaggerSupport}
import no.ndla.search.{BaseIndexService, Elastic4sClient}

class ComponentRegistry(properties: LearningpathApiProperties)
    extends BaseComponentRegistry[LearningpathApiProperties]
    with LearningpathControllerV2
    with InternController
    with HealthController
    with ConfigController
    with FolderController
    with StatsController
    with NdlaSwaggerSupport
    with NdlaControllerBase
    with UserController
    with LearningPathRepositoryComponent
    with ConfigRepository
    with FolderRepository
    with UserRepository
    with ReadService
    with ConfigService
    with UserService
    with FolderConverterService
    with FolderReadService
    with FolderWriteService
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
    with LearningpathApiInfo
    with DBLearningPath
    with DBLearningStep
    with NdlaController
    with RedisClient {
  override val props: LearningpathApiProperties = properties
  override val migrator                         = new DBMigrator
  override val dataSource: HikariDataSource     = DataSource.getHikariDataSource
  DataSource.connectToDatabase()

  implicit val swagger: LearningpathSwagger = new LearningpathSwagger

  lazy val learningPathRepository                         = new LearningPathRepository
  lazy val configRepository                               = new ConfigRepository
  lazy val folderRepository                               = new FolderRepository
  lazy val userRepository                                 = new UserRepository
  lazy val readService                                    = new ReadService
  lazy val updateService                                  = new UpdateService
  lazy val searchConverterService                         = new SearchConverterService
  lazy val searchService                                  = new SearchService
  lazy val searchIndexService                             = new SearchIndexService
  lazy val converterService                               = new ConverterService
  lazy val clock                                          = new SystemClock
  lazy val learningpathControllerV2                       = new LearningpathControllerV2
  lazy val internController                               = new InternController
  lazy val configController                               = new ConfigController
  lazy val folderController                               = new FolderController
  lazy val statsController                                = new StatsController
  lazy val userController                                 = new UserController
  lazy val resourcesApp                                   = new ResourcesApp
  lazy val taxonomyApiClient                              = new TaxonomyApiClient
  lazy val ndlaClient                                     = new NdlaClient
  lazy val imageApiClient                                 = new ImageApiClient
  lazy val feideApiClient                                 = new FeideApiClient
  lazy val healthController                               = new HealthController
  lazy val languageValidator                              = new LanguageValidator
  lazy val titleValidator                                 = new TitleValidator
  lazy val learningPathValidator                          = new LearningPathValidator
  lazy val learningStepValidator                          = new LearningStepValidator
  var e4sClient: NdlaE4sClient                            = Elastic4sClientFactory.getClient(props.SearchServer)
  lazy val searchApiClient                                = new SearchApiClient
  lazy val oembedProxyClient                              = new OembedProxyClient
  lazy val redisClient                                    = new RedisClient(props.RedisHost, props.RedisPort)
  lazy val configService: ConfigService                   = new ConfigService
  lazy val userService: UserService                       = new UserService
  lazy val folderConverterService: FolderConverterService = new FolderConverterService
  lazy val folderReadService: FolderReadService           = new FolderReadService
  lazy val folderWriteService: FolderWriteService         = new FolderWriteService
}
