/*
 * Part of NDLA scalatestsuite
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.scalatestsuite

import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.utility.{DockerImageName, MountableFile}

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.time.Duration
import scala.util.Try
import sys.env

trait ElasticsearchIntegrationSuite extends UnitTestSuite with ContainerSuite {
  val ElasticsearchImage: String = "docker.elastic.co/elasticsearch/elasticsearch:8.18.1"

  private var standaloneEsContainer: Option[ElasticsearchContainer] = None

  private def esImageName: DockerImageName = {
    val imgName = env.getOrElse("SEARCH_ENGINE_IMAGE", ElasticsearchImage)
    DockerImageName.parse(imgName)
  }

  private def startEsContainer(): ElasticsearchContainer = {
    val container = new ElasticsearchContainer(esImageName)
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
    container
  }

  private def isElasticsearchReady(info: SharedContainerInfo): Boolean = {
    Try {
      val request = HttpRequest
        .newBuilder(URI.create(s"http://${info.data("host")}:${info.data("port")}"))
        .timeout(Duration.ofSeconds(2))
        .GET()
        .build()
      val response = HttpClient
        .newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .build()
        .send(request, HttpResponse.BodyHandlers.discarding())

      response.statusCode() >= 200 && response.statusCode() < 500
    }.getOrElse(false)
  }

  private def getSearchServerEnvOrDefault = {
    val addr = env.getOrElse("SEARCH_SERVER", "http://localhost:9200")
    if (addr.startsWith("http://")) addr
    else s"http://$addr"
  }

  val elasticSearchHost: String =
    if (skipContainerSpawn) {
      val addr = getSearchServerEnvOrDefault
      println(s"Running '${this.getClass.getName}' elasticsearch at $addr (external)")
      addr
    } else if (disableSharedContainers) {
      val container = startEsContainer()
      container.start()
      standaloneEsContainer = Some(container)
      val addr = s"http://${container.getHttpHostAddress}"
      println(s"Running '${this.getClass.getName}' elasticsearch at $addr (standalone)")
      addr
    } else {
      val info = SharedContainer.acquire(
        name = "elasticsearch",
        healthCheckPort = 9200,
        healthCheck = isElasticsearchReady,
        startContainer = () => {
          val container = startEsContainer()
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

  val ElasticSearchEnabled: Boolean = true

  override def afterAll(): Unit = {
    super.afterAll()
    if (!skipContainerSpawn && disableSharedContainers) {
      standaloneEsContainer.foreach(_.stop())
    }
  }
}
