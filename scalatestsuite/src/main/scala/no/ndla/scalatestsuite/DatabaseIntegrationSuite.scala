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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.testcontainers.postgresql.PostgreSQLContainer

import scala.util.{Failure, Success, Try}
import sys.env

trait DatabaseIntegrationSuite extends UnitTestSuite with ContainerSuite {
  lazy val props: BaseProps & DatabaseProps

  val EnablePostgresContainer: Boolean = true
  val PostgresqlVersion: String        = "17.5"
  lazy val schemaName: String          = "testschema"

  lazy val postgresContainer: Try[PostgreSQLContainer] =
    if (EnablePostgresContainer) {
      val defaultUsername: String     = "postgres"
      val defaultDatabaseName: String = "postgres"
      val defaultPassword: String     = "hemmelig"

      if (skipContainerSpawn) {
        val x = mock[PostgreSQLContainer]
        when(x.getPassword).thenReturn(env.getOrElse("META_PASSWORD", defaultPassword)): Unit
        when(x.getUsername).thenReturn(env.getOrElse("META_USERNAME", defaultUsername)): Unit
        when(x.getDatabaseName).thenReturn(env.getOrElse("META_RESOURCE", defaultDatabaseName)): Unit
        when(x.getHost).thenReturn(env.getOrElse("META_SERVER", "localhost")): Unit
        when(x.getMappedPort(any[Int])).thenReturn(env.getOrElse("META_PORT", "5432").toInt): Unit
        Success(x)
      } else {
        Try {
          val c: PgContainer = PgContainer(PostgresqlVersion, defaultUsername, defaultPassword, defaultDatabaseName)
          c.start()
          c
        }
      }
    } else {
      Failure(new RuntimeException("Postgres disabled for this IntegrationSuite"))
    }

  def testDataSource: Try[DataSource] = postgresContainer.flatMap(pgc =>
    Try {
      val dataSourceConfig = new HikariConfig()
      dataSourceConfig.setUsername(pgc.getUsername)
      dataSourceConfig.setPassword(pgc.getPassword)
      dataSourceConfig.setDriverClassName("org.postgresql.Driver")
      dataSourceConfig.setJdbcUrl(s"jdbc:postgresql://${pgc.getHost}:${pgc.getMappedPort(5432)}/${pgc.getDatabaseName}")
      dataSourceConfig.setSchema(schemaName)
      dataSourceConfig.setMaximumPoolSize(10)
      new DataSource(dataSourceConfig)
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
    postgresContainer.foreach(container => {
      props.MetaUserName.setValue(container.getUsername)
      props.MetaPassword.setValue(container.getPassword)
      props.MetaResource.setValue(container.getDatabaseName)
      props.MetaServer.setValue(container.getHost)
      props.MetaPort.setValue(container.getMappedPort(5432))
      props.MetaSchema.setValue(schemaName)
    })
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    postgresContainer match {
      case Failure(_) =>
        cancel("Postgres container unavailable; skipping integration suite.")
      case Success(_) =>
    }
    setDatabaseEnvironment()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    restoreDatabaseEnv()
    postgresContainer.foreach(c => c.stop())
  }
}
