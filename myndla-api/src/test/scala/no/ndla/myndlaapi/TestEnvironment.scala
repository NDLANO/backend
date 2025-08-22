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
import org.scalatestplus.mockito.MockitoSugar

trait TestEnvironment
    extends TapirApplication
    with MockitoSugar {
  given props                                          = new MyNdlaApiProperties
  given clock: SystemClock                             = mock[SystemClock]
  given dataSource: HikariDataSource                   = mock[HikariDataSource]
  given migrator: DBMigrator                           = mock[DBMigrator]
  given folderRepository: FolderRepository             = mock[FolderRepository]
  given robotRepository: RobotRepository               = mock[RobotRepository]
  given folderReadService: FolderReadService           = mock[FolderReadService]
  given folderWriteService: FolderWriteService         = mock[FolderWriteService]
  given folderConverterService: FolderConverterService = mock[FolderConverterService]
  given robotService: RobotService                     = mock[RobotService]
  given userService: UserService                       = mock[UserService]
  given configService: ConfigService                   = mock[ConfigService]
  given userRepository: UserRepository                 = mock[UserRepository]
  given configRepository: ConfigRepository             = mock[ConfigRepository]
  given feideApiClient: FeideApiClient                 = mock[FeideApiClient]
  given configController: ConfigController             = mock[ConfigController]
  given robotController: RobotController               = mock[RobotController]
  given redisClient: RedisClient                       = mock[RedisClient]
  given folderController: FolderController             = mock[FolderController]
  given userController: UserController                 = mock[UserController]
  given statsController: StatsController                           = mock[StatsController]
  given healthController: TapirHealthController        = mock[TapirHealthController]
  given nodebb: NodeBBClient                           = mock[NodeBBClient]
  given searchApiClient: SearchApiClient               = mock[SearchApiClient]
  given learningPathApiClient: LearningPathApiClient   = mock[LearningPathApiClient]
  given ndlaClient: NdlaClient                         = mock[NdlaClient]
  given myndlaApiClient: MyNDLAApiClient               = mock[MyNDLAApiClient]
  given DBUtil: DBUtility                              = mock[DBUtility]

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
