/*
 * Part of NDLA scalatestsuite
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.scalatestsuite

import io.circe.{Decoder, Encoder}
import org.testcontainers.containers.GenericContainer

import scala.util.Try

abstract class ContainerIntegrationSuiteBase[CT <: GenericContainer[?], O](using Encoder[O], Decoder[O]) {
  private val skipContainerSpawn: Boolean      = sys.env.getOrElse("NDLA_SKIP_CONTAINER_SPAWN", "false") == "true"
  private val disableSharedContainers: Boolean = sys.env.getOrElse("NDLA_DISABLE_SHARED_CONTAINERS", "false") == "true"
  private var standaloneContainer: Option[CT]  = None

  protected val containerName: String
  protected def createContainer(): CT
  protected def fromContainer(container: CT): O
  protected def fromEnv(): O
  protected def healthCheck(info: O): Boolean

  lazy val output: Try[O] = Try {
    if (skipContainerSpawn) {
      fromEnv()
    } else if (disableSharedContainers) {
      val container = createContainer()
      container.withReuse(false)
      container.start()
      standaloneContainer = Some(container)
      fromContainer(container)
    } else {
      SharedContainer
        .acquire(
          name = containerName,
          startContainer = {
            val container = createContainer()
            container.withReuse(true)
            container.start()
            SharedContainerInfo(container.getContainerId, fromContainer(container))
          },
          healthCheck = healthCheck,
        )
        .data
    }
  }

  def close(): Unit = if (!skipContainerSpawn && disableSharedContainers) standaloneContainer.foreach(_.stop())
}
