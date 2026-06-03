/*
 * Part of NDLA scalatestsuite
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.scalatestsuite

import com.zaxxer.hikari.HikariConfig
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import no.ndla.common.configuration.BaseProps
import no.ndla.database.{DataSource, DatabaseProps}

import java.sql.DriverManager
import scala.util.Try
import sys.env

trait DatabaseIntegrationSuite extends UnitTestSuite {
  case class PgConnectionInfo(host: String, port: Int, username: String, password: String, databaseName: String)

  lazy val props: BaseProps & DatabaseProps

  val PostgresqlVersion: String = "17.5"
  lazy val schemaName: String   = s"testschema_${ProcessHandle.current().pid()}"

  private val defaultUsername: String     = "postgres"
  private val defaultDatabaseName: String = "postgres"
  private val defaultPassword: String     = "hemmelig"

  private given Codec[PgConnectionInfo] = deriveCodec

  protected object postgresContainer extends ContainerIntegrationSuiteBase[PgContainer, PgConnectionInfo] {
    override protected val containerName: String = "postgres"

    override protected def createContainer(): PgContainer =
      PgContainer(PostgresqlVersion, defaultUsername, defaultPassword, defaultDatabaseName)

    override protected def fromContainer(c: PgContainer): PgConnectionInfo = PgConnectionInfo(
      host = c.getHost,
      port = c.getMappedPort(5432).intValue(),
      username = c.getUsername,
      password = c.getPassword,
      databaseName = c.getDatabaseName,
    )

    override protected def fromEnv(): PgConnectionInfo = PgConnectionInfo(
      host = env.getOrElse("META_SERVER", "localhost"),
      port = env.getOrElse("META_PORT", "5432").toInt,
      username = env.getOrElse("META_USERNAME", defaultUsername),
      password = env.getOrElse("META_PASSWORD", defaultPassword),
      databaseName = env.getOrElse("META_RESOURCE", defaultDatabaseName),
    )

    override protected def healthCheck(info: PgConnectionInfo): Boolean = Try {
      val url  = s"jdbc:postgresql://${info.host}:${info.port}/${info.databaseName}"
      val conn = DriverManager.getConnection(url, info.username, info.password)
      conn.close()
    }.isSuccess
  }

  lazy val pgConnectionInfo: Try[PgConnectionInfo] = postgresContainer.output

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
    postgresContainer.close()
  }
}
