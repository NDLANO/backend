/*
 * Part of NDLA myndla-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.myndlaapi

import com.zaxxer.hikari.HikariDataSource
import no.ndla.common.Clock
import no.ndla.common.configuration.BaseComponentRegistry
import no.ndla.myndlaapi.controller.{
  ArenaController,
  ConfigController,
  ErrorHandling,
  FolderController,
  InternController,
  StatsController,
  SwaggerDocControllerConfig,
  UserController
}
import no.ndla.myndlaapi.integration.{DataSource, SearchApiClient}
import no.ndla.myndlaapi.integration.nodebb.NodeBBClient
import no.ndla.myndlaapi.repository.{ArenaRepository, ConfigRepository, FolderRepository, UserRepository}
import no.ndla.myndlaapi.service.{
  ArenaReadService,
  ConfigService,
  ConverterService,
  FolderConverterService,
  FolderReadService,
  FolderWriteService,
  ImportService,
  UserService
}
import no.ndla.network.NdlaClient
import no.ndla.network.clients.{FeideApiClient, RedisClient}
import no.ndla.network.tapir.TapirApplication

class ComponentRegistry(properties: MyNdlaApiProperties)
    extends BaseComponentRegistry[MyNdlaApiProperties]
    with Props
    with ErrorHandling
    with TapirApplication
    with Clock
    with SwaggerDocControllerConfig
    with DataSource
    with DBMigrator
    with ArenaReadService
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
    with ArenaController
    with MyNDLAAuthHelpers
    with ArenaRepository
    with ImportService
    with NodeBBClient
    with InternController
    with SearchApiClient
    with NdlaClient {
  override val props: MyNdlaApiProperties = properties

  lazy val healthController: TapirHealthController        = new TapirHealthController
  lazy val clock: SystemClock                             = new SystemClock
  lazy val migrator                                       = new DBMigrator
  lazy val folderController: FolderController             = new FolderController
  lazy val feideApiClient: FeideApiClient                 = new FeideApiClient
  lazy val redisClient                                    = new RedisClient(props.RedisHost, props.RedisPort)
  lazy val folderRepository: FolderRepository             = new FolderRepository
  lazy val folderConverterService: FolderConverterService = new FolderConverterService
  lazy val folderReadService: FolderReadService           = new FolderReadService
  lazy val folderWriteService: FolderWriteService         = new FolderWriteService
  lazy val userRepository: UserRepository                 = new UserRepository
  lazy val userService: UserService                       = new UserService
  lazy val userController: UserController                 = new UserController
  lazy val configRepository: ConfigRepository             = new ConfigRepository
  lazy val configService: ConfigService                   = new ConfigService
  lazy val configController: ConfigController             = new ConfigController
  lazy val statsController: StatsController               = new StatsController
  lazy val arenaRepository: ArenaRepository               = new ArenaRepository
  lazy val arenaReadService: ArenaReadService             = new ArenaReadService
  lazy val arenaController: ArenaController               = new ArenaController
  lazy val converterService: ConverterService             = new ConverterService
  lazy val importService: ImportService                   = new ImportService
  lazy val nodebb: NodeBBClient                           = new NodeBBClient
  lazy val internController: InternController             = new InternController
  lazy val searchApiClient: SearchApiClient               = new SearchApiClient
  lazy val ndlaClient: NdlaClient                         = new NdlaClient

  override val dataSource: HikariDataSource = DataSource.getHikariDataSource
  DataSource.connectToDatabase()

  private val swagger = new SwaggerController(
    List(
      healthController,
      folderController,
      userController,
      configController,
      statsController,
      arenaController,
      internController
    ),
    SwaggerDocControllerConfig.swaggerInfo
  )
  override def services: List[TapirController] = swagger.getServices()

}
