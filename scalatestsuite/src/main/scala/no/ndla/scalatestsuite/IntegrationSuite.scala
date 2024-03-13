/*
 * Part of NDLA scalatestsuite.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.scalatestsuite

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.testcontainers.containers.{GenericContainer, PostgreSQLContainer}
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.utility.DockerImageName

import java.time.Duration
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}
import sys.env

abstract class IntegrationSuite(
    EnableElasticsearchContainer: Boolean = false,
    EnablePostgresContainer: Boolean = false,
    EnableRedisContainer: Boolean = false,
    PostgresqlVersion: String = "13.12",
    ElasticsearchImage: String = "6409dd6", // elasticsearch 8.11.4
    schemaName: String = "testschema"
) extends UnitTestSuite {

  val skipContainerSpawn: Boolean = env.getOrElse("NDLA_SKIP_CONTAINER_SPAWN", "false") == "true"

  val elasticSearchContainer: Try[ElasticsearchContainer] = if (EnableElasticsearchContainer) {
    if (skipContainerSpawn) {
      val esMock = mock[ElasticsearchContainer]
      val found  = env.get("SEARCH_SERVER").map(x => x.stripPrefix("http://"))
      when(esMock.getHttpHostAddress).thenReturn(found.getOrElse("localhost:9200")): Unit
      Success(esMock)
    } else {
      val imageFromEnv = env.get("SEARCH_ENGINE_IMAGE")
      val imgName =
        imageFromEnv.getOrElse(
          s"950645517739.dkr.ecr.eu-central-1.amazonaws.com/ndla/search-engine:$ElasticsearchImage"
        )

      val searchEngineImage = DockerImageName
        .parse(imgName)
        .asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch")

      Try {
        val container = new ElasticsearchContainer(searchEngineImage) {
          this.setWaitStrategy(new HostPortWaitStrategy().withStartupTimeout(Duration.ofSeconds(100)))
        }
        container.addEnv("xpack.security.enabled", "false")
        container.addEnv("ES_JAVA_OPTS", "-Xms1g -Xmx1g")
        container.addEnv("discovery.type", "single-node")
        container.start()
        container
      }
    }

  } else { Failure(new RuntimeException("Search disabled for this IntegrationSuite")) }

  val elasticSearchHost: Try[String] = elasticSearchContainer.map(c => {
    val addr = s"http://${c.getHttpHostAddress}"
    println(s"Running '${this.getClass.getName}' elasticsearch at $addr")
    addr
  })

  val postgresContainer: Try[PostgreSQLContainer[_]] = if (EnablePostgresContainer) {
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

  case class RedisContainer(genericContainer: GenericContainer[Nothing], port: Int)
  val redisContainer: Try[RedisContainer] = if (EnableRedisContainer) {
    if (skipContainerSpawn) {
      val redisMock = mock[GenericContainer[Nothing]]
      val redisPort = env.getOrElse("REDIS_PORT", "6379").toInt
      Success(RedisContainer(redisMock, redisPort))
    } else {
      val redisPort      = findFreePort
      val redisContainer = new GenericContainer(DockerImageName.parse("redis:6.2"))
      redisContainer.setPortBindings(List(s"$redisPort:6379").asJava)
      redisContainer.setWaitStrategy(new HostPortWaitStrategy().withStartupTimeout(Duration.ofSeconds(100)))
      redisContainer.start()
      Success(RedisContainer(redisContainer, redisPort))
    }
  } else {
    Failure(new RuntimeException("Redis disabled for this IntegrationSuite"))
  }

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

    postgresContainer.foreach(container => {
      setPropEnv(
        PropertyKeys.MetaUserNameKey -> container.getUsername,
        PropertyKeys.MetaPasswordKey -> container.getPassword,
        PropertyKeys.MetaResourceKey -> container.getDatabaseName,
        PropertyKeys.MetaServerKey   -> container.getHost,
        PropertyKeys.MetaPortKey     -> container.getMappedPort(5432).toString,
        PropertyKeys.MetaSchemaKey   -> schemaName
      )
    })
  }

  override def beforeAll(): Unit = setDatabaseEnvironment()
  override def afterAll(): Unit = {
    setPropEnv(previousDatabaseEnv)
    elasticSearchContainer.foreach(c => c.stop())
    postgresContainer.foreach(c => c.stop())
    redisContainer.foreach(c => c.genericContainer.stop())
  }
}
