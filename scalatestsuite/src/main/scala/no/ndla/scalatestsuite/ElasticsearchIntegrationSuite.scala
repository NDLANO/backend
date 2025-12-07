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

import scala.util.{Failure, Success, Try}
import sys.env

trait ElasticsearchIntegrationSuite extends UnitTestSuite with ContainerSuite {
  val EnableElasticsearchContainer: Boolean = true
  val ElasticsearchImage: String            = "c3f7a34" // elasticsearch 8.18.1

  private val managedElasticsearch: Try[SharedContainerManager.Managed[ElasticsearchContainer]] =
    if (EnableElasticsearchContainer) {
      if (skipContainerSpawn) {
        val esMock = mock[ElasticsearchContainer]
        val found  = env.get("SEARCH_SERVER").map(x => x.stripPrefix("http://"))
        when(esMock.getHttpHostAddress).thenReturn(found.getOrElse("localhost:9200")): Unit
        Success(SharedContainerManager.Managed(esMock, () => ()))
      } else {
        val imageFromEnv = env.get("SEARCH_ENGINE_IMAGE")
        val imgName      = imageFromEnv.getOrElse(
          s"950645517739.dkr.ecr.eu-central-1.amazonaws.com/ndla/search-engine:$ElasticsearchImage"
        )
        SharedContainerManager.acquireElasticsearch(imgName, imageFromEnv)
      }

    } else {
      Failure(new RuntimeException("Search disabled for this IntegrationSuite"))
    }

  val elasticSearchContainer: Try[ElasticsearchContainer] = managedElasticsearch.map(_.container)

  val elasticSearchHost: Try[String] = elasticSearchContainer.map(c => {
    val addr = s"http://${c.getHttpHostAddress}"
    println(s"Running '${this.getClass.getName}' elasticsearch at $addr")
    addr
  })

  override def afterAll(): Unit = {
    super.afterAll()
    managedElasticsearch.foreach(_.release())
  }
}
