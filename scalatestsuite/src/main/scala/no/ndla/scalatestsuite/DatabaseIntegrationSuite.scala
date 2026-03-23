/*
 * Part of NDLA scalatestsuite
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.scalatestsuite

import com.zaxxer.hikari.HikariConfig
import no.ndla.common.configuration.BaseProps
import no.ndla.database.{DataSource, DatabaseProps}

import java.sql.DriverManager
import scala.util.{Failure, Try}
import sys.env

trait DatabaseIntegrationSuite extends UnitTestSuite with ContainerSuite {
  lazy val props: BaseProps & DatabaseProps

  val EnablePostgresContainer: Boolean = true
  val PostgresqlVersion: String        = "17.5"
  lazy val schemaName: String          = s"testschema_${ProcessHandle.current().pid()}"

  private val defaultUsername: String     = "postgres"
  private val defaultDatabaseName: String = "postgres"
  private val defaultPassword: String     = "hemmelig"

  private var standalonePgContainer: Option[PgContainer] = None

  case class PgConnectionInfo(host: String, port: Int, username: String, password: String, databaseName: String)

  private def startPgContainer(): PgContainer = {
    val c = PgContainer(PostgresqlVersion, defaultUsername, defaultPassword, defaultDatabaseName)
    c
  }

  val pgConnectionInfo: Try[PgConnectionInfo] =
    if (EnablePostgresContainer) {
      if (skipContainerSpawn) {
        Try {
          PgConnectionInfo(
            host = env.getOrElse("META_SERVER", "localhost"),
            port = env.getOrElse("META_PORT", "5432").toInt,
            username = env.getOrElse("META_USERNAME", defaultUsername),
            password = env.getOrElse("META_PASSWORD", defaultPassword),
            databaseName = env.getOrElse("META_RESOURCE", defaultDatabaseName),
          )
        }
      } else if (disableSharedContainers) {
        Try {
          val c = startPgContainer()
          c.start()
          standalonePgContainer = Some(c)
          PgConnectionInfo(
            host = c.getHost,
            port = c.getMappedPort(5432),
            username = c.getUsername,
            password = c.getPassword,
            databaseName = c.getDatabaseName,
          )
        }
      } else {
        Try {
          val info = SharedContainer.acquire(
            name = "postgres",
            healthCheckPort = 5432,
            healthCheck = info => {
              Try {
                val url  = s"jdbc:postgresql://${info.data("host")}:${info.data("port")}/${info.data("databaseName")}"
                val conn = DriverManager.getConnection(url, info.data("username"), info.data("password"))
                conn.close()
              }.isSuccess
            },
            startContainer = () => {
              val c = startPgContainer()
              c.withReuse(true): Unit
              c.start()
              SharedContainerInfo(
                containerId = c.getContainerId,
                data = Map(
                  "host"         -> c.getHost,
                  "port"         -> c.getMappedPort(5432).toString,
                  "username"     -> c.getUsername,
                  "password"     -> c.getPassword,
                  "databaseName" -> c.getDatabaseName,
                ),
              )
            },
          )
          PgConnectionInfo(
            host = info.data("host"),
            port = info.data("port").toInt,
            username = info.data("username"),
            password = info.data("password"),
            databaseName = info.data("databaseName"),
          )
        }
      }
    } else {
      Failure(new RuntimeException("Postgres disabled for this IntegrationSuite"))
    }

  def testDataSource: Try[DataSource] = pgConnectionInfo.flatMap(pgc =>
    Try {
      val dataSourceConfig = new HikariConfig()
      dataSourceConfig.setUsername(pgc.username)
      dataSourceConfig.setPassword(pgc.password)
      dataSourceConfig.setDriverClassName("org.postgresql.Driver")
      dataSourceConfig.setJdbcUrl(s"jdbc:postgresql://${pgc.host}:${pgc.port}/${pgc.databaseName}")
      dataSourceConfig.setSchema(schemaName)
      dataSourceConfig.setMaximumPoolSize(2)
      new DataSource(dataSourceConfig)(using props)
    }
  )

  private val prevUserName = props.MetaUserName.reference
  private val prevPassword = props.MetaPassword.reference
  private val prevResource = props.MetaResource.reference
  private val prevServer   = props.MetaServer.reference
  private val prevPort     = props.MetaPort.reference
  private val prevSchema   = props.MetaSchema.reference

  protected def restoreDatabaseEnv(): Unit = {
    props.MetaUserName.setReference(prevUserName)
    props.MetaPassword.setReference(prevPassword)
    props.MetaResource.setReference(prevResource)
    props.MetaServer.setReference(prevServer)
    props.MetaPort.setReference(prevPort)
    props.MetaSchema.setReference(prevSchema)
  }

  protected def setDatabaseEnvironment(): Unit = {
    pgConnectionInfo.foreach(pgc => {
      props.MetaUserName.setValue(pgc.username)
      props.MetaPassword.setValue(pgc.password)
      props.MetaResource.setValue(pgc.databaseName)
      props.MetaServer.setValue(pgc.host)
      props.MetaPort.setValue(pgc.port)
      props.MetaSchema.setValue(schemaName)
    })
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    setDatabaseEnvironment()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    restoreDatabaseEnv()
    if (!skipContainerSpawn && EnablePostgresContainer) {
      if (disableSharedContainers) {
        standalonePgContainer.foreach(_.stop())
      } else {
        SharedContainer.release("postgres")
      }
    }
  }
}
