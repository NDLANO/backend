/*
 * Part of NDLA scalatestsuite
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.scalatestsuite

import org.mockito.Mockito.when
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.utility.DockerImageName

import java.time.Duration
import scala.util.{Failure, Success, Try}
import sys.env

trait ElasticsearchIntegrationSuite extends UnitTestSuite with ContainerSuite {
  val EnableElasticsearchContainer: Boolean = true
  val ElasticsearchImage: String            = "c3f7a34" // elasticsearch 8.18.1

  val elasticSearchContainer: Try[ElasticsearchContainer] = if (EnableElasticsearchContainer) {
    if (skipContainerSpawn) {
      val esMock = mock[ElasticsearchContainer]
      val found  = env.get("SEARCH_SERVER").map(x => x.stripPrefix("http://"))
      when(esMock.getHttpHostAddress).thenReturn(found.getOrElse("localhost:9200")): Unit
      Success(esMock)
    } else {
      val imageFromEnv = env.get("SEARCH_ENGINE_IMAGE")
      val imgName      =
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

  override def afterAll(): Unit = {
    super.afterAll()
    elasticSearchContainer.foreach(c => c.stop())
  }
}
