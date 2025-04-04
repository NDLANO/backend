/*
 * Part of NDLA scalatestsuite
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.scalatestsuite

import scala.sys.env

trait ContainerSuite {
  val skipContainerSpawn: Boolean = env.getOrElse("NDLA_SKIP_CONTAINER_SPAWN", "false") == "true"
}
