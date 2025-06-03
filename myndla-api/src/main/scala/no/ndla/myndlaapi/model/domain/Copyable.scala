/*
 * Part of NDLA myndla-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.myndlaapi.model.domain

import no.ndla.common.model.domain.ResourceType

import java.util.UUID

/** Traits that contains fields that are required for copying a folder and its resources. Used so we can generalize the
  * copy method for both api and domain input.
  */
trait CopyableFolder {
  val id: UUID
  val name: String
  val description: Option[String]
  val subfolders: List[CopyableFolder]
  val resources: List[CopyableResource]
  val rank: Int
}

trait CopyableResource {
  val resourceType: ResourceType
  val path: String
  val tags: List[String]
  val resourceId: String
  val rank: Option[Int]
}
