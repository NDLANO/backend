/*
 * Part of NDLA scalatestsuite
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.scalatestsuite

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.utility.DockerImageName

import java.time.Duration
import scala.util.{Failure, Try}
import sys.env

trait RedisIntegrationSuite extends UnitTestSuite with ContainerSuite {

  val EnableRedisContainer: Boolean = true

  private var standaloneRedisContainer: Option[GenericContainer[Nothing]] = None

  private def startRedisContainer(): GenericContainer[Nothing] = {
    val c = new GenericContainer(DockerImageName.parse("redis:6.2"))
    c.addExposedPort(6379)
    c.setWaitStrategy(new HostPortWaitStrategy().withStartupTimeout(Duration.ofSeconds(100)))
    c
  }

  val redisPort: Try[Int] =
    if (EnableRedisContainer) {
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
    } else {
      Failure(new RuntimeException("Redis disabled for this IntegrationSuite"))
    }

  override def afterAll(): Unit = {
    super.afterAll()
    if (!skipContainerSpawn && EnableRedisContainer) {
      if (disableSharedContainers) {
        standaloneRedisContainer.foreach(_.stop())
      } else {
        SharedContainer.release("redis")
      }
    }
  }
}
