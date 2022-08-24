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
import no.ndla.learningpathapi.controller.{
  ConfigController,
  CorrelationIdSupport,
  FolderController,
  HealthController,
  InternController,
  LearningpathControllerV2,
  NdlaController,
  UserController
}
import no.ndla.learningpathapi.integration._
import no.ndla.learningpathapi.model.api.ErrorHelpers
import no.ndla.learningpathapi.model.domain.{
  DBFolder,
  DBFolderResource,
  DBLearningPath,
  DBLearningStep,
  DBFeideUser,
  DBResource
}
import no.ndla.learningpathapi.model.domain.config.DBConfigMeta
import no.ndla.learningpathapi.repository.{
  ConfigRepository,
  FolderRepository,
  LearningPathRepositoryComponent,
  UserRepository
}
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
import no.ndla.network.clients.FeideApiClient
import no.ndla.search.{BaseIndexService, Elastic4sClient, Elastic4sClientFactory, NdlaE4sClient}

class ComponentRegistry(properties: LearningpathApiProperties)
    extends LearningpathControllerV2
    with InternController
    with HealthController
    with ConfigController
    with FolderController
    with UserController
    with LearningPathRepositoryComponent
    with ConfigRepository
    with FolderRepository
    with UserRepository
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
    with LanguageValidator
    with LearningPathValidator
    with LearningStepValidator
    with TitleValidator
    with SearchApiClient
    with Props
    with DBMigrator
    with DBFolder
    with DBResource
    with DBFolderResource
    with DBFeideUser
    with TextValidator
    with UrlValidator
    with CorrelationIdSupport
    with ErrorHelpers
    with LearningpathApiInfo
    with DBLearningPath
    with DBLearningStep
    with DBConfigMeta
    with NdlaController {
  override val props: LearningpathApiProperties = properties
  override val migrator                         = new DBMigrator
  override val dataSource: HikariDataSource     = DataSource.getHikariDataSource
  DataSource.connectToDatabase()

  implicit val swagger: LearningpathSwagger = new LearningpathSwagger

  lazy val learningPathRepository   = new LearningPathRepository
  lazy val configRepository         = new ConfigRepository
  lazy val folderRepository         = new FolderRepository
  lazy val userRepository           = new UserRepository
  lazy val readService              = new ReadService
  lazy val updateService            = new UpdateService
  lazy val searchConverterService   = new SearchConverterService
  lazy val searchService            = new SearchService
  lazy val searchIndexService       = new SearchIndexService
  lazy val converterService         = new ConverterService
  lazy val clock                    = new SystemClock
  lazy val learningpathControllerV2 = new LearningpathControllerV2
  lazy val internController         = new InternController
  lazy val configController         = new ConfigController
  lazy val folderController         = new FolderController
  lazy val userController           = new UserController
  lazy val resourcesApp             = new ResourcesApp
  lazy val taxononyApiClient        = new TaxonomyApiClient
  lazy val ndlaClient               = new NdlaClient
  lazy val imageApiClient           = new ImageApiClient
  lazy val feideApiClient           = new FeideApiClient
  lazy val healthController         = new HealthController
  lazy val languageValidator        = new LanguageValidator
  lazy val titleValidator           = new TitleValidator
  lazy val learningPathValidator    = new LearningPathValidator
  lazy val learningStepValidator    = new LearningStepValidator
  var e4sClient: NdlaE4sClient      = Elastic4sClientFactory.getClient(props.SearchServer)
  lazy val searchApiClient          = new SearchApiClient
  lazy val oembedProxyClient        = new OembedProxyClient
}
