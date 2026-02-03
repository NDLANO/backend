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
  InternController,
  RobotController,
  StatsController,
  SwaggerDocControllerConfig,
  UserController,
}
import no.ndla.myndlaapi.db.migrationwithdependencies.V16__MigrateResourcePaths
import no.ndla.myndlaapi.integration.{
  InternalMyNDLAApiClient,
  LearningPathApiClient,
  SearchApiClient,
  TaxonomyApiClient,
}
import no.ndla.myndlaapi.integration.nodebb.NodeBBClient
import no.ndla.myndlaapi.repository.{ConfigRepository, FolderRepository, RobotRepository, UserRepository}
import no.ndla.myndlaapi.service.{
  ConfigService,
  FolderConverterService,
  FolderReadService,
  FolderWriteService,
  RobotService,
  UserService,
}
import no.ndla.network.NdlaClient
import no.ndla.network.clients.{FeideApiClient, RedisClient}
import no.ndla.network.tapir.{
  ErrorHelpers,
  Routes,
  SwaggerController,
  TapirApplication,
  TapirController,
  TapirHealthController,
}

class ComponentRegistry(properties: MyNdlaApiProperties) extends TapirApplication[MyNdlaApiProperties] {
  given props: MyNdlaApiProperties = properties
  implicit lazy val clock: Clock   = new Clock
  given dataSource: DataSource     = DataSource.getDataSource
  given migrator: DBMigrator       = DBMigrator(v16__MigrateResourcePaths)
  given dbUtil: DBUtility          = new DBUtility

  given ndlaClient: NdlaClient                                  = new NdlaClient
  implicit lazy val myndlaApiClient: InternalMyNDLAApiClient    = new InternalMyNDLAApiClient
  implicit lazy val redisClient: RedisClient                    = new RedisClient(props.RedisHost, props.RedisPort)
  implicit lazy val feideApiClient: FeideApiClient              = new FeideApiClient
  implicit lazy val nodebb: NodeBBClient                        = new NodeBBClient
  given errorHelpers: ErrorHelpers                              = new ErrorHelpers
  given errorHandling: ControllerErrorHandling                  = new ControllerErrorHandling
  implicit lazy val folderRepository: FolderRepository          = new FolderRepository
  given folderConverterService: FolderConverterService          = new FolderConverterService
  implicit lazy val userRepository: UserRepository              = new UserRepository
  given configRepository: ConfigRepository                      = new ConfigRepository
  given configService: ConfigService                            = new ConfigService
  implicit lazy val folderReadService: FolderReadService        = new FolderReadService
  implicit lazy val folderWriteService: FolderWriteService      = new FolderWriteService
  implicit lazy val userService: UserService                    = new UserService
  given robotRepository: RobotRepository                        = new RobotRepository
  given robotService: RobotService                              = new RobotService
  given searchApiClient: SearchApiClient                        = new SearchApiClient
  given taxonomyApiClient: TaxonomyApiClient                    = new TaxonomyApiClient
  given learningPathApiClient: LearningPathApiClient            = new LearningPathApiClient
  lazy val v16__MigrateResourcePaths: V16__MigrateResourcePaths = new V16__MigrateResourcePaths

  given userController: UserController          = new UserController
  lazy val statsController: StatsController     = new StatsController
  given configController: ConfigController      = new ConfigController
  given healthController: TapirHealthController = new TapirHealthController
  given folderController: FolderController      = new FolderController
  given robotController: RobotController        = new RobotController
  given internController: InternController      = new InternController

  given swagger: SwaggerController = new SwaggerController(
    List(
      healthController,
      folderController,
      robotController,
      userController,
      configController,
      statsController,
      internController,
    ),
    SwaggerDocControllerConfig.swaggerInfo,
  )
  given services: List[TapirController] = swagger.getServices()
  given routes: Routes                  = new Routes
}
