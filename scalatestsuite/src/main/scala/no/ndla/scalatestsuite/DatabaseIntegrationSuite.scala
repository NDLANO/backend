/*
 * Part of NDLA scalatestsuite
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.scalatestsuite

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import no.ndla.common.configuration.HasBaseProps
import no.ndla.database.HasDatabaseProps
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.testcontainers.containers.PostgreSQLContainer
import scala.util.{Failure, Success, Try}
import sys.env

trait DatabaseIntegrationSuite extends UnitTestSuite with ContainerSuite {
  this: HasBaseProps & HasDatabaseProps =>

  val EnablePostgresContainer: Boolean = true
  val PostgresqlVersion: String        = "16.3"
  val schemaName: String               = "testschema"

  val postgresContainer: Try[PostgreSQLContainer[?]] = if (EnablePostgresContainer) {
    val defaultUsername: String     = "postgres"
    val defaultDatabaseName: String = "postgres"
    val defaultPassword: String     = "hemmelig"

    if (skipContainerSpawn) {
      val x = mock[PostgreSQLContainer[Nothing]]
      when(x.getPassword).thenReturn(env.getOrElse("META_PASSWORD", defaultPassword)): Unit
      when(x.getUsername).thenReturn(env.getOrElse("META_USERNAME", defaultUsername)): Unit
      when(x.getDatabaseName).thenReturn(env.getOrElse("META_RESOURCE", defaultDatabaseName)): Unit
      when(x.getMappedPort(any[Int])).thenReturn(env.getOrElse("META_PORT", "5432").toInt): Unit
      Success(x)
    } else {
      val c: PgContainer = PgContainer(
        PostgresqlVersion,
        defaultUsername,
        defaultPassword,
        defaultDatabaseName
      )
      c.start()
      Success(c)
    }
  } else { Failure(new RuntimeException("Postgres disabled for this IntegrationSuite")) }

  def testDataSource: Try[HikariDataSource] = postgresContainer.flatMap(pgc =>
    Try {
      val dataSourceConfig = new HikariConfig()
      dataSourceConfig.setUsername(pgc.getUsername)
      dataSourceConfig.setPassword(pgc.getPassword)
      dataSourceConfig.setDriverClassName("org.postgresql.Driver")
      dataSourceConfig.setJdbcUrl(
        s"jdbc:postgresql://${pgc.getHost}:${pgc.getMappedPort(5432)}/${pgc.getDatabaseName}"
      )
      dataSourceConfig.setSchema(schemaName)
      dataSourceConfig.setMaximumPoolSize(10)
      new HikariDataSource(dataSourceConfig)
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
    setDatabaseEnvironment()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    restoreDatabaseEnv()
    postgresContainer.foreach(c => c.stop())
  }
}
