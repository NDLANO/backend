/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi

import com.zaxxer.hikari.HikariDataSource
import no.ndla.common.Clock
import no.ndla.myndlaapi.controller.{
  ArenaController,
  ConfigController,
  ErrorHelpers,
  FolderController,
  StatsController,
  SwaggerDocControllerConfig,
  UserController
}
import no.ndla.myndlaapi.integration.DataSource
import no.ndla.myndlaapi.integration.nodebb.NodeBBClient
import no.ndla.myndlaapi.repository.{ArenaRepository, ConfigRepository, FolderRepository, UserRepository}
import no.ndla.myndlaapi.service.{ArenaReadService, ConfigService, ConverterService, FolderConverterService, FolderReadService, FolderWriteService, ImportService, UserService}
import no.ndla.network.clients.{FeideApiClient, RedisClient}
import no.ndla.network.tapir.{
  NdlaMiddleware,
  Routes,
  Service,
  SwaggerControllerConfig,
  TapirErrorHelpers,
  TapirHealthController
}
import org.mockito.Mockito.reset
import org.scalatestplus.mockito.MockitoSugar.mock

trait TestEnvironment
    extends Props
    with Clock
    with TapirHealthController
    with SwaggerControllerConfig
    with SwaggerDocControllerConfig
    with DataSource
    with DBMigrator
    with ArenaReadService
    with ArenaRepository
    with ArenaController
    with MyNDLAAuthHelpers
    with ConverterService
    with FolderRepository
    with FolderReadService
    with FolderWriteService
    with FolderConverterService
    with UserService
    with ConfigService
    with UserRepository
    with ConfigRepository
    with FeideApiClient
    with ConfigController
    with RedisClient
    with FolderController
    with UserController
    with StatsController
    with ErrorHelpers
    with Routes[Eff]
    with NdlaMiddleware
    with TapirErrorHelpers
    with ImportService
    with NodeBBClient {
  val props                                          = new MyNdlaApiProperties
  lazy val clock: SystemClock                        = mock[SystemClock]
  val dataSource: HikariDataSource                   = mock[HikariDataSource]
  val migrator: DBMigrator                           = mock[DBMigrator]
  val arenaReadService: ArenaReadService             = mock[ArenaReadService]
  val folderRepository: FolderRepository             = mock[FolderRepository]
  val folderReadService: FolderReadService           = mock[FolderReadService]
  val folderWriteService: FolderWriteService         = mock[FolderWriteService]
  val folderConverterService: FolderConverterService = mock[FolderConverterService]
  val userService: UserService                       = mock[UserService]
  val configService: ConfigService                   = mock[ConfigService]
  val userRepository: UserRepository                 = mock[UserRepository]
  val configRepository: ConfigRepository             = mock[ConfigRepository]
  val feideApiClient: FeideApiClient                 = mock[FeideApiClient]
  val configController: ConfigController             = mock[ConfigController]
  val redisClient: RedisClient                       = mock[RedisClient]
  val folderController: FolderController             = mock[FolderController]
  val userController: UserController                 = mock[UserController]
  val statsController: StatsController               = mock[StatsController]
  val arenaController: ArenaController               = mock[ArenaController]
  val arenaRepository: ArenaRepository               = mock[ArenaRepository]
  val converterService: ConverterService             = mock[ConverterService]
  val importService: ImportService                   = mock[ImportService]
  val nodebb: NodeBBClient                           = mock[NodeBBClient]

  def services: List[Service[Eff]] = List.empty

  def resetMocks(): Unit = {
    reset(clock)
    reset(migrator)
    reset(dataSource)
    reset(arenaReadService)
    reset(folderRepository)
    reset(folderReadService)
    reset(folderWriteService)
    reset(folderConverterService)
    reset(userService)
    reset(configService)
    reset(userRepository)
    reset(configRepository)
    reset(feideApiClient)
    reset(configController)
    reset(redisClient)
    reset(folderController)
    reset(userController)
    reset(arenaController)
    reset(arenaRepository)
    reset(converterService)
  }
}
