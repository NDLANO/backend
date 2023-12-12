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
import no.ndla.myndla.repository.{ConfigRepository, FolderRepository, UserRepository}
import no.ndla.myndla.service.{
  ConfigService,
  FolderConverterService,
  FolderReadService,
  FolderWriteService,
  UserService
}
import no.ndla.myndlaapi.controller.{
  ConfigController,
  ErrorHelpers,
  FolderController,
  StatsController,
  SwaggerDocControllerConfig,
  UserController
}
import no.ndla.myndlaapi.integration.DataSource
import no.ndla.myndlaapi.service.ReadService
import no.ndla.network.clients.{FeideApiClient, RedisClient}
import no.ndla.network.tapir.{
  NdlaMiddleware,
  Routes,
  Service,
  SwaggerControllerConfig,
  TapirErrorHelpers,
  TapirHealthController
}

class ComponentRegistry(properties: MyNdlaApiProperties)
    extends BaseComponentRegistry[MyNdlaApiProperties]
    with Props
    with ErrorHelpers
    with Routes[Eff]
    with TapirErrorHelpers
    with NdlaMiddleware
    with Clock
    with TapirHealthController
    with SwaggerControllerConfig
    with SwaggerDocControllerConfig
    with DataSource
    with DBMigrator
    with ReadService
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
    with StatsController {
  override val props: MyNdlaApiProperties = properties

  lazy val healthController                               = new TapirHealthController[Eff]
  lazy val clock: SystemClock                             = new SystemClock
  lazy val migrator                                       = new DBMigrator
  lazy val readService: ReadService                       = new ReadService
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

  override val dataSource: Option[HikariDataSource] =
    Option.when(props.migrateToLocalDB)(DataSource.getHikariDataSource)
  override val lpDs: HikariDataSource = DataSource.getLpDs

  private val swagger = new SwaggerController[Eff](
    List(
      healthController,
      folderController,
      userController,
      configController,
      statsController
    ),
    SwaggerDocControllerConfig.swaggerInfo
  )
  override val services: List[Service[Eff]] = swagger.getServices()
}
