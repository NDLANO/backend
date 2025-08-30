/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi

import no.ndla.common.Clock
import no.ndla.database.{DBMigrator, DBUtility, DataSource}
import no.ndla.myndlaapi.controller.{
  ConfigController,
  ControllerErrorHandling,
  FolderController,
  RobotController,
  StatsController,
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
import no.ndla.network.clients.{FeideApiClient, MyNDLAApiClient, RedisClient}
import no.ndla.network.tapir.{
  ErrorHelpers,
  Routes,
  SwaggerController,
  TapirApplication,
  TapirController,
  TapirHealthController
}
import org.mockito.Mockito.reset
import org.scalatestplus.mockito.MockitoSugar

trait TestEnvironment extends TapirApplication[MyNdlaApiProperties] with MockitoSugar {
  implicit lazy val props: MyNdlaApiProperties                     = new MyNdlaApiProperties
  implicit lazy val routes: Routes                                 = mock[Routes]
  implicit lazy val errorHandling: ControllerErrorHandling         = mock[ControllerErrorHandling]
  implicit lazy val errorHelpers: ErrorHelpers                     = mock[ErrorHelpers]
  implicit lazy val clock: Clock                                   = mock[Clock]
  implicit lazy val dataSource: DataSource                         = mock[DataSource]
  implicit lazy val migrator: DBMigrator                           = mock[DBMigrator]
  implicit lazy val folderRepository: FolderRepository             = mock[FolderRepository]
  implicit lazy val robotRepository: RobotRepository               = mock[RobotRepository]
  implicit lazy val folderReadService: FolderReadService           = mock[FolderReadService]
  implicit lazy val folderWriteService: FolderWriteService         = mock[FolderWriteService]
  implicit lazy val folderConverterService: FolderConverterService = mock[FolderConverterService]
  implicit lazy val robotService: RobotService                     = mock[RobotService]
  implicit lazy val userService: UserService                       = mock[UserService]
  implicit lazy val configService: ConfigService                   = mock[ConfigService]
  implicit lazy val userRepository: UserRepository                 = mock[UserRepository]
  implicit lazy val configRepository: ConfigRepository             = mock[ConfigRepository]
  implicit lazy val feideApiClient: FeideApiClient                 = mock[FeideApiClient]
  implicit lazy val configController: ConfigController             = mock[ConfigController]
  implicit lazy val robotController: RobotController               = mock[RobotController]
  implicit lazy val redisClient: RedisClient                       = mock[RedisClient]
  implicit lazy val folderController: FolderController             = mock[FolderController]
  implicit lazy val userController: UserController                 = mock[UserController]
  implicit lazy val statsController: StatsController               = mock[StatsController]
  implicit lazy val healthController: TapirHealthController        = mock[TapirHealthController]
  implicit lazy val nodebb: NodeBBClient                           = mock[NodeBBClient]
  implicit lazy val searchApiClient: SearchApiClient               = mock[SearchApiClient]
  implicit lazy val learningPathApiClient: LearningPathApiClient   = mock[LearningPathApiClient]
  implicit lazy val ndlaClient: NdlaClient                         = mock[NdlaClient]
  implicit lazy val myndlaApiClient: MyNDLAApiClient               = mock[MyNDLAApiClient]
  implicit lazy val DBUtil: DBUtility                              = mock[DBUtility]
  implicit lazy val services: List[TapirController]                = List.empty
  implicit lazy val swagger: SwaggerController                     = mock[SwaggerController]

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
