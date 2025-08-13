/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi

import com.zaxxer.hikari.HikariDataSource
import no.ndla.common.Clock
import no.ndla.database.{DBMigrator, DBUtility, DataSource}
import no.ndla.myndlaapi.controller.{
  ConfigController,
  ErrorHandling,
  FolderController,
  RobotController,
  StatsController,
  SwaggerDocControllerConfig,
  UserController
}
import no.ndla.myndlaapi.integration.{LearningPathApiClient, SearchApiClient}
import no.ndla.myndlaapi.integration.nodebb.NodeBBClient
import no.ndla.myndlaapi.repository.{ConfigRepository, FolderRepository, RobotRepository, UserRepository}
import no.ndla.myndlaapi.service.{
  ConfigService,
  FolderConverterService,
  FolderReadService,
  FolderWriteService,
  RobotService,
  UserService
}
import no.ndla.network.NdlaClient
import no.ndla.network.clients.{FeideApiClient, RedisClient}
import no.ndla.network.tapir.TapirApplication
import org.mockito.Mockito.reset
import org.scalatestplus.mockito.MockitoSugar.mock

trait TestEnvironment
    extends TapirApplication
    with Props
    with Clock
    with SwaggerDocControllerConfig
    with DataSource
    with DBMigrator
    with MyNDLAAuthHelpers
    with FolderRepository
    with RobotRepository
    with FolderReadService
    with FolderWriteService
    with FolderConverterService
    with UserService
    with ConfigService
    with UserRepository
    with DBUtility
    with ConfigRepository
    with FeideApiClient
    with ConfigController
    with RedisClient
    with FolderController
    with RobotController
    with RobotService
    with UserController
    with StatsController
    with ErrorHandling
    with NodeBBClient
    with SearchApiClient
    with LearningPathApiClient
    with NdlaClient {
  override lazy val props                                          = new MyNdlaApiProperties
  override lazy val clock: SystemClock                             = mock[SystemClock]
  override lazy val dataSource: HikariDataSource                   = mock[HikariDataSource]
  override lazy val migrator: DBMigrator                           = mock[DBMigrator]
  override lazy val folderRepository: FolderRepository             = mock[FolderRepository]
  override lazy val robotRepository: RobotRepository               = mock[RobotRepository]
  override lazy val folderReadService: FolderReadService           = mock[FolderReadService]
  override lazy val folderWriteService: FolderWriteService         = mock[FolderWriteService]
  override lazy val folderConverterService: FolderConverterService = mock[FolderConverterService]
  override lazy val robotService: RobotService                     = mock[RobotService]
  override lazy val userService: UserService                       = mock[UserService]
  override lazy val configService: ConfigService                   = mock[ConfigService]
  override lazy val userRepository: UserRepository                 = mock[UserRepository]
  override lazy val configRepository: ConfigRepository             = mock[ConfigRepository]
  override lazy val feideApiClient: FeideApiClient                 = mock[FeideApiClient]
  override lazy val configController: ConfigController             = mock[ConfigController]
  override lazy val robotController: RobotController               = mock[RobotController]
  override lazy val redisClient: RedisClient                       = mock[RedisClient]
  override lazy val folderController: FolderController             = mock[FolderController]
  override lazy val userController: UserController                 = mock[UserController]
  val statsController: StatsController                             = mock[StatsController]
  override lazy val healthController: TapirHealthController        = mock[TapirHealthController]
  override lazy val nodebb: NodeBBClient                           = mock[NodeBBClient]
  override lazy val searchApiClient: SearchApiClient               = mock[SearchApiClient]
  override lazy val learningPathApiClient: LearningPathApiClient   = mock[LearningPathApiClient]
  override lazy val ndlaClient: NdlaClient                         = mock[NdlaClient]
  override lazy val myndlaApiClient: MyNDLAApiClient               = mock[MyNDLAApiClient]
  override lazy val DBUtil: DBUtility                              = mock[DBUtility]

  def services: List[TapirController] = List.empty
  val swagger: SwaggerController      = mock[SwaggerController]

  def resetMocks(): Unit = {
    reset(clock)
    reset(migrator)
    reset(dataSource)
    reset(folderRepository)
    reset(folderReadService)
    reset(folderWriteService)
    reset(folderConverterService)
    reset(userService)
    reset(configService)
    reset(userRepository)
    reset(robotRepository)
    reset(configRepository)
    reset(feideApiClient)
    reset(configController)
    reset(redisClient)
    reset(folderController)
    reset(userController)
    reset(robotController)
    reset(ndlaClient)
    reset(searchApiClient)
    reset(robotService)
  }
}
