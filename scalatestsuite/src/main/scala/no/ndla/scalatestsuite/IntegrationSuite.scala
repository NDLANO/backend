/*
 * Part of NDLA scalatestsuite.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.scalatestsuite

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import no.ndla.network.secrets.PropertyKeys
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.utility.DockerImageName

import scala.util.{Failure, Success, Try}

abstract class IntegrationSuite(
    EnableElasticsearchContainer: Boolean = false,
    EnablePostgresContainer: Boolean = false,
    PostgresqlVersion: String = "12.4",
    ElasticsearchImage: String = "d63726c", // elasticsearch 6.8.4
    schemaName: String = "testschema"
) extends UnitTestSuite {

  val elasticSearchContainer: Try[ElasticsearchContainer] = if (EnableElasticsearchContainer) {
    val imgName = s"950645517739.dkr.ecr.eu-central-1.amazonaws.com/ndla/search-engine:$ElasticsearchImage"
    val searchEngineImage = DockerImageName
      .parse(imgName)
      .asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch")

    Try {
      val container = new ElasticsearchContainer(searchEngineImage)
      container.start()
      container
    }
  } else { Failure(new RuntimeException("Search disabled for this IntegrationSuite")) }

  val elasticSearchHost: Try[String] = elasticSearchContainer.map(c => {
    val addr = s"http://${c.getHttpHostAddress}"
    println(s"Running '${this.getClass.getName}' elasticsearch at $addr")
    addr
  })

  val postgresContainer: Try[PostgreSQLContainer[Nothing]] = if (EnablePostgresContainer) {
    val username = "postgres"
    val password: String = "hemmelig"
    val resource: String = "postgres"

    val c = new PostgreSQLContainer(s"postgres:$PostgresqlVersion")
    c.withDatabaseName(resource)
    c.withUsername(username)
    c.withPassword(password)
    c.start()

    Success(c)
  } else { Failure(new RuntimeException("Postgres disabled for this IntegrationSuite")) }

  val testDataSource: Try[HikariDataSource] = postgresContainer.flatMap(pgc =>
    Try {
      val dataSourceConfig = new HikariConfig()
      dataSourceConfig.setUsername(pgc.getUsername)
      dataSourceConfig.setPassword(pgc.getPassword)
      dataSourceConfig.setJdbcUrl(
        s"jdbc:postgresql://${pgc.getContainerIpAddress}:${pgc.getMappedPort(5432)}/${pgc.getDatabaseName}")
      dataSourceConfig.setSchema(schemaName)
      dataSourceConfig.setMaximumPoolSize(10)
      new HikariDataSource(dataSourceConfig)
  })

  private var previousDatabaseEnv = Map.empty[String, String]

  protected def setDatabaseEnvironment(): Unit = {
    previousDatabaseEnv = getPropEnvs(
      PropertyKeys.MetaUserNameKey,
      PropertyKeys.MetaPasswordKey,
      PropertyKeys.MetaResourceKey,
      PropertyKeys.MetaServerKey,
      PropertyKeys.MetaPortKey,
      PropertyKeys.MetaSchemaKey
    )

    postgresContainer.map(container => {
      setPropEnv(
        PropertyKeys.MetaUserNameKey -> container.getUsername,
        PropertyKeys.MetaPasswordKey -> container.getPassword,
        PropertyKeys.MetaResourceKey -> container.getDatabaseName,
        PropertyKeys.MetaServerKey -> container.getContainerIpAddress,
        PropertyKeys.MetaPortKey -> container.getMappedPort(5432).toString,
        PropertyKeys.MetaSchemaKey -> schemaName
      )
    })
  }

  override def beforeAll(): Unit = setDatabaseEnvironment()
  override def afterAll(): Unit = {
    setPropEnv(previousDatabaseEnv)
    elasticSearchContainer.map(c => c.stop())
    postgresContainer.map(c => c.stop())
  }
}
