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
import no.ndla.common.configuration.BaseComponentRegistry
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
import no.ndla.network.clients.{FeideApiClient, RedisClient}
import no.ndla.network.tapir.TapirApplication

class ComponentRegistry(properties: MyNdlaApiProperties) extends TapirApplication[MyNdlaApiProperties] {
  given props: MyNdlaApiProperties                              = properties
  given healthController: TapirHealthController                 = new TapirHealthController
  given clock: SystemClock                                      = new SystemClock
  given folderController: FolderController                      = new FolderController
  given robotController: RobotController                        = new RobotController
  given feideApiClient: FeideApiClient                          = new FeideApiClient
  given redisClient                                             = new RedisClient(props.RedisHost, props.RedisPort)
  given folderRepository: FolderRepository                      = new FolderRepository
  given folderConverterService: FolderConverterService          = new FolderConverterService
  given folderReadService: FolderReadService                    = new FolderReadService
  given folderWriteService: FolderWriteService                  = new FolderWriteService
  given userRepository: UserRepository                          = new UserRepository
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
  given DBUtil                                                  = new DBUtility

  given migrator: DBMigrator         = DBMigrator(v16__MigrateResourcePaths)
  given dataSource: HikariDataSource = DataSource.getDataSource

  val swagger = new SwaggerController(
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
  override def services: List[TapirController] = swagger.getServices()

}
