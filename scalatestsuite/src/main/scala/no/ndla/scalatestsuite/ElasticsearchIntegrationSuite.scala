/*
 * Part of NDLA scalatestsuite
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.scalatestsuite

import org.mockito.Mockito.when
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.utility.{DockerImageName, MountableFile}

import java.time.Duration
import scala.util.{Failure, Success, Try}
import sys.env

trait ElasticsearchIntegrationSuite extends UnitTestSuite with ContainerSuite {
  val EnableElasticsearchContainer: Boolean = true
  val ElasticsearchImage: String            = "docker.elastic.co/elasticsearch/elasticsearch:8.18.1"

  val elasticSearchContainer: Try[ElasticsearchContainer] =
    if (EnableElasticsearchContainer) {
      if (skipContainerSpawn) {
        val esMock = mock[ElasticsearchContainer]
        val found  = env.get("SEARCH_SERVER").map(x => x.stripPrefix("http://"))
        when(esMock.getHttpHostAddress).thenReturn(found.getOrElse("localhost:9200")): Unit
        Success(esMock)
      } else {
        val imgName           = env.getOrElse("SEARCH_ENGINE_IMAGE", ElasticsearchImage)
        val searchEngineImage = DockerImageName.parse(imgName)

        Try {
          val container = new ElasticsearchContainer(searchEngineImage)
          container.withStartupTimeout(Duration.ofSeconds(180))
          container.addEnv("xpack.security.enabled", "false")
          container.addEnv("ES_JAVA_OPTS", "-Xms1g -Xmx1g")
          container.addEnv("discovery.type", "single-node")
          container.withCopyFileToContainer(
            MountableFile.forClasspathResource("search-engine/compound-words-norwegian-wordlist.txt"),
            "/usr/share/elasticsearch/config/compound-words-norwegian-wordlist.txt",
          )
          container.withCopyFileToContainer(
            MountableFile.forClasspathResource("search-engine/hyph"),
            "/usr/share/elasticsearch/config/hyph",
          )
          container.start()
          container
        }
      }

    } else {
      Failure(new RuntimeException("Search disabled for this IntegrationSuite"))
    }

  val elasticSearchHost: String = elasticSearchContainer match {
    case Success(c) =>
      val addr = s"http://${c.getHttpHostAddress}"
      println(s"Running '${this.getClass.getName}' elasticsearch at $addr")
      addr
    case Failure(ex) => throw ex
  }

  override def afterAll(): Unit = {
    super.afterAll()
    elasticSearchContainer.foreach(c => c.stop())
  }
}
