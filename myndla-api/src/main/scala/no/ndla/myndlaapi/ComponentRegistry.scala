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

class ComponentRegistry(properties: MyNdlaApiProperties)
    extends BaseComponentRegistry[MyNdlaApiProperties]
    with Props
    with ErrorHandling
    with TapirApplication
    with Clock
    with SwaggerDocControllerConfig
    with DataSource
    with DBMigrator
    with DBUtility
    with FolderRepository
    with FolderReadService
    with FolderWriteService
    with RobotRepository
    with FolderConverterService
    with UserService
    with ConfigService
    with RobotService
    with UserRepository
    with ConfigRepository
    with FeideApiClient
    with ConfigController
    with RedisClient
    with FolderController
    with RobotController
    with UserController
    with StatsController
    with MyNDLAAuthHelpers
    with NodeBBClient
    with SearchApiClient
    with TaxonomyApiClient
    with LearningPathApiClient
    with V16__MigrateResourcePaths
    with NdlaClient {
  override lazy val props: MyNdlaApiProperties                     = properties
  override lazy val healthController: TapirHealthController        = new TapirHealthController
  override lazy val clock: SystemClock                             = new SystemClock
  override lazy val folderController: FolderController             = new FolderController
  override lazy val robotController: RobotController               = new RobotController
  override lazy val feideApiClient: FeideApiClient                 = new FeideApiClient
  override lazy val redisClient                                    = new RedisClient(props.RedisHost, props.RedisPort)
  override lazy val folderRepository: FolderRepository             = new FolderRepository
  override lazy val folderConverterService: FolderConverterService = new FolderConverterService
  override lazy val folderReadService: FolderReadService           = new FolderReadService
  override lazy val folderWriteService: FolderWriteService         = new FolderWriteService
  override lazy val userRepository: UserRepository                 = new UserRepository
  override lazy val robotRepository: RobotRepository               = new RobotRepository
  override lazy val robotService: RobotService                     = new RobotService
  override lazy val userService: UserService                       = new UserService
  override lazy val userController: UserController                 = new UserController
  override lazy val configRepository: ConfigRepository             = new ConfigRepository
  override lazy val configService: ConfigService                   = new ConfigService
  override lazy val configController: ConfigController             = new ConfigController
  lazy val statsController: StatsController                        = new StatsController
  override lazy val nodebb: NodeBBClient                           = new NodeBBClient
  override lazy val searchApiClient: SearchApiClient               = new SearchApiClient
  override lazy val taxonomyApiClient: TaxonomyApiClient           = new TaxonomyApiClient
  override lazy val learningPathApiClient: LearningPathApiClient   = new LearningPathApiClient
  override lazy val ndlaClient: NdlaClient                         = new NdlaClient
  override lazy val myndlaApiClient: MyNDLAApiClient               = new MyNDLAApiClient
  lazy val v16__MigrateResourcePaths: V16__MigrateResourcePaths    = new V16__MigrateResourcePaths
  override lazy val DBUtil                                         = new DBUtility

  override lazy val migrator: DBMigrator         = DBMigrator(v16__MigrateResourcePaths)
  override lazy val dataSource: HikariDataSource = DataSource.getHikariDataSource

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
