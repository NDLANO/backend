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

trait ElasticsearchIntegrationSuite extends UnitTestSuite {
  val ElasticsearchImage: String = "docker.elastic.co/elasticsearch/elasticsearch:8.18.1"

  protected object elasticsearchContainer extends ContainerIntegrationSuiteBase[ElasticsearchContainer, String] {
    override protected val containerName: String = "elasticsearch"

    private def esImageName: DockerImageName =
      DockerImageName.parse(env.getOrElse("SEARCH_ENGINE_IMAGE", ElasticsearchImage))

    override protected def createContainer(): ElasticsearchContainer = {
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

    override protected def fromContainer(c: ElasticsearchContainer): String = {
      val addr = s"http://${c.getHttpHostAddress}"
      println(s"Running '${ElasticsearchIntegrationSuite.this.getClass.getName}' elasticsearch at $addr")
      addr
    }

    override protected def fromEnv(): String = {
      val addr       = env.getOrElse("SEARCH_SERVER", "http://localhost:9200")
      val normalized =
        if (addr.startsWith("http://")) addr
        else s"http://$addr"
      println(
        s"Running '${ElasticsearchIntegrationSuite.this.getClass.getName}' elasticsearch at $normalized (external)"
      )
      normalized
    }

    override protected def healthCheck(url: String): Boolean = Try {
      val request  = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(2)).GET().build()
      val response = HttpClient
        .newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .build()
        .send(request, HttpResponse.BodyHandlers.discarding())
      response.statusCode() >= 200 && response.statusCode() < 500
    }.getOrElse(false)
  }

  lazy val elasticSearchHost: String = elasticsearchContainer.output.get

  override def afterAll(): Unit = {
    super.afterAll()
    elasticsearchContainer.close()
  }
}
