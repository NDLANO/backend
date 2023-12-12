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
  HealthController,
  InternController,
  LearningpathControllerV2,
  NdlaController,
  StatsController
}
import no.ndla.learningpathapi.integration._
import no.ndla.learningpathapi.model.api.ErrorHelpers
import no.ndla.learningpathapi.model.domain.{DBLearningPath, DBLearningStep}
import no.ndla.learningpathapi.repository.LearningPathRepositoryComponent
import no.ndla.learningpathapi.service._
import no.ndla.learningpathapi.service.search.{SearchConverterServiceComponent, SearchIndexService, SearchService}
import no.ndla.learningpathapi.validation._
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
import org.mockito.scalatest.MockitoSugar

trait TestEnvironment
    extends LearningpathControllerV2
    with StatsController
    with ConfigController
    with UserService
    with FolderConverterService
    with FolderReadService
    with FolderWriteService
    with ConfigService
    with NdlaControllerBase
    with NdlaSwaggerSupport
    with LearningPathRepositoryComponent
    with ConfigRepository
    with FeideApiClient
    with FolderRepository
    with UserRepository
    with ReadService
    with UpdateService
    with SearchConverterServiceComponent
    with SearchService
    with SearchIndexService
    with BaseIndexService
    with SearchApiClient
    with TaxonomyApiClient
    with NdlaClient
    with ImageApiClientComponent
    with ConverterService
    with OembedProxyClient
    with Elastic4sClient
    with DataSource
    with MockitoSugar
    with Clock
    with HealthController
    with LanguageValidator
    with LearningPathValidator
    with LearningStepValidator
    with TitleValidator
    with TextValidator
    with UrlValidator
    with DBLearningPath
    with DBLearningStep
    with NdlaController
    with ErrorHelpers
    with Props
    with InternController
    with DBMigrator
    with LearningpathApiInfo
    with RedisClient {
  val props = new LearningpathApiProperties

  val migrator: DBMigrator         = mock[DBMigrator]
  val dataSource: HikariDataSource = mock[HikariDataSource]

  val learningPathRepository: LearningPathRepository                   = mock[LearningPathRepository]
  val learningPathRepositoryComponent: LearningPathRepositoryComponent = mock[LearningPathRepositoryComponent]
  val configRepository: ConfigRepository                               = mock[ConfigRepository]
  val readService: ReadService                                         = mock[ReadService]
  val updateService: UpdateService                                     = mock[UpdateService]
  val searchConverterService: SearchConverterService                   = mock[SearchConverterService]
  val searchService: SearchService                                     = mock[SearchService]
  val searchIndexService: SearchIndexService                           = mock[SearchIndexService]
  val converterService: ConverterService                               = org.mockito.Mockito.spy(new ConverterService)
  val clock: SystemClock                                               = mock[SystemClock]
  val taxonomyApiClient: TaxonomyApiClient                             = mock[TaxonomyApiClient]
  val ndlaClient: NdlaClient                                           = mock[NdlaClient]
  val imageApiClient: ImageApiClient                                   = mock[ImageApiClient]
  val languageValidator: LanguageValidator                             = mock[LanguageValidator]
  val learningpathControllerV2: LearningpathControllerV2               = mock[LearningpathControllerV2]
  val configController: ConfigController                               = mock[ConfigController]
  val statsController: StatsController                                 = mock[StatsController]
  val healthController: HealthController                               = mock[HealthController]
  val internController: InternController                               = mock[InternController]
  val learningStepValidator: LearningStepValidator                     = mock[LearningStepValidator]
  val learningPathValidator: LearningPathValidator                     = mock[LearningPathValidator]
  val titleValidator: TitleValidator                                   = mock[TitleValidator]
  var e4sClient: NdlaE4sClient                                         = mock[NdlaE4sClient]
  val searchApiClient: SearchApiClient                                 = mock[SearchApiClient]
  val oembedProxyClient: OembedProxyClient                             = mock[OembedProxyClient]
  val feideApiClient: FeideApiClient                                   = mock[FeideApiClient]
  val folderRepository: FolderRepository                               = mock[FolderRepository]
  val userRepository: UserRepository                                   = mock[UserRepository]
  val redisClient: RedisClient                                         = mock[RedisClient]
  val userService: UserService                                         = mock[UserService]
  val folderReadService: FolderReadService                             = mock[FolderReadService]
  val folderWriteService: FolderWriteService                           = mock[FolderWriteService]
  val configService: ConfigService                                     = mock[ConfigService]
  val folderConverterService: FolderConverterService                   = mock[FolderConverterService]

  def resetMocks(): Unit = {
    reset(
      dataSource,
      learningPathRepository,
      configRepository,
      readService,
      updateService,
      searchService,
      searchIndexService,
      converterService,
      searchConverterService,
      languageValidator,
      titleValidator,
      e4sClient,
      oembedProxyClient,
      feideApiClient,
      folderRepository,
      userRepository
    )
  }
}
