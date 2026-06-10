/*
 * Part of NDLA scalatestsuite
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.scalatestsuite

import org.testcontainers.utility.DockerImageName

import scala.util.Try
import sys.env

trait RedisIntegrationSuite extends UnitTestSuite {
  protected object valkeyContainer extends ContainerIntegrationSuiteBase[ValkeyContainer, Int] {
    override protected val containerName: String              = "valkey"
    override protected def createContainer(): ValkeyContainer =
      new ValkeyContainer(DockerImageName.parse("valkey/valkey:9.0"))
    override protected def fromContainer(c: ValkeyContainer): Int = c.getMappedPort(6379).intValue()
    override protected def fromEnv(): Int                         = env.getOrElse("REDIS_PORT", "6379").toInt
    override protected def healthCheck(port: Int): Boolean        = SharedContainer.isReachable("localhost", port)
  }

  val redisPort: Try[Int] = valkeyContainer.output

  override def afterAll(): Unit = {
    super.afterAll()
    valkeyContainer.close()
  }
}
