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
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}
import sys.env

trait RedisIntegrationSuite extends UnitTestSuite with ContainerSuite {

  val EnableRedisContainer: Boolean = true

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

  override def afterAll(): Unit = {
    super.afterAll()
    redisContainer.foreach(c => c.genericContainer.stop())
  }
}
