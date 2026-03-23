/*
 * Part of NDLA scalatestsuite
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.scalatestsuite

import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.utility.DockerImageName

import java.time.Duration
import scala.util.{Failure, Try}
import sys.env

trait ElasticsearchIntegrationSuite extends UnitTestSuite with ContainerSuite {
  val EnableElasticsearchContainer: Boolean = true
  val ElasticsearchImage: String            = "c3f7a34" // elasticsearch 8.18.1

  private var standaloneEsContainer: Option[ElasticsearchContainer] = None

  private def esImageName: DockerImageName = {
    val imgName = env.getOrElse(
      "SEARCH_ENGINE_IMAGE",
      s"950645517739.dkr.ecr.eu-central-1.amazonaws.com/ndla/search-engine:$ElasticsearchImage",
    )
    DockerImageName.parse(imgName).asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch")
  }

  private def startEsContainer(): ElasticsearchContainer = {
    val container = new ElasticsearchContainer(esImageName) {
      this.setWaitStrategy(new HostPortWaitStrategy().withStartupTimeout(Duration.ofSeconds(100)))
    }
    container.addEnv("xpack.security.enabled", "false")
    container.addEnv("ES_JAVA_OPTS", "-Xms1g -Xmx1g")
    container.addEnv("discovery.type", "single-node")
    container
  }

  val elasticSearchHost: Try[String] =
    if (EnableElasticsearchContainer) {
      if (skipContainerSpawn) {
        val addr       = env.get("SEARCH_SERVER").getOrElse("http://localhost:9200")
        val normalized =
          if (addr.startsWith("http://")) addr
          else s"http://$addr"
        Try {
          println(s"Running '${this.getClass.getName}' elasticsearch at $normalized (external)")
          normalized
        }
      } else if (disableSharedContainers) {
        Try {
          val container = startEsContainer()
          container.start()
          standaloneEsContainer = Some(container)
          val addr = s"http://${container.getHttpHostAddress}"
          println(s"Running '${this.getClass.getName}' elasticsearch at $addr (standalone)")
          addr
        }
      } else {
        Try {
          val container = startEsContainer()
          val info      = SharedContainer.acquire(
            name = "elasticsearch",
            healthCheckPort = 9200,
            startContainer = () => {
              container.withReuse(true): Unit
              container.start()
              val hostAddress = container.getHttpHostAddress
              val host        = hostAddress.split(":")(0)
              val port        = hostAddress.split(":")(1)
              SharedContainerInfo(containerId = container.getContainerId, data = Map("host" -> host, "port" -> port))
            },
          )
          val addr = s"http://${info.data("host")}:${info.data("port")}"
          println(s"Running '${this.getClass.getName}' elasticsearch at $addr")
          addr
        }
      }
    } else {
      Failure(new RuntimeException("Search disabled for this IntegrationSuite"))
    }

  override def afterAll(): Unit = {
    super.afterAll()
    if (!skipContainerSpawn && EnableElasticsearchContainer) {
      if (disableSharedContainers) {
        standaloneEsContainer.foreach(_.stop())
      } else {
        SharedContainer.release("elasticsearch")
      }
    }
  }
}
