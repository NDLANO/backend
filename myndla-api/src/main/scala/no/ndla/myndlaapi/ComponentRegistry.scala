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
  StatsController,
  SwaggerDocControllerConfig,
  UserController
}
import no.ndla.myndlaapi.db.migrationwithdependencies.V16__MigrateResourcePaths
import no.ndla.myndlaapi.integration.{LearningPathApiClient, SearchApiClient, TaxonomyApiClient}
import no.ndla.myndlaapi.integration.nodebb.NodeBBClient
import no.ndla.myndlaapi.repository.{ConfigRepository, FolderRepository, UserRepository}
import no.ndla.myndlaapi.service.{
  ConfigService,
  FolderConverterService,
  FolderReadService,
  FolderWriteService,
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
    with MyNDLAAuthHelpers
    with NodeBBClient
    with SearchApiClient
    with TaxonomyApiClient
    with LearningPathApiClient
    with V16__MigrateResourcePaths
    with NdlaClient {
  override val props: MyNdlaApiProperties = properties

  lazy val healthController: TapirHealthController              = new TapirHealthController
  lazy val clock: SystemClock                                   = new SystemClock
  lazy val folderController: FolderController                   = new FolderController
  lazy val feideApiClient: FeideApiClient                       = new FeideApiClient
  lazy val redisClient                                          = new RedisClient(props.RedisHost, props.RedisPort)
  lazy val folderRepository: FolderRepository                   = new FolderRepository
  lazy val folderConverterService: FolderConverterService       = new FolderConverterService
  lazy val folderReadService: FolderReadService                 = new FolderReadService
  lazy val folderWriteService: FolderWriteService               = new FolderWriteService
  lazy val userRepository: UserRepository                       = new UserRepository
  lazy val userService: UserService                             = new UserService
  lazy val userController: UserController                       = new UserController
  lazy val configRepository: ConfigRepository                   = new ConfigRepository
  lazy val configService: ConfigService                         = new ConfigService
  lazy val configController: ConfigController                   = new ConfigController
  lazy val statsController: StatsController                     = new StatsController
  lazy val nodebb: NodeBBClient                                 = new NodeBBClient
  lazy val searchApiClient: SearchApiClient                     = new SearchApiClient
  lazy val taxonomyApiClient: TaxonomyApiClient                 = new TaxonomyApiClient
  lazy val learningPathApiClient: LearningPathApiClient         = new LearningPathApiClient
  lazy val ndlaClient: NdlaClient                               = new NdlaClient
  lazy val myndlaApiClient: MyNDLAApiClient                     = new MyNDLAApiClient
  lazy val v16__MigrateResourcePaths: V16__MigrateResourcePaths = new V16__MigrateResourcePaths
  lazy val DBUtil                                               = new DBUtility

  override val migrator: DBMigrator         = DBMigrator(v16__MigrateResourcePaths)
  override val dataSource: HikariDataSource = DataSource.getHikariDataSource
  DataSource.connectToDatabase()

  private val swagger = new SwaggerController(
    List(
      healthController,
      folderController,
      userController,
      configController,
      statsController
    ),
    SwaggerDocControllerConfig.swaggerInfo
  )
  override def services: List[TapirController] = swagger.getServices()

}
