/*
 * Part of NDLA scalatestsuite
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.scalatestsuite

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

class ValkeyContainer(dockerImageName: DockerImageName) extends GenericContainer[ValkeyContainer](dockerImageName) {
  addExposedPort(ValkeyContainer.ValkeyPort)
  setWaitStrategy(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1));
}

object ValkeyContainer {
  val ValkeyPort: Int = 6379
}
