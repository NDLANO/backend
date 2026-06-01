/*
 * Part of NDLA scalatestsuite
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.scalatestsuite

import com.redis.testcontainers.RedisContainer
import org.testcontainers.utility.DockerImageName

import scala.util.Try
import sys.env

trait RedisIntegrationSuite extends UnitTestSuite with ContainerSuite {
  private var standaloneRedisContainer: Option[RedisContainer] = None

  private def startRedisContainer(): RedisContainer = {
    val c = new RedisContainer(DockerImageName.parse("redis:6.2"))
    c.addExposedPort(6379)
    c
  }

  val redisPort: Try[Int] =
    if (skipContainerSpawn) {
      Try(env.getOrElse("REDIS_PORT", "6379").toInt)
    } else if (disableSharedContainers) {
      Try {
        val c = startRedisContainer()
        c.start()
        standaloneRedisContainer = Some(c)
        c.getMappedPort(6379)
      }
    } else {
      Try {
        val info = SharedContainer.acquire(
          name = "redis",
          healthCheckPort = 6379,
          startContainer = () => {
            val c = startRedisContainer()
            c.withReuse(true): Unit
            c.start()
            SharedContainerInfo(
              containerId = c.getContainerId,
              data = Map("host" -> c.getHost, "port" -> c.getMappedPort(6379).toString),
            )
          },
        )
        info.data("port").toInt
      }
    }

  override def afterAll(): Unit = {
    super.afterAll()
    if (!skipContainerSpawn && disableSharedContainers) {
      standaloneRedisContainer.foreach(_.stop())
    }
  }
}
