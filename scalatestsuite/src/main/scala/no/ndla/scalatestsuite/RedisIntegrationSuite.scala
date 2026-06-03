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

trait RedisIntegrationSuite extends UnitTestSuite {
  protected object redisContainer extends ContainerIntegrationSuiteBase[RedisContainer, Int] {
    override protected val containerName: String                 = "redis"
    override protected def createContainer(): RedisContainer     = new RedisContainer(DockerImageName.parse("redis:6.2"))
    override protected def fromContainer(c: RedisContainer): Int = c.getMappedPort(6379).intValue()
    override protected def fromEnv(): Int                        = env.getOrElse("REDIS_PORT", "6379").toInt
    override protected def healthCheck(port: Int): Boolean       = SharedContainer.isReachable("localhost", port)
  }

  val redisPort: Try[Int] = redisContainer.output

  override def afterAll(): Unit = {
    super.afterAll()
    redisContainer.close()
  }
}
