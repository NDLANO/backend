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
  ControllerErrorHandling,
  FolderController,
  RobotController,
  StatsController,
  SwaggerDocControllerConfig,
  UserController
}
import no.ndla.myndlaapi.db.migrationwithdependencies.V16__MigrateResourcePaths
import no.ndla.myndlaapi.integration.{LearningPathApiClient, SearchApiClient, TaxonomyApiClient}
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

class ComponentRegistry(properties: MyNdlaApiProperties) extends TapirApplication[MyNdlaApiProperties] {
  given props: MyNdlaApiProperties                              = properties
  given errorHandling: ControllerErrorHandling                  = new ControllerErrorHandling
  given errorHelpers: ErrorHelpers                              = new ErrorHelpers
  given healthController: TapirHealthController                 = new TapirHealthController
  implicit lazy val clock: Clock                                = new Clock
  given folderController: FolderController                      = new FolderController
  given robotController: RobotController                        = new RobotController
  implicit lazy val feideApiClient: FeideApiClient              = new FeideApiClient
  given redisClient: RedisClient                                = new RedisClient(props.RedisHost, props.RedisPort)
  implicit lazy val folderRepository: FolderRepository          = new FolderRepository
  given folderConverterService: FolderConverterService          = new FolderConverterService
  given folderReadService: FolderReadService                    = new FolderReadService
  given folderWriteService: FolderWriteService                  = new FolderWriteService
  implicit lazy val userRepository: UserRepository              = new UserRepository
  given robotRepository: RobotRepository                        = new RobotRepository
  given robotService: RobotService                              = new RobotService
  given userService: UserService                                = new UserService
  given userController: UserController                          = new UserController
  given configRepository: ConfigRepository                      = new ConfigRepository
  given configService: ConfigService                            = new ConfigService
  given configController: ConfigController                      = new ConfigController
  lazy val statsController: StatsController                     = new StatsController
  given nodebb: NodeBBClient                                    = new NodeBBClient
  given searchApiClient: SearchApiClient                        = new SearchApiClient
  given taxonomyApiClient: TaxonomyApiClient                    = new TaxonomyApiClient
  given learningPathApiClient: LearningPathApiClient            = new LearningPathApiClient
  given ndlaClient: NdlaClient                                  = new NdlaClient
  given myndlaApiClient: MyNDLAApiClient                        = new MyNDLAApiClient
  lazy val v16__MigrateResourcePaths: V16__MigrateResourcePaths = new V16__MigrateResourcePaths
  given dbUtil: DBUtility                                       = new DBUtility

  given migrator: DBMigrator   = new DBMigrator(v16__MigrateResourcePaths)
  given dataSource: DataSource = DataSource.getDataSource

  given swagger: SwaggerController = new SwaggerController(
    List(
      healthController,
      folderController,
      robotController,
      userController,
      configController,
      statsController
    ),
    SwaggerDocControllerConfig.swaggerInfo
  )
  given services: List[TapirController] = swagger.getServices()
  given routes: Routes                  = new Routes
}
